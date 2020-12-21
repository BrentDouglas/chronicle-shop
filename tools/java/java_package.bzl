load("@io_machinecode_tools//tools/java:checkstyle.bzl", "checkstyle_test")
load("@io_machinecode_tools//tools/java:google_java_format.bzl", "google_java_format_test")

def java_package(
        name,
        **kwargs):
    srcs = kwargs.pop("srcs", [])
    if not srcs:
        srcs = native.glob(["*.java"])
    native.filegroup(
        name = "srcs",
        srcs = srcs,
    )

    native.java_library(
        name = name,
        srcs = [":srcs"],
        **kwargs
    )

    checkstyle_test(
        name = "check",
        srcs = [
            ":srcs",
        ],
        config = "//tools/checkstyle:config",
        data = [
            "//tools/checkstyle:header",
        ],
        tags = ["check"],
    )

    google_java_format_test(
        name = "format",
        srcs = [
            ":srcs",
        ],
        tags = ["check"],
    )
