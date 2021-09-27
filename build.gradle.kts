plugins {
  id("com.github.ben-manes.versions") version ("0.39.0")
}


// lookout for TYPESAFE_PROJECT_ACCESSORS feature preview
val javaProjects = subprojects - project(":swift-app") - project(":swift-library")

configure(javaProjects) {
  apply(plugin = "java-library")

  repositories {
    mavenCentral()
  }

  tasks.withType<Test>() {
    useJUnitPlatform()
    testLogging {
      showStandardStreams = true
      exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
      events("skipped", "failed")
    }
  }
}