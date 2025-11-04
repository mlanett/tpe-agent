plugins {
    application
}

dependencies {
    implementation(project(":tpe-agent"))
}

application {
    mainClass.set("Demo")
}

evaluationDependsOn(":tpe-agent")

tasks {
    val monitoringAgentAgentJar = project(":tpe-agent").tasks.named("agentJar")

    named<JavaExec>("run") {
        dependsOn(monitoringAgentAgentJar)
        doFirst {
            val agentJarFile = monitoringAgentAgentJar.get().outputs.files.singleFile
            jvmArgs("-javaagent:${agentJarFile.absolutePath}")
        }
    }
}

