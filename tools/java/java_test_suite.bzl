load("//tools/java:java_package.bzl", "java_package")

def java_test_suite(
        name,
        **kwargs):
    """
    Generate test rules for each test individually.

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
    test_names = []
    for src in srcs:
        clazz = src[src.rfind("/") + 1:-5]
        test_names.append(clazz)
        native.java_test(
            name = clazz,
            srcs = [src],
            deps = deps,
            **kwargs
        )
    native.test_suite(
        name = name + "-suite",
        tests = test_names,
    )
