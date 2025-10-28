plugins {
    idea
}

subprojects {
    apply(plugin = "java")

    repositories {
        mavenCentral()
    }

    group = "mlanett"

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        withSourcesJar()
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
