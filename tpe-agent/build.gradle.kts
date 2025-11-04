import org.gradle.jvm.tasks.Jar

plugins {
    `java-library`
}

/*
 * Temporary (until this module is extracted):
 * The root project applies the Kotlin JVM plugin to every subproject,
 * which forces its BOM into our shadow jar file.
 * Thus, we must UNDO it.
 */

plugins.withId("org.jetbrains.kotlin.jvm") {
    // Turn off Kotlin for this project
    tasks.named("compileKotlin") { enabled = false }
    tasks.named("compileTestKotlin") { enabled = false }

    configurations.all {
        exclude(group = "org.jetbrains.kotlin")
    }
}

dependencies {
    implementation("net.bytebuddy:byte-buddy:1.15.3")
    implementation("net.bytebuddy:byte-buddy-agent:1.15.3")
}

val agentJar = tasks.register<Jar>("agentJar") {
    archiveClassifier.set("agent")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(sourceSets.main.get().output)
    from({
        configurations.runtimeClasspath.get()
            .filter { it.exists() }
            .map { if (it.isDirectory) it else zipTree(it) }
    })

    manifest {
        attributes(
            "Premain-Class" to "mlanett.tpe_agent.ThreadPoolExecutorAgent",
            "Agent-Class" to "mlanett.tpe_agent.ThreadPoolExecutorAgent",
            "Can-Redefine-Classes" to "true",
            "Can-Retransform-Classes" to "true",
            "Boot-Class-Path" to "tpe-agent-agent.jar"
        )
    }
}

tasks.assemble { dependsOn(agentJar) }
