plugins {
    java
    kotlin("jvm") version "1.8.10"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib", libs.versions.kotlin.get()))
    implementation(kotlin("reflect", libs.versions.kotlin.get()))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${libs.versions.kotlinCoroutines.get()}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:${libs.versions.kotlinCoroutines.get()}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:${libs.versions.kotlinCoroutines.get()}")

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${libs.versions.kotlinCoroutines.get()}")
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("app.cash.turbine:turbine:0.12.1")
}

tasks.test {
    useJUnitPlatform()
}