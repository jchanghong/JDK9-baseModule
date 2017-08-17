/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package java.lang.module;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jdk.internal.module.ModuleBootstrap;
import jdk.internal.module.ModulePatcher;
import jdk.internal.module.ModulePath;
import jdk.internal.module.SystemModuleFinder;



public interface ModuleFinder {


    Optional<ModuleReference> find(String name);


    Set<ModuleReference> findAll();


    static ModuleFinder ofSystem() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission("accessSystemModules"));
            PrivilegedAction<ModuleFinder> pa = ModuleFinder::privilegedOfSystem;
            return AccessController.doPrivileged(pa);
        } else {
            return privilegedOfSystem();
        }
    }


    private static ModuleFinder privilegedOfSystem() {
        String home = System.getProperty("java.home");
        Path modules = Paths.get(home, "lib", "modules");
        if (Files.isRegularFile(modules)) {
            return SystemModuleFinder.getInstance();
        } else {
            Path dir = Paths.get(home, "modules");
            if (Files.isDirectory(dir)) {
                return privilegedOf(ModuleBootstrap.patcher(), dir);
            } else {
                throw new InternalError("Unable to detect the run-time image");
            }
        }
    }


    private static ModuleFinder privilegedOf(ModulePatcher patcher, Path dir) {
        ModuleFinder finder = ModulePath.of(patcher, dir);
        return new ModuleFinder() {
            @Override
            public Optional<ModuleReference> find(String name) {
                PrivilegedAction<Optional<ModuleReference>> pa = () -> finder.find(name);
                return AccessController.doPrivileged(pa);
            }
            @Override
            public Set<ModuleReference> findAll() {
                PrivilegedAction<Set<ModuleReference>> pa = finder::findAll;
                return AccessController.doPrivileged(pa);
            }
        };
    }


    static ModuleFinder of(Path... entries) {
        // special case zero entries
        if (entries.length == 0) {
            return new ModuleFinder() {
                @Override
                public Optional<ModuleReference> find(String name) {
                    Objects.requireNonNull(name);
                    return Optional.empty();
                }

                @Override
                public Set<ModuleReference> findAll() {
                    return Collections.emptySet();
                }
            };
        }

        return ModulePath.of(entries);
    }


    static ModuleFinder compose(ModuleFinder... finders) {
        // copy the list and check for nulls
        final List<ModuleFinder> finderList = List.of(finders);

        return new ModuleFinder() {
            private final Map<String, ModuleReference> nameToModule = new HashMap<>();
            private Set<ModuleReference> allModules;

            @Override
            public Optional<ModuleReference> find(String name) {
                // cached?
                ModuleReference mref = nameToModule.get(name);
                if (mref != null)
                    return Optional.of(mref);
                Optional<ModuleReference> omref = finderList.stream()
                        .map(f -> f.find(name))
                        .flatMap(Optional::stream)
                        .findFirst();
                omref.ifPresent(m -> nameToModule.put(name, m));
                return omref;
            }

            @Override
            public Set<ModuleReference> findAll() {
                if (allModules != null)
                    return allModules;
                // seed with modules already found
                Set<ModuleReference> result = new HashSet<>(nameToModule.values());
                finderList.stream()
                          .flatMap(f -> f.findAll().stream())
                          .forEach(mref -> {
                              String name = mref.descriptor().name();
                              if (nameToModule.putIfAbsent(name, mref) == null) {
                                  result.add(mref);
                              }
                          });
                allModules = Collections.unmodifiableSet(result);
                return allModules;
            }
        };
    }

}
