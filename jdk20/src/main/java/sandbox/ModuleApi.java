package sandbox;

import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.util.List;

public class ModuleApi {
    public static void main(String[] args) throws ClassNotFoundException {
        findSystemModule();


        // TODO create module dynamically

        loadClassInAnotherModulePath(
                null,
                "java.desktop",
                "my.ModuleMain"
        );
    }

    private static void loadClassInAnotherModulePath(
            Path theJarPath,
            String moduleName,
            String className
    ) throws ClassNotFoundException {
        ModuleFinder systemFinder = ModuleFinder.of(theJarPath);
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
        moduleLayer.findLoader(moduleName).loadClass(className);
    }

    private static void findSystemModule() {
        ModuleLayer.boot().findModule("java.desktop").ifPresent(System.out::println);
    }
}
