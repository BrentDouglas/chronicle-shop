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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.machinecode.shop.cart.command.AddProductToCartCommand;
import io.machinecode.shop.cart.command.CalculateTotalForCartCommand;
import io.machinecode.shop.cart.command.RemoveProductFromCartCommand;
import io.machinecode.shop.cart.command.UpdateProductQuantityInCartCommand;
import io.machinecode.shop.cart.event.CartErrorEvent;
import io.machinecode.shop.cart.event.CartProductAddedEvent;
import io.machinecode.shop.cart.event.CartProductRemovedEvent;
import io.machinecode.shop.cart.event.CartProductUpdatedEvent;
import io.machinecode.shop.cart.event.CartTotalEvent;
import io.machinecode.shop.cart.support.TestCartListener;
import io.machinecode.shop.product.event.DisplayProductCreatedEvent;
import io.machinecode.shop.product.event.DisplayProductDeletedEvent;
import io.machinecode.shop.product.event.DisplayProductUpdatedEvent;
import io.machinecode.shop.product.model.DisplayProductId;
import io.machinecode.shop.product.model.ProductType;
import java.util.List;
import net.openhft.chronicle.core.values.LongValue;
import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ChronicleMapBuilder;
import net.openhft.chronicle.values.Values;
import org.junit.Before;
import org.junit.Test;

/** @author <a href="mailto:brent.n.douglas@gmail.com">Brent Douglas</a> */
public class CartServiceTest {
  private final ChronicleMap<CartProductId, CartProduct> products =
      ChronicleMapBuilder.of(CartProductId.class, CartProduct.class)
          .constantValueSizeBySample(Values.newNativeReference(CartProduct.class))
          .entries(20L)
          .create();
  private final ChronicleMap<LongValue, Cart> carts =
      ChronicleMapBuilder.of(LongValue.class, Cart.class)
          .constantValueSizeBySample(Values.newNativeReference(Cart.class))
          .entries(20L)
          .create();
  private final ChronicleMap<CartItemId, CartItem> cartItems =
      ChronicleMapBuilder.of(CartItemId.class, CartItem.class)
          .constantValueSizeBySample(Values.newNativeReference(CartItem.class))
          .entries(20L)
          .create();
  private final TestCartListener listener = new TestCartListener();
  private final CartService service = new CartService(listener, products, carts, cartItems);

  @Before
  public void setUp() {
    listener.reset();
  }

  @Test
  public void close() {
    final ChronicleMap<CartProductId, CartProduct> products = mock(ChronicleMap.class);
    final ChronicleMap<LongValue, Cart> carts = mock(ChronicleMap.class);
    final ChronicleMap<CartItemId, CartItem> cartItems = mock(ChronicleMap.class);
    final CartService service = new CartService(listener, products, carts, cartItems);
    service.close();
    verify(products).close();
    verify(carts).close();
    verify(cartItems).close();
  }

  @Test
  public void sendAddProductToCartCommandNoProduct() {
    service.sendAddProductToCartCommand(
        new AddProductToCartCommand(1L, new DisplayProductId(1L, ProductType.PRODUCT), 7));
    assertEquals(List.of(new CartErrorEvent(1L, "Product does not exist")), listener.getEvents());
  }

  @Test
  public void sendAddProductToCartCommandNotPositiveQuantity() {
    service.onDisplayProductCreated(
        new DisplayProductCreatedEvent(new DisplayProductId(1L, ProductType.PRODUCT), 2D, "desc"));
    service.sendAddProductToCartCommand(
        new AddProductToCartCommand(1L, new DisplayProductId(1L, ProductType.PRODUCT), 0));
    assertEquals(
        List.of(new CartErrorEvent(1L, "Quantity must be positive")), listener.getEvents());
  }

