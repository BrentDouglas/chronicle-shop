/*
 * Machine Code Limited ("COMPANY") Confidential and Proprietary
 * Unpublished Copyright (C) 2020 Machine Code Limited, All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains the property of
 * COMPANY. The intellectual and technical concepts contained herein are
 * proprietary to COMPANY and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material is
 * strictly forbidden unless prior written permission is obtained from COMPANY.
 * Access to the source code contained herein is hereby forbidden to anyone
 * except current COMPANY employees, managers or contractors who have executed
 * Confidentiality and Non-disclosure agreements explicitly covering such
 * access.
 *
 * The copyright notice above does not evidence any actual or intended
 * publication or disclosure of this source code, which includes information
 * that is confidential and/or proprietary, and is a trade secret, of COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC PERFORMANCE, OR PUBLIC
 * DISPLAY OF OR THROUGH USE OF THIS SOURCE CODE WITHOUT THE EXPRESS WRITTEN
 * CONSENT OF COMPANY IS STRICTLY PROHIBITED, AND IN VIOLATION OF APPLICABLE
 * LAWS AND INTERNATIONAL TREATIES. THE RECEIPT OR POSSESSION OF THIS SOURCE
 * CODE AND/OR RELATED INFORMATION DOES NOT CONVEY OR IMPLY ANY RIGHTS TO
 * REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS, OR TO MANUFACTURE, USE, OR
 * SELL ANYTHING THAT IT MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package io.machinecode.shop.cart.domain;

import io.machinecode.shop.cart.api.CartDispatcher;
import io.machinecode.shop.cart.api.CartListener;
import io.machinecode.shop.cart.command.AddProductToCartCommand;
import io.machinecode.shop.cart.command.CalculateTotalForCartCommand;
import io.machinecode.shop.cart.command.RemoveProductFromCartCommand;
import io.machinecode.shop.cart.command.UpdateProductQuantityInCartCommand;
import io.machinecode.shop.cart.event.CartErrorEvent;
import io.machinecode.shop.cart.event.CartProductAddedEvent;
import io.machinecode.shop.cart.event.CartProductRemovedEvent;
import io.machinecode.shop.cart.event.CartProductUpdatedEvent;
import io.machinecode.shop.cart.event.CartTotalEvent;
import io.machinecode.shop.product.api.DisplayProductListener;
import io.machinecode.shop.product.event.DisplayProductCreatedEvent;
import io.machinecode.shop.product.event.DisplayProductDeletedEvent;
import io.machinecode.shop.product.event.DisplayProductUpdatedEvent;
import io.machinecode.shop.product.model.DisplayProductId;
import java.util.Objects;
import net.openhft.chronicle.core.values.LongValue;
import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ExternalMapQueryContext;
import net.openhft.chronicle.map.MapEntry;
import net.openhft.chronicle.values.Values;

/** @author <a href="mailto:brent.n.douglas@gmail.com">Brent Douglas</a> */
public class CartService implements DisplayProductListener, CartDispatcher, AutoCloseable {
  private final CartProductId productId = Values.newHeapInstance(CartProductId.class);
  private final CartItemId cartItemId = Values.newHeapInstance(CartItemId.class);
  private final LongValue cartId = Values.newHeapInstance(LongValue.class);

  private final CartProduct product = Values.newHeapInstance(CartProduct.class);
  private final Cart cart = Values.newHeapInstance(Cart.class);
  private final CartItem cartItem = Values.newHeapInstance(CartItem.class);

  private final CartErrorEvent cartErrorEvent = new CartErrorEvent();
  private final CartProductUpdatedEvent cartProductUpdatedEvent = new CartProductUpdatedEvent();
  private final CartProductAddedEvent cartProductAddedEvent = new CartProductAddedEvent();
  private final CartProductRemovedEvent cartProductRemovedEvent = new CartProductRemovedEvent();
  private final CartTotalEvent cartTotalEvent = new CartTotalEvent();
  private final DisplayProductId displayProductId = new DisplayProductId();

  private final ChronicleMap<CartProductId, CartProduct> products;
  private final ChronicleMap<LongValue, Cart> carts;
  private final ChronicleMap<CartItemId, CartItem> cartItems;

  private final CartListener cartListener;

  public CartService(
      final CartListener cartListener,
      final ChronicleMap<CartProductId, CartProduct> products,
      final ChronicleMap<LongValue, Cart> carts,
      final ChronicleMap<CartItemId, CartItem> cartItems) {
    this.cartListener = cartListener;
    this.products = products;
    this.carts = carts;
    this.cartItems = cartItems;
  }

