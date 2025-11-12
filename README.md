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
val AGENT_VERSION: String = project.findProperty("tpeAgentVersion") as String? ?: "0.1.0"

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

**Note**: The library is published to Maven Central, so no additional repository configuration is needed!

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

**Note**: This requires Java 9+ with the `jdk.attach.allowAttachSelf` system property or the `-XX:+EnableDynamicAgentLoading` flag. The `-javaagent` approach (Option 1) is recommended as it's simpler and doesn't require these additional flags.

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

### Complete Example

Here's a complete example that creates multiple thread pools and monitors them:

```java
import mlanett.tpe_agent.ThreadPoolExecutorMetrics;
import mlanett.tpe_agent.ThreadPoolExecutorRegistry;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ExampleApplication {
    
    private static final long RUN_DURATION_SECONDS = 10;
    private static final long REPORT_INTERVAL_SECONDS = 5;
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== ThreadPoolExecutor Agent Example ===");
        System.out.println("Creating thread pools and monitoring them for " + RUN_DURATION_SECONDS + " seconds...");
        System.out.println();
        
        // Create several thread pools with different configurations
        ThreadPoolExecutor fastPool = createPool("fast-pool", 2, 4);
        ThreadPoolExecutor slowPool = createPool("slow-pool", 1, 2);
        ThreadPoolExecutor singlePool = createPool("single-threaded", 1, 1);
        
        // Create a scheduled executor for periodic reporting
        ScheduledExecutorService reporter = Executors.newSingleThreadScheduledExecutor();
        
        // Start periodic reporting
        reporter.scheduleAtFixedRate(
            () -> reportMetrics(System.currentTimeMillis()),
            0,
            REPORT_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        );
        
        // Submit workloads to keep pools active
        AtomicInteger taskCounter = new AtomicInteger(0);
        
        // Fast pool: many short tasks
        ScheduledExecutorService fastWorkload = Executors.newSingleThreadScheduledExecutor();
        fastWorkload.scheduleAtFixedRate(() -> {
            fastPool.submit(() -> {
                try {
                    Thread.sleep(100); // Short task
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }, 0, 200, TimeUnit.MILLISECONDS);
        
        // Slow pool: fewer longer tasks
        ScheduledExecutorService slowWorkload = Executors.newSingleThreadScheduledExecutor();
        slowWorkload.scheduleAtFixedRate(() -> {
            slowPool.submit(() -> {
                try {
                    Thread.sleep(2000); // Longer task
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }, 0, 1500, TimeUnit.MILLISECONDS);
        
        // Single pool: occasional tasks
        ScheduledExecutorService singleWorkload = Executors.newSingleThreadScheduledExecutor();
        singleWorkload.scheduleAtFixedRate(() -> {
            singlePool.submit(() -> {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }, 0, 1000, TimeUnit.MILLISECONDS);
        
        // Run for the specified duration
        Thread.sleep(TimeUnit.SECONDS.toMillis(RUN_DURATION_SECONDS));
        
        // Shutdown workloads
        fastWorkload.shutdown();
        slowWorkload.shutdown();
        singleWorkload.shutdown();
        reporter.shutdown();
        
        // Final report
        System.out.println();
        System.out.println("=== Final Report ===");
        reportMetrics(System.currentTimeMillis());
        
        // Shutdown pools
        System.out.println();
        System.out.println("Shutting down thread pools...");
        shutdownPool("fast-pool", fastPool);
        shutdownPool("slow-pool", slowPool);
        shutdownPool("single-threaded", singlePool);
        
        System.out.println("Example completed.");
    }
    
    private static ThreadPoolExecutor createPool(String name, int coreSize, int maxSize) {
        AtomicInteger threadCounter = new AtomicInteger(0);
        
        ThreadFactory threadFactory = new ThreadFactory() {
            @SuppressWarnings("unused") // Used by reflection in ThreadPoolExecutorRegistry
            private final String namePrefix = name + "-thread-";
            
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, name + "-" + threadCounter.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            }
        };
        
        return new ThreadPoolExecutor(
            coreSize,
            maxSize,
            0L,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(10),
            threadFactory
        );
    }
    
    private static void reportMetrics(long timestamp) {
        Map<String, ThreadPoolExecutorMetrics> snapshot = 
            ThreadPoolExecutorRegistry.getInstance().snapshot();
        
        if (snapshot.isEmpty()) {
            System.out.println("[" + formatTime(timestamp) + "] No thread pools discovered yet.");
            return;
        }
        
        System.out.println("[" + formatTime(timestamp) + "] Discovered " + snapshot.size() + " thread pool(s):");
        
        for (Map.Entry<String, ThreadPoolExecutorMetrics> entry : snapshot.entrySet()) {
            String poolName = entry.getKey();
            ThreadPoolExecutorMetrics metrics = entry.getValue();
            
            System.out.printf("  %s:\n", poolName);
            System.out.printf("    Queue: %d | Active: %d/%d | Pool: %d (core=%d, max=%d) | Tasks: %d completed, %d total\n",
                metrics.getQueuedCount(),
                metrics.getActiveCount(),
                metrics.getPoolSize(),
                metrics.getPoolSize(),
                metrics.getCorePoolSize(),
                metrics.getMaximumPoolSize(),
                metrics.getCompletedTaskCount(),
                metrics.getTaskCount()
            );
        }
        System.out.println();
    }
    
    private static String formatTime(long timestamp) {
        long seconds = (timestamp / 1000) % 60;
        long minutes = (timestamp / 60000) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
    
    private static void shutdownPool(String name, ThreadPoolExecutor pool) {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                System.out.println("  " + name + ": Force shutting down...");
                pool.shutdownNow();
                pool.awaitTermination(5, TimeUnit.SECONDS);
            }
            System.out.println("  " + name + ": Shutdown complete");
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
```

See the `example/` directory in this repository for the complete working example.

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
