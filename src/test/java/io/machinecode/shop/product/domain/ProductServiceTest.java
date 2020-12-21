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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
import io.machinecode.shop.product.model.PriceType;
import io.machinecode.shop.product.model.ProductType;
import io.machinecode.shop.product.support.TestProductListener;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;
import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.core.values.LongValue;
import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ChronicleMapBuilder;
import net.openhft.chronicle.values.Values;
import org.junit.After;
import org.junit.Test;

/** @author <a href="mailto:brent.n.douglas@gmail.com">Brent Douglas</a> */
public class ProductServiceTest {
  private final ChronicleMap<LongValue, Product> products =
      ChronicleMapBuilder.of(LongValue.class, Product.class)
          .constantValueSizeBySample(Values.newNativeReference(Product.class))
          .entries(20L)
          .create();
  private final ChronicleMap<LongValue, ProductBundle> productBundles =
      ChronicleMapBuilder.of(LongValue.class, ProductBundle.class)
          .constantValueSizeBySample(Values.newNativeReference(ProductBundle.class))
          .entries(20L)
          .create();
  private final ChronicleMap<LongValue, ProductBundleItem> productBundleItems =
      ChronicleMapBuilder.of(LongValue.class, ProductBundleItem.class)
          .constantValueSizeBySample(Values.newNativeReference(ProductBundleItem.class))
          .entries(20L)
          .create();
  private final ChronicleMap<LongValue, IntValue> productBundleCount =
      ChronicleMapBuilder.of(LongValue.class, IntValue.class).entries(20L).create();
  private final TestProductListener listener = new TestProductListener();
  private final ProductService service =
      new ProductService(
          listener,
          listener,
          listener,
          products,
          productBundles,
          productBundleItems,
          productBundleCount);

  @After
  public void tearDown() {
    listener.reset();
  }

  @Test
  public void close() {
    final ChronicleMap<LongValue, Product> products = mock(ChronicleMap.class);
    final ChronicleMap<LongValue, ProductBundle> productBundles = mock(ChronicleMap.class);
    final ChronicleMap<LongValue, ProductBundleItem> productBundleItems = mock(ChronicleMap.class);
    final ChronicleMap<LongValue, IntValue> productBundleCount = mock(ChronicleMap.class);
    final ProductService service =
        new ProductService(
            listener,
            listener,
            listener,
            products,
            productBundles,
            productBundleItems,
            productBundleCount);
    service.close();
    verify(products).close();
    verify(productBundles).close();
    verify(productBundleItems).close();
    verify(productBundleCount).close();
  }

  @Test
  public void sendCreateProductCommand() {
    service.sendCreateProductCommand(new CreateProductCommand(1L, 10.50D, "desc"));
    assertEquals(
        List.of(
            new ProductCreatedEvent(1L, 10.50D, "desc"),
            new DisplayProductCreatedEvent(
                new DisplayProductId(1L, ProductType.PRODUCT), 10.50D, "desc")),
        listener.getEvents());
  }

  @Test
  public void sendCreateProductCommandInvalidId() {
    service.sendCreateProductCommand(new CreateProductCommand(0L, 10.50D, "desc"));
    assertEquals(
        List.of(new ProductErrorEvent(0L, "Product id must be larger than 0")),
        listener.getEvents());
  }

  @Test
  public void sendCreateProductCommandNegativePrice() {
    service.sendCreateProductCommand(new CreateProductCommand(1L, -0.01D, "desc 1"));
    assertEquals(
        List.of(new ProductErrorEvent(1L, "Product price may not be negative")),
        listener.getEvents());
  }

