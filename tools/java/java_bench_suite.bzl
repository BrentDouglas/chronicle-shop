load("//tools/java:java_package.bzl", "java_package")

def java_bench_suite(
        name,
        **kwargs):
    """
    Generate binary rules for each benchmark individually.

    Args:
      name: The name of the test suite
      kargs: Other args for the test run. See https://docs.bazel.build/versions/master/be/java.html#java_test
    """
    srcs = kwargs.pop("srcs", [])
    deps = kwargs.pop("deps", [])
    if not srcs:
        srcs = native.glob(["*.java"])
    java_package(
        name = name,
        srcs = srcs,
        deps = deps,
    )
    for src in srcs:
        clazz = src[src.rfind("/") + 1:-5]
        native.java_binary(
            name = clazz,
            main_class = src[src.rfind("java") + 1:-5].replace("/", "."),
            srcs = [src],
            deps = deps,
            **kwargs
        )
