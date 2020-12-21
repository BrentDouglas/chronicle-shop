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
package io.machinecode.shop.cart.api;

import io.machinecode.shop.cart.event.CartErrorEvent;
import io.machinecode.shop.cart.event.CartProductAddedEvent;
import io.machinecode.shop.cart.event.CartProductRemovedEvent;
import io.machinecode.shop.cart.event.CartProductUpdatedEvent;
import io.machinecode.shop.cart.event.CartTotalEvent;

/** @author <a href="mailto:brent.n.douglas@gmail.com">Brent Douglas</a> */
public interface CartListener {

  void onCartProductAddedEvent(final CartProductAddedEvent event);

  void onCartProductUpdatedEvent(final CartProductUpdatedEvent event);

  void onCartProductRemovedEvent(final CartProductRemovedEvent event);

  void onCartTotalEvent(final CartTotalEvent event);

  void onCartErrorEvent(final CartErrorEvent event);
}
