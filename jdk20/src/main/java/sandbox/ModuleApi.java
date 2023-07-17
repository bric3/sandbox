/*
 * sandbox
 *
 * Copyright (c) 2021,today - Brice Dutheil <brice.dutheil@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package sandbox;

import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class ModuleApi {
    public static void main(String[] args) throws ClassNotFoundException {
        findSystemModule("java.desktop");


        // TODO create module dynamically

        // Stupid code
        var c = loadClassInAnotherModulePath(
                null,
                "java.desktop",
                "my.ModuleMain"
        );
        System.out.println(
                "loaded in '" +
                "java.desktop" +
                "'? " + c
        );
    }

    @SuppressWarnings("unchecked")
    private static <T> Optional<Class<T>> loadClassInAnotherModulePath(
            Path theJarPath,
            String moduleName,
            String className
    ) throws ClassNotFoundException {
        ModuleFinder systemFinder = theJarPath == null ? ModuleFinder.of() : ModuleFinder.of(theJarPath);
        ModuleLayer bootLayout = ModuleLayer.boot();

        // resolves the configuration that has bootLayout as parent
        Configuration newConfiguration = bootLayout.configuration().resolve(
                systemFinder,
                ModuleFinder.of(),
                List.of(moduleName)
        );

        ModuleLayer moduleLayer = bootLayout.defineModulesWithOneLoader(
                newConfiguration,
                ClassLoader.getSystemClassLoader()
        );
        ClassLoader loader = moduleLayer.findLoader(moduleName);
        if (loader == null) {
            // system class loader
            return Optional.empty();
        }
        return Optional.of((Class<T>) loader.loadClass(className));
    }

    private static void findSystemModule(String moduleName) {
        ModuleLayer.boot().findModule(moduleName).ifPresent(System.out::println);
    }
}
