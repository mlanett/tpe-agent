package mlanett.tpe_agent;

import mlanett.tpe_agent.ThreadPoolRegistrySingleton;
import mlanett.tpe_agent.util.Logger;

/**
 * Installs the ThreadPoolExecutorRegistry into the global bootstrap registry.
 * 
 * By registering the registry in the bootstrap API's static field, we make it
 * accessible to all classloaders including application code.
 */
final class RegistryInstaller {
    
    private static final Logger log = new Logger("RegistryInstaller");
    
    /**
     * Install the registry into the global ThreadPoolRegistrySingleton.
     * This should be called once during agent initialization.
     */
    static void installGlobalRegistry() {
        try {
            ThreadPoolExecutorRegistry registry = ThreadPoolExecutorRegistry.getInstance();
            ThreadPoolRegistrySingleton.registerIfAbsent(registry);
            
            if (ThreadPoolRegistrySingleton.isRegistered()) {
                log.info("Global thread pool registry installed");
            } else {
                log.warn("Failed to register thread pool registry - another registry may already be installed");
            }
        } catch (RuntimeException e) {
            log.error("Failed to register thread pool registry", e);
            throw e;
        }
    }
    
    /**
     * Force-install the registry, replacing any existing one.
     * This should only be used for testing.
     */
    static void forceInstallGlobalRegistry() {
        try {
            log.warn("Force-installing global registry. This is not intended for production use.");
            
            ThreadPoolExecutorRegistry registry = ThreadPoolExecutorRegistry.getInstance();
            ThreadPoolRegistrySingleton.forceRegister(registry);
            
            log.info("Global thread pool registry force-installed");
        } catch (RuntimeException e) {
            log.error("Failed to force-register thread pool registry", e);
            throw e;
        }
    }
    
    private RegistryInstaller() {
        // Prevent instantiation
    }
}

