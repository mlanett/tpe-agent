plugins {
    application
}

val AGENT = ":monitoring-agent"

dependencies {
    implementation(project(AGENT))
}

application {
    mainClass.set("Demo")
}

evaluationDependsOn(AGENT)

tasks {
    val monitoringAgentAgentJar = project(AGENT).tasks.named("agentJar")

    named<JavaExec>("run") {
        dependsOn(monitoringAgentAgentJar)
        doFirst {
            val agentJarFile = monitoringAgentAgentJar.get().outputs.files.singleFile
            jvmArgs("-javaagent:${agentJarFile.absolutePath}")
        }
    }
}

