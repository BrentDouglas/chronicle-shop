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
import java.util.stream.LongStream;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

/** @author <a href="mailto:brent.n.douglas@gmail.com">Brent Douglas</a> */
@State(Scope.Benchmark)
public class LargerBench extends BaseBench {

  public static void main(String... args) throws Exception {
    run(LargerBench.class);
  }

  private static final CreateProductCommand[] CREATE_PRODUCT_COMMANDS =
      LongStream.range(1L, 1001L)
          .mapToObj(it -> new CreateProductCommand(it, 3D, "created"))
          .toArray(CreateProductCommand[]::new);
  private static final UpdateProductCommand[] UPDATE_PRODUCT_COMMANDS =
      LongStream.range(1L, 1001L)
          .mapToObj(it -> new UpdateProductCommand(it, 8D, "updated"))
          .toArray(UpdateProductCommand[]::new);
  private static final CreateProductBundleCommand[] CREATE_PRODUCT_BUNDLE_COMMANDS =
      LongStream.range(1L, 1001L)
          .mapToObj(
              it ->
                  new CreateProductBundleCommand(
                      it,
                      new BundleItem[] {
                        new BundleItem(it, it, new PriceDetail(20D, PriceType.PERCENTAGE))
                      },
                      "bundle"))
          .toArray(CreateProductBundleCommand[]::new);
  private static final DeleteProductBundleCommand[] DELETE_PRODUCT_BUNDLE_COMMANDS =
      LongStream.range(1L, 1001L)
          .mapToObj(it -> new DeleteProductBundleCommand(it))
          .toArray(DeleteProductBundleCommand[]::new);
  private static final DeleteProductCommand[] DELETE_PRODUCT_COMMANDS =
      LongStream.range(1L, 1001L)
          .mapToObj(it -> new DeleteProductCommand(it))
          .toArray(DeleteProductCommand[]::new);
  private static final AddProductToCartCommand[] ADD_PRODUCT_TO_CART_COMMANDS =
      LongStream.range(1L, 1001L)
          .mapToObj(
              it ->
                  new AddProductToCartCommand(it, new DisplayProductId(it, ProductType.PRODUCT), 7))
          .toArray(AddProductToCartCommand[]::new);
  private static final AddProductToCartCommand[] ADD_BUNDLE_TO_CART_COMMANDS =
      LongStream.range(1L, 1001L)
          .mapToObj(
              it ->
                  new AddProductToCartCommand(it, new DisplayProductId(it, ProductType.BUNDLE), 9))
          .toArray(AddProductToCartCommand[]::new);
  private static final UpdateProductQuantityInCartCommand[]
      UPDATE_BUNDLE_QUANTITY_IN_CART_COMMANDS =
          LongStream.range(1L, 1001L)
              .mapToObj(
                  it ->
                      new UpdateProductQuantityInCartCommand(
                          it, new DisplayProductId(it, ProductType.BUNDLE), 6))
              .toArray(UpdateProductQuantityInCartCommand[]::new);
  private static final CalculateTotalForCartCommand[] CALCULATE_TOTAL_FOR_CART_COMMANDS =
      LongStream.range(1L, 1001L)
          .mapToObj(it -> new CalculateTotalForCartCommand(it))
          .toArray(CalculateTotalForCartCommand[]::new);
  private static final RemoveProductFromCartCommand[] REMOVE_PRODUCT_FROM_CART_COMMANDS =
      LongStream.range(1L, 1001L)
          .mapToObj(
              it ->
                  new RemoveProductFromCartCommand(
                      it, new DisplayProductId(it, ProductType.PRODUCT)))
          .toArray(RemoveProductFromCartCommand[]::new);
  private static final RemoveProductFromCartCommand[] REMOVE_PRODUCT_FROM_CART_COMMAND =
      LongStream.range(1L, 1001L)
          .mapToObj(
              it ->
                  new RemoveProductFromCartCommand(
                      it, new DisplayProductId(it, ProductType.BUNDLE)))
          .toArray(RemoveProductFromCartCommand[]::new);

  long iteration = 1L;

  @Setup(Level.Trial)
  public void setUp() {
    setUp(LargerBench.class);
  }

  @TearDown(Level.Trial)
  public void tearDown() {
    tearDown(LargerBench.class);
  }

  @Benchmark
  public long doBench() {
    sendCommands();
    return iteration++;
  }

  public static void sendCommands() {
    for (int i = 0; i < CREATE_PRODUCT_COMMANDS.length; i++) {
      productDispatcher.sendCreateProductCommand(CREATE_PRODUCT_COMMANDS[i]);
    }
    for (int i = 0; i < UPDATE_PRODUCT_COMMANDS.length; i++) {
      productDispatcher.sendUpdateProductCommand(UPDATE_PRODUCT_COMMANDS[i]);
    }
    for (int i = 0; i < CREATE_PRODUCT_BUNDLE_COMMANDS.length; i++) {
      productDispatcher.sendCreateProductBundleCommand(CREATE_PRODUCT_BUNDLE_COMMANDS[i]);
    }

    for (int i = 0; i < ADD_PRODUCT_TO_CART_COMMANDS.length; i++) {
      cartDispatcher.sendAddProductToCartCommand(ADD_PRODUCT_TO_CART_COMMANDS[i]);
    }
    for (int i = 0; i < ADD_BUNDLE_TO_CART_COMMANDS.length; i++) {
      cartDispatcher.sendAddProductToCartCommand(ADD_BUNDLE_TO_CART_COMMANDS[i]);
    }
    for (int i = 0; i < UPDATE_BUNDLE_QUANTITY_IN_CART_COMMANDS.length; i++) {
      cartDispatcher.sendUpdateProductQuantityInCartCommand(
          UPDATE_BUNDLE_QUANTITY_IN_CART_COMMANDS[i]);
    }
    for (int i = 0; i < CALCULATE_TOTAL_FOR_CART_COMMANDS.length; i++) {
      cartDispatcher.sendCalculateTotalForCartCommand(CALCULATE_TOTAL_FOR_CART_COMMANDS[i]);
    }
    for (int i = 0; i < REMOVE_PRODUCT_FROM_CART_COMMANDS.length; i++) {
      cartDispatcher.sendRemoveProductFromCartCommand(REMOVE_PRODUCT_FROM_CART_COMMANDS[i]);
    }
    for (int i = 0; i < REMOVE_PRODUCT_FROM_CART_COMMAND.length; i++) {
      cartDispatcher.sendRemoveProductFromCartCommand(REMOVE_PRODUCT_FROM_CART_COMMAND[i]);
    }

    for (int i = 0; i < DELETE_PRODUCT_BUNDLE_COMMANDS.length; i++) {
      productDispatcher.sendDeleteProductBundleCommand(DELETE_PRODUCT_BUNDLE_COMMANDS[i]);
    }
    for (int i = 0; i < DELETE_PRODUCT_COMMANDS.length; i++) {
      productDispatcher.sendDeleteProductCommand(DELETE_PRODUCT_COMMANDS[i]);
    }
  }
}
