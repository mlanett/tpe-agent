rootProject.name = "tpe-agent"

include(
    "demo",
    "example",
    "monitoring-agent",
    "tpe-agent",
)

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
