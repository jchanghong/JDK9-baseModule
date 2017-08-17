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

import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public final class Configuration {

    // @see Configuration#empty()
    private static final Configuration EMPTY_CONFIGURATION = new Configuration();

    // parent configurations, in search order
    private final List<Configuration> parents;

    private final Map<ResolvedModule, Set<ResolvedModule>> graph;
    private final Set<ResolvedModule> modules;
    private final Map<String, ResolvedModule> nameToModule;

    // constraint on target platform
    private final String targetPlatform;

    String targetPlatform() { return targetPlatform; }

    private Configuration() {
        this.parents = Collections.emptyList();
        this.graph = Collections.emptyMap();
        this.modules = Collections.emptySet();
        this.nameToModule = Collections.emptyMap();
        this.targetPlatform = null;
    }

    private Configuration(List<Configuration> parents,
                          Resolver resolver,
                          boolean check)
    {
        Map<ResolvedModule, Set<ResolvedModule>> g = resolver.finish(this, check);

        @SuppressWarnings(value = {"rawtypes", "unchecked"})
        Entry<String, ResolvedModule>[] nameEntries
            = (Entry<String, ResolvedModule>[])new Entry[g.size()];
        ResolvedModule[] moduleArray = new ResolvedModule[g.size()];
        int i = 0;
        for (ResolvedModule resolvedModule : g.keySet()) {
            moduleArray[i] = resolvedModule;
            nameEntries[i] = Map.entry(resolvedModule.name(), resolvedModule);
            i++;
        }

        this.parents = Collections.unmodifiableList(parents);
        this.graph = g;
        this.modules = Set.of(moduleArray);
        this.nameToModule = Map.ofEntries(nameEntries);

        this.targetPlatform = resolver.targetPlatform();
    }


    public Configuration resolve(ModuleFinder before,
                                 ModuleFinder after,
                                 Collection<String> roots)
    {
        return resolve(before, List.of(this), after, roots);
    }



    public Configuration resolveAndBind(ModuleFinder before,
                                        ModuleFinder after,
                                        Collection<String> roots)
    {
        return resolveAndBind(before, List.of(this), after, roots);
    }



    static Configuration resolveAndBind(ModuleFinder finder,
                                        Collection<String> roots,
                                        boolean check,
                                        PrintStream traceOutput)
    {
        List<Configuration> parents = List.of(empty());
        Resolver resolver = new Resolver(finder, parents, ModuleFinder.of(), traceOutput);
        resolver.resolve(roots).bind();

        return new Configuration(parents, resolver, check);
    }



    public static Configuration resolve(ModuleFinder before,
                                        List<Configuration> parents,
                                        ModuleFinder after,
                                        Collection<String> roots)
    {
        Objects.requireNonNull(before);
        Objects.requireNonNull(after);
        Objects.requireNonNull(roots);

        List<Configuration> parentList = new ArrayList<>(parents);
        if (parentList.isEmpty())
            throw new IllegalArgumentException("'parents' is empty");

        Resolver resolver = new Resolver(before, parentList, after, null);
        resolver.resolve(roots);

        return new Configuration(parentList, resolver, true);
    }


    public static Configuration resolveAndBind(ModuleFinder before,
                                               List<Configuration> parents,
                                               ModuleFinder after,
                                               Collection<String> roots)
    {
        Objects.requireNonNull(before);
        Objects.requireNonNull(after);
        Objects.requireNonNull(roots);

        List<Configuration> parentList = new ArrayList<>(parents);
        if (parentList.isEmpty())
            throw new IllegalArgumentException("'parents' is empty");

        Resolver resolver = new Resolver(before, parentList, after, null);
        resolver.resolve(roots).bind();

        return new Configuration(parentList, resolver, true);
    }



    public static Configuration empty() {
        return EMPTY_CONFIGURATION;
    }



    public List<Configuration> parents() {
        return parents;
    }



    public Set<ResolvedModule> modules() {
        return modules;
    }



    public Optional<ResolvedModule> findModule(String name) {
        Objects.requireNonNull(name);
        ResolvedModule m = nameToModule.get(name);
        if (m != null)
            return Optional.of(m);

        if (!parents.isEmpty()) {
            return configurations()
                    .skip(1)  // skip this configuration
                    .map(cf -> cf.nameToModule)
                    .filter(map -> map.containsKey(name))
                    .map(map -> map.get(name))
                    .findFirst();
        }

        return Optional.empty();
    }


    Set<ModuleDescriptor> descriptors() {
        if (modules.isEmpty()) {
            return Collections.emptySet();
        } else {
            return modules.stream()
                    .map(ResolvedModule::reference)
                    .map(ModuleReference::descriptor)
                    .collect(Collectors.toSet());
        }
    }

    Set<ResolvedModule> reads(ResolvedModule m) {
        return Collections.unmodifiableSet(graph.get(m));
    }


    Stream<Configuration> configurations() {
        List<Configuration> allConfigurations = this.allConfigurations;
        if (allConfigurations == null) {
            allConfigurations = new ArrayList<>();
            Set<Configuration> visited = new HashSet<>();
            Deque<Configuration> stack = new ArrayDeque<>();
            visited.add(this);
            stack.push(this);
            while (!stack.isEmpty()) {
                Configuration layer = stack.pop();
                allConfigurations.add(layer);

                // push in reverse order
                for (int i = layer.parents.size() - 1; i >= 0; i--) {
                    Configuration parent = layer.parents.get(i);
                    if (!visited.contains(parent)) {
                        visited.add(parent);
                        stack.push(parent);
                    }
                }
            }
            this.allConfigurations = Collections.unmodifiableList(allConfigurations);
        }
        return allConfigurations.stream();
    }

    private volatile List<Configuration> allConfigurations;



    @Override
    public String toString() {
        return modules().stream()
                .map(ResolvedModule::name)
                .collect(Collectors.joining(", "));
    }
}
