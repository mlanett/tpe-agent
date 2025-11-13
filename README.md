# ThreadPoolExecutor Agent

TPE-Agent is a lightweight JVM agent that automatically tracks all `ThreadPoolExecutor` instances in your application.

Modern Java applications often have many ThreadPoolExecutor instances, created either directly or by libraries. These thread pools present a risk when poorly configured: unbounded queues or thread pools can cause unlimited memory consumption during high load, potentially crashing your application.

ThreadPoolExecutor instances are difficult to track manually. This agent automatically discovers and monitors all of them using bytecode instrumentation. It's designed to be minimal, with no built-in reporting mechanism. Instead, it exposes a simple API that lets your application collect metrics and report them however you prefer (logs, APM tools, metrics systems, etc.).

## Features

- Automatic discovery of all ThreadPoolExecutor instances (including subclasses)
- Tracks pools created by application code and third-party libraries
- Zero configuration required
- Minimal overhead using ByteBuddy instrumentation
- Thread-safe registry with weak references (no memory leaks)
- Provides comprehensive metrics: queue size, active threads, completed tasks, pool sizes, etc.
- Works with Java 11+

## Quick Start

### Installation

#### Using Gradle

Add the dependency to your `build.gradle.kts`:

```kotlin
plugins {
    `java`
    application
}

repositories {
    mavenCentral()
}

// Version of tpe-agent to use - can be overridden with -PtpeAgentVersion=x.y.z
val AGENT_VERSION: String = "0.2.0"

val agentConfiguration: Configuration = 
    configurations.create("agent").apply {
        isCanBeConsumed = false
        isCanBeResolved = true
        isTransitive = false
    }

dependencies {
    // For using ThreadPoolExecutorMetrics and other classes
    implementation("io.github.mlanett:tpe-agent:$AGENT_VERSION")

    add(agentConfiguration.name, "io.github.mlanett:tpe-agent:$AGENT_VERSION:agent@jar")
}

fun resolveAgentJar(): File {
    return checkNotNull(agentConfiguration).singleFile
}

tasks.test {
    useJUnitPlatform()
    dependsOn(agentConfiguration)

    doFirst {
        val agentJar = resolveAgentJar().absolutePath
        jvmArgs("-javaagent:$agentJar")
        jvmArgs("-XX:+EnableDynamicAgentLoading")
        jvmArgs("-Djdk.attach.allowAttachSelf=true")
        jvmArgs("--add-opens", "java.base/java.util.concurrent=ALL-UNNAMED")
    }
}

tasks.named<JavaExec>("run") {
    dependsOn(agentConfiguration)

    doFirst {
        val agentJar = resolveAgentJar().absolutePath
        jvmArgs("-javaagent:$agentJar")
    }
}
```

#### Using Maven

Add the dependency to your `pom.xml`:

```xml
<dependencies>
    <dependency>
        <groupId>io.github.mlanett</groupId>
        <artifactId>tpe-agent</artifactId>
        <version>0.1.0</version>
    </dependency>
</dependencies>
```

Note: The library is published to Maven Central, so no additional repository configuration is needed!

### Usage

There are two ways to use the agent:

#### Option 1: JVM Agent (Recommended)

Use the `-javaagent` JVM flag to load the agent at startup. This ensures all ThreadPoolExecutors are tracked from the beginning:

```bash
java -javaagent:/path/to/tpe-agent-0.1.0-agent.jar -jar your-application.jar
```

