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
package io.machinecode.shop.product.domain;

import io.machinecode.shop.product.api.DisplayProductListener;
import io.machinecode.shop.product.api.ProductBundleListener;
import io.machinecode.shop.product.api.ProductDispatcher;
import io.machinecode.shop.product.api.ProductListener;
import io.machinecode.shop.product.command.CreateProductBundleCommand;
import io.machinecode.shop.product.command.CreateProductCommand;
import io.machinecode.shop.product.command.DeleteProductBundleCommand;
import io.machinecode.shop.product.command.DeleteProductCommand;
import io.machinecode.shop.product.command.UpdateProductCommand;
import io.machinecode.shop.product.event.DisplayProductCreatedEvent;
import io.machinecode.shop.product.event.DisplayProductDeletedEvent;
import io.machinecode.shop.product.event.DisplayProductUpdatedEvent;
import io.machinecode.shop.product.event.ProductBundleCreatedEvent;
import io.machinecode.shop.product.event.ProductBundleDeletedEvent;
import io.machinecode.shop.product.event.ProductBundleErrorEvent;
import io.machinecode.shop.product.event.ProductCreatedEvent;
import io.machinecode.shop.product.event.ProductDeletedEvent;
import io.machinecode.shop.product.event.ProductErrorEvent;
import io.machinecode.shop.product.event.ProductUpdatedEvent;
import io.machinecode.shop.product.model.BundleItem;
import io.machinecode.shop.product.model.DisplayProductId;
import io.machinecode.shop.product.model.PriceDetail;
import io.machinecode.shop.product.model.ProductType;
import java.util.Objects;
import net.openhft.chronicle.core.util.StringUtils;
import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.core.values.LongValue;
import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ExternalMapQueryContext;
import net.openhft.chronicle.map.MapEntry;
import net.openhft.chronicle.values.Values;

/** @author <a href="mailto:brent.n.douglas@gmail.com">Brent Douglas</a> */
public class ProductService implements ProductDispatcher, AutoCloseable {
  private final LongValue productId = Values.newHeapInstance(LongValue.class);
  private final LongValue bundleId = Values.newHeapInstance(LongValue.class);
  private final LongValue itemId = Values.newHeapInstance(LongValue.class);
  private final StringBuilder description = new StringBuilder();

  private final IntValue bundlesContainingProduct = Values.newHeapInstance(IntValue.class);

  private final Product product = Values.newHeapInstance(Product.class);
  private final ProductBundle productBundle = Values.newHeapInstance(ProductBundle.class);
  private final ProductBundleItem productBundleItem =
      Values.newHeapInstance(ProductBundleItem.class);
  private final ProductPriceInfo priceInfo = Values.newHeapInstance(ProductPriceInfo.class);

  private final ProductBundleCreatedEvent productBundleCreatedEvent =
      new ProductBundleCreatedEvent();
  private final ProductBundleDeletedEvent productBundleDeletedEvent =
      new ProductBundleDeletedEvent();
  private final ProductBundleErrorEvent productBundleErrorEvent = new ProductBundleErrorEvent();
  private final ProductCreatedEvent productCreatedEvent = new ProductCreatedEvent();
  private final ProductDeletedEvent productDeletedEvent = new ProductDeletedEvent();
  private final ProductErrorEvent productErrorEvent = new ProductErrorEvent();
  private final ProductUpdatedEvent productUpdatedEvent = new ProductUpdatedEvent();
  private final DisplayProductCreatedEvent displayProductCreatedEvent =
      new DisplayProductCreatedEvent();
  private final DisplayProductUpdatedEvent displayProductUpdatedEvent =
      new DisplayProductUpdatedEvent();
  private final DisplayProductDeletedEvent displayProductDeletedEvent =
      new DisplayProductDeletedEvent();
  private final DisplayProductId displayProductId = new DisplayProductId();

  private final ChronicleMap<LongValue, Product> products;
  private final ChronicleMap<LongValue, ProductBundle> productBundles;
  private final ChronicleMap<LongValue, ProductBundleItem> productBundleItems;
  private final ChronicleMap<LongValue, IntValue> productBundleCount;