  @Test
  public void sendCreateProductCommandDescriptionTooLong() {
    service.sendCreateProductCommand(
        new CreateProductCommand(1L, 10.50D, "a".repeat(Product.MAX_DESCRIPTION_LENGTH)));
    service.sendCreateProductCommand(
        new CreateProductCommand(2L, 10.5D, "a".repeat(Product.MAX_DESCRIPTION_LENGTH + 1)));
    assertEquals(
        List.of(
            new ProductCreatedEvent(1L, 10.50D, "a".repeat(Product.MAX_DESCRIPTION_LENGTH)),
            new DisplayProductCreatedEvent(
                new DisplayProductId(1L, ProductType.PRODUCT),
                10.50D,
                "a".repeat(Product.MAX_DESCRIPTION_LENGTH)),
            new ProductErrorEvent(2L, "Product description is too long")),
        listener.getEvents());
  }

  @Test
  public void sendCreateProductCommandExistingProduct() {
    service.sendCreateProductCommand(new CreateProductCommand(1L, 10.50D, "desc"));
    service.sendCreateProductCommand(new CreateProductCommand(1L, 20.50D, "other desc"));
    assertEquals(
        List.of(
            new ProductCreatedEvent(1L, 10.50D, "desc"),
            new DisplayProductCreatedEvent(
                new DisplayProductId(1L, ProductType.PRODUCT), 10.50D, "desc"),
            new ProductErrorEvent(1L, "Product already exists")),
        listener.getEvents());
  }

  @Test
  public void sendUpdateProductCommand() {
    service.sendCreateProductCommand(new CreateProductCommand(1L, 10.50D, "desc"));
    service.sendUpdateProductCommand(new UpdateProductCommand(1L, 20.50D, "new desc"));
    assertEquals(
        List.of(
            new ProductCreatedEvent(1L, 10.50D, "desc"),
                new DisplayProductCreatedEvent(
                    new DisplayProductId(1L, ProductType.PRODUCT), 10.50D, "desc"),
            new ProductUpdatedEvent(1L, 20.50D, "new desc"),
                new DisplayProductUpdatedEvent(
                    new DisplayProductId(1L, ProductType.PRODUCT), 20.50D, "new desc")),
        listener.getEvents());
  }

  @Test
  public void sendUpdateProductCommandMatchingData() {
    service.sendCreateProductCommand(new CreateProductCommand(1L, 10.50D, "desc"));
    service.sendUpdateProductCommand(new UpdateProductCommand(1L, 10.50D, "desc"));
    assertEquals(
        List.of(
            new ProductCreatedEvent(1L, 10.50D, "desc"),
            new DisplayProductCreatedEvent(
                new DisplayProductId(1L, ProductType.PRODUCT), 10.50D, "desc")),
        listener.getEvents());
  }

  @Test
  public void sendUpdateProductCommandWrongId() {
    service.sendCreateProductCommand(new CreateProductCommand(1L, 10.50D, "desc"));
    service.sendUpdateProductCommand(new UpdateProductCommand(2L, 20.50D, "new desc"));
    assertEquals(
        List.of(
            new ProductCreatedEvent(1L, 10.50D, "desc"),
            new DisplayProductCreatedEvent(
                new DisplayProductId(1L, ProductType.PRODUCT), 10.50D, "desc"),
            new ProductErrorEvent(2L, "Product does not exist")),
        listener.getEvents());
  }

  @Test
  public void sendUpdateProductCommandNegativePrice() {
    service.sendCreateProductCommand(new CreateProductCommand(1L, 10.50D, "desc"));
    service.sendUpdateProductCommand(new UpdateProductCommand(1L, -0.01D, "new desc"));
    assertEquals(
        List.of(
            new ProductCreatedEvent(1L, 10.50D, "desc"),
            new DisplayProductCreatedEvent(
                new DisplayProductId(1L, ProductType.PRODUCT), 10.50D, "desc"),
            new ProductErrorEvent(1L, "Product price may not be negative")),
        listener.getEvents());
  }

