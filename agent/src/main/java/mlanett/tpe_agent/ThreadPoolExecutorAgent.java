package mlanett.tpe_agent;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.JavaModule;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.isSubTypeOf;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.none;

import mlanett.tpe_agent.advice.ThreadPoolExecutorConstructorAdvice;
import mlanett.tpe_agent.advice.ThreadPoolExecutorShutdownAdvice;
import mlanett.tpe_agent.advice.ThreadPoolExecutorTerminatedAdvice;
import mlanett.tpe_agent.util.Logger;

/**
 * ByteBuddy agent which intercepts ThreadPoolExecutor lifecycle events so we can
 * register every pool instance with ThreadPoolExecutorRegistry.
 */
public final class ThreadPoolExecutorAgent {
    private static final Logger log = new Logger("ThreadPoolExecutorAgent");
    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);

    private ThreadPoolExecutorAgent() {
        // Prevent instantiation
    }

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        // Install the global registry first, before any instrumentation
        RegistryInstaller.installGlobalRegistry();
        install(instrumentation);
    }

    public static void agentmain(String agentArgs, Instrumentation instrumentation) {
        // Install the global registry first, before any instrumentation
        RegistryInstaller.installGlobalRegistry();
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
                        log.error("Failed to self-attach ByteBuddyAgent", throwable);
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
            log.info(String.format("Install ThreadPoolExecutor agent: retransformSupported=%b redefineSupported=%b", instrumentation.isRetransformClassesSupported(), instrumentation.isRedefineClassesSupported()));

            File injectionDir = createTempDir();

            new AgentBuilder.Default()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .with(AgentBuilder.LocationStrategy.ForClassLoader.STRONG
                    .withFallbackTo(ClassFileLocator.ForClassLoader.ofBootLoader()))
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
                        instrumentation.retransformClasses(ThreadPoolExecutor.class);
                    } else {
                        log.warn("ThreadPoolExecutor is not modifiable; instrumentation may not apply");
                    }
                } catch (UnmodifiableClassException exception) {
                    log.error("Failed to retransform ThreadPoolExecutor", exception);
                }
            }
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
                log.debug("Discovered ThreadPoolExecutor (loaded=" + loaded + ", loader=" + classLoader + ")");
            }
        }

        @Override
        public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, boolean loaded, DynamicType dynamicType) {
            if ("java.util.concurrent.ThreadPoolExecutor".equals(typeDescription.getName())) {
                log.debug("Transforming ThreadPoolExecutor");
            }
        }

        @Override
        public void onIgnored(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, boolean loaded) {
            if ("java.util.concurrent.ThreadPoolExecutor".equals(typeDescription.getName())) {
                log.debug("Ignored ThreadPoolExecutor during instrumentation");
            }
        }

        @Override
        public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded, Throwable throwable) {
            if ("java.util.concurrent.ThreadPoolExecutor".equals(typeName)) {
                log.error("Error instrumenting ThreadPoolExecutor", throwable);
            }
        }
    }
}