  @Test
  public void sendAddProductToCartCommandExistingCart() {
    DisplayProductId displayProductId1 = new DisplayProductId(1L, ProductType.PRODUCT);
    DisplayProductId displayProductId2 = new DisplayProductId(2L, ProductType.PRODUCT);
    service.onDisplayProductCreated(new DisplayProductCreatedEvent(displayProductId1, 2D, "desc"));
    service.onDisplayProductCreated(new DisplayProductCreatedEvent(displayProductId2, 4D, "desc"));
    service.sendAddProductToCartCommand(new AddProductToCartCommand(1L, displayProductId1, 1));
    service.sendAddProductToCartCommand(new AddProductToCartCommand(1L, displayProductId2, 2));
    assertEquals(
        List.of(
            new CartProductAddedEvent(1L, displayProductId1, 1),
            new CartProductAddedEvent(1L, displayProductId2, 2)),
        listener.getEvents());
  }

  @Test
  public void sendAddProductToCartCommandExistingProduct() {
    DisplayProductId displayProductId1 = new DisplayProductId(1L, ProductType.PRODUCT);
    service.onDisplayProductCreated(new DisplayProductCreatedEvent(displayProductId1, 2D, "desc"));
    service.sendAddProductToCartCommand(new AddProductToCartCommand(1L, displayProductId1, 1));
    service.sendAddProductToCartCommand(new AddProductToCartCommand(1L, displayProductId1, 2));
    assertEquals(
        List.of(
            new CartProductAddedEvent(1L, displayProductId1, 1),
            new CartErrorEvent(1L, "Product is already in the cart")),
        listener.getEvents());
  }

  @Test
  public void sendUpdateProductToCartCommandNotPositiveQuantity() {
    DisplayProductId displayProductId1 = new DisplayProductId(1L, ProductType.PRODUCT);
    service.sendUpdateProductQuantityInCartCommand(
        new UpdateProductQuantityInCartCommand(1L, displayProductId1, 0));
    assertEquals(
        List.of(new CartErrorEvent(1L, "Quantity must be greater than 0")), listener.getEvents());
  }

  @Test
  public void sendUpdateProductQuantityInCartCommandNoCart() {
    DisplayProductId displayProductId1 = new DisplayProductId(1L, ProductType.PRODUCT);
    service.sendUpdateProductQuantityInCartCommand(
        new UpdateProductQuantityInCartCommand(1L, displayProductId1, 1));
    assertEquals(List.of(new CartErrorEvent(1L, "Cart does not exist")), listener.getEvents());
  }

  @Test
  public void sendUpdateProductQuantityInCartCommandNoProduct() {
    DisplayProductId displayProductId1 = new DisplayProductId(1L, ProductType.PRODUCT);
    DisplayProductId displayProductId2 = new DisplayProductId(2L, ProductType.PRODUCT);
    service.onDisplayProductCreated(new DisplayProductCreatedEvent(displayProductId1, 2D, "desc"));
    service.sendAddProductToCartCommand(new AddProductToCartCommand(1L, displayProductId1, 1));
    service.sendUpdateProductQuantityInCartCommand(
        new UpdateProductQuantityInCartCommand(1L, displayProductId2, 1));
    assertEquals(
        List.of(
            new CartProductAddedEvent(1L, displayProductId1, 1),
            new CartErrorEvent(1L, "Product does not exist")),
        listener.getEvents());
  }

  @Test
  public void sendUpdateProductQuantityInCartCommandNoProductBecauseItWasRemoved() {
    DisplayProductId displayProductId1 = new DisplayProductId(1L, ProductType.PRODUCT);
    service.onDisplayProductCreated(new DisplayProductCreatedEvent(displayProductId1, 2D, "desc1"));
    service.sendAddProductToCartCommand(new AddProductToCartCommand(1L, displayProductId1, 1));
    service.onDisplayProductDeleted(new DisplayProductDeletedEvent(displayProductId1));
    service.sendUpdateProductQuantityInCartCommand(
        new UpdateProductQuantityInCartCommand(1L, displayProductId1, 4));
    assertEquals(
        List.of(
            new CartProductAddedEvent(1L, displayProductId1, 1),
            new CartProductRemovedEvent(1L, displayProductId1),
            new CartErrorEvent(1L, "Product does not exist")),
        listener.getEvents());
  }

