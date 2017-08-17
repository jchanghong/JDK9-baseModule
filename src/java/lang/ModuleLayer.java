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

package java.lang;

import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ResolvedModule;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jdk.internal.loader.ClassLoaderValue;
import jdk.internal.loader.Loader;
import jdk.internal.loader.LoaderPool;
import jdk.internal.module.ServicesCatalog;
import sun.security.util.SecurityConstants;




public final class ModuleLayer {

    // the empty layer
    private static final ModuleLayer EMPTY_LAYER
        = new ModuleLayer(Configuration.empty(), List.of(), null);

    // the configuration from which this ;ayer was created
    private final Configuration cf;

    // parent layers, empty in the case of the empty layer
    private final List<ModuleLayer> parents;

    // maps module name to jlr.Module
    private final Map<String, Module> nameToModule;


    private ModuleLayer(Configuration cf,
                        List<ModuleLayer> parents,
                        Function<String, ClassLoader> clf)
    {
        this.cf = cf;
        this.parents = parents; // no need to do defensive copy

        Map<String, Module> map;
        if (parents.isEmpty()) {
            map = Collections.emptyMap();
        } else {
            map = Module.defineModules(cf, clf, this);
        }
        this.nameToModule = map; // no need to do defensive copy
    }


    public static final class Controller {
        private final ModuleLayer layer;

        Controller(ModuleLayer layer) {
            this.layer = layer;
        }


        public ModuleLayer layer() {
            return layer;
        }

        private void ensureInLayer(Module source) {
            if (source.getLayer() != layer)
                throw new IllegalArgumentException(source + " not in layer");
        }



        public Controller addReads(Module source, Module target) {
            ensureInLayer(source);
            source.implAddReads(target);
            return this;
        }


        public Controller addExports(Module source, String pn, Module target) {
            ensureInLayer(source);
            source.implAddExports(pn, target);
            return this;
        }


        public Controller addOpens(Module source, String pn, Module target) {
            ensureInLayer(source);
            source.implAddOpens(pn, target);
            return this;
        }
    }



    public ModuleLayer defineModulesWithOneLoader(Configuration cf,
                                                  ClassLoader parentLoader) {
        return defineModulesWithOneLoader(cf, List.of(this), parentLoader).layer();
    }



    public ModuleLayer defineModulesWithManyLoaders(Configuration cf,
                                                    ClassLoader parentLoader) {
        return defineModulesWithManyLoaders(cf, List.of(this), parentLoader).layer();
    }



    public ModuleLayer defineModules(Configuration cf,
                                     Function<String, ClassLoader> clf) {
        return defineModules(cf, List.of(this), clf).layer();
    }


    public static Controller defineModulesWithOneLoader(Configuration cf,
                                                        List<ModuleLayer> parentLayers,
                                                        ClassLoader parentLoader)
    {
        List<ModuleLayer> parents = new ArrayList<>(parentLayers);
        checkConfiguration(cf, parents);

        checkCreateClassLoaderPermission();
        checkGetClassLoaderPermission();

        try {
            Loader loader = new Loader(cf.modules(), parentLoader);
            loader.initRemotePackageMap(cf, parents);
            ModuleLayer layer =  new ModuleLayer(cf, parents, mn -> loader);
            return new Controller(layer);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new LayerInstantiationException(e.getMessage());
        }
    }


    public static Controller defineModulesWithManyLoaders(Configuration cf,
                                                          List<ModuleLayer> parentLayers,
                                                          ClassLoader parentLoader)
    {
        List<ModuleLayer> parents = new ArrayList<>(parentLayers);
        checkConfiguration(cf, parents);

        checkCreateClassLoaderPermission();
        checkGetClassLoaderPermission();

        LoaderPool pool = new LoaderPool(cf, parents, parentLoader);
        try {
            ModuleLayer layer = new ModuleLayer(cf, parents, pool::loaderFor);
            return new Controller(layer);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new LayerInstantiationException(e.getMessage());
        }
    }


    public static Controller defineModules(Configuration cf,
                                           List<ModuleLayer> parentLayers,
                                           Function<String, ClassLoader> clf)
    {
        List<ModuleLayer> parents = new ArrayList<>(parentLayers);
        checkConfiguration(cf, parents);
        Objects.requireNonNull(clf);

        checkGetClassLoaderPermission();

        // The boot layer is checked during module system initialization
        if (boot() != null) {
            checkForDuplicatePkgs(cf, clf);
        }

        try {
            ModuleLayer layer = new ModuleLayer(cf, parents, clf);
            return new Controller(layer);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new LayerInstantiationException(e.getMessage());
        }
    }



    private static void checkConfiguration(Configuration cf,
                                           List<ModuleLayer> parentLayers)
    {
        Objects.requireNonNull(cf);

        List<Configuration> parentConfigurations = cf.parents();
        if (parentLayers.size() != parentConfigurations.size())
            throw new IllegalArgumentException("wrong number of parents");

        int index = 0;
        for (ModuleLayer parent : parentLayers) {
            if (parent.configuration() != parentConfigurations.get(index)) {
                throw new IllegalArgumentException(
                        "Parent of configuration != configuration of this Layer");
            }
            index++;
        }
    }

