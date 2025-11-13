# Example Application

This example demonstrates how to use the ThreadPoolExecutor agent to monitor thread pools in a Java application.

## Running the Example

From the project root:

```bash
make run
```

### Manual Java Command

After building (`make build` in the project root):

```bash
java -javaagent:../agent/build/libs/tpe-agent-0.1.0-agent.jar \
     -cp build/libs/example-0.1.0.jar:../api/build/libs/api-0.1.0.jar:../bootstrap-api/build/libs/bootstrap-api-0.1.0.jar \
     ExampleApplication
```

## What the Example Does

The application:

1. Creates multiple ThreadPoolExecutors with different configurations.
2. Submits workloads to keep pools active.
3. Reports metrics to stdout periodically.
4. Runs for 10 seconds, then shuts down.

## Tests

This module has test code which does basically the same thing as the example application, but in convenient JUnit format.

```bash
../gradlew :example:test
```

The tests verify:

- Agent loads correctly via `-javaagent` parameter
- ThreadPoolExecutor instances are automatically discovered
- Metrics can be read through the API
- Cross-classloader communication works

## How It Works

### 1. Agent Installation

The agent JAR is loaded via `-javaagent` parameter:

- Contains the bootstrap API classes
- Appended to bootstrap classpath via `Boot-Class-Path` manifest
- Registers the registry implementation into `ThreadPoolRegistrySingleton`

### 2. Automatic Discovery

When ThreadPoolExecutors are created:

- ByteBuddy instrumentation intercepts constructors
- Pools are automatically registered with the registry
- No manual registration needed

### 3. Metrics Access

Application code accesses metrics via the API:

```java
import mlanett.tpe_agent.GlobalThreadPoolRegistry;
import mlanett.tpe_agent.IThreadPoolMetrics;

Map<String, IThreadPoolMetrics> pools = GlobalThreadPoolRegistry.snapshot();
```

## Dependencies

### Compile-Time

- `api` module - Application-facing API

### Runtime

- `bootstrap-api` - Bootstrap classpath API (included in agent JAR)
- `agent` - Agent implementation (loaded via `-javaagent`)
