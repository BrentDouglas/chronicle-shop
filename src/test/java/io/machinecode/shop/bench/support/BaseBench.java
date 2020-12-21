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
package io.machinecode.shop.bench.support;

import io.machinecode.shop.App;
import io.machinecode.shop.cart.api.CartDispatcher;
import io.machinecode.shop.product.api.ProductDispatcher;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.TimeUnit;
import net.openhft.chronicle.threads.EventGroup;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.profile.AsyncProfiler;
import org.openjdk.jmh.profile.JavaFlightRecorderProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

/** @author <a href="mailto:brent.n.douglas@gmail.com">Brent Douglas</a> */
@State(Scope.Benchmark)
public abstract class BaseBench {
  private static final Path ROOT = Paths.get(System.getProperty("java.io.tmpdir"), "shop");
  private static File dir;
  private static App app;
  public static ProductDispatcher productDispatcher;
  public static CartDispatcher cartDispatcher;

  public static void setUp(Class<?> bench) {
    dir = Paths.get(ROOT.toString(), bench.getSimpleName()).toFile();
    deleteDir(dir);
    dir.mkdirs();
    app = new App(dir, new EventGroup(false), 1_000, 1_000);
    productDispatcher = app.getProductModule().getDispatcher();
    cartDispatcher = app.getCartModule().getDispatcher();
  }

  public static void tearDown(Class<?> bench) {
    app.close();
    deleteDir(dir);
  }

  public static void run(Class<?> bench) throws Exception {
    final String outputDir =
        Path.of(ROOT.toString(), "profiles", BaseBench.class.getSimpleName()).toString();
    new Runner(
            new OptionsBuilder()
                .include(bench.getName())
                .mode(Mode.AverageTime)
                .timeUnit(TimeUnit.MICROSECONDS)
                .warmupTime(TimeValue.seconds(10))
                .warmupIterations(5)
                .measurementTime(TimeValue.seconds(10))
                .measurementIterations(10)
                .timeout(TimeValue.seconds(10))
                .addProfiler(AsyncProfiler.class, "output=flamegraph;dir=" + outputDir)
                .addProfiler(JavaFlightRecorderProfiler.class, "dir=" + outputDir)
                .forks(1)
                .jvmArgsAppend("-da", "-dsa")
                .build())
        .run();
  }

  private static void deleteDir(File dir) {
    try {
      if (!dir.exists()) {
        return;
      }
      Files.walkFileTree(
          dir.toPath(),
          new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult postVisitDirectory(final Path dir, final IOException exc)
                throws IOException {
              Files.delete(dir);
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
                throws IOException {
              Files.delete(file);
              return FileVisitResult.CONTINUE;
            }
          });
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }
}
