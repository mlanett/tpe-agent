package mlanett.tpe_agent;

import java.util.Map;

/**
 * Registry interface for tracking ThreadPoolExecutor instances.
 * This interface is on the bootstrap classpath and visible to all classloaders.
 * 
 * The actual implementation is provided by the agent and registered via
 * ThreadPoolRegistrySingleton.registerIfAbsent().
 */
public interface IThreadPoolRegistry {
    
    /**
     * Returns a snapshot of all currently tracked thread pools and their metrics.
     * The map keys are pool names (e.g., "fast-pool@a1b2c3d4").
     * 
     * @return immutable map of pool names to their current metrics
     */
    Map<String, IThreadPoolMetrics> snapshot();
    
    /**
     * Returns the number of thread pools currently being tracked.
     * 
     * @return count of active pools
     */
    int getPoolCount();
}
