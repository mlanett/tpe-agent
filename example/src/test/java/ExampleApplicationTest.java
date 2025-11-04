import mlanett.tpe_agent.ThreadPoolExecutorMetrics;
import mlanett.tpe_agent.ThreadPoolExecutorRegistry;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test verifying that the agent loaded via -javaagent parameter
 * correctly discovers ThreadPoolExecutor instances without manual registration.
 * 
 * This test verifies:
 * - Agent JAR can be downloaded from Maven Central
 * - Agent loads correctly via -javaagent parameter
 * - ThreadPoolExecutor instances are automatically discovered
 * - Library classes are accessible and work correctly
 */
class ExampleApplicationTest {

    @Test
    void agentDiscoversThreadPoolExecutorsAutomatically() throws InterruptedException {
        String poolName = "test-executor-auto-discovery";
        AtomicInteger threadCounter = new AtomicInteger(0);
        ThreadPoolExecutor testExecutor = makeExecutor(poolName, threadCounter);

        // Submit a task to ensure the pool is active
        testExecutor.submit(slowAction(200));
        Thread.sleep(100); // Allow time for task to start and agent to detect the pool

        // Verify automatic discovery - NO manual registration needed
        Map<String, ThreadPoolExecutorMetrics> executors = 
            ThreadPoolExecutorRegistry.getInstance().snapshot();
        
        String discoveredName = executors.keySet().stream()
            .filter(name -> name.startsWith(poolName))
            .findFirst()
            .orElse(null);

        assertNotNull(discoveredName, 
            "Test pool should be automatically discovered by agent. Found pools: " + executors.keySet());

        testExecutor.shutdown();
        assertTrue(testExecutor.awaitTermination(1, TimeUnit.SECONDS));
    }

    @Test
    void canReadThreadPoolExecutorMetrics() throws InterruptedException {
        String poolName = "test-executor-metrics";
        AtomicInteger threadCounter = new AtomicInteger(0);
        ThreadPoolExecutor testExecutor = makeExecutor(poolName, threadCounter);

        // Submit a task to activate the pool
        testExecutor.submit(slowAction(200));
        Thread.sleep(100); // Allow time for discovery and task execution

        // Verify metrics can be read
        Map<String, ThreadPoolExecutorMetrics> executors = 
            ThreadPoolExecutorRegistry.getInstance().snapshot();
        
        assertFalse(executors.isEmpty(), "Should have at least one executor discovered");

        String discoveredName = executors.keySet().stream()
            .filter(name -> name.startsWith(poolName))
            .findFirst()
            .orElse(null);
        
        assertNotNull(discoveredName, "Pool should be discovered");
        
        ThreadPoolExecutorMetrics metrics = executors.get(discoveredName);
        assertNotNull(metrics, "Metrics should be available");
        
        // Verify metric values are reasonable
        assertTrue(metrics.getQueueSize() >= 0, "Queue size should be non-negative");
        assertTrue(metrics.getActiveCount() >= 0, "Active count should be non-negative");
        assertTrue(metrics.getCompletedTaskCount() >= 0, "Completed task count should be non-negative");
        assertTrue(metrics.getCorePoolSize() > 0, "Core pool size should be positive");
        assertTrue(metrics.getMaximumPoolSize() > 0, "Maximum pool size should be positive");
        assertTrue(metrics.getPoolSize() >= 0, "Pool size should be non-negative");
        assertTrue(metrics.getTaskCount() >= 0, "Task count should be non-negative");

        testExecutor.shutdown();
        assertTrue(testExecutor.awaitTermination(1, TimeUnit.SECONDS));
    }

    private ThreadPoolExecutor makeExecutor(String poolName, AtomicInteger threadCounter) {
        // Create a ThreadFactory with a namePrefix field that the registry can discover
        ThreadFactory threadFactory = new ThreadFactory() {
            @SuppressWarnings("unused") // Used by reflection in ThreadPoolExecutorRegistry
            private final String namePrefix = poolName + "-thread-";

            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, poolName + "-" + threadCounter.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            }
        };

        return new ThreadPoolExecutor(
            /* core pool size */ 1,
            /* maximum pool size */ 1,
            /* keep alive time */ 0L,
            /* time unit */ TimeUnit.MILLISECONDS,
            /* work queue */ new ArrayBlockingQueue<>(1),
            threadFactory
        );
    }

    private Runnable slowAction(long sleepMs) {
        return () -> {
            try {
                Thread.sleep(sleepMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
    }
}

