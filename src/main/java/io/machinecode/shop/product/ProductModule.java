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
package io.machinecode.shop.product;

import io.machinecode.shop.product.api.DisplayProductListener;
import io.machinecode.shop.product.api.ProductBundleListener;
import io.machinecode.shop.product.api.ProductDispatcher;
import io.machinecode.shop.product.api.ProductListener;
import io.machinecode.shop.product.domain.Product;
import io.machinecode.shop.product.domain.ProductBundle;
import io.machinecode.shop.product.domain.ProductBundleItem;
import io.machinecode.shop.product.domain.ProductService;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.core.values.LongValue;
import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ChronicleMapBuilder;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.values.Values;

/** @author <a href="mailto:brent.n.douglas@gmail.com">Brent Douglas</a> */
public class ProductModule implements AutoCloseable {
  private final ProductDispatcher dispatcher;
  private final MethodReader reader;
  private final ProductService productService;
  private final ChronicleQueue productOutput;

  public ProductModule(
      final long entries, final ChronicleQueue productInput, final ChronicleQueue productOutput) {
    this.productOutput = productOutput;
    ProductListener productListener =
        productOutput.acquireAppender().methodWriterBuilder(ProductListener.class).get();
    ProductBundleListener productBundleListener =
        productOutput.acquireAppender().methodWriterBuilder(ProductBundleListener.class).get();
    DisplayProductListener displayProductListener =
        productOutput.acquireAppender().methodWriterBuilder(DisplayProductListener.class).get();
    ChronicleMap<LongValue, Product> products =
        ChronicleMapBuilder.of(LongValue.class, Product.class)
            .constantValueSizeBySample(Values.newNativeReference(Product.class))
            .entries(entries)
            .create();
    ChronicleMap<LongValue, ProductBundle> productBundles =
        ChronicleMapBuilder.of(LongValue.class, ProductBundle.class)
            .constantValueSizeBySample(Values.newNativeReference(ProductBundle.class))
            .entries(entries)
            .create();
    ChronicleMap<LongValue, ProductBundleItem> productBundleItems =
        ChronicleMapBuilder.of(LongValue.class, ProductBundleItem.class)
            .constantValueSizeBySample(Values.newNativeReference(ProductBundleItem.class))
            .entries(entries)
            .create();
    ChronicleMap<LongValue, IntValue> productBundleCount =
        ChronicleMapBuilder.of(LongValue.class, IntValue.class).entries(entries).create();
    this.productService =
        new ProductService(
            productListener,
            productBundleListener,
            displayProductListener,
            products,
            productBundles,
            productBundleItems,
            productBundleCount);
    this.dispatcher = this.productService;
    //    this.dispatcher =
    //        productInput.acquireAppender().methodWriterBuilder(ProductDispatcher.class).get();
    this.reader = productInput.createTailer().methodReader(this.productService);
  }

  public MethodReader addProductBundleListener(ProductBundleListener listener) {
    return productOutput.createTailer().methodReader(listener);
  }

  public MethodReader addDisplayProductListener(DisplayProductListener listener) {
    return productOutput.createTailer().methodReader(listener);
  }

  public MethodReader addProductListener(ProductListener listener) {
    return productOutput.createTailer().methodReader(listener);
  }

  public ProductDispatcher getDispatcher() {
    return dispatcher;
  }

  public MethodReader getReader() {
    return reader;
  }

  @Override
  public void close() {
    this.productService.close();
    this.reader.close();
  }
}
