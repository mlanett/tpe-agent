plugins {
    idea
}

group = "io.github.mlanett"
version = "0.0.13"

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
        // Don't automatically add sourcesJar/javadocJar for all subprojects
        // Let plugins (like vanniktech maven-publish) handle this automatically
        // withSourcesJar() and withJavadocJar() are handled by the vanniktech plugin
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
