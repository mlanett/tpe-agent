plugins {
    `java`
    application
}

repositories {
    mavenLocal()  // Check local Maven repository first (for testing unpublished versions)
    mavenCentral()
}

// Version of tpe-agent to use - can be overridden with -PtpeAgentVersion=x.y.z
// Defaults to the root project version if available locally, otherwise latest published version
val tpeAgentVersion: String = project.findProperty("tpeAgentVersion") as String? ?: rootProject.version.toString()

dependencies {
    // Library dependency for accessing ThreadPoolExecutorRegistry and ThreadPoolExecutorMetrics
    implementation("io.github.mlanett:tpe-agent:$tpeAgentVersion")

    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.1")
}

// Configuration to download the agent JAR
val agentConfiguration: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

dependencies {
    agentConfiguration("io.github.mlanett:tpe-agent:$tpeAgentVersion:agent@jar")
}

application {
    mainClass.set("mlanett.tpe_agent.example.ExampleApplication")
}

// Configure Java version
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.test {
    useJUnitPlatform()
    // Ensure the agent JAR is resolved before tests run
    dependsOn(agentConfiguration)
    
    // Set jvmArgs - this must be done during configuration, not execution
    doFirst {
        val agentJar = agentConfiguration.singleFile.absolutePath
        jvmArgs("-javaagent:$agentJar")
        // Enable retransformation for already-loaded classes
        jvmArgs("-XX:+EnableDynamicAgentLoading")
        jvmArgs("-Djdk.attach.allowAttachSelf=true")
        // Open java.base module for instrumentation (required for ThreadPoolExecutor)
        jvmArgs("--add-opens", "java.base/java.util.concurrent=ALL-UNNAMED")
    }
}

// Task to run the example application with the agent
tasks.named<JavaExec>("run") {
    doFirst {
        val agentJar = agentConfiguration.singleFile.absolutePath
        jvmArgs("-javaagent:$agentJar")
    }
}
