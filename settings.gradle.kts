rootProject.name = "tpe-agent"

include(
    "monitoring-agent",
    "tpe-agent",
    "demo",
)

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