  private final ProductListener productListener;
  private final ProductBundleListener productBundleListener;
  private final DisplayProductListener displayProductListener;

  public ProductService(
      final ProductListener productListener,
      final ProductBundleListener productBundleListener,
      final DisplayProductListener displayProductListener,
      final ChronicleMap<LongValue, Product> products,
      final ChronicleMap<LongValue, ProductBundle> productBundles,
      final ChronicleMap<LongValue, ProductBundleItem> productBundleItems,
      final ChronicleMap<LongValue, IntValue> productBundleCount) {
    this.products = products;
    this.productBundles = productBundles;
    this.productBundleItems = productBundleItems;
    this.productBundleCount = productBundleCount;
    this.productListener = productListener;
    this.productBundleListener = productBundleListener;
    this.displayProductListener = displayProductListener;
  }

  @Override
  public void close() {
    this.productBundleCount.close();
    this.productBundleItems.close();
    this.productBundles.close();
    this.products.close();
  }

  @Override
  public void sendCreateProductCommand(final CreateProductCommand command) {
    long id = command.getId();
    if (isProductDataInvalid(id, command.getPrice(), command.getDescription())) {
      return;
    }
    final LongValue productId = getProductId(id);
    try (final ExternalMapQueryContext<LongValue, Product, ?> productCtx =
        products.queryContext(productId)) {
      productCtx.updateLock().lock();
      final MapEntry<LongValue, Product> productEntry = productCtx.entry();
      if (productEntry != null) {
        sendProductError(id, "Product already exists");
        return;
      }
      final Product product = this.product;
      product.setId(id);
      product.setPrice(command.getPrice());
      product.setDescription(command.getDescription());
      productCtx.absentEntry().doInsert(productCtx.wrapValueAsData(product));
    }
    ProductCreatedEvent productCreatedEvent = this.productCreatedEvent;
    productCreatedEvent.setId(id);
    productCreatedEvent.setPrice(command.getPrice());
    productCreatedEvent.setDescription(command.getDescription());
    productListener.onProductCreated(productCreatedEvent);
    DisplayProductId displayProductId = this.displayProductId;
    displayProductId.setId(id);
    displayProductId.setType(ProductType.PRODUCT);
    DisplayProductCreatedEvent displayProductCreatedEvent = this.displayProductCreatedEvent;
    displayProductCreatedEvent.setId(displayProductId);
    displayProductCreatedEvent.setPrice(command.getPrice());
    displayProductCreatedEvent.setDescription(command.getDescription());
    displayProductListener.onDisplayProductCreated(displayProductCreatedEvent);
  }

  @Override
  public void sendUpdateProductCommand(final UpdateProductCommand command) {
    final long id = command.getId();
    if (isProductDataInvalid(id, command.getPrice(), command.getDescription())) {
      return;
    }
    final LongValue productId = getProductId(id);
    try (final ExternalMapQueryContext<LongValue, Product, ?> productCtx =
        products.queryContext(productId)) {
      productCtx.updateLock().lock();
      final MapEntry<LongValue, Product> productEntry = productCtx.entry();
      if (productEntry == null) {
        sendProductError(id, "Product does not exist");
        return;
      }
      final Product product = productEntry.value().getUsing(this.product);
      product.getUsingDescription(description);
      if (Objects.equals(product.getPrice(), command.getPrice())
          && StringUtils.isEqual(description, command.getDescription())) {
        return;
      }
      product.setPrice(command.getPrice());
      product.setDescription(command.getDescription());
      productEntry.doReplaceValue(productCtx.wrapValueAsData(product));
    }
    ProductUpdatedEvent productUpdatedEvent = this.productUpdatedEvent;
    productUpdatedEvent.setId(id);
    productUpdatedEvent.setPrice(command.getPrice());
    productUpdatedEvent.setDescription(command.getDescription());
    productListener.onProductUpdated(productUpdatedEvent);
    DisplayProductId displayProductId = this.displayProductId;
    displayProductId.setId(id);
    displayProductId.setType(ProductType.PRODUCT);
    DisplayProductUpdatedEvent displayProductUpdatedEvent = this.displayProductUpdatedEvent;
    displayProductUpdatedEvent.setId(displayProductId);
    displayProductUpdatedEvent.setPrice(command.getPrice());
    displayProductUpdatedEvent.setDescription(command.getDescription());
    displayProductListener.onDisplayProductUpdated(displayProductUpdatedEvent);
  }

