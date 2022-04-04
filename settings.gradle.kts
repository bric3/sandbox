import org.gradle.internal.jvm.inspection.JvmMetadataDetector
import org.gradle.jvm.toolchain.internal.JavaInstallationRegistry


// Required for nokee...
// See
// * https://github.com/nokeedev/gradle-native/issues/349
// * https://github.com/nokeedev/gradle-native/issues/350
pluginManagement {
  repositories {
    maven {
      name = "Nokee Release Repository"
      url = uri("https://repo.nokee.dev/release")
      mavenContent {
        includeGroupByRegex("dev\\.nokee.*")
        includeGroupByRegex("dev\\.gradleplugins.*")
      }
    }
    maven {
      name = "Nokee Snapshot Repository"
      url = uri("https://repo.nokee.dev/snapshot")
      mavenContent {
        includeGroupByRegex("dev\\.nokee.*")
        includeGroupByRegex("dev\\.gradleplugins.*")
      }
    }
    gradlePluginPortal()
  }
  val nokeeVersion = "0.4.556-202110111448.5620125b"  // found on https://services.nokee.dev/versions/all.json
  resolutionStrategy {
    eachPlugin {
      if (requested.id.id.startsWith("dev.nokee.")) {
        useModule("${requested.id.id}:${requested.id.id}.gradle.plugin:${nokeeVersion}")
      }
    }
  }
}


// Doc https://docs.gradle.org/7.2/userguide/platforms.html
// API https://docs.gradle.org/7.2/javadoc/org/gradle/api/initialization/dsl/VersionCatalogBuilder.html

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")



rootProject.name = "sandbox"

include("jmh-stuff", "jmh-panama")
include("jdk11", "jdk16", "jdk17")
include("cmem", "swift-app", "swift-library")
include("jmh-panama")
