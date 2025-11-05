import org.gradle.jvm.tasks.Jar

plugins {
    `java`
    application
}

repositories {
    mavenLocal()  // Check local Maven repository first (for testing unpublished versions)
    mavenCentral()
}

// Version of tpe-agent to use - can be overridden with -PtpeAgentVersion=x.y.z
val AGENT_VERSION: String = project.findProperty("tpeAgentVersion") as String? ?: "0.0.16"

val agentConfiguration: Configuration = 
    configurations.create("agent").apply {
        isCanBeConsumed = false
        isCanBeResolved = true
        isTransitive = false
    }

dependencies {
    implementation("io.github.mlanett:tpe-agent:$AGENT_VERSION")

    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.1")

    add(agentConfiguration.name, "io.github.mlanett:tpe-agent:$AGENT_VERSION:agent@jar")
}

application {
    mainClass.set("ExampleApplication")
}

// Configure Java version
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

fun resolveAgentJar(): File {
    return checkNotNull(agentConfiguration).singleFile
}

tasks.test {
    useJUnitPlatform()
    dependsOn(agentConfiguration)

    doFirst {
        val agentJar = resolveAgentJar().absolutePath
        jvmArgs("-javaagent:$agentJar")
        jvmArgs("-XX:+EnableDynamicAgentLoading")
        jvmArgs("-Djdk.attach.allowAttachSelf=true")
        jvmArgs("--add-opens", "java.base/java.util.concurrent=ALL-UNNAMED")
    }
}

tasks.named<JavaExec>("run") {
    dependsOn(agentConfiguration)

    doFirst {
        val agentJar = resolveAgentJar().absolutePath
        jvmArgs("-javaagent:$agentJar")
    }
}