  @Override
  public void sendDeleteProductCommand(final DeleteProductCommand command) {
    final long id = command.getId();
    final LongValue productId = getProductId(id);
    if (productBundleCount.containsKey(productId)) {
      sendProductError(id, "Cannot remove product which is part of a bundle");
      return;
    }
    try (final ExternalMapQueryContext<LongValue, Product, ?> productCtx =
        products.queryContext(productId)) {
      productCtx.updateLock().lock();
      final MapEntry<LongValue, Product> productEntry = productCtx.entry();
      if (productEntry == null) {
        sendProductError(id, "Product does not exist");
        return;
      }
      productEntry.doRemove();
    }
    ProductDeletedEvent productDeletedEvent = this.productDeletedEvent;
    productDeletedEvent.setId(id);
    productListener.onProductDeleted(productDeletedEvent);
    DisplayProductId displayProductId = this.displayProductId;
    displayProductId.setId(id);
    displayProductId.setType(ProductType.PRODUCT);
    DisplayProductDeletedEvent displayProductDeletedEvent = this.displayProductDeletedEvent;
    displayProductDeletedEvent.setId(displayProductId);
    displayProductListener.onDisplayProductDeleted(displayProductDeletedEvent);
  }

  @Override
  public void sendCreateProductBundleCommand(final CreateProductBundleCommand command) {
    final long id = command.getId();
    final BundleItem[] items = command.getItems();
    if (isProductBundleDataInvalid(id, command.getDescription(), items)) {
      return;
    }
    final LongValue bundleId = getBundleId(id);
    if (isBundleProductListInvalid(items, id)) {
      return;
    }
    try (final ExternalMapQueryContext<LongValue, ProductBundle, ?> bundleCtx =
        productBundles.queryContext(bundleId)) {
      bundleCtx.updateLock().lock();
      final MapEntry<LongValue, ProductBundle> bundleEntry = bundleCtx.entry();
      if (bundleEntry != null) {
        sendProductBundleError(id, "Bundle already exists");
        return;
      }
      final ProductBundle bundle = this.productBundle;
      bundle.setId(id);
      double price = 0D;
      ChronicleMap<LongValue, ProductBundleItem> productBundleItems = this.productBundleItems;
      for (int i = 0; i < items.length; i++) {
        final BundleItem item = items[i];
        final LongValue itemId = getItemId(item.getId());
        bundle.setItemIdAt(i, itemId);
        try (final ExternalMapQueryContext<LongValue, ProductBundleItem, ?> itemCtx =
            productBundleItems.queryContext(itemId)) {
          itemCtx.updateLock().lock();
          if (itemCtx.entry() != null) {
            sendProductBundleError(id, "Bundle item already exists");
            return;
          }
          ProductPriceInfo priceInfo = this.priceInfo;
          PriceDetail priceDetail = item.getPrice();
          priceInfo.setType(priceDetail.getType());
          priceInfo.setValue(priceDetail.getValue());
          final ProductBundleItem productBundleItem = this.productBundleItem;
          productBundleItem.setProductId(item.getProductId());
          productBundleItem.setPriceInfo(priceInfo);

          itemCtx.absentEntry().doInsert(itemCtx.wrapValueAsData(productBundleItem));

          LongValue productId = getProductId(item.getProductId());
          price += getItemPrice(item, productId);
          addProductBundleMapping(bundleId, productId);
        }
      }
      bundle.setItemCount(items.length);
      bundle.setDescription(command.getDescription());
      bundleCtx.absentEntry().doInsert(bundleCtx.wrapValueAsData(bundle));
      ProductBundleCreatedEvent productBundleCreatedEvent = this.productBundleCreatedEvent;
      productBundleCreatedEvent.setId(id);
      productBundleCreatedEvent.setItems(items);
      productBundleCreatedEvent.setDescription(command.getDescription());
      productBundleListener.onProductBundleCreated(productBundleCreatedEvent);
      DisplayProductId displayProductId = this.displayProductId;
      displayProductId.setId(id);
      displayProductId.setType(ProductType.BUNDLE);
      DisplayProductCreatedEvent displayProductCreatedEvent = this.displayProductCreatedEvent;
      displayProductCreatedEvent.setId(displayProductId);
      displayProductCreatedEvent.setPrice(price);
      displayProductCreatedEvent.setDescription(command.getDescription());
      displayProductListener.onDisplayProductCreated(displayProductCreatedEvent);
    }
  }

