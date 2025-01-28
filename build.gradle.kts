/*
 * sandbox
 *
 * Copyright (c) 2021,today - Brice Dutheil <brice.dutheil@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
import org.jetbrains.gradle.ext.copyright
import org.jetbrains.gradle.ext.settings

plugins {
  id("sandbox.build-stats-conventions")
  alias(libs.plugins.ideaExt)
}

idea {
  project.settings {
    withIDEADir {
      val scope = resolve("scopes")
      scope.mkdirs()

      val file = scope.resolve("sandbox.xml")
      file.writeText("<component/>")

      withIDEAFileXml(file.toRelativeString(this)) {
        val root = this.asNode()
        root.attributes()["name"] = "DependencyValidationManager"
        root.appendNode(
          "scope", mapOf(
            "name" to rootProject.name,
            "pattern" to "file[${rootProject.name}]:*/"
          )
        )
      }
    }

    copyright {
      val mpl2Copyright = profiles.create("MPL2") {
        keyword = "MPL2"
        notice = rootProject.file("HEADER").readLines().joinToString("\n") { line ->
          line.replace("\${year}", "2021, today")
            .replace("\${name}", "Brice Dutheil")
        }
      }
      scopes = mapOf(
        rootProject.name to mpl2Copyright.name
      )
    }
  }
}