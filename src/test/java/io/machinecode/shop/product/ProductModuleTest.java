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

import static org.junit.Assert.assertEquals;

import io.machinecode.shop.product.command.CreateProductBundleCommand;
import io.machinecode.shop.product.command.CreateProductCommand;
import io.machinecode.shop.product.event.DisplayProductCreatedEvent;
import io.machinecode.shop.product.event.ProductBundleCreatedEvent;
import io.machinecode.shop.product.event.ProductCreatedEvent;
import io.machinecode.shop.product.model.BundleItem;
import io.machinecode.shop.product.model.DisplayProductId;
import io.machinecode.shop.product.model.PriceDetail;
import io.machinecode.shop.product.model.PriceType;
import io.machinecode.shop.product.model.ProductType;
import io.machinecode.shop.product.support.TestProductListener;
import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.queue.ChronicleQueue;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/** @author <a href="mailto:brent.n.douglas@gmail.com">Brent Douglas</a> */
public class ProductModuleTest {
  private static final File DIR =
      Paths.get(System.getenv("TEST_TMPDIR"), ProductModuleTest.class.getSimpleName()).toFile();
  private static ProductModule module;
  private static ChronicleQueue productInput;
  private static ChronicleQueue productOutput;

  private final TestProductListener listener = new TestProductListener();

  @BeforeClass
  public static void setUpAll() {
    DIR.mkdir();
    DIR.deleteOnExit();
    productInput =
        ChronicleQueue.singleBuilder(Paths.get(DIR.getAbsolutePath(), "product-commands")).build();
    productOutput =
        ChronicleQueue.singleBuilder(Paths.get(DIR.getAbsolutePath(), "product-events")).build();
    module = new ProductModule(10L, productInput, productOutput);
    module.getDispatcher().sendCreateProductCommand(new CreateProductCommand(1L, 1D, "product"));
    module
        .getDispatcher()
        .sendCreateProductBundleCommand(
            new CreateProductBundleCommand(
                1L,
                new BundleItem[] {
                  new BundleItem(1L, 1L, new PriceDetail(50D, PriceType.PERCENTAGE))
                },
                "bundle"));
    while (module.getReader().readOne())
      ;
  }

  @AfterClass
  public static void tearDownAll() {
    productInput.close();
    productOutput.close();
    module.close();
    for (File file : DIR.listFiles()) {
      file.delete();
    }
  }

  @After
  public void tearDown() {
    listener.reset();
  }

  @Test
  public void productListenerWorks() {
    MethodReader reader = module.addProductListener(listener);
    runTest(reader);
  }

  @Test
  public void productBundleListenerWorks() {
    MethodReader reader = module.addProductBundleListener(listener);
    runTest(reader);
  }

  @Test
  public void displayProductListenerWorks() {
    MethodReader reader = module.addDisplayProductListener(listener);
    runTest(reader);
  }

  public void runTest(MethodReader reader) {
    while (reader.readOne())
      ;
    assertEquals(
        List.of(
            new ProductCreatedEvent(1L, 1D, "product"),
            new DisplayProductCreatedEvent(
                new DisplayProductId(1L, ProductType.PRODUCT), 1D, "product"),
            new ProductBundleCreatedEvent(
                1L,
                new BundleItem[] {
                  new BundleItem(1L, 1L, new PriceDetail(50D, PriceType.PERCENTAGE))
                },
                "bundle"),
            new DisplayProductCreatedEvent(
                new DisplayProductId(1L, ProductType.BUNDLE), 0.5D, "bundle")),
        listener.getEvents());
  }
}
