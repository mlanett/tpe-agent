package mlanett.tpe_agent;

import java.util.Collections;
import java.util.Map;

/**
 * Static facade providing access to the IThreadPoolRegistry implementation.
 * This class is on the bootstrap classpath and provides a shared access point
 * across all classloaders.
 */
public final class ThreadPoolRegistrySingleton {
    
    private static final IThreadPoolRegistry NO_OP = new NoOpThreadPoolRegistry();
    
    private static volatile IThreadPoolRegistry provider = NO_OP;
    
    /**
     * Register a IThreadPoolRegistry implementation if none has been registered yet.
     * This method is thread-safe and idempotent.
     * 
     * @param registry the registry implementation to register
     */
    public static synchronized void registerIfAbsent(IThreadPoolRegistry registry) {
        if (registry != null && provider == NO_OP) {
            provider = registry;
        }
    }
    
    /**
     * Force-register a IThreadPoolRegistry implementation, replacing any existing one.
     * This should only be used for testing.
     * 
     * @param registry the registry implementation to register
     */
    public static synchronized void forceRegister(IThreadPoolRegistry registry) {
        if (registry != null) {
            provider = registry;
        }
    }
    
    /**
     * Get the currently registered IThreadPoolRegistry.
     * 
     * @return the registered registry, or a no-op implementation if none registered
     */
    public static IThreadPoolRegistry get() {
        return provider;
    }
    
    /**
     * Check if a real registry has been registered.
     * 
     * @return true if a registry is registered, false if using no-op
     */
    public static boolean isRegistered() {
        return provider != NO_OP;
    }
    
    /**
     * Get a snapshot of all tracked thread pools.
     * Convenience method that delegates to get().snapshot().
     * 
     * @return map of pool names to metrics
     */
    public static Map<String, IThreadPoolMetrics> snapshot() {
        return provider.snapshot();
    }
    
    /**
     * Get the count of tracked thread pools.
     * Convenience method that delegates to get().getPoolCount().
     * 
     * @return number of tracked pools
     */
    public static int getPoolCount() {
        return provider.getPoolCount();
    }
    
    // Not intended to be instantiated
    private ThreadPoolRegistrySingleton() {
    }
    
    /**
     * No-op implementation used when no real registry is registered.
     */
    private static final class NoOpThreadPoolRegistry implements IThreadPoolRegistry {
        
        @Override
        public Map<String, IThreadPoolMetrics> snapshot() {
            return Collections.emptyMap();
        }
        
        @Override
        public int getPoolCount() {
            return 0;
        }
    }
}