  @Test
  public void sendUpdateProductCommandDescriptionTooLong() {
    service.sendCreateProductCommand(
        new CreateProductCommand(1L, 10.50D, "a".repeat(Product.MAX_DESCRIPTION_LENGTH)));
    service.sendUpdateProductCommand(
        new UpdateProductCommand(1L, 10.5D, "a".repeat(Product.MAX_DESCRIPTION_LENGTH + 1)));
    assertEquals(
        List.of(
            new ProductCreatedEvent(1L, 10.50D, "a".repeat(Product.MAX_DESCRIPTION_LENGTH)),
            new DisplayProductCreatedEvent(
                new DisplayProductId(1L, ProductType.PRODUCT),
                10.50D,
                "a".repeat(Product.MAX_DESCRIPTION_LENGTH)),
            new ProductErrorEvent(1L, "Product description is too long")),
        listener.getEvents());
  }

  @Test
  public void sendUpdateProductCommandNoCreate() {
    service.sendUpdateProductCommand(new UpdateProductCommand(1L, 20.50D, "new desc"));
    assertEquals(
        List.of(new ProductErrorEvent(1L, "Product does not exist")), listener.getEvents());
  }

  @Test
  public void sendDeleteProductCommand() {
    service.sendCreateProductCommand(new CreateProductCommand(1L, 10.50D, "desc"));
    service.sendDeleteProductCommand(new DeleteProductCommand(1L));
    assertEquals(
        List.of(
            new ProductCreatedEvent(1L, 10.50D, "desc"),
            new DisplayProductCreatedEvent(
                new DisplayProductId(1L, ProductType.PRODUCT), 10.50D, "desc"),
            new ProductDeletedEvent(1L),
            new DisplayProductDeletedEvent(new DisplayProductId(1L, ProductType.PRODUCT))),
        listener.getEvents());
  }

  @Test
  public void sendDeleteProductCommandWrongId() {
    service.sendCreateProductCommand(new CreateProductCommand(1L, 10.50D, "desc"));
    service.sendDeleteProductCommand(new DeleteProductCommand(2L));
    assertEquals(
        List.of(
            new ProductCreatedEvent(1L, 10.50D, "desc"),
            new DisplayProductCreatedEvent(
                new DisplayProductId(1L, ProductType.PRODUCT), 10.50D, "desc"),
            new ProductErrorEvent(2L, "Product does not exist")),
        listener.getEvents());
  }

  @Test
  public void sendDeleteProductCommandNoCreate() {
    service.sendDeleteProductCommand(new DeleteProductCommand(1L));
    assertEquals(
        List.of(new ProductErrorEvent(1L, "Product does not exist")), listener.getEvents());
  }

  @Test
  public void sendDeleteProductCommandInBundle() {
    service.sendCreateProductCommand(new CreateProductCommand(1L, 10.50D, "desc"));
    service.sendCreateProductBundleCommand(
        new CreateProductBundleCommand(
            1L,
            new BundleItem[] {new BundleItem(1L, 1L, new PriceDetail(10.5D, PriceType.PRICE))},
            "bundle desc"));
    service.sendDeleteProductCommand(new DeleteProductCommand(1L));

    assertEquals(
        List.of(
            new ProductCreatedEvent(1L, 10.50D, "desc"),
            new DisplayProductCreatedEvent(
                new DisplayProductId(1L, ProductType.PRODUCT), 10.50D, "desc"),
            new ProductBundleCreatedEvent(
                1L,
                new BundleItem[] {new BundleItem(1L, 1L, new PriceDetail(10.5D, PriceType.PRICE))},
                "bundle desc"),
            new DisplayProductCreatedEvent(
                new DisplayProductId(1L, ProductType.BUNDLE), 10.50D, "bundle desc"),
            new ProductErrorEvent(1L, "Cannot remove product which is part of a bundle")),
        listener.getEvents());
  }

