DEFAULT_GOAL := help
.PHONY: help build clean test publish patch

help:
	@echo "build            - Compiles the code and builds all artifacts"
	@echo "clean            - Deletes all build artifacts"
	@echo "test             - Runs tests"
	@echo "publish          - Publishes the library to GitHub Packages"
	@echo "patch            - Prepare for a patch release by incrementing the library version's patch component."
	@echo "run-example      - Runs the example application (w/o gradle)"
	@echo "run-prerelease   - Runs the prerelease application (w/o gradle)"

build: # Compiles the code and builds all artifacts
	./gradlew assemble
	./gradlew build

clean: # Removes all build artifacts
	./gradlew clean
	rm -rf */bin */build build

run-example: build # Run the example application with standalone Java (no Gradle classloader)
	java -javaagent:agent/build/libs/tpe-agent-0.1.0-agent.jar \
	     -cp example/build/libs/example-0.1.0.jar:api/build/libs/api-0.1.0.jar:bootstrap-api/build/libs/bootstrap-api-0.1.0.jar \
	     ExampleApplication

run-prerelease: # Run the prerelease application
	./gradlew :prerelease:assemble
	java -javaagent:agent/build/libs/tpe-agent-0.2.0-agent.jar \
	     -cp prerelease/build/libs/prerelease-0.2.0.jar:agent/build/libs/tpe-agent-0.2.0.jar \
	     PrereleaseApplication

test: build run-prerelease # Run all tests
	./gradlew :agent:test --rerun

publish:
	./gradlew publish

publish-local:
	./gradlew :agent:publishToMavenLocal

patch: # Prepare for a patch release by incrementing the library version's patch component.
	@current_version=$$(grep '^version = ' build.gradle.kts | sed 's/version = "\(.*\)"/\1/'); \
	major=$$(echo $$current_version | cut -d. -f1); \
	minor=$$(echo $$current_version | cut -d. -f2); \
	patch=$$(echo $$current_version | cut -d. -f3); \
	new_patch=$$((patch + 1)); \
	new_version="$$major.$$minor.$$new_patch"; \
	echo "Updating version from $$current_version to $$new_version"; \
	sed -i.bak "s/^version = \"$$current_version\"/version = \"$$new_version\"/" build.gradle.kts && rm build.gradle.kts.bak

minor: # Prepare for a minor release by incrementing the library version's minor component.
	@current_version=$$(grep '^version = ' build.gradle.kts | sed 's/version = "\(.*\)"/\1/'); \
	major=$$(echo $$current_version | cut -d. -f1); \
	minor=$$(echo $$current_version | cut -d. -f2); \
	new_minor=$$((minor + 1)); \
	new_version="$$major.$$new_minor.0"; \
	echo "Updating version from $$current_version to $$new_version"; \
	sed -i.bak "s/^version = \"$$current_version\"/version = \"$$new_version\"/" build.gradle.kts && rm build.gradle.kts.bak

tag: # Commit and publish the current version. Be sure to update the version before doing this.
	@current_version=$$(grep '^version = ' build.gradle.kts | sed 's/version = "\(.*\)"/\1/'); \
	git commit -m v$$current_version build.gradle.kts || true; \
	git tag -a v$$current_version -m v$$current_version; \
	git push origin v$$current_version;
