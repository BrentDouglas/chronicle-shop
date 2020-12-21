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

import static org.junit.Assert.assertEquals;

import io.machinecode.shop.cart.command.AddProductToCartCommand;
import io.machinecode.shop.cart.event.CartProductAddedEvent;
import io.machinecode.shop.cart.support.TestCartListener;
import io.machinecode.shop.product.api.DisplayProductListener;
import io.machinecode.shop.product.event.DisplayProductCreatedEvent;
import io.machinecode.shop.product.model.DisplayProductId;
import io.machinecode.shop.product.model.ProductType;
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
public class CartModuleTest {
  private static final File DIR =
      Paths.get(System.getenv("TEST_TMPDIR"), CartModuleTest.class.getSimpleName()).toFile();
  private static CartModule module;
  private static ChronicleQueue cartInput;
  private static ChronicleQueue cartOutput;
  private static ChronicleQueue productOutput;

  private final TestCartListener listener = new TestCartListener();

  @BeforeClass
  public static void setUpAll() {
    DIR.mkdir();
    DIR.deleteOnExit();
    cartInput =
        ChronicleQueue.singleBuilder(Paths.get(DIR.getAbsolutePath(), "cart-commands")).build();
    cartOutput =
        ChronicleQueue.singleBuilder(Paths.get(DIR.getAbsolutePath(), "cart-events")).build();
    productOutput =
        ChronicleQueue.singleBuilder(Paths.get(DIR.getAbsolutePath(), "product-events")).build();
    module = new CartModule(10L, cartInput, productOutput, cartOutput);
    DisplayProductListener productListener =
        productOutput.acquireAppender().methodWriter(DisplayProductListener.class);
    productListener.onDisplayProductCreated(
        new DisplayProductCreatedEvent(
            new DisplayProductId(1L, ProductType.PRODUCT), 1D, "product"));
    for (final MethodReader reader : module.getReaders()) {
      while (reader.readOne())
        ;
    }
    module
        .getDispatcher()
        .sendAddProductToCartCommand(
            new AddProductToCartCommand(1L, new DisplayProductId(1L, ProductType.PRODUCT), 3));
  }

  @AfterClass
  public static void tearDownAll() {
    cartInput.close();
    cartOutput.close();
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
  public void cartListener() {
    MethodReader reader = module.addListener(listener);
    runTest(reader);
  }

  public void runTest(MethodReader reader) {
    while (reader.readOne())
      ;
    assertEquals(
        List.of(new CartProductAddedEvent(1L, new DisplayProductId(1L, ProductType.PRODUCT), 3)),
        listener.getEvents());
  }
}
