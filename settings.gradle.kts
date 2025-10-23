rootProject.name = "tpe-agent"

include(
    "monitoring-agent",
)

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
