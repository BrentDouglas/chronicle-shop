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
package io.machinecode.shop.bench;

import io.machinecode.shop.bench.support.BaseBench;
import io.machinecode.shop.cart.command.AddProductToCartCommand;
import io.machinecode.shop.cart.command.CalculateTotalForCartCommand;
import io.machinecode.shop.cart.command.RemoveProductFromCartCommand;
import io.machinecode.shop.cart.command.UpdateProductQuantityInCartCommand;
import io.machinecode.shop.product.command.CreateProductBundleCommand;
import io.machinecode.shop.product.command.CreateProductCommand;
import io.machinecode.shop.product.command.DeleteProductBundleCommand;
import io.machinecode.shop.product.command.DeleteProductCommand;
import io.machinecode.shop.product.command.UpdateProductCommand;
import io.machinecode.shop.product.model.BundleItem;
import io.machinecode.shop.product.model.DisplayProductId;
import io.machinecode.shop.product.model.PriceDetail;
import io.machinecode.shop.product.model.PriceType;
import io.machinecode.shop.product.model.ProductType;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

/** @author <a href="mailto:brent.n.douglas@gmail.com">Brent Douglas</a> */
@State(Scope.Benchmark)
public class SingleBench extends BaseBench {

  public static void main(String... args) throws Exception {
    run(SingleBench.class);
  }

  private static final CreateProductCommand createProductCommand =
      new CreateProductCommand(1L, 3D, "created");
  private static final UpdateProductCommand updateProductCommand =
      new UpdateProductCommand(1L, 8D, "updated");
  private static final CreateProductBundleCommand createProductBundleCommand =
      new CreateProductBundleCommand(
          1L,
          new BundleItem[] {new BundleItem(1L, 1L, new PriceDetail(20D, PriceType.PERCENTAGE))},
          "bundle");
  private static final DeleteProductBundleCommand deleteProductBundleCommand =
      new DeleteProductBundleCommand(1L);
  private static final DeleteProductCommand deleteProductCommand = new DeleteProductCommand(1L);
  private static final AddProductToCartCommand addProductToCartCommand =
      new AddProductToCartCommand(1L, new DisplayProductId(1L, ProductType.PRODUCT), 7);
  private static final AddProductToCartCommand addBundleToCartCommand =
      new AddProductToCartCommand(1L, new DisplayProductId(1L, ProductType.BUNDLE), 9);
  private static final UpdateProductQuantityInCartCommand updateBundleQuantityInCartCommand =
      new UpdateProductQuantityInCartCommand(1L, new DisplayProductId(1L, ProductType.BUNDLE), 6);
  private static final CalculateTotalForCartCommand calculateTotalForCartCommand =
      new CalculateTotalForCartCommand(1L);
  private static final RemoveProductFromCartCommand removeProductFromCartCommand =
      new RemoveProductFromCartCommand(1L, new DisplayProductId(1L, ProductType.PRODUCT));
  private static final RemoveProductFromCartCommand removeBundleFromCartCommand =
      new RemoveProductFromCartCommand(1L, new DisplayProductId(1L, ProductType.BUNDLE));

  long iteration = 0L;

  @Setup(Level.Trial)
  public void setUp() {
    setUp(SingleBench.class);
  }

  @TearDown(Level.Trial)
  public void tearDown() {
    tearDown(SingleBench.class);
  }

  @Benchmark
  public long doBench() {
    sendCommands();
    return iteration++;
  }

  public static void sendCommands() {
    productDispatcher.sendCreateProductCommand(createProductCommand);
    productDispatcher.sendUpdateProductCommand(updateProductCommand);
    productDispatcher.sendCreateProductBundleCommand(createProductBundleCommand);

    cartDispatcher.sendAddProductToCartCommand(addProductToCartCommand);
    cartDispatcher.sendAddProductToCartCommand(addBundleToCartCommand);
    cartDispatcher.sendUpdateProductQuantityInCartCommand(updateBundleQuantityInCartCommand);
    cartDispatcher.sendCalculateTotalForCartCommand(calculateTotalForCartCommand);
    cartDispatcher.sendRemoveProductFromCartCommand(removeProductFromCartCommand);
    cartDispatcher.sendRemoveProductFromCartCommand(removeBundleFromCartCommand);

    productDispatcher.sendDeleteProductBundleCommand(deleteProductBundleCommand);
    productDispatcher.sendDeleteProductCommand(deleteProductCommand);
  }
}
