plugins {
    kotlin("jvm")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

description = "Awesome logging experience for Ktor HttpClient."
val nameHumanReadable = "Ktor Client Awesome Logging"

val kotlinVersion: String by project
val ktorVersion: String by project
dependencies {
    implementation(kotlin("stdlib", kotlinVersion))
    api("io.ktor", "ktor-client-core", ktorVersion)
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-slf4j", "1.7.3")
}

publishing {
    publications {
        create<MavenPublication>(project.name) {
            from(components["kotlin"])

            artifact(tasks.getByName<Zip>("javadocJar"))
            artifact(tasks.getByName<Zip>("sourcesJar"))

            pom {
                name.set(nameHumanReadable)
                description.set(project.description)
                url.set("https://github.com/linked-planet/ktor-plugins/${project.name}")
                inceptionYear.set("2023")
                licenses {
                    license {
                        name.set("The Apache Software License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        name.set("Alexander Weickmann")
                        email.set("alexander.weickmann@linked-planet.com")
                        url.set("https://github.com/weickmanna")
                        organization.set("linked-planet GmbH")
                        organizationUrl.set("https://linked-planet.com")
                    }
                }
                scm {
                    url.set("https://github.com/linked-planet/ktor-plugins.git")
                    connection.set("scm:git:git://github.com/linked-planet/ktor-plugins.git")
                    developerConnection.set("scm:git:git://github.com/linked-planet/ktor-plugins.git")
                }
            }
        }
    }
}
