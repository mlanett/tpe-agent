DEFAULT_GOAL := help
.PHONY: help build clean test publish patch

help:
	@echo "  build    - Compiles the code and builds all artifacts"
	@echo "  clean    - Deletes all build artifacts"
	@echo "  test     - Runs tests"
	@echo "  publish  - Publishes the library to GitHub Packages"
	@echo "  patch    - Increments the patch version in build.gradle.kts"

build:
	./gradlew build

clean:
	./gradlew clean
	rm -rf monitoring-agent/build build

test:
	./gradlew :monitoring-agent:test --tests ThreadPoolExecutorTest --info --rerun

publish:
	./gradlew publish

patch:
	@current_version=$$(grep '^version = ' build.gradle.kts | sed 's/version = "\(.*\)"/\1/'); \
	major=$$(echo $$current_version | cut -d. -f1); \
	minor=$$(echo $$current_version | cut -d. -f2); \
	patch=$$(echo $$current_version | cut -d. -f3); \
	new_patch=$$((patch + 1)); \
	new_version="$$major.$$minor.$$new_patch"; \
	echo "Updating version from $$current_version to $$new_version"; \
	sed -i.bak "s/^version = \"$$current_version\"/version = \"$$new_version\"/" build.gradle.kts && rm build.gradle.kts.bak
