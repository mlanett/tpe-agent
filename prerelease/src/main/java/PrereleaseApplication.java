import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import mlanett.tpe_agent.GlobalThreadPoolRegistry;
import mlanett.tpe_agent.IThreadPoolMetrics;

/**
 * Example application demonstrating ThreadPoolExecutor monitoring via the agent.
 * 
 * This application:
 * - Creates multiple ThreadPoolExecutors with different configurations
 * - Submits workloads to keep them active
 * - Periodically reports metrics to stdout
 * - Runs for approximately 60 seconds
 */
public class PrereleaseApplication {
    
    private static final long RUN_DURATION_SECONDS = 10;
    private static final long REPORT_INTERVAL_SECONDS = 5;
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("ThreadPoolExecutor Agent Example");
        System.out.println("Creating thread pools and monitoring them for " + RUN_DURATION_SECONDS + " seconds...");
        System.out.println();
        
        // Create several thread pools with different configurations
        ThreadPoolExecutor fastPool = createPool("fast-pool", 2, 4);
        ThreadPoolExecutor slowPool = createPool("slow-pool", 1, 2);
        ThreadPoolExecutor singlePool = createPool("single-threaded", 1, 1);
        
        // Create a scheduled executor for periodic reporting
        ScheduledExecutorService reporter = Executors.newSingleThreadScheduledExecutor();
        
        AtomicInteger maxDiscoveredPools = new AtomicInteger(0);

        // Start periodic reporting
        reporter.scheduleAtFixedRate(
            () -> reportMetrics(maxDiscoveredPools, System.currentTimeMillis()),
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
        System.out.println("Final Report");
        reportMetrics(maxDiscoveredPools, System.currentTimeMillis());
        
        // Shutdown pools
        System.out.println();
        System.out.println("Shutting down thread pools...");
        shutdownPool("fast-pool", fastPool);
        shutdownPool("slow-pool", slowPool);
        shutdownPool("single-threaded", singlePool);
        
        System.out.println("Example completed.");

        // Exit 1 if maxDiscoveredPools is 0
        if (maxDiscoveredPools.get() == 0) {
            System.err.println("No thread pools discovered. Exiting...");
            System.exit(1);
        }
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
    
    private static void reportMetrics(AtomicInteger maxDiscoveredPools, Long timestamp) {
        Map<String, IThreadPoolMetrics> snapshot = GlobalThreadPoolRegistry.snapshot();
        
        // Set atomic to the max of its current value and snapshot size
        maxDiscoveredPools.set(Math.max(maxDiscoveredPools.get(), snapshot.size()));

        if (snapshot.isEmpty()) {
            System.out.println("[" + formatTime(timestamp) + "] No thread pools discovered yet.");
            return;
        }
        
        System.out.println("[" + formatTime(timestamp) + "] Discovered " + snapshot.size() + " thread pool(s):");
        
        for (Map.Entry<String, IThreadPoolMetrics> entry : snapshot.entrySet()) {
            String poolName = entry.getKey();
            IThreadPoolMetrics metrics = entry.getValue();
            
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
