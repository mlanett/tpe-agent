plugins {
    idea
}

group = "mlanett"
version = "0.0.4"

subprojects {
    apply(plugin = "java")

    repositories {
        mavenCentral()
    }

    group = rootProject.group
    version = rootProject.version

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        withSourcesJar()
        withJavadocJar()  // Required for Maven Central
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