  @Test
  public void sendUpdateProductQuantityInCartCommandNotInCart() {
    DisplayProductId displayProductId1 = new DisplayProductId(1L, ProductType.PRODUCT);
    DisplayProductId displayProductId2 = new DisplayProductId(2L, ProductType.PRODUCT);
    service.onDisplayProductCreated(new DisplayProductCreatedEvent(displayProductId1, 2D, "desc"));
    service.onDisplayProductCreated(new DisplayProductCreatedEvent(displayProductId2, 2D, "desc"));
    service.sendAddProductToCartCommand(new AddProductToCartCommand(1L, displayProductId1, 1));
    service.sendUpdateProductQuantityInCartCommand(
        new UpdateProductQuantityInCartCommand(1L, displayProductId2, 1));
    assertEquals(
        List.of(
            new CartProductAddedEvent(1L, displayProductId1, 1),
            new CartErrorEvent(1L, "Product is not in the cart")),
        listener.getEvents());
  }

  @Test
  public void sendUpdateProductQuantity() {
    DisplayProductId displayProductId1 = new DisplayProductId(1L, ProductType.PRODUCT);
    service.onDisplayProductCreated(new DisplayProductCreatedEvent(displayProductId1, 2D, "desc"));
    service.sendAddProductToCartCommand(new AddProductToCartCommand(1L, displayProductId1, 1));
    service.sendUpdateProductQuantityInCartCommand(
        new UpdateProductQuantityInCartCommand(1L, displayProductId1, 2));
    assertEquals(
        List.of(
            new CartProductAddedEvent(1L, displayProductId1, 1),
            new CartProductUpdatedEvent(1L, displayProductId1, 2)),
        listener.getEvents());
  }

  @Test
  public void sendRemoveProductFromCartCommandNoCart() {
    DisplayProductId displayProductId1 = new DisplayProductId(1L, ProductType.PRODUCT);
    service.onDisplayProductCreated(new DisplayProductCreatedEvent(displayProductId1, 2D, "desc"));
    service.sendRemoveProductFromCartCommand(
        new RemoveProductFromCartCommand(1L, displayProductId1));
    assertEquals(List.of(new CartErrorEvent(1L, "Cart does not exist")), listener.getEvents());
  }

  @Test
  public void sendRemoveProductFromCartCommandNotInCart() {
    DisplayProductId displayProductId1 = new DisplayProductId(1L, ProductType.PRODUCT);
    DisplayProductId displayProductId2 = new DisplayProductId(2L, ProductType.PRODUCT);
    service.onDisplayProductCreated(new DisplayProductCreatedEvent(displayProductId1, 2D, "desc"));
    service.onDisplayProductCreated(new DisplayProductCreatedEvent(displayProductId2, 2D, "desc"));
    service.sendAddProductToCartCommand(new AddProductToCartCommand(1L, displayProductId1, 1));
    service.sendRemoveProductFromCartCommand(
        new RemoveProductFromCartCommand(1L, displayProductId2));
    assertEquals(
        List.of(
            new CartProductAddedEvent(1L, displayProductId1, 1),
            new CartErrorEvent(1L, "Product is not in the cart")),
        listener.getEvents());
  }

  @Test
  public void sendRemoveProductFromCartCommand() {
    DisplayProductId displayProductId1 = new DisplayProductId(1L, ProductType.PRODUCT);
    service.onDisplayProductCreated(new DisplayProductCreatedEvent(displayProductId1, 2D, "desc"));
    service.sendAddProductToCartCommand(new AddProductToCartCommand(1L, displayProductId1, 1));
    service.sendRemoveProductFromCartCommand(
        new RemoveProductFromCartCommand(1L, displayProductId1));
    assertEquals(
        List.of(
            new CartProductAddedEvent(1L, displayProductId1, 1),
            new CartProductRemovedEvent(1L, displayProductId1)),
        listener.getEvents());
  }

  @Test
  public void sendRemoveProductFromCartCommandNoProduct() {
    DisplayProductId displayProductId1 = new DisplayProductId(1L, ProductType.PRODUCT);
    DisplayProductId displayProductId2 = new DisplayProductId(1L, ProductType.BUNDLE);
    service.onDisplayProductCreated(new DisplayProductCreatedEvent(displayProductId1, 2D, "desc"));
    service.sendAddProductToCartCommand(new AddProductToCartCommand(1L, displayProductId1, 1));
    service.sendRemoveProductFromCartCommand(
        new RemoveProductFromCartCommand(1L, displayProductId2));
    assertEquals(
        List.of(
            new CartProductAddedEvent(1L, displayProductId1, 1),
            new CartErrorEvent(1L, "Product does not exist")),
        listener.getEvents());
  }

