# Example Integration Test Project

This project serves as both an integration test (verifying published artifacts work correctly) and an example of how to use the ThreadPoolExecutor agent in a real application.

## Purpose

- **Integration Test**: Verifies that artifacts published to Maven Central can be downloaded and used correctly
- **Example**: Demonstrates real-world usage of the agent with a working application

## Running the Example Application

To run the example application that creates thread pools and monitors them:

```bash
./gradlew run
```

Or if you're in the example directory:

```bash
cd example
../gradlew run
```

The application will:

- Load the agent via `-javaagent` parameter
- Create multiple ThreadPoolExecutors with different configurations
- Submit workloads to keep them active
- Report metrics to stdout every 5 seconds
- Run for 60 seconds, then shut down cleanly

## Running Tests

To run the integration tests:

```bash
./gradlew :example:test
```

The tests verify:

- Agent JAR can be downloaded from Maven Central
- Agent loads correctly via `-javaagent` parameter
- ThreadPoolExecutor instances are automatically discovered (no manual registration needed)
- Library classes are accessible and metrics can be read

## Configuration

The version of `tpe-agent` to use can be configured via a Gradle property:

```bash
./gradlew :example:test -PtpeAgentVersion=0.0.11
```

By default, it uses version `0.0.11` (as defined in `build.gradle.kts`).

## How It Works

1. **Agent Loading**: The `build.gradle.kts` configures both the `test` and `run` tasks to automatically download the agent JAR from Maven Central and load it via the `-javaagent` JVM parameter.

2. **Library Dependency**: The project depends on `mlanett:tpe-agent` library which provides `ThreadPoolExecutorRegistry` and `ThreadPoolExecutorMetrics` classes.

3. **Automatic Discovery**: When ThreadPoolExecutors are created, the agent automatically instruments them and registers them with the registry - no manual registration needed.

4. **Metrics Collection**: The application can query `ThreadPoolExecutorRegistry.getInstance().snapshot()` to get current metrics for all discovered pools.
