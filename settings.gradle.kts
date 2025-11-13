rootProject.name = "tpe-agent"

include(
    "agent",
    "bootstrap-api",
    "example",
    "prerelease",
)

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
