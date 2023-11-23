plugins {
    kotlin("jvm") version "1.9.20" apply (false)

    // derive gradle version from git tag
    id("pl.allegro.tech.build.axion-release") version "1.15.5"

    // publishing
    id("io.github.gradle-nexus.publish-plugin") version "1.3.0"
    id("signing")
    `maven-publish`

    // provide & configure dependencyUpdates
    id("com.github.ben-manes.versions") version "0.50.0"
    id("se.ascp.gradle.gradle-versions-filter") version "0.1.16"
}

ext.set("kotlinVersion", "1.9.20")
ext.set("ktorVersion", "2.3.6")

val libVersion: String = scmVersion.version
allprojects {
    repositories {
        mavenCentral()
    }
    group = "com.linked-planet"
    version = libVersion
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    signing {
        useInMemoryPgpKeys(
            findProperty("signingKey").toString(),
            findProperty("signingPassword").toString()
        )
        sign(publishing.publications)
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    tasks {
        register("javadocJar", Jar::class) {
            archiveClassifier.set("javadoc")
            from("${layout.buildDirectory}/javadoc")
        }
        register("sourcesJar", Jar::class) {
            archiveClassifier.set("sources")
            from("$projectDir/src/main/kotlin")
        }
    }
}

nexusPublishing {
    this.repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
        }
    }
}

// do not generate extra load on Nexus with new staging repository if signing fails
val initializeSonatypeStagingRepository by tasks.existing
initializeSonatypeStagingRepository {
    shouldRunAfter(tasks.withType<Sign>())
}
