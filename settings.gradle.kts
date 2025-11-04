rootProject.name = "tpe-agent"

include(
    "example",
    "monitoring-agent",
    "tpe-agent",
    "quickcheck-monitoring",
    "quickcheck-tpe",
)

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
