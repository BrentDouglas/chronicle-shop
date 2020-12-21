# Example Shop

This repository contains an example of a shop built on chronicle.

## Dependencies

The project requires bazel to build and lcov to generate the coverage report
and these will need to be installed using your systems package manager. E.g.
for ArchLinux you can run `sudo pacman -S bazel lcov`, on Mac
`brew install bazel lcov` or on Ubuntu `sudo apt-get install bazel lcov`.

There is a Makefile in the root of the project that can be used to run a
bunch of common operations, e.g. `make run` will build and run the binary,
`make test` runs the tests, `make check` runs the linters and `make coverage`
generates a coverage report.

## Assumptions

The first assumption is that this will be run/tested on Linux, the async
profiler part I didn't make work for other systems as I only have a Linux
box at home.

- I have assumed that only the core is relevant so didn't build out a UI
so currently it can only be interacted with programmatically, e.g. in the
tests and benchmarks.
- I also assumed persistence is out of scope so haven't configured it for
the maps or queues. 

## Building

The project is built using bazel. It can be built using the command

```$shell
bazel build //:shop
```

A deployment jar can be created with this:

```$shell
bazel build //:shop_deploy.jar
```

To build everything within the workspace use

```$shell
bazel build //...
```

## Running

It can be run directly using bazel. It takes three command line switches:

-d the root dir for it to run in
-p the number of products to support
-c the number of carts to support

The directory flag is required. e.g.

```$shell
bazel run //:shop -- -d /tmp/shop
```

You could also run it by invoking the entry script:

```$shell
bazel build //:shop
./bazel-bin/shop -d /tmp/shop
```

Or by running the deployment jar:

```$shell
bazel build //:shop_deploy.jar
java -jar bazel-bin/shop_deploy.jar -d /tmp/shop
```

## Testing

To run all the tests you can use the command

```$shell
bazel test //...
```

Individual tests can be run by specifying them on the command line e.g.

```$shell
bazel test //src/test/java/io/machinecode/shop:MainTest
```

## Setting up IDEA

This project includes config to allow development using the
bazel IDEA plugin which can be installed from the regular Jetbrains
plugin repository. Once it is installed you can import the project
setup by selecting 'Import bazel project' from the File menu,
selecting the path to this repository, then select
'Import project view file' and point it to `tools/idea/all.bazelproject`.

## Code style

The project uses [google-java-format](https://github.com/google/google-java-format)
in the default google style. This is available as an IDEA plugin or you can run
`make format` every now and then.