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
package io.machinecode.shop.product.support;

import io.machinecode.shop.product.api.DisplayProductListener;
import io.machinecode.shop.product.api.ProductBundleListener;
import io.machinecode.shop.product.api.ProductListener;
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
import java.util.ArrayList;
import java.util.List;

/** @author <a href="mailto:brent.n.douglas@gmail.com">Brent Douglas</a> */
public class TestProductListener
    implements ProductListener, ProductBundleListener, DisplayProductListener {
  private final List<Object> events = new ArrayList<>();

  public void reset() {
    events.clear();
  }

  public List<Object> getEvents() {
    return events;
  }

  @Override
  public void onProductCreated(final ProductCreatedEvent event) {
    events.add(event.deepCopy());
  }

  @Override
  public void onProductUpdated(final ProductUpdatedEvent event) {
    events.add(event.deepCopy());
  }

  @Override
  public void onProductDeleted(final ProductDeletedEvent event) {
    events.add(event.deepCopy());
  }

  @Override
  public void onProductError(final ProductErrorEvent event) {
    events.add(event.deepCopy());
  }

  @Override
  public void onProductBundleCreated(final ProductBundleCreatedEvent event) {
    events.add(event.deepCopy());
  }

  @Override
  public void onProductBundleDeleted(final ProductBundleDeletedEvent event) {
    events.add(event.deepCopy());
  }

  @Override
  public void onProductBundleError(final ProductBundleErrorEvent event) {
    events.add(event.deepCopy());
  }

  @Override
  public void onDisplayProductCreated(final DisplayProductCreatedEvent event) {
    events.add(event.deepCopy());
  }

  @Override
  public void onDisplayProductUpdated(final DisplayProductUpdatedEvent event) {
    events.add(event.deepCopy());
  }

  @Override
  public void onDisplayProductDeleted(final DisplayProductDeletedEvent event) {
    events.add(event.deepCopy());
  }
}
