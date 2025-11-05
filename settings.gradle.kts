rootProject.name = "tpe-agent"

include(
    "example",
    "monitoring-agent",
    "quickcheck",
)

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
