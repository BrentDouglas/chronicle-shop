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

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;
import java.io.File;
import java.nio.file.Paths;
import net.openhft.chronicle.core.threads.EventLoop;
import net.openhft.chronicle.threads.EventGroup;

/** @author <a href="mailto:brent.n.douglas@gmail.com">Brent Douglas</a> */
public class Main {

  public static void main(final String... args) {
    final Options options = getOptions(args);
    final EventLoop eventLoop = new EventGroup(false);
    final App app = new App(options.getDir(), eventLoop, options.getProducts(), options.getCarts());
    app.start();
  }

  static Main.Options getOptions(final String... args) {
    final Getopt opt =
        new Getopt(
            "shop",
            args,
            "d:p:c:",
            new LongOpt[] {
              new LongOpt("directory", LongOpt.REQUIRED_ARGUMENT, null, 'd'),
              new LongOpt("products", LongOpt.OPTIONAL_ARGUMENT, null, 'p'),
              new LongOpt("carts", LongOpt.OPTIONAL_ARGUMENT, null, 'c'),
            });

    File dir = null;
    long products = 1000L;
    long carts = 1000L;

    int c;
    while ((c = opt.getopt()) != -1) {
      switch (c) {
        case 'd':
          dir = Paths.get(opt.getOptarg()).toFile();
          break;
        case 'p':
          products = Long.parseLong(opt.getOptarg());
          break;
        case 'c':
          carts = Long.parseLong(opt.getOptarg());
          break;
        default:
          throw new IllegalArgumentException("Invalid option");
      }
    }
    if (dir == null) {
      throw new IllegalStateException("Directory argument -d is required");
    }
    return new Options(dir, carts, products);
  }

  static class Options {
    final File dir;
    final long carts;
    final long products;

    Options(final File dir, final long carts, final long products) {
      this.dir = dir;
      this.carts = carts;
      this.products = products;
    }

    public File getDir() {
      return dir;
    }

    public long getCarts() {
      return carts;
    }

    public long getProducts() {
      return products;
    }
  }
}
