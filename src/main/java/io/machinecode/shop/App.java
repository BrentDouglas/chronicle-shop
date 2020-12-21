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
package io.machinecode.shop;

import io.machinecode.shop.cart.CartModule;
import io.machinecode.shop.product.ProductModule;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.threads.EventLoop;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue;

/** @author <a href="mailto:brent.n.douglas@gmail.com">Brent Douglas</a> */
public class App implements AutoCloseable {
  private final SingleChronicleQueue productInput;
  private final SingleChronicleQueue productOutput;
  private final SingleChronicleQueue cartInput;
  private final SingleChronicleQueue cartOutput;
  private final ProductModule productModule;
  private final CartModule cartModule;
  private final List<MethodReader> readers;
  private final EventLoop eventLoop;

  public App(final File dir, final EventLoop eventLoop, final long products, final long carts) {
    this.eventLoop = eventLoop;
    this.productInput =
        ChronicleQueue.singleBuilder(Paths.get(dir.getAbsolutePath(), "product-commands")).build();
    this.productOutput =
        ChronicleQueue.singleBuilder(Paths.get(dir.getAbsolutePath(), "product-events")).build();
    this.cartInput =
        ChronicleQueue.singleBuilder(Paths.get(dir.getAbsolutePath(), "cart-commands")).build();
    this.cartOutput =
        ChronicleQueue.singleBuilder(Paths.get(dir.getAbsolutePath(), "cart-events")).build();
    this.productModule = new ProductModule(products, productInput, productOutput);
    this.cartModule = new CartModule(carts, cartInput, productOutput, cartOutput);
    final List<MethodReader> readers = new ArrayList<>();
    readers.add(productModule.getReader());
    readers.addAll(cartModule.getReaders());
    this.readers = List.copyOf(readers);
    for (final MethodReader reader : this.readers) {
      eventLoop.addHandler(reader::readOne);
    }
  }

  public ProductModule getProductModule() {
    return productModule;
  }

  public CartModule getCartModule() {
    return cartModule;
  }

  public List<MethodReader> getReaders() {
    return readers;
  }

  public void start() {
    eventLoop.start();
  }

  @Override
  public void close() {
    eventLoop.stop();
    this.productModule.close();
    this.cartModule.close();
  }
}
