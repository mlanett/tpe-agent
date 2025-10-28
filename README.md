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
dependencies {
    // For using ThreadPoolExecutorMetrics and other classes
    implementation("mlanett:tpe-agent:0.0.3")
}

// For using the agent JAR with -javaagent
val agentConfiguration: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

dependencies {
    agentConfiguration("mlanett:tpe-agent:0.0.3:agent@jar")
}

tasks.test {
    doFirst {
        jvmArgs("-javaagent:${agentConfiguration.singleFile.absolutePath}")
    }
}
```

#### Using Maven

Add the dependency to your `pom.xml`:

```xml
<dependencies>
    <dependency>
        <groupId>mlanett</groupId>
        <artifactId>tpe-agent</artifactId>
        <version>0.0.3</version>
    </dependency>
</dependencies>
```

**Note**: The library is published to Maven Central, so no additional repository configuration is needed!

### Usage

There are two ways to use the agent:

#### Option 1: JVM Agent (Recommended)

Use the `-javaagent` JVM flag to load the agent at startup. This ensures all ThreadPoolExecutors are tracked from the beginning:

```bash
java -javaagent:/path/to/tpe-agent-0.0.3-agent.jar -jar your-application.jar
```

Or download from [GitHub Releases](https://github.com/mlanett/tpe-agent/releases) and use:

```bash
java -javaagent:./tpe-agent-0.0.3-agent.jar -jar your-application.jar
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

**Note**: This requires Java 9+ with the `jdk.attach.allowAttachSelf` system property or the `--enable-dynamic-agent-loading` flag.

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
            System.out.printf("  Queue size: %d%n", metrics.getQueueSize());
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

### Example with Scheduled Monitoring

```java
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Application {
    public static void main(String[] args) {
        ThreadPoolExecutorAgent.ensureInstalled();

        // Schedule periodic monitoring
        ScheduledExecutorService monitor = Executors.newSingleThreadScheduledExecutor();
        monitor.scheduleAtFixedRate(() -> {
            var snapshot = ThreadPoolExecutorRegistry.getInstance().snapshot();

            snapshot.forEach((name, metrics) -> {
                // Check for potential issues
                if (metrics.getQueueSize() > 1000) {
                    System.err.printf("WARNING: Large queue in %s: %d items%n",
                        name, metrics.getQueueSize());
                }
                if (metrics.getPoolSize() == metrics.getMaximumPoolSize()) {
                    System.err.printf("WARNING: Pool %s at maximum capacity%n", name);
                }
            });
        }, 60, 60, TimeUnit.SECONDS);

        // Start your application
        startApplication();
    }
}
```

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

**Methods:**

- `int getQueueSize()` - Number of tasks waiting in the queue
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

### Publishing

To publish to GitHub Packages, you need:

1. A GitHub Personal Access Token with `write:packages` scope
2. Set credentials in `~/.gradle/gradle.properties`:

   ```properties
   gpr.user=your-github-username
   gpr.token=your-personal-access-token
   ```

3. Run: `./gradlew publish`

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

This is the same license used by ByteBuddy, which this project depends on.

## Acknowledgments

- Built with [ByteBuddy](https://bytebuddy.net/) for bytecode instrumentation
- Inspired by the need to monitor ThreadPoolExecutor instances in production environments
