DEFAULT_GOAL := help
.PHONY: help build clean test publish patch

help:
	@echo "  build    - Compiles the code and builds all artifacts"
	@echo "  clean    - Deletes all build artifacts"
	@echo "  test     - Runs tests"
	@echo "  publish  - Publishes the library to GitHub Packages"
	@echo "  patch    - Increments the patch version in build.gradle.kts"

build: # Compiles the code and builds all artifacts
	./gradlew build

clean: # Removes all build artifacts
	./gradlew clean
	rm -rf */bin */build build

run-example:
	./gradlew :example:run

run-demo:
	./gradlew :demo:run

test: # Run all tests
	./gradlew :monitoring-agent:test --tests ThreadPoolExecutorTest --info --rerun
	./gradlew :quickcheck-tpe:run # End-to-end demonstration and proof that the agent is working.
	./gradlew :quickcheck-monitoring:run # End-to-end demonstration and proof that the agent is working.
	./gradlew :example:run # End-to-end demonstration and proof that the agent is working.

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

tag: # Commit and publish the current version. Use in combination e.g. make patch tag
	@current_version=$$(grep '^version = ' build.gradle.kts | sed 's/version = "\(.*\)"/\1/'); \
	git commit -m v$$current_version build.gradle.kts || true; \
	git tag -a v$$current_version -m v$$current_version; \
	git push origin v$$current_version;