  @Test
  public void sendCreateProductBundleCommand() {
    service.sendCreateProductCommand(new CreateProductCommand(1L, 10.50D, "desc 1"));
    service.sendCreateProductCommand(new CreateProductCommand(2L, 30.0D, "desc 2"));
    service.sendCreateProductCommand(new CreateProductCommand(3L, 11.0D, "desc 3"));
    service.sendCreateProductBundleCommand(
        new CreateProductBundleCommand(
            1L,
            new BundleItem[] {
              new BundleItem(1L, 1L, new PriceDetail(7.0D, PriceType.PRICE)),
              new BundleItem(2L, 2L, new PriceDetail(50.0D, PriceType.PERCENTAGE)),
              new BundleItem(3L, 3L, new PriceDetail(2.0D, PriceType.DISCOUNT))
            },
            "bundle desc"));
    assertEquals(
        List.of(
            new ProductCreatedEvent(1L, 10.50D, "desc 1"),
            new DisplayProductCreatedEvent(
                new DisplayProductId(1L, ProductType.PRODUCT), 10.50D, "desc 1"),
            new ProductCreatedEvent(2L, 30.0D, "desc 2"),
            new DisplayProductCreatedEvent(
                new DisplayProductId(2L, ProductType.PRODUCT), 30.0D, "desc 2"),
            new ProductCreatedEvent(3L, 11.0D, "desc 3"),
            new DisplayProductCreatedEvent(
                new DisplayProductId(3L, ProductType.PRODUCT), 11.0D, "desc 3"),
            new ProductBundleCreatedEvent(
                1L,
                new BundleItem[] {
                  new BundleItem(1L, 1L, new PriceDetail(7.0D, PriceType.PRICE)),
                  new BundleItem(2L, 2L, new PriceDetail(50.0D, PriceType.PERCENTAGE)),
                  new BundleItem(3L, 3L, new PriceDetail(2.0D, PriceType.DISCOUNT))
                },
                "bundle desc"),
            new DisplayProductCreatedEvent(
                new DisplayProductId(1L, ProductType.BUNDLE), 31.0D, "bundle desc")),
        listener.getEvents());
  }

  @Test
  public void sendCreateProductBundleCommandInvalidId() {
    service.sendCreateProductCommand(new CreateProductCommand(1L, 10.50D, "desc 1"));
    service.sendCreateProductBundleCommand(
        new CreateProductBundleCommand(
            0L,
            new BundleItem[] {new BundleItem(1L, 1L, new PriceDetail(11.0D, PriceType.PRICE))},
            "bundle desc"));
    assertEquals(
        List.of(
            new ProductCreatedEvent(1L, 10.50D, "desc 1"),
            new DisplayProductCreatedEvent(
                new DisplayProductId(1L, ProductType.PRODUCT), 10.50D, "desc 1"),
            new ProductBundleErrorEvent(0L, "Bundle id must be larger than 0")),
        listener.getEvents());
  }

  @Test
  public void sendCreateProductBundleCommandNoPrice() {
    service.sendCreateProductCommand(new CreateProductCommand(1L, 10.50D, "desc 1"));
    service.sendCreateProductBundleCommand(
        new CreateProductBundleCommand(
            1L, new BundleItem[] {new BundleItem(1L, 1L, null)}, "bundle desc"));
    assertEquals(
        List.of(
            new ProductCreatedEvent(1L, 10.50D, "desc 1"),
            new DisplayProductCreatedEvent(
                new DisplayProductId(1L, ProductType.PRODUCT), 10.50D, "desc 1"),
            new ProductBundleErrorEvent(1L, "Bundle price may not be null")),
        listener.getEvents());
  }

  @Test
  public void sendCreateProductBundleCommandNoPriceType() {
    service.sendCreateProductCommand(new CreateProductCommand(1L, 10.50D, "desc 1"));
    service.sendCreateProductBundleCommand(
        new CreateProductBundleCommand(
            1L,
            new BundleItem[] {new BundleItem(1L, 1L, new PriceDetail(11.0D, null))},
            "bundle desc"));
    assertEquals(
        List.of(
            new ProductCreatedEvent(1L, 10.50D, "desc 1"),
            new DisplayProductCreatedEvent(
                new DisplayProductId(1L, ProductType.PRODUCT), 10.50D, "desc 1"),
            new ProductBundleErrorEvent(1L, "Bundle price may not be null")),
        listener.getEvents());
  }

