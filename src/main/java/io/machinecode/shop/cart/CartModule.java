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
package io.machinecode.shop.cart;

import io.machinecode.shop.cart.api.CartDispatcher;
import io.machinecode.shop.cart.api.CartListener;
import io.machinecode.shop.cart.domain.Cart;
import io.machinecode.shop.cart.domain.CartItem;
import io.machinecode.shop.cart.domain.CartItemId;
import io.machinecode.shop.cart.domain.CartProduct;
import io.machinecode.shop.cart.domain.CartProductId;
import io.machinecode.shop.cart.domain.CartService;
import java.util.List;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.values.LongValue;
import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ChronicleMapBuilder;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.values.Values;

/** @author <a href="mailto:brent.n.douglas@gmail.com">Brent Douglas</a> */
public class CartModule implements AutoCloseable {
  private final CartDispatcher dispatcher;
  private final List<MethodReader> readers;
  private final CartService cartService;
  private final ChronicleQueue cartOutput;

  public CartModule(
      final long entries,
      final ChronicleQueue cartInput,
      final ChronicleQueue productOutput,
      final ChronicleQueue cartOutput) {
    this.cartOutput = cartOutput;
    CartListener listener =
        cartOutput.acquireAppender().methodWriterBuilder(CartListener.class).get();
    ChronicleMap<CartProductId, CartProduct> products =
        ChronicleMapBuilder.of(CartProductId.class, CartProduct.class)
            .constantValueSizeBySample(Values.newNativeReference(CartProduct.class))
            .entries(entries)
            .create();
    ChronicleMap<LongValue, Cart> carts =
        ChronicleMapBuilder.of(LongValue.class, Cart.class)
            .constantValueSizeBySample(Values.newNativeReference(Cart.class))
            .entries(entries)
            .create();
    ChronicleMap<CartItemId, CartItem> cartItems =
        ChronicleMapBuilder.of(CartItemId.class, CartItem.class)
            .constantValueSizeBySample(Values.newNativeReference(CartItem.class))
            .entries(entries)
            .create();
    this.cartService = new CartService(listener, products, carts, cartItems);
    this.dispatcher = this.cartService;
    //    this.dispatcher =
    // cartInput.acquireAppender().methodWriterBuilder(CartDispatcher.class).get();
    this.readers =
        List.of(
            productOutput.createTailer().methodReader(cartService),
            cartInput.createTailer().methodReader(cartService));
  }

  public MethodReader addListener(CartListener listener) {
    return cartOutput.createTailer().methodReader(listener);
  }

  public CartDispatcher getDispatcher() {
    return dispatcher;
  }

  public List<MethodReader> getReaders() {
    return readers;
  }

  @Override
  public void close() {
    this.cartService.close();
  }
}