  @Override
  public void close() {
    this.products.close();
    this.carts.close();
    this.cartItems.close();
  }

  @Override
  public void sendAddProductToCartCommand(final AddProductToCartCommand command) {
    if (command.getQuantity() <= 0) {
      sendCartError(command.getId(), "Quantity must be positive");
      return;
    }
    final LongValue cartId = getCartId(command.getId());
    final CartProductId productId = this.getCartProductId(command.getDisplayProductId());
    if (!products.containsKey(productId)) {
      sendCartError(command.getId(), "Product does not exist");
      return;
    }
    final CartItemId cartItemId = getCartItemId(command.getId(), productId);
    final CartItem cartItem = this.cartItem;
    cartItem.setQuantity(command.getQuantity());
    cartItem.setProductId(productId);
    try (ExternalMapQueryContext<LongValue, Cart, ?> cartCtx = this.carts.queryContext(cartId)) {
      cartCtx.updateLock().lock();
      MapEntry<LongValue, Cart> cartEntry = cartCtx.entry();
      if (cartEntry != null) {
        final Cart cart = this.cart;
        cartEntry.value().getUsing(cart);
        if (!tryAddCartItem(cartId, cartItemId, cartItem)) {
          return;
        }
        final int index = cart.getCount();
        cart.setCount(index + 1);
        cart.setProductIdAt(index, productId);
        cartEntry.doReplaceValue(cartCtx.wrapValueAsData(cart));
      } else {
        if (!tryAddCartItem(cartId, cartItemId, cartItem)) {
          return;
        }
        final Cart cart = this.cart;
        cart.setId(command.getId());
        cart.setCount(1);
        cart.setProductIdAt(0, productId);
        cartCtx.absentEntry().doInsert(cartCtx.wrapValueAsData(cart));
      }
    }
    CartProductAddedEvent cartProductAddedEvent = this.cartProductAddedEvent;
    cartProductAddedEvent.setId(command.getId());
    cartProductAddedEvent.setDisplayProductId(command.getDisplayProductId());
    cartProductAddedEvent.setQuantity(command.getQuantity());
    this.cartListener.onCartProductAddedEvent(cartProductAddedEvent);
  }

  @Override
  public void sendUpdateProductQuantityInCartCommand(
      final UpdateProductQuantityInCartCommand command) {
    if (command.getQuantity() < 1) {
      sendCartError(command.getId(), "Quantity must be greater than 0");
      return;
    }
    final LongValue cartId = getCartId(command.getId());
    final CartProductId productId = this.getCartProductId(command.getDisplayProductId());
    final CartItemId cartItemId = getCartItemId(command.getId(), productId);
    try (ExternalMapQueryContext<LongValue, Cart, ?> cartCtx = this.carts.queryContext(cartId)) {
      cartCtx.updateLock().lock();
      MapEntry<LongValue, Cart> cartEntry = cartCtx.entry();
      if (cartEntry == null) {
        sendCartError(command.getId(), "Cart does not exist");
        return;
      }
      if (!products.containsKey(productId)) {
        removeProductFromCart(productId, cartCtx, cartEntry, command.getId());
        return;
      }
      try (ExternalMapQueryContext<CartItemId, CartItem, ?> cartItemCtx =
          this.cartItems.queryContext(cartItemId)) {
        cartItemCtx.updateLock().lock();
        MapEntry<CartItemId, CartItem> cartItemEntry = cartItemCtx.entry();
        if (cartItemEntry == null) {
          sendCartError(cartId.getValue(), "Product is not in the cart");
          return;
        }
        final CartItem cartItem = this.cartItem;
        cartItemEntry.value().getUsing(cartItem);
        cartItem.setQuantity(command.getQuantity());
        cartItemEntry.doReplaceValue(cartItemCtx.wrapValueAsData(cartItem));
      }
    }
    CartProductUpdatedEvent cartProductUpdatedEvent = this.cartProductUpdatedEvent;
    cartProductUpdatedEvent.setId(command.getId());
    cartProductUpdatedEvent.setDisplayProductId(command.getDisplayProductId());
    cartProductUpdatedEvent.setQuantity(command.getQuantity());
    this.cartListener.onCartProductUpdatedEvent(cartProductUpdatedEvent);
  }

