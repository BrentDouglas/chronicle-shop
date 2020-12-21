# Commands
open := $(shell if [ "$$(uname)" == "Darwin" ]; then echo "open"; else echo "xdg-open"; fi)

# Argument to pass to the build system
a := $(shell echo "$${a:-}")
ifndef args
args := $(a)
endif

.PHONY: help
help:
	@echo ""
	@echo "-- Available make targets:"
	@echo ""
	@echo "   all                        - Build everything"
	@echo "   build                      - Build the binary"
	@echo "   run                        - Run the binary"
	@echo "   check                      - Run linters"
	@echo "   test                       - Run the tests"
	@echo "   bench                      - Run the benchmarks"
	@echo "   coverage                   - Get the test coverage"
	@echo "   format                     - Format the sources"
	@echo ""


.PHONY: all
all: build check build-coverage

.PHONY: run
run:
	@bazel build //:shop \
		$(args)

.PHONY: build
build:
	@bazel build //:shop \
		$(args)

.PHONY: format
format:
	@bazel build @com_github_bazelbuild_buildtools//buildifier \
				@google_java_format//jar
	@find . -type f \( -name BUILD -or -name BUILD.bazel \) \
		| xargs $$(bazel info bazel-bin)/external/com_github_bazelbuild_buildtools/buildifier/*/buildifier
	@java -jar $$(bazel info execution_root)/external/google_java_format/jar/downloaded.jar -i \
		$$(find src/ -type f -name '*.java')

.PHONY: check
check:
	@bazel test //...:all \
		--build_tag_filters=check \
		--test_tag_filters=check \
		$(args)

.PHONY: bench
bench:
	@bazel test //...:all \
		--build_tag_filters=bench \
		--test_tag_filters=bench \
		$(args)
	@mkdir -p .srv/prof
	@rm -rf .srv/prof && mkdir -p .srv/prof
	@bash -c "(cd .srv/prof && unzip $$(bazel info bazel-testlogs)/src/test/java/io/machinecode/shop/bench/*/test.outputs/outputs.zip)"

.PHONY: test
test:
	@bazel test //...:all \
		--build_tag_filters=-check,-bench \
		--test_tag_filters=-check,-bench \
		$(args)

.PHONY: build-coverage
build-coverage:
	@if [ -e bazel-out ]; then find bazel-out -name coverage.dat -exec rm {} +; fi
	@bazel coverage \
		//src/main/java/io/machinecode/shop/...:all \
		//src/test/java/io/machinecode/shop/...:all \
		--test_tag_filters=-check,-bench \
		$(args)
	@bazel build //:coverage \
		$(args)

.PHONY: coverage
coverage: build-coverage
	@mkdir -p .srv/cov
	@rm -rf .srv/cov && mkdir -p .srv/cov
	@bash -c "(cd .srv/cov && tar xf $$(bazel info bazel-bin)/coverage.tar)"
	@$(open) .srv/cov/index.html