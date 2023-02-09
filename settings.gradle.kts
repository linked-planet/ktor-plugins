pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "ktor-plugins"

include("ktor-server-onelogin-saml", "ktor-client-awesome-logging")