  @Override
  public void sendRemoveProductFromCartCommand(final RemoveProductFromCartCommand command) {
    final LongValue cartId = getCartId(command.getId());
    final CartProductId productId = this.getCartProductId(command.getDisplayProductId());
    final long id = cartId.getValue();
    final CartItemId cartItemId = getCartItemId(id, productId);
    try (ExternalMapQueryContext<LongValue, Cart, ?> cartCtx = this.carts.queryContext(cartId)) {
      cartCtx.updateLock().lock();
      MapEntry<LongValue, Cart> cartEntry = cartCtx.entry();
      if (cartEntry == null) {
        sendCartError(id, "Cart does not exist");
        return;
      }
      if (!products.containsKey(productId)) {
        removeProductFromCart(productId, cartCtx, cartEntry, command.getId());
        return;
      }
      try (ExternalMapQueryContext<CartItemId, CartItem, ?> cartItemCtx =
          this.cartItems.queryContext(cartItemId)) {
        cartItemCtx.updateLock().lock();
        MapEntry<CartItemId, CartItem> cartItemEntry = cartItemCtx.entry();
        if (cartItemEntry == null) {
          sendCartError(cartId.getValue(), "Product is not in the cart");
          return;
        }
        cartItemEntry.doRemove();
        final Cart cart = this.cart;
        if (removeProductFromCart(cart, productId)) {
          cartEntry.doReplaceValue(cartCtx.wrapValueAsData(cart));
        }
      }
    }
  }

  private void removeProductFromCart(
      final CartProductId productId,
      final ExternalMapQueryContext<LongValue, Cart, ?> cartCtx,
      final MapEntry<LongValue, Cart> cartEntry,
      final long id) {
    final Cart cart = this.cart;
    cartEntry.value().getUsing(cart);
    if (removeProductFromCart(cart, productId)) {
      cartEntry.doReplaceValue(cartCtx.wrapValueAsData(cart));
    }
    sendCartError(id, "Product does not exist");
  }

  @Override
  public void sendCalculateTotalForCartCommand(final CalculateTotalForCartCommand command) {
    final long id = command.getId();
    final LongValue cartId = getCartId(id);
    double totalPrice = 0D;
    try (ExternalMapQueryContext<LongValue, Cart, ?> cartCtx = this.carts.queryContext(cartId)) {
      cartCtx.updateLock().lock();
      MapEntry<LongValue, Cart> cartEntry = cartCtx.entry();
      if (cartEntry == null) {
        sendCartError(id, "Cart does not exist");
        return;
      }
      final Cart cart = this.cart;
      cartEntry.value().getUsing(cart);
      final ChronicleMap<CartProductId, CartProduct> products = this.products;
      final ChronicleMap<CartItemId, CartItem> cartItems = this.cartItems;
      final CartProduct product = this.product;
      final CartItem cartItem = this.cartItem;
      for (int i = 0; i < cart.getCount(); i++) {
        final CartProductId productId = cart.getProductIdAt(i);

        double productPrice;
        try (ExternalMapQueryContext<CartProductId, CartProduct, ?> productCtx =
            products.queryContext(productId)) {
          productCtx.readLock().lock();
          MapEntry<CartProductId, CartProduct> productEntry = productCtx.entry();
          if (productEntry == null) {
            final CartItemId cartItemId = getCartItemId(id, productId);
            try (ExternalMapQueryContext<CartItemId, CartItem, ?> cartItemCtx =
                cartItems.queryContext(cartItemId)) {
              cartItemCtx.updateLock().lock();
              MapEntry<CartItemId, CartItem> cartItemEntry = cartItemCtx.entry();
              if (cartItemEntry != null) {
                cartItemEntry.doRemove();
              }
            }
            if (removeProductFromCart(cart, productId)) {
              i = -1;
              continue;
            }
          }
          productEntry.value().getUsing(product);
          productPrice = product.getPrice();
        }

        final CartItemId cartItemId = getCartItemId(id, productId);
        try (ExternalMapQueryContext<CartItemId, CartItem, ?> cartItemCtx =
            cartItems.queryContext(cartItemId)) {
          cartItemCtx.readLock().lock();
          MapEntry<CartItemId, CartItem> cartItemEntry = cartItemCtx.entry();
          if (cartItemEntry == null) {
            sendCartError(cartId.getValue(), "Product is not in the cart");
            return;
          }
          cartItemEntry.value().getUsing(cartItem);
          totalPrice += productPrice * cartItem.getQuantity();
        }
      }
      cartEntry.doReplaceValue(cartCtx.wrapValueAsData(cart));
    }
    CartTotalEvent cartTotalEvent = this.cartTotalEvent;
    cartTotalEvent.setId(id);
    cartTotalEvent.setTotal(totalPrice);
    cartListener.onCartTotalEvent(cartTotalEvent);
  }

