DEFAULT_GOAL := help
.PHONY: help build clean test publish

help:
	@echo "  build    - Compiles the code and builds all artifacts"
	@echo "  clean    - Deletes all build artifacts"
	@echo "  test     - Runs tests"
	@echo "  publish  - Publishes the library to GitHub Packages"

build:
	./gradlew build

clean:
	./gradlew clean
	rm -rf monitoring-agent/build build

test:
	./gradlew :monitoring-agent:test --tests ThreadPoolExecutorTest --info --rerun

publish:
	./gradlew publish
