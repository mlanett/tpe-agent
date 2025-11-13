import org.gradle.jvm.tasks.Jar

plugins {
    `java`
    application
}

application {
    mainClass.set("PrereleaseApplication")
}

repositories {
    mavenLocal()  // Check local Maven repository first (for testing unpublished versions)
    mavenCentral()
}

val tpe_javaagent: Configuration = 
    configurations.create("agent").apply {
        isCanBeConsumed = false // Other modules can not depend on this.
        isCanBeResolved = true // We can get actual jar files (!).
        isTransitive = false // Don't inherit dependencies from it (nor should we have any).
    }

dependencies {
    // Use the local agent module for API classes
    implementation(project(":agent"))
    
    // Bootstrap API classes are needed for compile-time access to IThreadPoolMetrics, etc.
    // At runtime these come from the agent JAR on the bootstrap classpath
    compileOnly(project(":bootstrap-api"))
    testImplementation(project(":bootstrap-api"))

    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.1")

    // Use the local agent JAR
    add(tpe_javaagent.name, project(path = ":agent", configuration = "agentJar"))
}

fun resolveAgentJar(): File {
    return checkNotNull(tpe_javaagent).singleFile
}

tasks.test {
    useJUnitPlatform()
    dependsOn(tpe_javaagent)

    doFirst {
        val agentJar = resolveAgentJar().absolutePath
        jvmArgs("-javaagent:$agentJar")
        jvmArgs("-XX:+EnableDynamicAgentLoading")
        jvmArgs("-Djdk.attach.allowAttachSelf=true")
        jvmArgs("--add-opens", "java.base/java.util.concurrent=ALL-UNNAMED")
    }
}

tasks.named<JavaExec>("run") {
    dependsOn(tpe_javaagent)

    doFirst {
        val agentJar = resolveAgentJar().absolutePath
        jvmArgs("-javaagent:$agentJar")
    }
}

tasks {
    val copyTpeAgent by registering(Copy::class) {
        from(tpe_javaagent)
        into(layout.buildDirectory.dir("libs"))
        rename { "tpe-agent-agent.jar" }
    }

    assemble {
        finalizedBy(copyTpeAgent)
    }
}

tasks.test {
    doFirst {
        jvmArgs("-javaagent:${tpe_javaagent.singleFile.absolutePath}")
    }
}

tasks.named<JavaExec>("run") {
    workingDir = project.rootProject.projectDir
    jvmArgs(
        "-javaagent:${tpe_javaagent.singleFile.absolutePath}"
    )
}