  @Test
  public void sendCreateProductBundleCommandNoProducts() {
    service.sendCreateProductCommand(new CreateProductCommand(1L, 10.50D, "desc 1"));
    service.sendCreateProductBundleCommand(
        new CreateProductBundleCommand(1L, new BundleItem[] {}, "bundle desc 1"));
    service.sendCreateProductBundleCommand(
        new CreateProductBundleCommand(2L, null, "bundle desc 2"));
    assertEquals(
        List.of(
            new ProductCreatedEvent(1L, 10.50D, "desc 1"),
            new DisplayProductCreatedEvent(
                new DisplayProductId(1L, ProductType.PRODUCT), 10.50D, "desc 1"),
            new ProductBundleErrorEvent(1L, "Bundle must provide a product"),
            new ProductBundleErrorEvent(2L, "Bundle must provide a product")),
        listener.getEvents());
  }

  @Test
  public void sendCreateProductBundleCommandExistingBundle() {
    service.sendCreateProductCommand(new CreateProductCommand(1L, 10.50D, "desc 1"));
    service.sendCreateProductBundleCommand(
        new CreateProductBundleCommand(
            1L,
            new BundleItem[] {
              new BundleItem(1L, 1L, new PriceDetail(50D, PriceType.PRICE)),
              new BundleItem(1L, 1L, new PriceDetail(50D, PriceType.PRICE))
            },
            "bundle desc"));
    assertEquals(
        List.of(
            new ProductCreatedEvent(1L, 10.50D, "desc 1"),
            new DisplayProductCreatedEvent(
                new DisplayProductId(1L, ProductType.PRODUCT), 10.50D, "desc 1"),
            new ProductBundleErrorEvent(1L, "Bundle item already exists")),
        listener.getEvents());
  }

  @Test
  public void sendCreateProductBundleCommandTooManyProducts() {
    service.sendCreateProductCommand(new CreateProductCommand(1L, 10.50D, "desc 1"));
    service.sendCreateProductBundleCommand(
        new CreateProductBundleCommand(
            1L,
            LongStream.range(0, ProductBundle.MAX_ITEMS_PER_BUNDLE + 1)
                .mapToObj(it -> new BundleItem(1L, 1L, new PriceDetail(50D, PriceType.PRICE)))
                .toArray(BundleItem[]::new),
            "bundle desc"));
    assertEquals(
        List.of(
            new ProductCreatedEvent(1L, 10.50D, "desc 1"),
            new DisplayProductCreatedEvent(
                new DisplayProductId(1L, ProductType.PRODUCT), 10.50D, "desc 1"),
            new ProductBundleErrorEvent(1L, "Bundle has too many products")),
        listener.getEvents());
  }

  @Test
  public void sendCreateProductBundleCommandNegativePrice() {
    service.sendCreateProductCommand(new CreateProductCommand(1L, 10.50D, "desc 1"));
    service.sendCreateProductBundleCommand(
        new CreateProductBundleCommand(
            1L,
            new BundleItem[] {new BundleItem(1L, 1L, new PriceDetail(-0.01D, PriceType.PRICE))},
            "bundle desc"));
    assertEquals(
        List.of(
            new ProductCreatedEvent(1L, 10.50D, "desc 1"),
            new DisplayProductCreatedEvent(
                new DisplayProductId(1L, ProductType.PRODUCT), 10.50D, "desc 1"),
            new ProductBundleErrorEvent(1L, "Bundle price may not be negative")),
        listener.getEvents());
  }

