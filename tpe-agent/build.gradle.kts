import org.gradle.jvm.tasks.Jar

plugins {
    `java-library`
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
