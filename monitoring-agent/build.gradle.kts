import org.gradle.jvm.tasks.Jar

plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    implementation("net.bytebuddy:byte-buddy:1.15.3")
    implementation("net.bytebuddy:byte-buddy-agent:1.15.3")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
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

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "mlanett"
            artifactId = "tpe-agent"
            version = project.version.toString()

            from(components["java"])
            
            // Also publish the agent JAR as an additional artifact
            artifact(agentJar) {
                classifier = "agent"
            }

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
        }
    }
    
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/mlanett/tpe-agent")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.token") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