  @Override
  public void sendDeleteProductBundleCommand(final DeleteProductBundleCommand command) {
    final long id = command.getId();
    final LongValue bundleId = getBundleId(id);
    final ProductBundle bundle = this.productBundle;
    try (final ExternalMapQueryContext<LongValue, ProductBundle, ?> bundleCtx =
        productBundles.queryContext(bundleId)) {
      bundleCtx.updateLock().lock();
      final MapEntry<LongValue, ProductBundle> bundleEntry = bundleCtx.entry();
      if (bundleEntry == null) {
        sendProductBundleError(id, "Bundle does not exist");
        return;
      }
      bundleEntry.value().getUsing(bundle);
      for (int i = bundle.getItemCount() - 1; i >= 0; i--) {
        final LongValue itemId = bundle.getItemIdAt(i);
        try (final ExternalMapQueryContext<LongValue, ProductBundleItem, ?> itemCtx =
            productBundleItems.queryContext(itemId)) {
          itemCtx.updateLock().lock();
          final MapEntry<LongValue, ProductBundleItem> itemEntry = itemCtx.entry();
          if (itemEntry == null) {
            continue;
          }
          ProductBundleItem productBundleItem = this.productBundleItem;
          itemEntry.value().getUsing(productBundleItem);
          LongValue productId = getProductId(productBundleItem.getProductId());
          try (ExternalMapQueryContext<LongValue, IntValue, ?> productBundlesCtx =
              productBundleCount.queryContext(productId)) {
            productBundlesCtx.updateLock().lock();
            MapEntry<LongValue, IntValue> productBundlesEntry = productBundlesCtx.entry();
            if (productBundlesEntry == null) {
              continue;
            }
            IntValue bundlesContainingProduct = this.bundlesContainingProduct;
            productBundlesEntry.value().getUsing(bundlesContainingProduct);
            final int count = bundlesContainingProduct.getValue();
            int remaining = count - 1;
            if (remaining == 0) {
              productBundlesEntry.doRemove();
            } else {
              bundlesContainingProduct.setValue(remaining);
              productBundlesEntry.doReplaceValue(
                  productBundlesCtx.wrapValueAsData(bundlesContainingProduct));
            }
          }
          itemEntry.doRemove();
        }
      }
      bundleEntry.doRemove();
    }
    ProductBundleDeletedEvent productBundleDeletedEvent = this.productBundleDeletedEvent;
    productBundleDeletedEvent.setId(id);
    productBundleListener.onProductBundleDeleted(productBundleDeletedEvent);
    DisplayProductId displayProductId = this.displayProductId;
    displayProductId.setId(id);
    displayProductId.setType(ProductType.BUNDLE);
    DisplayProductDeletedEvent displayProductDeletedEvent = this.displayProductDeletedEvent;
    displayProductDeletedEvent.setId(displayProductId);
    displayProductListener.onDisplayProductDeleted(displayProductDeletedEvent);
  }

  private double getItemPrice(final BundleItem item, final LongValue productId) {
    double price = 0;
    try (final ExternalMapQueryContext<LongValue, Product, ?> productCtx =
        products.queryContext(productId)) {
      MapEntry<LongValue, Product> entry = productCtx.entry();
      Product product = this.product;
      entry.value().getUsing(product);
      PriceDetail detail = item.getPrice();
      switch (detail.getType()) {
        case PRICE:
          price += detail.getValue();
          break;
        case DISCOUNT:
          price += Math.max(product.getPrice() - detail.getValue(), 0D);
          break;
        case PERCENTAGE:
          price += ((detail.getValue() / 100.0D) * product.getPrice());
          break;
        default:
          throw new UnsupportedOperationException("Unhandled price type " + detail.getType());
      }
    }
    return price;
  }