  @Test
  public void sendCreateProductBundleCommandBundleExists() {
    service.sendCreateProductCommand(new CreateProductCommand(1L, 10.50D, "desc 1"));
    service.sendCreateProductCommand(new CreateProductCommand(2L, 3.0D, "desc 2"));
    service.sendCreateProductBundleCommand(
        new CreateProductBundleCommand(
            1L,
            new BundleItem[] {new BundleItem(1L, 1L, new PriceDetail(10.0D, PriceType.PRICE))},
            "bundle desc 1"));
    service.sendCreateProductBundleCommand(
        new CreateProductBundleCommand(
            1L,
            new BundleItem[] {new BundleItem(2L, 2L, new PriceDetail(3.0D, PriceType.PRICE))},
            "bundle desc 2"));
    assertEquals(
        List.of(
            new ProductCreatedEvent(1L, 10.50D, "desc 1"),
            new DisplayProductCreatedEvent(
                new DisplayProductId(1L, ProductType.PRODUCT), 10.50D, "desc 1"),
            new ProductCreatedEvent(2L, 3.0D, "desc 2"),
            new DisplayProductCreatedEvent(
                new DisplayProductId(2L, ProductType.PRODUCT), 3.0D, "desc 2"),
            new ProductBundleCreatedEvent(
                1L,
                new BundleItem[] {new BundleItem(1L, 1L, new PriceDetail(10.0D, PriceType.PRICE))},
                "bundle desc 1"),
            new DisplayProductCreatedEvent(
                new DisplayProductId(1L, ProductType.BUNDLE), 10.0D, "bundle desc 1"),
            new ProductBundleErrorEvent(1L, "Bundle already exists")),
        listener.getEvents());
  }

  @Test
  public void sendCreateProductBundleCommandDescriptionTooLong() {
    service.sendCreateProductCommand(new CreateProductCommand(1L, 10.50D, "desc 1"));
    service.sendCreateProductBundleCommand(
        new CreateProductBundleCommand(
            1L,
            new BundleItem[] {new BundleItem(1L, 1L, new PriceDetail(10.0D, PriceType.PRICE))},
            "a".repeat(ProductBundle.MAX_DESCRIPTION_LENGTH)));
    service.sendCreateProductBundleCommand(
        new CreateProductBundleCommand(
            2L,
            new BundleItem[] {new BundleItem(2L, 1L, new PriceDetail(10.0D, PriceType.PRICE))},
            "b".repeat(ProductBundle.MAX_DESCRIPTION_LENGTH + 1)));
    assertEquals(
        List.of(
            new ProductCreatedEvent(1L, 10.50D, "desc 1"),
            new DisplayProductCreatedEvent(
                new DisplayProductId(1L, ProductType.PRODUCT), 10.50D, "desc 1"),
            new ProductBundleCreatedEvent(
                1L,
                new BundleItem[] {new BundleItem(1L, 1L, new PriceDetail(10.0D, PriceType.PRICE))},
                "a".repeat(ProductBundle.MAX_DESCRIPTION_LENGTH)),
            new DisplayProductCreatedEvent(
                new DisplayProductId(1L, ProductType.BUNDLE),
                10.00D,
                "a".repeat(ProductBundle.MAX_DESCRIPTION_LENGTH)),
            new ProductBundleErrorEvent(2L, "Bundle description is too long")),
        listener.getEvents());
  }

  @Test
  public void sendCreateProductBundleCommandInvalidProduct() {
    service.sendCreateProductBundleCommand(
        new CreateProductBundleCommand(
            1L,
            new BundleItem[] {new BundleItem(1L, 1L, new PriceDetail(10.0D, PriceType.PRICE))},
            "bundle desc"));
    assertEquals(
        List.of(new ProductBundleErrorEvent(1L, "Product does not exist")), listener.getEvents());
  }

