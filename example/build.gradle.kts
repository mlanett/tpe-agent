import org.gradle.api.tasks.bundling.Jar

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
val tpeAgentVersion: String =
    project.findProperty("tpeAgentVersion") as String? ?: rootProject.version.toString()

val agentProjectPath = ":monitoring-agent"
val agentProject = rootProject.findProject(agentProjectPath)

if (agentProject != null) {
    evaluationDependsOn(agentProjectPath)
}

val agentConfiguration: Configuration? = if (agentProject == null) {
    configurations.create("agent").apply {
        isCanBeConsumed = false
        isCanBeResolved = true
        isTransitive = false
    }
} else {
    null
}

dependencies {
    if (agentProject != null) {
        implementation(project(agentProjectPath))
    } else {
        implementation("io.github.mlanett:tpe-agent:$tpeAgentVersion")
    }

    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.1")

    if (agentConfiguration != null) {
        add(agentConfiguration.name, "io.github.mlanett:tpe-agent:$tpeAgentVersion:agent@jar")
    }
}

application {
    mainClass.set("mlanett.tpe_agent.example.ExampleApplication")
}

// Configure Java version
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

fun resolveAgentJar(): File {
    return if (agentProject != null) {
        project(agentProjectPath)
            .tasks
            .named<Jar>("agentJar")
            .get()
            .archiveFile
            .get()
            .asFile
    } else {
        checkNotNull(agentConfiguration).singleFile
    }
}

tasks.test {
    useJUnitPlatform()
    if (agentProject != null) {
        dependsOn(project(agentProjectPath).tasks.named("agentJar"))
    } else {
        dependsOn(agentConfiguration)
    }

    doFirst {
        val agentJar = resolveAgentJar().absolutePath
        jvmArgs("-javaagent:$agentJar")
        jvmArgs("-XX:+EnableDynamicAgentLoading")
        jvmArgs("-Djdk.attach.allowAttachSelf=true")
        jvmArgs("--add-opens", "java.base/java.util.concurrent=ALL-UNNAMED")
    }
}

tasks.named<JavaExec>("run") {
    if (agentProject != null) {
        dependsOn(project(agentProjectPath).tasks.named("agentJar"))
    } else {
        dependsOn(agentConfiguration)
    }

    doFirst {
        val agentJar = resolveAgentJar().absolutePath
        jvmArgs("-javaagent:$agentJar")
    }
}
