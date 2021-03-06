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

import static org.junit.Assert.assertEquals;

import java.io.File;
import org.junit.Test;

/** @author <a href="mailto:brent.n.douglas@gmail.com">Brent Douglas</a> */
public class MainTest {

  @Test
  public void getOptionsDefault() {
    Main.Options options = Main.getOptions("-d", "/tmp");
    assertEquals(1000L, options.getCarts());
    assertEquals(1000L, options.getProducts());
    assertEquals(new File("/tmp"), options.getDir());
  }

  @Test
  public void getOptions() {
    Main.Options options = Main.getOptions("-d", "/tmp", "-p", "10", "-c", "10");
    assertEquals(10L, options.getCarts());
    assertEquals(10L, options.getProducts());
    assertEquals(new File("/tmp"), options.getDir());
  }

  @Test(expected = IllegalStateException.class)
  public void getOptionsNoDir() {
    Main.Options options = Main.getOptions();
  }
}
