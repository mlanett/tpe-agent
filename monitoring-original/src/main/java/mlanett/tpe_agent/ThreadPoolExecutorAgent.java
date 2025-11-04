package mlanett.tpe_agent;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.JavaModule;
import net.bytebuddy.dynamic.ClassFileLocator;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Collections;

import static net.bytebuddy.matcher.ElementMatchers.any;
import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.isSubTypeOf;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.none;

/**
 * Byte Buddy agent that intercepts ThreadPoolExecutor lifecycle events so we can
 * register every pool instance with ThreadPoolExecutorRegistry.
 */
public final class ThreadPoolExecutorAgent {
    private static final Logger log = new Logger("ThreadPoolExecutorAgent");
    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);

    static {
        System.out.println("ThreadPoolExecutorAgent loaded (class loader=" + ThreadPoolExecutorAgent.class.getClassLoader() + ")");
    }

    private ThreadPoolExecutorAgent() {
        // Prevent instantiation
    }

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        install(instrumentation);
    }

    public static void agentmain(String agentArgs, Instrumentation instrumentation) {
        install(instrumentation);
    }

    public static void ensureInstalled() {
        if (!INSTALLED.get()) {
            synchronized (INSTALLED) {
                if (!INSTALLED.get()) {
                    Instrumentation instrumentation = null;
                    try {
                        instrumentation = ByteBuddyAgent.install();
                    } catch (Throwable throwable) {
                        log.warn("Failed to self-attach ByteBuddyAgent", throwable);
                    }

                    if (instrumentation != null) {
                        install(instrumentation);
                    }
                }
            }
        }
    }

    private static void install(Instrumentation instrumentation) {
        if (INSTALLED.compareAndSet(false, true)) {
            log.info("Installing ThreadPoolExecutor agent; retransformSupported="
                + instrumentation.isRetransformClassesSupported()
                + ", redefineSupported=" + instrumentation.isRedefineClassesSupported());

            File injectionDir = createTempDir();

            new AgentBuilder.Default()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .with(AgentBuilder.LocationStrategy.ForClassLoader.STRONG
                    .withFallbackTo(ClassFileLocator.ForClassLoader.ofBootLoader()))
                .with(AgentBuilder.Listener.StreamWriting.toSystemOut())
                .with(new DebugListener())
                .with(new AgentBuilder.Listener.ModuleReadEdgeCompleting(
                    instrumentation,
                    true,
                    Collections.singleton(JavaModule.ofType(ThreadPoolExecutorConstructorAdvice.class))))
                .ignore(none())
                .with(new AgentBuilder.InjectionStrategy.UsingInstrumentation(instrumentation, injectionDir))
                .assureReadEdgeTo(
                    instrumentation,
                    ThreadPoolExecutorConstructorAdvice.class,
                    ThreadPoolExecutorShutdownAdvice.class,
                    ThreadPoolExecutorTerminatedAdvice.class,
                    ThreadPoolExecutorRegistry.class
                )
                .type(isSubTypeOf(ThreadPoolExecutor.class))
                .transform(new AgentBuilder.Transformer() {
                    @Override
                    public DynamicType.Builder<?> transform(
                            DynamicType.Builder<?> builder,
                            TypeDescription typeDescription,
                            ClassLoader classLoader,
                            JavaModule module,
                            ProtectionDomain protectionDomain) {
                        log.info("Transforming ThreadPoolExecutor subclass: " + typeDescription.getName());
                        return builder
                            .visit(Advice.to(ThreadPoolExecutorConstructorAdvice.class).on(isConstructor()))
                            .visit(Advice.to(ThreadPoolExecutorShutdownAdvice.class)
                                .on(named("shutdown").or(named("shutdownNow"))))
                            .visit(Advice.to(ThreadPoolExecutorTerminatedAdvice.class).on(named("terminated")));
                    }
                })
                .installOn(instrumentation);

            if (isLoaded(ThreadPoolExecutor.class)) {
                try {
                    if (instrumentation.isModifiableClass(ThreadPoolExecutor.class)) {
                        log.info("Retransforming already loaded ThreadPoolExecutor");
                        instrumentation.retransformClasses(ThreadPoolExecutor.class);
                        log.info("Retransformation requested");
                    } else {
                        log.warn("ThreadPoolExecutor is not modifiable; instrumentation may not apply", null);
                    }
                } catch (UnmodifiableClassException exception) {
                    log.warn("Failed to retransform ThreadPoolExecutor", exception);
                }
            }

            log.info("Installed ThreadPoolExecutor instrumentation");
        }
    }

    private static boolean isLoaded(Class<?> clazz) {
        try {
            ClassLoader loader = clazz.getClassLoader();
            if (loader == null) {
                return true;
            }
            return Class.forName(clazz.getName(), false, loader) != null;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }

    private static File createTempDir() {
        try {
            File dir = java.nio.file.Files.createTempDirectory("tpe-agent").toFile();
            dir.deleteOnExit();
            return dir;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to create temporary directory for agent injection", exception);
        }
    }

    private static class DebugListener extends AgentBuilder.Listener.Adapter {
        @Override
        public void onDiscovery(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded) {
            if ("java.util.concurrent.ThreadPoolExecutor".equals(typeName)) {
                log.info("Discovered ThreadPoolExecutor (loaded=" + loaded + ", loader=" + classLoader + ")");
            }
        }

        @Override
        public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, boolean loaded, DynamicType dynamicType) {
            if ("java.util.concurrent.ThreadPoolExecutor".equals(typeDescription.getName())) {
                log.info("Transforming ThreadPoolExecutor");
            }
        }

        @Override
        public void onIgnored(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, boolean loaded) {
            if ("java.util.concurrent.ThreadPoolExecutor".equals(typeDescription.getName())) {
                log.info("Ignored ThreadPoolExecutor during instrumentation");
            }
        }

        @Override
        public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded, Throwable throwable) {
            if ("java.util.concurrent.ThreadPoolExecutor".equals(typeName)) {
                log.warn("Error instrumenting ThreadPoolExecutor", throwable);
            }
        }
    }
}