    private static void checkCreateClassLoaderPermission() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
            sm.checkPermission(SecurityConstants.CREATE_CLASSLOADER_PERMISSION);
    }

    private static void checkGetClassLoaderPermission() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
            sm.checkPermission(SecurityConstants.GET_CLASSLOADER_PERMISSION);
    }


    private static void checkForDuplicatePkgs(Configuration cf,
                                              Function<String, ClassLoader> clf)
    {
        // HashMap allows null keys
        Map<ClassLoader, Set<String>> loaderToPackages = new HashMap<>();
        for (ResolvedModule resolvedModule : cf.modules()) {
            ModuleDescriptor descriptor = resolvedModule.reference().descriptor();
            ClassLoader loader = clf.apply(descriptor.name());

            Set<String> loaderPackages
                = loaderToPackages.computeIfAbsent(loader, k -> new HashSet<>());

            for (String pkg : descriptor.packages()) {
                boolean added = loaderPackages.add(pkg);
                if (!added) {
                    throw fail("More than one module with package %s mapped" +
                               " to the same class loader", pkg);
                }
            }
        }
    }


    private static LayerInstantiationException fail(String fmt, Object ... args) {
        String msg = String.format(fmt, args);
        return new LayerInstantiationException(msg);
    }



    public Configuration configuration() {
        return cf;
    }



    public List<ModuleLayer> parents() {
        return parents;
    }



    Stream<ModuleLayer> layers() {
        List<ModuleLayer> allLayers = this.allLayers;
        if (allLayers != null)
            return allLayers.stream();

        allLayers = new ArrayList<>();
        Set<ModuleLayer> visited = new HashSet<>();
        Deque<ModuleLayer> stack = new ArrayDeque<>();
        visited.add(this);
        stack.push(this);

        while (!stack.isEmpty()) {
            ModuleLayer layer = stack.pop();
            allLayers.add(layer);

            // push in reverse order
            for (int i = layer.parents.size() - 1; i >= 0; i--) {
                ModuleLayer parent = layer.parents.get(i);
                if (!visited.contains(parent)) {
                    visited.add(parent);
                    stack.push(parent);
                }
            }
        }

        this.allLayers = allLayers = Collections.unmodifiableList(allLayers);
        return allLayers.stream();
    }

    private volatile List<ModuleLayer> allLayers;


    public Set<Module> modules() {
        Set<Module> modules = this.modules;
        if (modules == null) {
            this.modules = modules =
                Collections.unmodifiableSet(new HashSet<>(nameToModule.values()));
        }
        return modules;
    }

    private volatile Set<Module> modules;



    public Optional<Module> findModule(String name) {
        Objects.requireNonNull(name);
        if (this == EMPTY_LAYER)
            return Optional.empty();
        Module m = nameToModule.get(name);
        if (m != null)
            return Optional.of(m);

        return layers()
                .skip(1)  // skip this layer
                .map(l -> l.nameToModule)
                .filter(map -> map.containsKey(name))
                .map(map -> map.get(name))
                .findAny();
    }



    public ClassLoader findLoader(String name) {
        Optional<Module> om = findModule(name);

        // can't use map(Module::getClassLoader) as class loader can be null
        if (om.isPresent()) {
            return om.get().getClassLoader();
        } else {
            throw new IllegalArgumentException("Module " + name
                                               + " not known to this layer");
        }
    }


    @Override
    public String toString() {
        return modules().stream()
                .map(Module::getName)
                .collect(Collectors.joining(", "));
    }


    public static ModuleLayer empty() {
        return EMPTY_LAYER;
    }



    public static ModuleLayer boot() {
        return System.bootLayer;
    }


    ServicesCatalog getServicesCatalog() {
        ServicesCatalog servicesCatalog = this.servicesCatalog;
        if (servicesCatalog != null)
            return servicesCatalog;

        synchronized (this) {
            servicesCatalog = this.servicesCatalog;
            if (servicesCatalog == null) {
                servicesCatalog = ServicesCatalog.create();
                nameToModule.values().forEach(servicesCatalog::register);
                this.servicesCatalog = servicesCatalog;
            }
        }

        return servicesCatalog;
    }

    private volatile ServicesCatalog servicesCatalog;



    void bindToLoader(ClassLoader loader) {
        // CLV.computeIfAbsent(loader, (cl, clv) -> new CopyOnWriteArrayList<>())
        List<ModuleLayer> list = CLV.get(loader);
        if (list == null) {
            list = new CopyOnWriteArrayList<>();
            List<ModuleLayer> previous = CLV.putIfAbsent(loader, list);
            if (previous != null) list = previous;
        }
        list.add(this);
    }


    static Stream<ModuleLayer> layers(ClassLoader loader) {
        List<ModuleLayer> list = CLV.get(loader);
        if (list != null) {
            return list.stream();
        } else {
            return Stream.empty();
        }
    }

    // the list of layers with modules defined to a class loader
    private static final ClassLoaderValue<List<ModuleLayer>> CLV = new ClassLoaderValue<>();
}