  @Test
  public void sendRemoveProductFromCartCommandProductNotInCart() {
    DisplayProductId displayProductId1 = new DisplayProductId(1L, ProductType.PRODUCT);
    DisplayProductId displayProductId2 = new DisplayProductId(1L, ProductType.BUNDLE);
    service.onDisplayProductCreated(new DisplayProductCreatedEvent(displayProductId1, 2D, "desc"));
    service.onDisplayProductCreated(new DisplayProductCreatedEvent(displayProductId2, 4D, "desc"));
    service.sendAddProductToCartCommand(new AddProductToCartCommand(1L, displayProductId1, 1));
    service.sendRemoveProductFromCartCommand(
        new RemoveProductFromCartCommand(1L, displayProductId2));
    assertEquals(
        List.of(
            new CartProductAddedEvent(1L, displayProductId1, 1),
            new CartErrorEvent(1L, "Product is not in the cart")),
        listener.getEvents());
  }

  @Test
  public void sendCalculateTotalForCartCommand() {
    DisplayProductId displayProductId1 = new DisplayProductId(1L, ProductType.PRODUCT);
    DisplayProductId displayProductId2 = new DisplayProductId(1L, ProductType.BUNDLE);
    DisplayProductId displayProductId3 = new DisplayProductId(2L, ProductType.BUNDLE);
    service.onDisplayProductCreated(new DisplayProductCreatedEvent(displayProductId1, 2D, "desc1"));
    service.onDisplayProductCreated(new DisplayProductCreatedEvent(displayProductId2, 4D, "desc2"));
    service.onDisplayProductCreated(new DisplayProductCreatedEvent(displayProductId3, 5D, "desc3"));
    service.sendAddProductToCartCommand(new AddProductToCartCommand(1L, displayProductId1, 3));
    service.sendCalculateTotalForCartCommand(new CalculateTotalForCartCommand(1L));
    service.sendAddProductToCartCommand(new AddProductToCartCommand(1L, displayProductId2, 2));
    service.sendCalculateTotalForCartCommand(new CalculateTotalForCartCommand(1L));
    service.onDisplayProductUpdated(new DisplayProductUpdatedEvent(displayProductId2, 8D, "desc2"));
    service.sendCalculateTotalForCartCommand(new CalculateTotalForCartCommand(1L));
    service.sendAddProductToCartCommand(new AddProductToCartCommand(1L, displayProductId3, 1));
    service.sendCalculateTotalForCartCommand(new CalculateTotalForCartCommand(1L));
    service.sendRemoveProductFromCartCommand(
        new RemoveProductFromCartCommand(1L, displayProductId1));
    service.sendCalculateTotalForCartCommand(new CalculateTotalForCartCommand(1L));
    service.onDisplayProductDeleted(new DisplayProductDeletedEvent(displayProductId2));
    service.sendCalculateTotalForCartCommand(new CalculateTotalForCartCommand(1L));
    assertEquals(
        List.of(
            new CartProductAddedEvent(1L, displayProductId1, 3),
            new CartTotalEvent(1L, 6D),
            new CartProductAddedEvent(1L, displayProductId2, 2),
            new CartTotalEvent(1L, 14D),
            new CartTotalEvent(1L, 22D),
            new CartProductAddedEvent(1L, displayProductId3, 1),
            new CartTotalEvent(1L, 27D),
            new CartProductRemovedEvent(1L, displayProductId1),
            new CartTotalEvent(1L, 21D),
            new CartProductRemovedEvent(1L, displayProductId2),
            new CartTotalEvent(1L, 5D)),
        listener.getEvents());
  }

  @Test
  public void sendCalculateTotalForCartCommandNoCart() {
    service.sendCalculateTotalForCartCommand(new CalculateTotalForCartCommand(1L));
    assertEquals(List.of(new CartErrorEvent(1L, "Cart does not exist")), listener.getEvents());
  }
}
