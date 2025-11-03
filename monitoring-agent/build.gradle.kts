import org.gradle.jvm.tasks.Jar
import org.gradle.plugins.signing.SigningExtension

plugins {
    `java-library`
    `maven-publish`
    signing
    id("com.vanniktech.maven.publish") version "0.34.0"
}

dependencies {
    implementation("net.bytebuddy:byte-buddy:1.15.3")
    implementation("net.bytebuddy:byte-buddy-agent:1.15.3")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.1")
}

tasks.test {
    // Enable dynamic agent loading for tests
    jvmArgs(
        "-XX:+EnableDynamicAgentLoading",
        "-Djdk.attach.allowAttachSelf=true"
    )
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
            "Boot-Class-Path" to "monitoring-agent-agent.jar"
        )
    }
}

tasks.assemble { dependsOn(agentJar) }

// Configure Maven Central publishing using vanniktech plugin
// This plugin handles Central Publisher Portal configuration automatically
// OSSRH was sunset on June 30, 2025, replaced by Central Publisher Portal
mavenPublishing {
    // Use io.github.mlanett as namespace (recommended for GitHub users)
    // If you want to use "mlanett", you need to verify the namespace at https://central.sonatype.com
    coordinates("io.github.mlanett", "tpe-agent", project.version.toString())
    
    pom {
        name.set("ThreadPoolExecutor Agent")
        description.set("A JVM agent for tracking ThreadPoolExecutor instances to monitor and prevent memory issues")
        url.set("https://github.com/mlanett/tpe-agent")
        
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        
        developers {
            developer {
                id.set("mlanett")
                name.set("Mark Lanett")
                organization.set("mlanett")
                organizationUrl.set("https://github.com/mlanett")
            }
        }
        
        scm {
            connection.set("scm:git:git://github.com/mlanett/tpe-agent.git")
            developerConnection.set("scm:git:ssh://github.com:mlanett/tpe-agent.git")
            url.set("https://github.com/mlanett/tpe-agent")
        }
    }
    
    val mavenCentralUsername =
        (project.findProperty("mavenCentralUsername") as String?)
            ?: System.getenv("ORG_GRADLE_PROJECT_mavenCentralUsername")
            ?: System.getenv("MAVEN_CENTRAL_USERNAME")
    val mavenCentralPassword =
        (project.findProperty("mavenCentralPassword") as String?)
            ?: System.getenv("ORG_GRADLE_PROJECT_mavenCentralPassword")
            ?: System.getenv("MAVEN_CENTRAL_PASSWORD")
    
    if (mavenCentralUsername != null && mavenCentralPassword != null) {
        // Publish to Maven Central via Central Publisher Portal
        // The plugin handles the Central Portal API configuration automatically
        publishToMavenCentral()
        
        // Configure signing (handled by plugin)
        val signingKeyRaw = (
            project.findProperty("signing.key") as String?
                ?: System.getenv("ORG_GRADLE_PROJECT_signingKey")
                ?: System.getenv("SIGNING_KEY")
        )
        // Handle newlines: GitHub secrets may have literal \n or actual newlines
        val signingKey = signingKeyRaw
            ?.replace("\\n", "\n")  // Replace escaped newlines
            ?.replace("\r\n", "\n") // Normalize Windows line endings
            ?.replace("\r", "\n")   // Normalize Mac line endings
        val signingPassword =
            (project.findProperty("signing.password") as String?)
                ?: System.getenv("ORG_GRADLE_PROJECT_signingPassword")
                ?: System.getenv("SIGNING_PASSWORD")
        
        if (signingKey != null && signingPassword != null) {
            // Debug: Log key info (without exposing the full key)
            logger.lifecycle("Signing configuration:")
            logger.lifecycle("  - Signing key present: ${signingKey.isNotBlank()}")
            logger.lifecycle("  - Signing key length: ${signingKey.length}")
            logger.lifecycle("  - Signing key starts with: ${signingKey.take(50)}...")
            logger.lifecycle("  - Signing key ends with: ...${signingKey.takeLast(50)}")
            logger.lifecycle("  - Signing key contains BEGIN: ${signingKey.contains("BEGIN PGP PRIVATE KEY")}")
            logger.lifecycle("  - Signing key contains END: ${signingKey.contains("END PGP PRIVATE KEY")}")
            logger.lifecycle("  - Signing password present: ${signingPassword.isNotBlank()}")
            
            try {
                extensions.configure<SigningExtension>("signing") {
                    useInMemoryPgpKeys(signingKey, signingPassword)
                }
                signAllPublications()
                logger.lifecycle("✓ Signing configured successfully")
            } catch (e: Exception) {
                logger.error("✗ Failed to configure signing", e)
                logger.error("Error message: ${e.message}")
                logger.error("Error class: ${e.javaClass.name}")
                if (e.cause != null) {
                    logger.error("Caused by: ${e.cause}")
                }
                throw e
            }
        } else {
            logger.warn("Signing credentials not found. Publications will not be signed.")
            logger.warn("  - SIGNING_KEY present: ${System.getenv("SIGNING_KEY") != null}")
            logger.warn("  - ORG_GRADLE_PROJECT_signingKey present: ${System.getenv("ORG_GRADLE_PROJECT_signingKey") != null}")
            logger.warn("  - signing.key property present: ${project.findProperty("signing.key") != null}")
        }
    } else {
        logger.warn("Maven Central credentials not found. Publishing to Maven Central will be skipped.")
    }
}

// Add the agent JAR as an additional artifact to the plugin's publication
// This needs to be done after the plugin has created the publication
afterEvaluate {
    publishing.publications.named<MavenPublication>("maven") {
        artifact(agentJar) {
            classifier = "agent"
        }
    }
    
    // Fix task dependency: ensure metadata generation waits for javadoc JAR
    tasks.named("generateMetadataFileForMavenPublication").configure {
        dependsOn(tasks.named("plainJavadocJar"))
    }
}