  @Override
  public void onDisplayProductCreated(final DisplayProductCreatedEvent event) {
    CartProductId productId = getCartProductId(event.getId());
    try (ExternalMapQueryContext<CartProductId, CartProduct, ?> productCtx =
        products.queryContext(productId)) {
      productCtx.updateLock().lock();
      MapEntry<CartProductId, CartProduct> productEntry = productCtx.entry();
      if (productEntry != null) {
        return;
      }
      CartProduct product = this.product;
      product.setId(getCartProductId(event.getId()));
      product.setPrice(event.getPrice());
      product.setDescription(event.getDescription());
      productCtx.absentEntry().doInsert(productCtx.wrapValueAsData(product));
    }
  }

  @Override
  public void onDisplayProductUpdated(final DisplayProductUpdatedEvent event) {
    CartProductId productId = getCartProductId(event.getId());
    try (ExternalMapQueryContext<CartProductId, CartProduct, ?> productCtx =
        products.queryContext(productId)) {
      productCtx.updateLock().lock();
      MapEntry<CartProductId, CartProduct> productEntry = productCtx.entry();
      if (productEntry == null) {
        return;
      }
      CartProduct product = this.product;
      productEntry.value().getUsing(product);
      product.setPrice(event.getPrice());
      product.setDescription(event.getDescription());
      productEntry.doReplaceValue(productCtx.wrapValueAsData(product));
    }
  }

  @Override
  public void onDisplayProductDeleted(final DisplayProductDeletedEvent event) {
    DisplayProductId id = event.getId();
    CartProductId productId = getCartProductId(id);
    try (ExternalMapQueryContext<CartProductId, CartProduct, ?> productCtx =
        products.queryContext(productId)) {
      productCtx.updateLock().lock();
      MapEntry<CartProductId, CartProduct> productEntry = productCtx.entry();
      if (productEntry == null) {
        return;
      }
      productEntry.doRemove();
    }
  }

  private boolean removeProductFromCart(final Cart cart, final CartProductId productId) {
    final DisplayProductId displayProductId = this.displayProductId;
    final int count = cart.getCount();
    boolean removed = false;
    for (int j = 0; j < count; j++) {
      CartProductId cur = cart.getProductIdAt(j);
      if (cur == null) {
        continue;
      }
      if (Objects.equals(cur, productId)) {
        displayProductId.setId(productId.getId());
        displayProductId.setType(productId.getType());
        sendCartProductRemovedEvent(cart.getId(), displayProductId);
        for (int k = j + 1; k < count; k++, j++) {
          CartProductId val = cart.getProductIdAt(k);
          cart.setProductIdAt(j, val);
        }
        removed = true;
        cart.setCount(count - 1);
        break;
      }
    }
    return removed;
  }

  private boolean tryAddCartItem(
      final LongValue cartId, final CartItemId cartItemId, final CartItem cartItem) {
    try (ExternalMapQueryContext<CartItemId, CartItem, ?> cartItemCtx =
        this.cartItems.queryContext(cartItemId)) {
      cartItemCtx.updateLock().lock();
      MapEntry<CartItemId, CartItem> cartItemEntry = cartItemCtx.entry();
      if (cartItemEntry == null) {
        cartItemCtx.absentEntry().doInsert(cartItemCtx.wrapValueAsData(cartItem));
      } else {
        sendCartError(cartId.getValue(), "Product is already in the cart");
        return false;
      }
    }
    return true;
  }

  private void sendCartProductRemovedEvent(final long id, final DisplayProductId displayProductId) {
    CartProductRemovedEvent cartProductRemovedEvent = this.cartProductRemovedEvent;
    cartProductRemovedEvent.setId(id);
    cartProductRemovedEvent.setDisplayProductId(displayProductId);
    cartListener.onCartProductRemovedEvent(cartProductRemovedEvent);
  }

  private void sendCartError(final long id, final String message) {
    CartErrorEvent cartErrorEvent = this.cartErrorEvent;
    cartErrorEvent.setId(id);
    cartErrorEvent.setMessage(message);
    cartListener.onCartErrorEvent(cartErrorEvent);
  }

  private CartItemId getCartItemId(final long id, final CartProductId productId) {
    final CartItemId cartItemId = this.cartItemId;
    cartItemId.setId(id);
    cartItemId.setProductId(productId);
    return cartItemId;
  }

  private CartProductId getCartProductId(final DisplayProductId id) {
    final CartProductId productId = this.productId;
    productId.setId(id.getId());
    productId.setType(id.getType());
    return productId;
  }

  private LongValue getCartId(final long id) {
    final LongValue cartId = this.cartId;
    cartId.setValue(id);
    return cartId;
  }
}
