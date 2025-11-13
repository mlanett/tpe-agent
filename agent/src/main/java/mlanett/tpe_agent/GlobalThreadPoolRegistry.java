package mlanett.tpe_agent;

import mlanett.tpe_agent.ThreadPoolRegistrySingleton;
import mlanett.tpe_agent.IThreadPoolMetrics;
import mlanett.tpe_agent.IThreadPoolRegistry;

import java.util.Map;

/**
 * Application-facing API for accessing thread pool metrics.
 * This provides a convenient facade over the bootstrap API.
 *
 * Applications can depend on this API at compile time and access
 * thread pool metrics collected by the agent at runtime.
 *
 * Example usage:
 * <pre>
 * Map pools = GlobalThreadPoolRegistry.snapshot();
 * for (Map.Entry entry : pools.entrySet()) {
 *     System.out.println(entry.getKey() + ": " +
 *         entry.getValue().getActiveCount() + " active threads");
 * }
 * </pre>
 */
public final class GlobalThreadPoolRegistry {

    /**
     * Get a snapshot of all tracked thread pools and their metrics.
     *
     * @return immutable map of pool names to their current metrics
     */
    public static Map<String, IThreadPoolMetrics> snapshot() {
        return ThreadPoolRegistrySingleton.snapshot();
    }

    /**
     * Get the number of thread pools currently being tracked.
     *
     * @return count of active pools
     */
    public static int getPoolCount() {
        return ThreadPoolRegistrySingleton.getPoolCount();
    }

    /**
     * Check if the agent has been installed and registered.
     *
     * @return true if agent is active, false otherwise
     */
    public static boolean isAgentInstalled() {
        return ThreadPoolRegistrySingleton.isRegistered();
    }

    /**
     * Get the underlying registry implementation.
     * Most applications should use the convenience methods instead.
     *
     * @return the registered IThreadPoolRegistry
     */
    public static IThreadPoolRegistry getRegistry() {
        return ThreadPoolRegistrySingleton.get();
    }

    // Not intended to be instantiated
    private GlobalThreadPoolRegistry() {
    }
}


