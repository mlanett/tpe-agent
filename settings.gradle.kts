rootProject.name = "tpe-agent"

include(
    "example",
    "monitoring-agent",
    "monitoring-original",
    "quickcheck-agent",
    "quickcheck-original",
)

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