  private boolean isProductDataInvalid(
      final long id, final double price, final CharSequence description) {
    if (id <= 0) {
      sendProductError(id, "Product id must be larger than 0");
      return true;
    }
    if (price < 0D) {
      sendProductError(id, "Product price may not be negative");
      return true;
    }
    if (description == null || description.length() > Product.MAX_DESCRIPTION_LENGTH) {
      sendProductError(id, "Product description is too long");
      return true;
    }
    return false;
  }

  private boolean isProductBundleDataInvalid(
      final long id, final CharSequence description, final BundleItem[] items) {
    if (id <= 0) {
      sendProductBundleError(id, "Bundle id must be larger than 0");
      return true;
    }
    if (description == null || description.length() > ProductBundle.MAX_DESCRIPTION_LENGTH) {
      sendProductBundleError(id, "Bundle description is too long");
      return true;
    }
    if (items == null || items.length == 0) {
      sendProductBundleError(id, "Bundle must provide a product");
      return true;
    }
    if (items.length > ProductBundle.MAX_ITEMS_PER_BUNDLE) {
      sendProductBundleError(id, "Bundle has too many products");
      return true;
    }
    for (final BundleItem item : items) {
      if (item.getPrice() == null || item.getPrice().getType() == null) {
        sendProductBundleError(id, "Bundle price may not be null");
        return true;
      }
      if (item.getPrice().getValue() < 0D) {
        sendProductBundleError(id, "Bundle price may not be negative");
        return true;
      }
    }
    return false;
  }

  private boolean isBundleProductListInvalid(final BundleItem[] items, final long id) {
    for (final BundleItem item : items) {
      final LongValue productId = getProductId(item.getProductId());
      try (final ExternalMapQueryContext<LongValue, Product, ?> productCtx =
          products.queryContext(productId); ) {
        final MapEntry<LongValue, Product> productEntry = productCtx.entry();
        if (productEntry == null) {
          sendProductBundleError(id, "Product does not exist");
          return true;
        }
      }
    }
    return false;
  }

  private void addProductBundleMapping(final LongValue bundleId, final LongValue productId) {
    IntValue bundlesContainingProduct = this.bundlesContainingProduct;
    try (final ExternalMapQueryContext<LongValue, IntValue, ?> productBundlesCtx =
        productBundleCount.queryContext(productId)) {
      productBundlesCtx.updateLock().lock();
      final MapEntry<LongValue, IntValue> productBundlesEntry = productBundlesCtx.entry();
      if (productBundlesEntry == null) {
        bundlesContainingProduct.setValue(1);
        productBundlesCtx
            .absentEntry()
            .doInsert(productBundlesCtx.wrapValueAsData(bundlesContainingProduct));
      } else {
        productBundlesEntry.value().getUsing(bundlesContainingProduct);
        bundlesContainingProduct.setValue(bundlesContainingProduct.getValue() + 1);
        productBundlesEntry.doReplaceValue(
            productBundlesCtx.wrapValueAsData(bundlesContainingProduct));
      }
    }
  }

  private void sendProductError(final long id, final String message) {
    ProductErrorEvent productErrorEvent = this.productErrorEvent;
    productErrorEvent.setId(id);
    productErrorEvent.setMessage(message);
    productListener.onProductError(productErrorEvent);
  }

  private void sendProductBundleError(final long id, final String message) {
    ProductBundleErrorEvent productBundleErrorEvent = this.productBundleErrorEvent;
    productBundleErrorEvent.setId(id);
    productBundleErrorEvent.setMessage(message);
    productBundleListener.onProductBundleError(productBundleErrorEvent);
  }

  private LongValue getItemId(final long id) {
    LongValue val = this.itemId;
    val.setValue(id);
    return val;
  }

  private LongValue getProductId(final long id) {
    LongValue val = this.productId;
    val.setValue(id);
    return val;
  }

  private LongValue getBundleId(final long id) {
    LongValue val = this.bundleId;
    val.setValue(id);
    return val;
  }
}
