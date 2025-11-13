package mlanett.tpe_agent;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test ThreadPoolExecutor discovery via Byte Buddy instrumentation and fallbacks.
 */
class ThreadPoolExecutorTest {

    @BeforeAll
    static void installAgent() {
        ThreadPoolExecutorAgent.ensureInstalled();
    }

    @Test
    void canManuallyRegisterThreadPoolExecutors() throws InterruptedException {
        String poolName = "test-executor-manual";
        AtomicInteger threadCounter = new AtomicInteger(0);
        ThreadPoolExecutor testExecutor = makeExecutor(poolName, threadCounter);

        // Manually register since auto-instrumentation may not work in all test environments
        ThreadPoolExecutorRegistry.getInstance().register(testExecutor);

        testExecutor.submit(slowAction(200));
        Thread.sleep(50); // Ensure slow action starts

        Map<String, ThreadPoolExecutorMetrics> executors = ThreadPoolExecutorRegistry.getInstance().snapshotLegacy();
        String discoveredName = executors.keySet().stream()
            .filter(name -> name.startsWith(poolName))
            .findFirst()
            .orElse(null);

        assertNotNull(discoveredName, "Test pool should be discovered. Found pools: " + executors.keySet());

        testExecutor.shutdown();
        assertTrue(testExecutor.awaitTermination(1, TimeUnit.SECONDS));
    }

    @Test
    void canSummarizeThreadPoolExecutorMetrics() throws InterruptedException {
        String poolName = "test-executor-metrics";
        AtomicInteger threadCounter = new AtomicInteger(0);
        ThreadPoolExecutor testExecutor = makeExecutor(poolName, threadCounter);

        // Manually register since auto-instrumentation may not work in all test environments
        ThreadPoolExecutorRegistry.getInstance().register(testExecutor);

        testExecutor.submit(slowAction(200));
        Thread.sleep(100); // Ensure slow action starts

        Map<String, ThreadPoolExecutorMetrics> executors = ThreadPoolExecutorRegistry.getInstance().snapshotLegacy();
        assertFalse(executors.isEmpty(), "Should have at least one executor");

        String firstPool = executors.keySet().iterator().next();
        ThreadPoolExecutorMetrics summary = executors.get(firstPool);

        assertNotNull(summary);
        assertTrue(summary.getQueuedCount() >= 0, "Queue size should be non-negative");
        assertTrue(summary.getActiveCount() >= 0, "Active count should be non-negative");
        assertTrue(summary.getCompletedTaskCount() >= 0, "Completed task count should be non-negative");
        assertTrue(summary.getCorePoolSize() > 0, "Core pool size should be positive");
        assertTrue(summary.getMaximumPoolSize() > 0, "Maximum pool size should be positive");

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

