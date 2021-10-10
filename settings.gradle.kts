// Doc https://docs.gradle.org/7.2/userguide/platforms.html
// API https://docs.gradle.org/7.2/javadoc/org/gradle/api/initialization/dsl/VersionCatalogBuilder.html
enableFeaturePreview("VERSION_CATALOGS")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")



rootProject.name = "sandbox"

include("jmh-stuff")
include("jdk11", "jdk16", "jdk17")
include("cmem", "swift-app", "swift-library")

// version catalog declared in ./gradle/libs.versions.toml