Or download from [GitHub Releases](https://github.com/mlanett/tpe-agent/releases) and use:

```bash
java -javaagent:./tpe-agent-0.1.0-agent.jar -jar your-application.jar
```

#### Option 2: Programmatic Installation

Call `ensureInstalled()` early in your application startup:

```java
import mlanett.tpe_agent.ThreadPoolExecutorAgent;

public class Application {
    public static void main(String[] args) {
        ThreadPoolExecutorAgent.ensureInstalled();

        startApplication();
    }
}
```

Note: This requires Java 9+ with the `jdk.attach.allowAttachSelf` system property or the `-XX:+EnableDynamicAgentLoading` flag. The `-javaagent` approach (Option 1) is recommended as it's simpler and doesn't require these additional flags.

### Collecting Metrics

Once the agent is installed, you can access thread pool metrics through the `ThreadPoolExecutorRegistry`:

```java
import mlanett.tpe_agent.ThreadPoolExecutorRegistry;
import mlanett.tpe_agent.ThreadPoolExecutorMetrics;
import java.util.Map;

public class MonitoringService {
    public void reportMetrics() {
        // Get a snapshot of all tracked ThreadPoolExecutors
        Map<String, ThreadPoolExecutorMetrics> pools =
            ThreadPoolExecutorRegistry.getInstance().snapshot();

        for (Map.Entry<String, ThreadPoolExecutorMetrics> entry : pools.entrySet()) {
            String poolName = entry.getKey();
            ThreadPoolExecutorMetrics metrics = entry.getValue();

            System.out.printf("Pool: %s%n", poolName);
            System.out.printf("  Queue size: %d%n", metrics.getQueuedCount());
            System.out.printf("  Active threads: %d%n", metrics.getActiveCount());
            System.out.printf("  Pool size: %d (core=%d, max=%d)%n",
                metrics.getPoolSize(),
                metrics.getCorePoolSize(),
                metrics.getMaximumPoolSize());
            System.out.printf("  Completed tasks: %d (total=%d)%n",
                metrics.getCompletedTaskCount(),
                metrics.getTaskCount());
        }
    }
}
```

### Example Application (independent consumer)

This repository includes an `example` module that acts as an independent consumer of the published `io.github.mlanett:tpe-agent` artifacts. It does not use any `project()` dependencies on internal modules.

To build and run the example against the locally built agent:

```bash
./gradlew :agent:publishToMavenLocal
./gradlew :example:build
./gradlew :example:run
```

This workflow:

- Publishes the current `agent` module as `io.github.mlanett:tpe-agent:${version}` (library JAR
  plus `:agent` classifier) into your local Maven repository.
- Builds the `example` module, which resolves `io.github.mlanett:tpe-agent:${version}` from
  `mavenLocal()` like any external application would.
- Runs the example with the agent attached via `-javaagent` (managed by the Gradle `run` task).

For more details, see `example/README.md`.

## API Reference

### `ThreadPoolExecutorAgent`

Entry point for installing the agent.

- `static void ensureInstalled()` - Installs the agent if not already installed (programmatic installation)

### `ThreadPoolExecutorRegistry`

Registry of all discovered ThreadPoolExecutor instances.

- `static ThreadPoolExecutorRegistry getInstance()` - Get the singleton registry instance
- `Map<String, ThreadPoolExecutorMetrics> snapshot()` - Get current metrics for all tracked pools
- `void register(ThreadPoolExecutor)` - Manually register a pool (rarely needed)
- `void unregister(ThreadPoolExecutor)` - Manually unregister a pool (rarely needed)

### `ThreadPoolExecutorMetrics`

Immutable snapshot of a ThreadPoolExecutor's metrics.

Methods:

- `int getQueuedCount()` - Number of tasks waiting in the queue
- `int getActiveCount()` - Number of threads actively executing tasks
- `long getCompletedTaskCount()` - Total number of completed tasks
- `long getTaskCount()` - Total number of tasks (completed + active + queued)
- `int getCorePoolSize()` - Core pool size configuration
- `int getMaximumPoolSize()` - Maximum pool size configuration
- `int getPoolSize()` - Current number of threads in the pool

## How It Works

The agent uses [ByteBuddy](https://bytebuddy.net/) to instrument the `ThreadPoolExecutor` class at runtime:

1. Intercepts all ThreadPoolExecutor constructors to register new instances
2. Intercepts `shutdown()` and `shutdownNow()` to track lifecycle
3. Uses weak references to avoid memory leaks when pools are garbage collected
4. Automatically derives pool names from ThreadFactory naming or class names

## Dependencies

TPE-Agent only depends on ByteBuddy, so is not increasing your service's transitive dependency footprint by much.

## Contributing

Contributions are welcome! Please feel free to submit issues or pull requests.

### Building from Source

```bash
./gradlew build
```

### Running Tests

```bash
./gradlew test
```

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

This is the same license used by ByteBuddy, which this project depends on.

## Acknowledgments

- Built with [ByteBuddy](https://bytebuddy.net/) for bytecode instrumentation
- Inspired by the need to monitor ThreadPoolExecutor instances in production environments
