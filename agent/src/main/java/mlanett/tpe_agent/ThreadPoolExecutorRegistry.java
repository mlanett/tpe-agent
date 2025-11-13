package mlanett.tpe_agent;

import mlanett.tpe_agent.IThreadPoolMetrics;
import mlanett.tpe_agent.IThreadPoolRegistry;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import mlanett.tpe_agent.util.Logger;

/**
 * Registry of live ThreadPoolExecutor instances discovered via instrumentation.
 * Uses weak references so terminated pools can be reclaimed without leaking memory.
 * 
 * Implements the bootstrap IThreadPoolRegistry interface so it can be registered
 * and accessed across classloader boundaries.
 */
public final class ThreadPoolExecutorRegistry implements IThreadPoolRegistry {

    private static final ThreadPoolExecutorRegistry INSTANCE = new ThreadPoolExecutorRegistry();
    private static final Logger log = new Logger("ThreadPoolExecutorRegistry");

    private final ConcurrentHashMap<Integer, ThreadPoolEntry> pools = new ConcurrentHashMap<>();

    public static ThreadPoolExecutorRegistry getInstance() {
        return INSTANCE;
    }

    private ThreadPoolExecutorRegistry() {
        // Prevent instantiation
    }

    public void register(ThreadPoolExecutor executor) {
        int id = System.identityHashCode(executor);
        ThreadPoolEntry entry = pools.get(id);

        if (entry != null && entry.reference.get() == executor) {
            return;
        }

        String name = buildName(executor);
        pools.put(id, new ThreadPoolEntry(name, new WeakReference<>(executor)));
        log.debug("Registered ThreadPoolExecutor=" + name);
        cleanUp();
    }

    public void unregister(ThreadPoolExecutor executor) {
        int id = System.identityHashCode(executor);
        if (pools.remove(id) != null) {
            log.debug("Unregistered ThreadPoolExecutor: " + describe(executor));
        }
    }

    @Override
    public Map<String, IThreadPoolMetrics> snapshot() {
        cleanUp();

        Map<String, IThreadPoolMetrics> result = new HashMap<>();
        pools.entrySet().removeIf(entry -> {
            ThreadPoolExecutor executor = entry.getValue().reference.get();
            if (executor == null) {
                return true;
            } else {
                result.put(entry.getValue().name, ThreadPoolMetrics.of(executor));
                return false;
            }
        });

        return Collections.unmodifiableMap(result);
    }
    
    @Override
    public int getPoolCount() {
        cleanUp();
        return pools.size();
    }
    
    /**
     * Legacy method for backward compatibility.
     * Applications should use snapshot() which returns the bootstrap interface.
     */
    public Map<String, ThreadPoolExecutorMetrics> snapshotLegacy() {
        cleanUp();

        Map<String, ThreadPoolExecutorMetrics> result = new HashMap<>();
        pools.entrySet().removeIf(entry -> {
            ThreadPoolExecutor executor = entry.getValue().reference.get();
            if (executor == null) {
                return true;
            } else {
                result.put(entry.getValue().name, ThreadPoolExecutorMetrics.of(executor));
                return false;
            }
        });

        return result;
    }

    private static String buildName(ThreadPoolExecutor executor) {
        String base = resolveThreadFactoryPrefix(executor.getThreadFactory());
        if (base == null) {
            base = executor.getClass().getSimpleName();
            if (base.isEmpty()) {
                base = "ThreadPoolExecutor";
            }
        }
        String hexId = Integer.toHexString(System.identityHashCode(executor));
        return base + "@" + hexId;
    }

    private static String resolveThreadFactoryPrefix(ThreadFactory threadFactory) {
        if (threadFactory == null) {
            return null;
        }

        Class<?> clazz = threadFactory.getClass();
        try {
            // We do a bit of hacky magic because ThreadPoolExecutor doesn't HAVE a typed name.
            Field namePrefixField = clazz.getDeclaredField("namePrefix");
            namePrefixField.setAccessible(true);
            Object fieldValue = namePrefixField.get(threadFactory);
            if (fieldValue instanceof String) {
                String stringValue = (String) fieldValue;
                if (!stringValue.isBlank()) {
                    if (stringValue.endsWith("-thread-")) {
                        return stringValue.substring(0, stringValue.length() - "-thread-".length());
                    }
                    return stringValue;
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // Field doesn't exist or isn't accessible, fall through.
        }

        String description = threadFactory.toString();
        if (!description.isBlank()) {
            return description;
        }
        return clazz.getSimpleName();
    }

    private void cleanUp() {
        pools.entrySet().removeIf(entry -> entry.getValue().reference.get() == null);
    }

    private static String describe(ThreadPoolExecutor executor) {
        return executor.getClass().getSimpleName() + "@" +
               Integer.toHexString(System.identityHashCode(executor));
    }

    private static final class ThreadPoolEntry {
        final String name;
        final WeakReference<ThreadPoolExecutor> reference;

        ThreadPoolEntry(String name, WeakReference<ThreadPoolExecutor> reference) {
            this.name = name;
            this.reference = reference;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ThreadPoolEntry that = (ThreadPoolEntry) o;
            return Objects.equals(name, that.name) &&
                   Objects.equals(reference, that.reference);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, reference);
        }
    }
}
