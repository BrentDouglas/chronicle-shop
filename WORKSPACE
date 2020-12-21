"""
A solution to the shop problem
"""

workspace(name = "io_machinecode_shop")

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

io_machinecode_tools_version = "5d4813cba6e7f8ab66610162f0db9057057de325"

http_archive(
    name = "io_machinecode_tools",
    sha256 = "f189b432434f1bda8046da4347d0139f451c2bce52bc5c5b66a968ba1039718b",
    strip_prefix = "tools-" + io_machinecode_tools_version,
    urls = [
        "https://mirror.bazel.build/github.com/BrentDouglas/tools/archive/%s.tar.gz" % io_machinecode_tools_version,
        "https://github.com/BrentDouglas/tools/archive/%s.tar.gz" % io_machinecode_tools_version,
    ],
)

load("@io_machinecode_tools//imports:go_repositories.bzl", "go_repositories")

go_repositories()

load("@io_bazel_rules_go//go:deps.bzl", "go_register_toolchains", "go_rules_dependencies")

go_rules_dependencies()

go_register_toolchains()

load("@bazel_gazelle//:deps.bzl", "gazelle_dependencies")

gazelle_dependencies()

load("@io_machinecode_tools//imports:stardoc_repositories.bzl", "stardoc_repositories")

stardoc_repositories()

load("@io_machinecode_tools//imports:java_repositories.bzl", "java_repositories")

java_repositories()

load("@io_machinecode_tools//imports:jmh_repositories.bzl", "jmh_repositories")

jmh_repositories()

load("@io_machinecode_tools//imports:checkstyle_repositories.bzl", "checkstyle_repositories")

checkstyle_repositories()

load("@io_machinecode_tools//imports:format_repositories.bzl", "format_repositories")

format_repositories()

load("@rules_jvm_external//:defs.bzl", "maven_install")
load("@io_machinecode_tools//:defs.bzl", "maven_repositories")

maven_install(
    artifacts = [
        "junit:junit:4.13.1",
        "org.mockito:mockito-all:1.10.19",
        "gnu.getopt:java-getopt:1.0.13",
        "net.openhft:chronicle-queue:5.20.115",
        "net.openhft:chronicle-map:3.20.84",
        "net.openhft:chronicle-values:2.20.80",
    ],
    fetch_sources = True,
    repositories = maven_repositories,
)

async_profiler_version = "1.8.2"

async_profiler_sha256 = "5543acd94624847c58ba9912f70aca66d75eb2ea4cb03bee58b8f16c6b83707c"

http_archive(
    name = "async_profiler",
    build_file = "//tools:async_profiler.BUILD.bazel",
    sha256 = async_profiler_sha256,
    strip_prefix = "async-profiler-%s-linux-x64" % async_profiler_version,
    urls = [
        "https://mirror.bazel.build/github.com/jvm-profiling-tools/async-profiler/releases/download/v%s/async-profiler-%s-linux-x64.tar.gz" % (async_profiler_version, async_profiler_version),
        "https://github.com/jvm-profiling-tools/async-profiler/releases/download/v%s/async-profiler-%s-linux-x64.tar.gz" % (async_profiler_version, async_profiler_version),
    ],
)