  @Test
  public void sendDeleteProductBundleCommand() {
    service.sendCreateProductCommand(new CreateProductCommand(1L, 10.50D, "desc 1"));
    service.sendCreateProductBundleCommand(
        new CreateProductBundleCommand(
            1L,
            new BundleItem[] {new BundleItem(1L, 1L, new PriceDetail(10.0D, PriceType.PRICE))},
            "bundle desc 1"));
    service.sendCreateProductBundleCommand(
        new CreateProductBundleCommand(
            2L,
            new BundleItem[] {new BundleItem(2L, 1L, new PriceDetail(5.0D, PriceType.PRICE))},
            "bundle desc 2"));
    service.sendDeleteProductBundleCommand(new DeleteProductBundleCommand(1L));
    service.sendDeleteProductBundleCommand(new DeleteProductBundleCommand(2L));
    service.sendDeleteProductCommand(new DeleteProductCommand(1L));
    assertEquals(
        List.of(
            new ProductCreatedEvent(1L, 10.50D, "desc 1"),
            new DisplayProductCreatedEvent(
                new DisplayProductId(1L, ProductType.PRODUCT), 10.50D, "desc 1"),
            new ProductBundleCreatedEvent(
                1L,
                new BundleItem[] {new BundleItem(1L, 1L, new PriceDetail(10.0D, PriceType.PRICE))},
                "bundle desc 1"),
            new DisplayProductCreatedEvent(
                new DisplayProductId(1L, ProductType.BUNDLE), 10.0D, "bundle desc 1"),
            new ProductBundleCreatedEvent(
                2L,
                new BundleItem[] {new BundleItem(2L, 1L, new PriceDetail(5.0D, PriceType.PRICE))},
                "bundle desc 2"),
            new DisplayProductCreatedEvent(
                new DisplayProductId(2L, ProductType.BUNDLE), 5.0D, "bundle desc 2"),
            new ProductBundleDeletedEvent(1L),
            new DisplayProductDeletedEvent(new DisplayProductId(1L, ProductType.BUNDLE)),
            new ProductBundleDeletedEvent(2L),
            new DisplayProductDeletedEvent(new DisplayProductId(2L, ProductType.BUNDLE)),
            new ProductDeletedEvent(1L),
            new DisplayProductDeletedEvent(new DisplayProductId(1L, ProductType.PRODUCT))),
        listener.getEvents());
  }

  @Test
  public void sendDeleteProductBundleCommandRemovesProductMapping() {
    service.sendCreateProductCommand(new CreateProductCommand(1L, 10.50D, "desc 1"));
    List<Object> expected = new ArrayList<>();
    expected.add(new ProductCreatedEvent(1L, 10.50D, "desc 1"));
    expected.add(
        new DisplayProductCreatedEvent(
            new DisplayProductId(1L, ProductType.PRODUCT), 10.50D, "desc 1"));
    for (int i = 0; i < 2 + 1; i++) {
      service.sendCreateProductBundleCommand(
          new CreateProductBundleCommand(
              1L,
              new BundleItem[] {new BundleItem(1L, 1L, new PriceDetail(10.0D, PriceType.PRICE))},
              "bundle desc"));
      service.sendDeleteProductBundleCommand(new DeleteProductBundleCommand(1L));
      expected.add(
          new ProductBundleCreatedEvent(
              1L,
              new BundleItem[] {new BundleItem(1L, 1L, new PriceDetail(10.0D, PriceType.PRICE))},
              "bundle desc"));
      expected.add(
          new DisplayProductCreatedEvent(
              new DisplayProductId(1L, ProductType.BUNDLE), 10.0D, "bundle desc"));
      expected.add(new ProductBundleDeletedEvent(1L));
      expected.add(new DisplayProductDeletedEvent(new DisplayProductId(1L, ProductType.BUNDLE)));
    }
    service.sendDeleteProductCommand(new DeleteProductCommand(1L));
    expected.add(new ProductDeletedEvent(1L));
    expected.add(new DisplayProductDeletedEvent(new DisplayProductId(1L, ProductType.PRODUCT)));
    assertEquals(expected, listener.getEvents());
  }

  @Test
  public void sendDeleteProductBundleCommandNoBundle() {
    service.sendDeleteProductBundleCommand(new DeleteProductBundleCommand(1L));
    assertEquals(
        List.of(new ProductBundleErrorEvent(1L, "Bundle does not exist")), listener.getEvents());
  }
}
