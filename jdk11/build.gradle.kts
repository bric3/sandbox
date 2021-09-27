/*
 * MIT License
 *
 * Copyright (c) 2021 Brice Dutheil <brice.dutheil@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

dependencies {
  api(libs.commons.math)

  implementation(libs.guava)

  implementation(libs.bundles.bytebuddy)
  implementation(libs.bundles.okhttp)
  implementation(libs.conscrypt)

  implementation(files("lib/spring-jdbc-4.1.6.RELEASE.jar"))

  testImplementation(libs.junit.jupiter)
  testImplementation(libs.assertj)
  testImplementation(libs.testcontainers)
}


java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(11))
  }
}

tasks.withType<JavaCompile>() {
  options.release.set(11)
}

// pass args this way ./gradlew runSparkline --args="-f numbers"
tasks.register<JavaExec>("runSparkline") {
  dependsOn(tasks.compileJava)
  mainClass.set("sandbox.Sparkline")
  classpath(configurations.runtimeClasspath)
}

tasks.register<JavaExec>("runIsATTY") {
  dependsOn(tasks.compileJava)
  mainClass.set("sandbox.IsATTY")
  classpath(configurations.runtimeClasspath)
}