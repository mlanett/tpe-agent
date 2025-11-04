package mlanett.tpe_agent;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Registry of live ThreadPoolExecutor instances discovered via instrumentation.
 * Uses weak references so terminated pools can be reclaimed without leaking memory.
 */
public final class ThreadPoolExecutorRegistry {

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
        log.info("Registered ThreadPoolExecutor=" + name);
        cleanUp();
    }

    public void unregister(ThreadPoolExecutor executor) {
        int id = System.identityHashCode(executor);
        if (pools.remove(id) != null) {
            log.debug("Unregistered ThreadPoolExecutor: " + describe(executor));
        }
    }

    public Map<String, ThreadPoolExecutorMetrics> snapshot() {
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
