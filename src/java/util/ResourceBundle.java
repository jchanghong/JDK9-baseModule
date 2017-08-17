/*
 * Copyright (c) 1996, 2017, Oracle and/or its affiliates. All rights reserved.
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

/*
 * (C) Copyright Taligent, Inc. 1996, 1997 - All Rights Reserved
 * (C) Copyright IBM Corp. 1996 - 1999 - All Rights Reserved
 *
 * The original version of this source code and documentation
 * is copyrighted and owned by Taligent, Inc., a wholly-owned
 * subsidiary of IBM. These materials are provided under terms
 * of a License Agreement between Taligent and Sun. This technology
 * is protected by multiple US and International patents.
 *
 * This notice and attribution to Taligent may not be removed.
 * Taligent is a registered trademark of Taligent, Inc.
 *
 */

package java.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.jar.JarEntry;
import java.util.spi.ResourceBundleControlProvider;
import java.util.spi.ResourceBundleProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jdk.internal.loader.BootLoader;
import jdk.internal.misc.JavaUtilResourceBundleAccess;
import jdk.internal.misc.SharedSecrets;
import jdk.internal.reflect.CallerSensitive;
import jdk.internal.reflect.Reflection;
import sun.security.action.GetPropertyAction;
import sun.util.locale.BaseLocale;
import sun.util.locale.LocaleObjectCache;
import static sun.security.util.SecurityConstants.GET_CLASSLOADER_PERMISSION;



public abstract class ResourceBundle {


    private static final int INITIAL_CACHE_SIZE = 32;

    static {
        SharedSecrets.setJavaUtilResourceBundleAccess(
            new JavaUtilResourceBundleAccess() {
                @Override
                public void setParent(ResourceBundle bundle,
                                      ResourceBundle parent) {
                    bundle.setParent(parent);
                }

                @Override
                public ResourceBundle getParent(ResourceBundle bundle) {
                    return bundle.parent;
                }

                @Override
                public void setLocale(ResourceBundle bundle, Locale locale) {
                    bundle.locale = locale;
                }

                @Override
                public void setName(ResourceBundle bundle, String name) {
                    bundle.name = name;
                }

                @Override
                public ResourceBundle getBundle(String baseName, Locale locale, Module module) {
                    // use the given module as the caller to bypass the access check
                    return getBundleImpl(module, module,
                                         baseName, locale,
                                         getDefaultControl(module, baseName));
                }

                @Override
                public ResourceBundle newResourceBundle(Class<? extends ResourceBundle> bundleClass) {
                    return ResourceBundleProviderHelper.newResourceBundle(bundleClass);
                }
            });
    }


    private static final ResourceBundle NONEXISTENT_BUNDLE = new ResourceBundle() {
            public Enumeration<String> getKeys() { return null; }
            protected Object handleGetObject(String key) { return null; }
            public String toString() { return "NONEXISTENT_BUNDLE"; }
        };



    private static final ConcurrentMap<CacheKey, BundleReference> cacheList
        = new ConcurrentHashMap<>(INITIAL_CACHE_SIZE);


    private static final ReferenceQueue<Object> referenceQueue = new ReferenceQueue<>();


    public String getBaseBundleName() {
        return name;
    }


    protected ResourceBundle parent = null;


    private Locale locale = null;


    private String name;


    private volatile boolean expired;


    private volatile CacheKey cacheKey;


    private volatile Set<String> keySet;


    public ResourceBundle() {
    }


    public final String getString(String key) {
        return (String) getObject(key);
    }


    public final String[] getStringArray(String key) {
        return (String[]) getObject(key);
    }


    public final Object getObject(String key) {
        Object obj = handleGetObject(key);
        if (obj == null) {
            if (parent != null) {
                obj = parent.getObject(key);
            }
            if (obj == null) {
                throw new MissingResourceException("Can't find resource for bundle "
                                                   +this.getClass().getName()
                                                   +", key "+key,
                                                   this.getClass().getName(),
                                                   key);
            }
        }
        return obj;
    }


    public Locale getLocale() {
        return locale;
    }

    private static ClassLoader getLoader(Module module) {
        PrivilegedAction<ClassLoader> pa = module::getClassLoader;
        return AccessController.doPrivileged(pa);
    }


    private static ClassLoader getLoaderForControl(Module module) {
        ClassLoader loader = getLoader(module);
        return loader == null ? ClassLoader.getSystemClassLoader() : loader;
    }


    protected void setParent(ResourceBundle parent) {
        assert parent != NONEXISTENT_BUNDLE;
        this.parent = parent;
    }


    private static final class CacheKey {
        // These four are the actual keys for lookup in Map.
        private final String name;
        private volatile Locale locale;
        private final KeyElementReference<Module> moduleRef;
        private final KeyElementReference<Module> callerRef;
        // this is the part of hashCode that pertains to module and callerModule
        // which can be GCed..
        private final int modulesHash;

        // bundle format which is necessary for calling
        // Control.needsReload().
        private volatile String format;

        // These time values are in CacheKey so that NONEXISTENT_BUNDLE
        // doesn't need to be cloned for caching.

        // The time when the bundle has been loaded
        private volatile long loadTime;

        // The time when the bundle expires in the cache, or either
        // Control.TTL_DONT_CACHE or Control.TTL_NO_EXPIRATION_CONTROL.
        private volatile long expirationTime;

        // Placeholder for an error report by a Throwable
        private volatile Throwable cause;

        // ResourceBundleProviders for loading ResourceBundles
        private volatile ServiceLoader<ResourceBundleProvider> providers;
        private volatile boolean providersChecked;

        // Boolean.TRUE if the factory method caller provides a ResourceBundleProvier.
        private volatile Boolean callerHasProvider;

        CacheKey(String baseName, Locale locale, Module module, Module caller) {
            Objects.requireNonNull(module);
            Objects.requireNonNull(caller);

            this.name = baseName;
            this.locale = locale;
            this.moduleRef = new KeyElementReference<>(module, referenceQueue, this);
            this.callerRef = new KeyElementReference<>(caller, referenceQueue, this);
            this.modulesHash = module.hashCode() ^ caller.hashCode();
        }

        CacheKey(CacheKey src) {
            // Create References to src's modules
            this.moduleRef = new KeyElementReference<>(
                Objects.requireNonNull(src.getModule()), referenceQueue, this);
            this.callerRef = new KeyElementReference<>(
                Objects.requireNonNull(src.getCallerModule()), referenceQueue, this);
            // Copy fields from src. ResourceBundleProviders related fields
            // and "cause" should not be copied.
            this.name = src.name;
            this.locale = src.locale;
            this.modulesHash = src.modulesHash;
            this.format = src.format;
            this.loadTime = src.loadTime;
            this.expirationTime = src.expirationTime;
        }

        String getName() {
            return name;
        }

        Locale getLocale() {
            return locale;
        }

        CacheKey setLocale(Locale locale) {
            this.locale = locale;
            return this;
        }

        Module getModule() {
            return moduleRef.get();
        }

        Module getCallerModule() {
            return callerRef.get();
        }

        ServiceLoader<ResourceBundleProvider> getProviders() {
            if (!providersChecked) {
                providers = getServiceLoader(getModule(), name);
                providersChecked = true;
            }
            return providers;
        }

        boolean hasProviders() {
            return getProviders() != null;
        }

        boolean callerHasProvider() {
            return callerHasProvider == Boolean.TRUE;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            try {
                final CacheKey otherEntry = (CacheKey)other;
                //quick check to see if they are not equal
                if (modulesHash != otherEntry.modulesHash) {
                    return false;
                }
                //are the names the same?
                if (!name.equals(otherEntry.name)) {
                    return false;
                }
                // are the locales the same?
                if (!locale.equals(otherEntry.locale)) {
                    return false;
                }
                // are modules and callerModules the same and non-null?
                Module module = getModule();
                Module caller = getCallerModule();
                return ((module != null) && (module.equals(otherEntry.getModule())) &&
                        (caller != null) && (caller.equals(otherEntry.getCallerModule())));
            } catch (NullPointerException | ClassCastException e) {
            }
            return false;
        }

        @Override
        public int hashCode() {
            return (name.hashCode() << 3) ^ locale.hashCode() ^ modulesHash;
        }

        String getFormat() {
            return format;
        }

        void setFormat(String format) {
            this.format = format;
        }

        private void setCause(Throwable cause) {
            if (this.cause == null) {
                this.cause = cause;
            } else {
                // Override the cause if the previous one is
                // ClassNotFoundException.
                if (this.cause instanceof ClassNotFoundException) {
                    this.cause = cause;
                }
            }
        }

        private Throwable getCause() {
            return cause;
        }

        @Override
        public String toString() {
            String l = locale.toString();
            if (l.length() == 0) {
                if (locale.getVariant().length() != 0) {
                    l = "__" + locale.getVariant();
                } else {
                    l = "\"\"";
                }
            }
            return "CacheKey[" + name +
                   ", locale=" + l +
                   ", module=" + getModule() +
                   ", callerModule=" + getCallerModule() +
                   ", format=" + format +
                   "]";
        }
    }


    private static interface CacheKeyReference {
        public CacheKey getCacheKey();
    }


    private static class KeyElementReference<T> extends WeakReference<T>
                                                implements CacheKeyReference {
        private final CacheKey cacheKey;

        KeyElementReference(T referent, ReferenceQueue<Object> q, CacheKey key) {
            super(referent, q);
            cacheKey = key;
        }

        @Override
        public CacheKey getCacheKey() {
            return cacheKey;
        }
    }


    private static class BundleReference extends SoftReference<ResourceBundle>
                                         implements CacheKeyReference {
        private final CacheKey cacheKey;

        BundleReference(ResourceBundle referent, ReferenceQueue<Object> q, CacheKey key) {
            super(referent, q);
            cacheKey = key;
        }

        @Override
        public CacheKey getCacheKey() {
            return cacheKey;
        }
    }


    @CallerSensitive
    public static final ResourceBundle getBundle(String baseName)
    {
        Class<?> caller = Reflection.getCallerClass();
        return getBundleImpl(baseName, Locale.getDefault(),
                             caller, getDefaultControl(caller, baseName));
    }


    @CallerSensitive
    public static final ResourceBundle getBundle(String baseName,
                                                 Control control) {
        Class<?> caller = Reflection.getCallerClass();
        Locale targetLocale = Locale.getDefault();
        checkNamedModule(caller);
        return getBundleImpl(baseName, targetLocale, caller, control);
    }


    @CallerSensitive
    public static final ResourceBundle getBundle(String baseName,
                                                 Locale locale)
    {
        Class<?> caller = Reflection.getCallerClass();
        return getBundleImpl(baseName, locale,
                             caller, getDefaultControl(caller, baseName));
    }


    @CallerSensitive
    public static ResourceBundle getBundle(String baseName, Module module) {
        return getBundleFromModule(Reflection.getCallerClass(), module, baseName,
                                   Locale.getDefault(),
                                   getDefaultControl(module, baseName));
    }


    @CallerSensitive
    public static ResourceBundle getBundle(String baseName, Locale targetLocale, Module module) {
        return getBundleFromModule(Reflection.getCallerClass(), module, baseName, targetLocale,
                                   getDefaultControl(module, baseName));
    }


    @CallerSensitive
    public static final ResourceBundle getBundle(String baseName, Locale targetLocale,
                                                 Control control) {
        Class<?> caller = Reflection.getCallerClass();
        checkNamedModule(caller);
        return getBundleImpl(baseName, targetLocale, caller, control);
    }


    @CallerSensitive
    public static ResourceBundle getBundle(String baseName, Locale locale,
                                           ClassLoader loader)
    {
        if (loader == null) {
            throw new NullPointerException();
        }
        Class<?> caller = Reflection.getCallerClass();
        return getBundleImpl(baseName, locale, caller, loader, getDefaultControl(caller, baseName));
    }


    @CallerSensitive
    public static ResourceBundle getBundle(String baseName, Locale targetLocale,
                                           ClassLoader loader, Control control) {
        if (loader == null || control == null) {
            throw new NullPointerException();
        }
        Class<?> caller = Reflection.getCallerClass();
        checkNamedModule(caller);
        return getBundleImpl(baseName, targetLocale, caller, loader, control);
    }

    private static Control getDefaultControl(Class<?> caller, String baseName) {
        return getDefaultControl(caller.getModule(), baseName);
    }

    private static Control getDefaultControl(Module targetModule, String baseName) {
        return targetModule.isNamed() ?
            Control.INSTANCE :
            ResourceBundleControlProviderHolder.getControl(baseName);
    }

    private static class ResourceBundleControlProviderHolder {
        private static final PrivilegedAction<List<ResourceBundleControlProvider>> pa =
            () -> {
                return Collections.unmodifiableList(
                    ServiceLoader.load(ResourceBundleControlProvider.class,
                                       ClassLoader.getSystemClassLoader()).stream()
                        .map(ServiceLoader.Provider::get)
                        .collect(Collectors.toList()));
            };

        private static final List<ResourceBundleControlProvider> CONTROL_PROVIDERS =
            AccessController.doPrivileged(pa);

        private static Control getControl(String baseName) {
            return CONTROL_PROVIDERS.isEmpty() ?
                Control.INSTANCE :
                CONTROL_PROVIDERS.stream()
                    .flatMap(provider -> Stream.ofNullable(provider.getControl(baseName)))
                    .findFirst()
                    .orElse(Control.INSTANCE);
        }
    }

    private static void checkNamedModule(Class<?> caller) {
        if (caller.getModule().isNamed()) {
            throw new UnsupportedOperationException(
                    "ResourceBundle.Control not supported in named modules");
        }
    }

    private static ResourceBundle getBundleImpl(String baseName,
                                                Locale locale,
                                                Class<?> caller,
                                                Control control) {
        return getBundleImpl(baseName, locale, caller, caller.getClassLoader(), control);
    }


    private static ResourceBundle getBundleImpl(String baseName,
                                                Locale locale,
                                                Class<?> caller,
                                                ClassLoader loader,
                                                Control control) {
        if (caller == null) {
            throw new InternalError("null caller");
        }
        Module callerModule = caller.getModule();

        // get resource bundles for a named module only if loader is the module's class loader
        if (callerModule.isNamed() && loader == getLoader(callerModule)) {
            return getBundleImpl(callerModule, callerModule, baseName, locale, control);
        }

        // find resource bundles from unnamed module of given class loader
        // Java agent can add to the bootclasspath e.g. via
        // java.lang.instrument.Instrumentation and load classes in unnamed module.
        // It may call RB::getBundle that will end up here with loader == null.
        Module unnamedModule = loader != null
            ? loader.getUnnamedModule()
            : BootLoader.getUnnamedModule();

        return getBundleImpl(callerModule, unnamedModule, baseName, locale, control);
    }

    private static ResourceBundle getBundleFromModule(Class<?> caller,
                                                      Module module,
                                                      String baseName,
                                                      Locale locale,
                                                      Control control) {
        Objects.requireNonNull(module);
        Module callerModule = caller.getModule();
        if (callerModule != module) {
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkPermission(GET_CLASSLOADER_PERMISSION);
            }
        }
        return getBundleImpl(callerModule, module, baseName, locale, control);
    }

    private static ResourceBundle getBundleImpl(Module callerModule,
                                                Module module,
                                                String baseName,
                                                Locale locale,
                                                Control control) {
        if (locale == null || control == null) {
            throw new NullPointerException();
        }

        // We create a CacheKey here for use by this call. The base name
        // and modules will never change during the bundle loading
        // process. We have to make sure that the locale is set before
        // using it as a cache key.
        CacheKey cacheKey = new CacheKey(baseName, locale, module, callerModule);
        ResourceBundle bundle = null;

        // Quick lookup of the cache.
        BundleReference bundleRef = cacheList.get(cacheKey);
        if (bundleRef != null) {
            bundle = bundleRef.get();
            bundleRef = null;
        }

        // If this bundle and all of its parents are valid (not expired),
        // then return this bundle. If any of the bundles is expired, we
        // don't call control.needsReload here but instead drop into the
        // complete loading process below.
        if (isValidBundle(bundle) && hasValidParentChain(bundle)) {
            return bundle;
        }

        // No valid bundle was found in the cache, so we need to load the
        // resource bundle and its parents.

        boolean isKnownControl = (control == Control.INSTANCE) ||
                                   (control instanceof SingleFormatControl);
        List<String> formats = control.getFormats(baseName);
        if (!isKnownControl && !checkList(formats)) {
            throw new IllegalArgumentException("Invalid Control: getFormats");
        }

        ResourceBundle baseBundle = null;
        for (Locale targetLocale = locale;
             targetLocale != null;
             targetLocale = control.getFallbackLocale(baseName, targetLocale)) {
            List<Locale> candidateLocales = control.getCandidateLocales(baseName, targetLocale);
            if (!isKnownControl && !checkList(candidateLocales)) {
                throw new IllegalArgumentException("Invalid Control: getCandidateLocales");
            }

            bundle = findBundle(callerModule, module, cacheKey,
                                candidateLocales, formats, 0, control, baseBundle);

            // If the loaded bundle is the base bundle and exactly for the
            // requested locale or the only candidate locale, then take the
            // bundle as the resulting one. If the loaded bundle is the base
            // bundle, it's put on hold until we finish processing all
            // fallback locales.
            if (isValidBundle(bundle)) {
                boolean isBaseBundle = Locale.ROOT.equals(bundle.locale);
                if (!isBaseBundle || bundle.locale.equals(locale)
                    || (candidateLocales.size() == 1
                        && bundle.locale.equals(candidateLocales.get(0)))) {
                    break;
                }

                // If the base bundle has been loaded, keep the reference in
                // baseBundle so that we can avoid any redundant loading in case
                // the control specify not to cache bundles.
                if (isBaseBundle && baseBundle == null) {
                    baseBundle = bundle;
                }
            }
        }

        if (bundle == null) {
            if (baseBundle == null) {
                throwMissingResourceException(baseName, locale, cacheKey.getCause());
            }
            bundle = baseBundle;
        }

        // keep callerModule and module reachable for as long as we are operating
        // with WeakReference(s) to them (in CacheKey)...
        Reference.reachabilityFence(callerModule);
        Reference.reachabilityFence(module);

        return bundle;
    }


    private static boolean checkList(List<?> a) {
        boolean valid = (a != null && !a.isEmpty());
        if (valid) {
            int size = a.size();
            for (int i = 0; valid && i < size; i++) {
                valid = (a.get(i) != null);
            }
        }
        return valid;
    }

    private static ResourceBundle findBundle(Module callerModule,
                                             Module module,
                                             CacheKey cacheKey,
                                             List<Locale> candidateLocales,
                                             List<String> formats,
                                             int index,
                                             Control control,
                                             ResourceBundle baseBundle) {
        Locale targetLocale = candidateLocales.get(index);
        ResourceBundle parent = null;
        if (index != candidateLocales.size() - 1) {
            parent = findBundle(callerModule, module, cacheKey,
                                candidateLocales, formats, index + 1,
                                control, baseBundle);
        } else if (baseBundle != null && Locale.ROOT.equals(targetLocale)) {
            return baseBundle;
        }

        // Before we do the real loading work, see whether we need to
        // do some housekeeping: If references to modules or
        // resource bundles have been nulled out, remove all related
        // information from the cache.
        Object ref;
        while ((ref = referenceQueue.poll()) != null) {
            cacheList.remove(((CacheKeyReference)ref).getCacheKey());
        }

        // flag indicating the resource bundle has expired in the cache
        boolean expiredBundle = false;

        // First, look up the cache to see if it's in the cache, without
        // attempting to load bundle.
        cacheKey.setLocale(targetLocale);
        ResourceBundle bundle = findBundleInCache(cacheKey, control);
        if (isValidBundle(bundle)) {
            expiredBundle = bundle.expired;
            if (!expiredBundle) {
                // If its parent is the one asked for by the candidate
                // locales (the runtime lookup path), we can take the cached
                // one. (If it's not identical, then we'd have to check the
                // parent's parents to be consistent with what's been
                // requested.)
                if (bundle.parent == parent) {
                    return bundle;
                }
                // Otherwise, remove the cached one since we can't keep
                // the same bundles having different parents.
                BundleReference bundleRef = cacheList.get(cacheKey);
                if (bundleRef != null && bundleRef.get() == bundle) {
                    cacheList.remove(cacheKey, bundleRef);
                }
            }
        }

        if (bundle != NONEXISTENT_BUNDLE) {
            trace("findBundle: %d %s %s formats: %s%n", index, candidateLocales, cacheKey, formats);
            if (module.isNamed()) {
                bundle = loadBundle(cacheKey, formats, control, module, callerModule);
            } else {
                bundle = loadBundle(cacheKey, formats, control, expiredBundle);
            }
            if (bundle != null) {
                if (bundle.parent == null) {
                    bundle.setParent(parent);
                }
                bundle.locale = targetLocale;
                bundle = putBundleInCache(cacheKey, bundle, control);
                return bundle;
            }

            // Put NONEXISTENT_BUNDLE in the cache as a mark that there's no bundle
            // instance for the locale.
            putBundleInCache(cacheKey, NONEXISTENT_BUNDLE, control);
        }
        return parent;
    }

    private static final String UNKNOWN_FORMAT = "";


    /*
     * Loads a ResourceBundle in named modules
     */
    private static ResourceBundle loadBundle(CacheKey cacheKey,
                                             List<String> formats,
                                             Control control,
                                             Module module,
                                             Module callerModule) {
        String baseName = cacheKey.getName();
        Locale targetLocale = cacheKey.getLocale();

        ResourceBundle bundle = null;
        if (cacheKey.hasProviders()) {
            if (callerModule == module) {
                bundle = loadBundleFromProviders(baseName,
                                                 targetLocale,
                                                 cacheKey.getProviders(),
                                                 cacheKey);
            } else {
                // load from provider if the caller module has access to the
                // service type and also declares `uses`
                ClassLoader loader = getLoader(module);
                Class<ResourceBundleProvider> svc =
                    getResourceBundleProviderType(baseName, loader);
                if (svc != null
                        && Reflection.verifyModuleAccess(callerModule, svc)
                        && callerModule.canUse(svc)) {
                    bundle = loadBundleFromProviders(baseName,
                                                     targetLocale,
                                                     cacheKey.getProviders(),
                                                     cacheKey);
                }
            }

            if (bundle != null) {
                cacheKey.setFormat(UNKNOWN_FORMAT);
            }
        }

        // If none of providers returned a bundle and the caller has no provider,
        // look up module-local bundles or from the class path
        if (bundle == null && !cacheKey.callerHasProvider()) {
            for (String format : formats) {
                try {
                    switch (format) {
                    case "java.class":
                        bundle = ResourceBundleProviderHelper
                            .loadResourceBundle(callerModule, module, baseName, targetLocale);

                        break;
                    case "java.properties":
                        bundle = ResourceBundleProviderHelper
                            .loadPropertyResourceBundle(callerModule, module, baseName, targetLocale);
                        break;
                    default:
                        throw new InternalError("unexpected format: " + format);
                    }

                    if (bundle != null) {
                        cacheKey.setFormat(format);
                        break;
                    }
                } catch (LinkageError|Exception e) {
                    cacheKey.setCause(e);
                }
            }
        }
        return bundle;
    }


    private static ServiceLoader<ResourceBundleProvider> getServiceLoader(Module module,
                                                                          String baseName)
    {
        if (!module.isNamed()) {
            return null;
        }

        ClassLoader loader = getLoader(module);
        Class<ResourceBundleProvider> service =
                getResourceBundleProviderType(baseName, loader);
        if (service != null && Reflection.verifyModuleAccess(module, service)) {
            try {
                // locate providers that are visible to the class loader
                // ServiceConfigurationError will be thrown if the module
                // does not declare `uses` the service type
                return ServiceLoader.load(service, loader, module);
            } catch (ServiceConfigurationError e) {
                // "uses" not declared
                return null;
            }
        }
        return null;
    }


    private static Class<ResourceBundleProvider>
            getResourceBundleProviderType(String baseName, ClassLoader loader)
    {
        // Look up <packagename> + ".spi." + <name>"Provider"
        int i = baseName.lastIndexOf('.');
        if (i <= 0) {
            return null;
        }

        String name = baseName.substring(i+1, baseName.length()) + "Provider";
        String providerName = baseName.substring(0, i) + ".spi." + name;

        // Use the class loader of the getBundle caller so that the caller's
        // visibility of the provider type is checked.
        return AccessController.doPrivileged(
            new PrivilegedAction<>() {
                @Override
                public Class<ResourceBundleProvider> run() {
                    try {
                        Class<?> c = Class.forName(providerName, false, loader);
                        if (ResourceBundleProvider.class.isAssignableFrom(c)) {
                            @SuppressWarnings("unchecked")
                            Class<ResourceBundleProvider> s = (Class<ResourceBundleProvider>) c;
                            return s;
                        }
                    } catch (ClassNotFoundException e) {}
                    return null;
                }
            });
    }


    private static ResourceBundle loadBundleFromProviders(String baseName,
                                                          Locale locale,
                                                          ServiceLoader<ResourceBundleProvider> providers,
                                                          CacheKey cacheKey)
    {
        if (providers == null) return null;

        return AccessController.doPrivileged(
                new PrivilegedAction<>() {
                    public ResourceBundle run() {
                        for (Iterator<ResourceBundleProvider> itr = providers.iterator(); itr.hasNext(); ) {
                            try {
                                ResourceBundleProvider provider = itr.next();
                                if (cacheKey != null && cacheKey.callerHasProvider == null
                                        && cacheKey.getModule() == provider.getClass().getModule()) {
                                    cacheKey.callerHasProvider = Boolean.TRUE;
                                }
                                ResourceBundle bundle = provider.getBundle(baseName, locale);
                                trace("provider %s %s locale: %s bundle: %s%n", provider, baseName, locale, bundle);
                                if (bundle != null) {
                                    return bundle;
                                }
                            } catch (ServiceConfigurationError | SecurityException e) {
                                if (cacheKey != null) {
                                    cacheKey.setCause(e);
                                }
                            }
                        }
                        if (cacheKey != null && cacheKey.callerHasProvider == null) {
                            cacheKey.callerHasProvider = Boolean.FALSE;
                        }
                        return null;
                    }
                });

    }

    /*
     * Legacy mechanism to load resource bundles
     */
    private static ResourceBundle loadBundle(CacheKey cacheKey,
                                             List<String> formats,
                                             Control control,
                                             boolean reload) {

        // Here we actually load the bundle in the order of formats
        // specified by the getFormats() value.
        Locale targetLocale = cacheKey.getLocale();

        Module module = cacheKey.getModule();
        if (module == null) {
            // should not happen
            throw new InternalError(
                "Module for cache key: " + cacheKey + " has been GCed.");
        }
        ClassLoader loader = getLoaderForControl(module);

        ResourceBundle bundle = null;
        for (String format : formats) {
            try {
                // ResourceBundle.Control.newBundle may be overridden
                bundle = control.newBundle(cacheKey.getName(), targetLocale, format,
                                           loader, reload);
            } catch (LinkageError | Exception error) {
                // We need to handle the LinkageError case due to
                // inconsistent case-sensitivity in ClassLoader.
                // See 6572242 for details.
                cacheKey.setCause(error);
            }
            if (bundle != null) {
                // Set the format in the cache key so that it can be
                // used when calling needsReload later.
                cacheKey.setFormat(format);
                bundle.name = cacheKey.getName();
                bundle.locale = targetLocale;
                // Bundle provider might reuse instances. So we should make
                // sure to clear the expired flag here.
                bundle.expired = false;
                break;
            }
        }

        return bundle;
    }

    private static boolean isValidBundle(ResourceBundle bundle) {
        return bundle != null && bundle != NONEXISTENT_BUNDLE;
    }


    private static boolean hasValidParentChain(ResourceBundle bundle) {
        long now = System.currentTimeMillis();
        while (bundle != null) {
            if (bundle.expired) {
                return false;
            }
            CacheKey key = bundle.cacheKey;
            if (key != null) {
                long expirationTime = key.expirationTime;
                if (expirationTime >= 0 && expirationTime <= now) {
                    return false;
                }
            }
            bundle = bundle.parent;
        }
        return true;
    }


    private static void throwMissingResourceException(String baseName,
                                                      Locale locale,
                                                      Throwable cause) {
        // If the cause is a MissingResourceException, avoid creating
        // a long chain. (6355009)
        if (cause instanceof MissingResourceException) {
            cause = null;
        }
        throw new MissingResourceException("Can't find bundle for base name "
                                           + baseName + ", locale " + locale,
                                           baseName + "_" + locale, // className
                                           "",                      // key
                                           cause);
    }


    private static ResourceBundle findBundleInCache(CacheKey cacheKey,
                                                    Control control) {
        BundleReference bundleRef = cacheList.get(cacheKey);
        if (bundleRef == null) {
            return null;
        }
        ResourceBundle bundle = bundleRef.get();
        if (bundle == null) {
            return null;
        }
        ResourceBundle p = bundle.parent;
        assert p != NONEXISTENT_BUNDLE;
        // If the parent has expired, then this one must also expire. We
        // check only the immediate parent because the actual loading is
        // done from the root (base) to leaf (child) and the purpose of
        // checking is to propagate expiration towards the leaf. For
        // example, if the requested locale is ja_JP_JP and there are
        // bundles for all of the candidates in the cache, we have a list,
        //
        // base <- ja <- ja_JP <- ja_JP_JP
        //
        // If ja has expired, then it will reload ja and the list becomes a
        // tree.
        //
        // base <- ja (new)
        //  "   <- ja (expired) <- ja_JP <- ja_JP_JP
        //
        // When looking up ja_JP in the cache, it finds ja_JP in the cache
        // which references to the expired ja. Then, ja_JP is marked as
        // expired and removed from the cache. This will be propagated to
        // ja_JP_JP.
        //
        // Now, it's possible, for example, that while loading new ja_JP,
        // someone else has started loading the same bundle and finds the
        // base bundle has expired. Then, what we get from the first
        // getBundle call includes the expired base bundle. However, if
        // someone else didn't start its loading, we wouldn't know if the
        // base bundle has expired at the end of the loading process. The
        // expiration control doesn't guarantee that the returned bundle and
        // its parents haven't expired.
        //
        // We could check the entire parent chain to see if there's any in
        // the chain that has expired. But this process may never end. An
        // extreme case would be that getTimeToLive returns 0 and
        // needsReload always returns true.
        if (p != null && p.expired) {
            assert bundle != NONEXISTENT_BUNDLE;
            bundle.expired = true;
            bundle.cacheKey = null;
            cacheList.remove(cacheKey, bundleRef);
            bundle = null;
        } else {
            CacheKey key = bundleRef.getCacheKey();
            long expirationTime = key.expirationTime;
            if (!bundle.expired && expirationTime >= 0 &&
                expirationTime <= System.currentTimeMillis()) {
                // its TTL period has expired.
                if (bundle != NONEXISTENT_BUNDLE) {
                    // Synchronize here to call needsReload to avoid
                    // redundant concurrent calls for the same bundle.
                    synchronized (bundle) {
                        expirationTime = key.expirationTime;
                        if (!bundle.expired && expirationTime >= 0 &&
                            expirationTime <= System.currentTimeMillis()) {
                            try {
                                Module module = cacheKey.getModule();
                                bundle.expired =
                                    module == null || // already GCed
                                    control.needsReload(key.getName(),
                                                        key.getLocale(),
                                                        key.getFormat(),
                                                        getLoaderForControl(module),
                                                        bundle,
                                                        key.loadTime);
                            } catch (Exception e) {
                                cacheKey.setCause(e);
                            }
                            if (bundle.expired) {
                                // If the bundle needs to be reloaded, then
                                // remove the bundle from the cache, but
                                // return the bundle with the expired flag
                                // on.
                                bundle.cacheKey = null;
                                cacheList.remove(cacheKey, bundleRef);
                            } else {
                                // Update the expiration control info. and reuse
                                // the same bundle instance
                                setExpirationTime(key, control);
                            }
                        }
                    }
                } else {
                    // We just remove NONEXISTENT_BUNDLE from the cache.
                    cacheList.remove(cacheKey, bundleRef);
                    bundle = null;
                }
            }
        }
        return bundle;
    }


    private static ResourceBundle putBundleInCache(CacheKey cacheKey,
                                                   ResourceBundle bundle,
                                                   Control control) {
        setExpirationTime(cacheKey, control);
        if (cacheKey.expirationTime != Control.TTL_DONT_CACHE) {
            CacheKey key = new CacheKey(cacheKey);
            BundleReference bundleRef = new BundleReference(bundle, referenceQueue, key);
            bundle.cacheKey = key;

            // Put the bundle in the cache if it's not been in the cache.
            BundleReference result = cacheList.putIfAbsent(key, bundleRef);

            // If someone else has put the same bundle in the cache before
            // us and it has not expired, we should use the one in the cache.
            if (result != null) {
                ResourceBundle rb = result.get();
                if (rb != null && !rb.expired) {
                    // Clear the back link to the cache key
                    bundle.cacheKey = null;
                    bundle = rb;
                    // Clear the reference in the BundleReference so that
                    // it won't be enqueued.
                    bundleRef.clear();
                } else {
                    // Replace the invalid (garbage collected or expired)
                    // instance with the valid one.
                    cacheList.put(key, bundleRef);
                }
            }
        }
        return bundle;
    }

    private static void setExpirationTime(CacheKey cacheKey, Control control) {
        long ttl = control.getTimeToLive(cacheKey.getName(),
                                         cacheKey.getLocale());
        if (ttl >= 0) {
            // If any expiration time is specified, set the time to be
            // expired in the cache.
            long now = System.currentTimeMillis();
            cacheKey.loadTime = now;
            cacheKey.expirationTime = now + ttl;
        } else if (ttl >= Control.TTL_NO_EXPIRATION_CONTROL) {
            cacheKey.expirationTime = ttl;
        } else {
            throw new IllegalArgumentException("Invalid Control: TTL=" + ttl);
        }
    }


    @CallerSensitive
    public static final void clearCache() {
        Class<?> caller = Reflection.getCallerClass();
        cacheList.keySet().removeIf(
            key -> key.getCallerModule() == caller.getModule()
        );
    }


    public static final void clearCache(ClassLoader loader) {
        Objects.requireNonNull(loader);
        cacheList.keySet().removeIf(
            key -> {
                Module m;
                return (m = key.getModule()) != null &&
                       getLoader(m) == loader;
            }
        );
    }


    protected abstract Object handleGetObject(String key);


    public abstract Enumeration<String> getKeys();


    public boolean containsKey(String key) {
        if (key == null) {
            throw new NullPointerException();
        }
        for (ResourceBundle rb = this; rb != null; rb = rb.parent) {
            if (rb.handleKeySet().contains(key)) {
                return true;
            }
        }
        return false;
    }


    public Set<String> keySet() {
        Set<String> keys = new HashSet<>();
        for (ResourceBundle rb = this; rb != null; rb = rb.parent) {
            keys.addAll(rb.handleKeySet());
        }
        return keys;
    }


    protected Set<String> handleKeySet() {
        if (keySet == null) {
            synchronized (this) {
                if (keySet == null) {
                    Set<String> keys = new HashSet<>();
                    Enumeration<String> enumKeys = getKeys();
                    while (enumKeys.hasMoreElements()) {
                        String key = enumKeys.nextElement();
                        if (handleGetObject(key) != null) {
                            keys.add(key);
                        }
                    }
                    keySet = keys;
                }
            }
        }
        return keySet;
    }




    public static class Control {

        public static final List<String> FORMAT_DEFAULT
            = List.of("java.class", "java.properties");


        public static final List<String> FORMAT_CLASS = List.of("java.class");


        public static final List<String> FORMAT_PROPERTIES
            = List.of("java.properties");


        public static final long TTL_DONT_CACHE = -1;


        public static final long TTL_NO_EXPIRATION_CONTROL = -2;

        private static final Control INSTANCE = new Control();


        protected Control() {
        }


        public static final Control getControl(List<String> formats) {
            if (formats.equals(Control.FORMAT_PROPERTIES)) {
                return SingleFormatControl.PROPERTIES_ONLY;
            }
            if (formats.equals(Control.FORMAT_CLASS)) {
                return SingleFormatControl.CLASS_ONLY;
            }
            if (formats.equals(Control.FORMAT_DEFAULT)) {
                return Control.INSTANCE;
            }
            throw new IllegalArgumentException();
        }


        public static final Control getNoFallbackControl(List<String> formats) {
            if (formats.equals(Control.FORMAT_DEFAULT)) {
                return NoFallbackControl.NO_FALLBACK;
            }
            if (formats.equals(Control.FORMAT_PROPERTIES)) {
                return NoFallbackControl.PROPERTIES_ONLY_NO_FALLBACK;
            }
            if (formats.equals(Control.FORMAT_CLASS)) {
                return NoFallbackControl.CLASS_ONLY_NO_FALLBACK;
            }
            throw new IllegalArgumentException();
        }


        public List<String> getFormats(String baseName) {
            if (baseName == null) {
                throw new NullPointerException();
            }
            return FORMAT_DEFAULT;
        }


        public List<Locale> getCandidateLocales(String baseName, Locale locale) {
            if (baseName == null) {
                throw new NullPointerException();
            }
            return new ArrayList<>(CANDIDATES_CACHE.get(locale.getBaseLocale()));
        }

        private static final CandidateListCache CANDIDATES_CACHE = new CandidateListCache();

        private static class CandidateListCache extends LocaleObjectCache<BaseLocale, List<Locale>> {
            protected List<Locale> createObject(BaseLocale base) {
                String language = base.getLanguage();
                String script = base.getScript();
                String region = base.getRegion();
                String variant = base.getVariant();

                // Special handling for Norwegian
                boolean isNorwegianBokmal = false;
                boolean isNorwegianNynorsk = false;
                if (language.equals("no")) {
                    if (region.equals("NO") && variant.equals("NY")) {
                        variant = "";
                        isNorwegianNynorsk = true;
                    } else {
                        isNorwegianBokmal = true;
                    }
                }
                if (language.equals("nb") || isNorwegianBokmal) {
                    List<Locale> tmpList = getDefaultList("nb", script, region, variant);
                    // Insert a locale replacing "nb" with "no" for every list entry
                    List<Locale> bokmalList = new LinkedList<>();
                    for (Locale l : tmpList) {
                        bokmalList.add(l);
                        if (l.getLanguage().length() == 0) {
                            break;
                        }
                        bokmalList.add(Locale.getInstance("no", l.getScript(), l.getCountry(),
                                l.getVariant(), null));
                    }
                    return bokmalList;
                } else if (language.equals("nn") || isNorwegianNynorsk) {
                    // Insert no_NO_NY, no_NO, no after nn
                    List<Locale> nynorskList = getDefaultList("nn", script, region, variant);
                    int idx = nynorskList.size() - 1;
                    nynorskList.add(idx++, Locale.getInstance("no", "NO", "NY"));
                    nynorskList.add(idx++, Locale.getInstance("no", "NO", ""));
                    nynorskList.add(idx++, Locale.getInstance("no", "", ""));
                    return nynorskList;
                }
                // Special handling for Chinese
                else if (language.equals("zh")) {
                    if (script.length() == 0 && region.length() > 0) {
                        // Supply script for users who want to use zh_Hans/zh_Hant
                        // as bundle names (recommended for Java7+)
                        switch (region) {
                        case "TW":
                        case "HK":
                        case "MO":
                            script = "Hant";
                            break;
                        case "CN":
                        case "SG":
                            script = "Hans";
                            break;
                        }
                    }
                }

                return getDefaultList(language, script, region, variant);
            }

            private static List<Locale> getDefaultList(String language, String script, String region, String variant) {
                List<String> variants = null;

                if (variant.length() > 0) {
                    variants = new LinkedList<>();
                    int idx = variant.length();
                    while (idx != -1) {
                        variants.add(variant.substring(0, idx));
                        idx = variant.lastIndexOf('_', --idx);
                    }
                }

                List<Locale> list = new LinkedList<>();

                if (variants != null) {
                    for (String v : variants) {
                        list.add(Locale.getInstance(language, script, region, v, null));
                    }
                }
                if (region.length() > 0) {
                    list.add(Locale.getInstance(language, script, region, "", null));
                }
                if (script.length() > 0) {
                    list.add(Locale.getInstance(language, script, "", "", null));
                    // Special handling for Chinese
                    if (language.equals("zh")) {
                        if (region.length() == 0) {
                            // Supply region(country) for users who still package Chinese
                            // bundles using old convension.
                            switch (script) {
                                case "Hans":
                                    region = "CN";
                                    break;
                                case "Hant":
                                    region = "TW";
                                    break;
                            }
                        }
                    }

                    // With script, after truncating variant, region and script,
                    // start over without script.
                    if (variants != null) {
                        for (String v : variants) {
                            list.add(Locale.getInstance(language, "", region, v, null));
                        }
                    }
                    if (region.length() > 0) {
                        list.add(Locale.getInstance(language, "", region, "", null));
                    }
                }
                if (language.length() > 0) {
                    list.add(Locale.getInstance(language, "", "", "", null));
                }
                // Add root locale at the end
                list.add(Locale.ROOT);

                return list;
            }
        }


        public Locale getFallbackLocale(String baseName, Locale locale) {
            if (baseName == null) {
                throw new NullPointerException();
            }
            Locale defaultLocale = Locale.getDefault();
            return locale.equals(defaultLocale) ? null : defaultLocale;
        }


        public ResourceBundle newBundle(String baseName, Locale locale, String format,
                                        ClassLoader loader, boolean reload)
                    throws IllegalAccessException, InstantiationException, IOException {
            /*
             * Legacy mechanism to locate resource bundle in unnamed module only
             * that is visible to the given loader and accessible to the given caller.
             */
            String bundleName = toBundleName(baseName, locale);
            ResourceBundle bundle = null;
            if (format.equals("java.class")) {
                try {
                    Class<?> c = loader.loadClass(bundleName);
                    // If the class isn't a ResourceBundle subclass, throw a
                    // ClassCastException.
                    if (ResourceBundle.class.isAssignableFrom(c)) {
                        @SuppressWarnings("unchecked")
                        Class<ResourceBundle> bundleClass = (Class<ResourceBundle>)c;
                        Module m = bundleClass.getModule();

                        // To access a resource bundle in a named module,
                        // either class-based or properties-based, the resource
                        // bundle must be opened unconditionally,
                        // same rule as accessing a resource file.
                        if (m.isNamed() && !m.isOpen(bundleClass.getPackageName())) {
                            throw new IllegalAccessException("unnamed module can't load " +
                                bundleClass.getName() + " in " + m.toString());
                        }
                        try {
                            // bundle in a unnamed module
                            Constructor<ResourceBundle> ctor = bundleClass.getConstructor();
                            if (!Modifier.isPublic(ctor.getModifiers())) {
                                return null;
                            }

                            // java.base may not be able to read the bundleClass's module.
                            PrivilegedAction<Void> pa1 = () -> { ctor.setAccessible(true); return null; };
                            AccessController.doPrivileged(pa1);
                            bundle = ctor.newInstance((Object[]) null);
                        } catch (InvocationTargetException e) {
                            uncheckedThrow(e);
                        }
                    } else {
                        throw new ClassCastException(c.getName()
                                + " cannot be cast to ResourceBundle");
                    }
                } catch (ClassNotFoundException|NoSuchMethodException e) {
                }
            } else if (format.equals("java.properties")) {
                final String resourceName = toResourceName0(bundleName, "properties");
                if (resourceName == null) {
                    return bundle;
                }

                final boolean reloadFlag = reload;
                InputStream stream = null;
                try {
                    stream = AccessController.doPrivileged(
                        new PrivilegedExceptionAction<>() {
                            public InputStream run() throws IOException {
                                URL url = loader.getResource(resourceName);
                                if (url == null) return null;

                                URLConnection connection = url.openConnection();
                                if (reloadFlag) {
                                    // Disable caches to get fresh data for
                                    // reloading.
                                    connection.setUseCaches(false);
                                }
                                return connection.getInputStream();
                            }
                        });
                } catch (PrivilegedActionException e) {
                    throw (IOException) e.getException();
                }
                if (stream != null) {
                    try {
                        bundle = new PropertyResourceBundle(stream);
                    } finally {
                        stream.close();
                    }
                }
            } else {
                throw new IllegalArgumentException("unknown format: " + format);
            }
            return bundle;
        }


        public long getTimeToLive(String baseName, Locale locale) {
            if (baseName == null || locale == null) {
                throw new NullPointerException();
            }
            return TTL_NO_EXPIRATION_CONTROL;
        }


        public boolean needsReload(String baseName, Locale locale,
                                   String format, ClassLoader loader,
                                   ResourceBundle bundle, long loadTime) {
            if (bundle == null) {
                throw new NullPointerException();
            }
            if (format.equals("java.class") || format.equals("java.properties")) {
                format = format.substring(5);
            }
            boolean result = false;
            try {
                String resourceName = toResourceName0(toBundleName(baseName, locale), format);
                if (resourceName == null) {
                    return result;
                }
                URL url = loader.getResource(resourceName);
                if (url != null) {
                    long lastModified = 0;
                    URLConnection connection = url.openConnection();
                    if (connection != null) {
                        // disable caches to get the correct data
                        connection.setUseCaches(false);
                        if (connection instanceof JarURLConnection) {
                            JarEntry ent = ((JarURLConnection)connection).getJarEntry();
                            if (ent != null) {
                                lastModified = ent.getTime();
                                if (lastModified == -1) {
                                    lastModified = 0;
                                }
                            }
                        } else {
                            lastModified = connection.getLastModified();
                        }
                    }
                    result = lastModified >= loadTime;
                }
            } catch (NullPointerException npe) {
                throw npe;
            } catch (Exception e) {
                // ignore other exceptions
            }
            return result;
        }


        public String toBundleName(String baseName, Locale locale) {
            if (locale == Locale.ROOT) {
                return baseName;
            }

            String language = locale.getLanguage();
            String script = locale.getScript();
            String country = locale.getCountry();
            String variant = locale.getVariant();

            if (language == "" && country == "" && variant == "") {
                return baseName;
            }

            StringBuilder sb = new StringBuilder(baseName);
            sb.append('_');
            if (script != "") {
                if (variant != "") {
                    sb.append(language).append('_').append(script).append('_').append(country).append('_').append(variant);
                } else if (country != "") {
                    sb.append(language).append('_').append(script).append('_').append(country);
                } else {
                    sb.append(language).append('_').append(script);
                }
            } else {
                if (variant != "") {
                    sb.append(language).append('_').append(country).append('_').append(variant);
                } else if (country != "") {
                    sb.append(language).append('_').append(country);
                } else {
                    sb.append(language);
                }
            }
            return sb.toString();

        }


        public final String toResourceName(String bundleName, String suffix) {
            StringBuilder sb = new StringBuilder(bundleName.length() + 1 + suffix.length());
            sb.append(bundleName.replace('.', '/')).append('.').append(suffix);
            return sb.toString();
        }

        private String toResourceName0(String bundleName, String suffix) {
            // application protocol check
            if (bundleName.contains("://")) {
                return null;
            } else {
                return toResourceName(bundleName, suffix);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void uncheckedThrow(Throwable t) throws T {
        if (t != null)
            throw (T)t;
        else
            throw new Error("Unknown Exception");
    }

    private static class SingleFormatControl extends Control {
        private static final Control PROPERTIES_ONLY
            = new SingleFormatControl(FORMAT_PROPERTIES);

        private static final Control CLASS_ONLY
            = new SingleFormatControl(FORMAT_CLASS);

        private final List<String> formats;

        protected SingleFormatControl(List<String> formats) {
            this.formats = formats;
        }

        public List<String> getFormats(String baseName) {
            if (baseName == null) {
                throw new NullPointerException();
            }
            return formats;
        }
    }

    private static final class NoFallbackControl extends SingleFormatControl {
        private static final Control NO_FALLBACK
            = new NoFallbackControl(FORMAT_DEFAULT);

        private static final Control PROPERTIES_ONLY_NO_FALLBACK
            = new NoFallbackControl(FORMAT_PROPERTIES);

        private static final Control CLASS_ONLY_NO_FALLBACK
            = new NoFallbackControl(FORMAT_CLASS);

        protected NoFallbackControl(List<String> formats) {
            super(formats);
        }

        public Locale getFallbackLocale(String baseName, Locale locale) {
            if (baseName == null || locale == null) {
                throw new NullPointerException();
            }
            return null;
        }
    }

    private static class ResourceBundleProviderHelper {

        static ResourceBundle newResourceBundle(Class<? extends ResourceBundle> bundleClass) {
            try {
                @SuppressWarnings("unchecked")
                Constructor<? extends ResourceBundle> ctor =
                    bundleClass.getConstructor();
                if (!Modifier.isPublic(ctor.getModifiers())) {
                    return null;
                }
                // java.base may not be able to read the bundleClass's module.
                PrivilegedAction<Void> pa = () -> { ctor.setAccessible(true); return null;};
                AccessController.doPrivileged(pa);
                try {
                    return ctor.newInstance((Object[]) null);
                } catch (InvocationTargetException e) {
                    uncheckedThrow(e);
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new InternalError(e);
                }
            } catch (NoSuchMethodException e) {
                throw new InternalError(e);
            }
            return null;
        }


        static ResourceBundle loadResourceBundle(Module callerModule,
                                                 Module module,
                                                 String baseName,
                                                 Locale locale)
        {
            String bundleName = Control.INSTANCE.toBundleName(baseName, locale);
            try {
                PrivilegedAction<Class<?>> pa = () -> Class.forName(module, bundleName);
                Class<?> c = AccessController.doPrivileged(pa, null, GET_CLASSLOADER_PERMISSION);
                trace("local in %s %s caller %s: %s%n", module, bundleName, callerModule, c);

                if (c == null) {
                    // if not found from the given module, locate resource bundle
                    // that is visible to the module's class loader
                    ClassLoader loader = getLoader(module);
                    if (loader != null) {
                        c = Class.forName(bundleName, false, loader);
                    } else {
                        c = BootLoader.loadClassOrNull(bundleName);
                    }
                    trace("loader for %s %s caller %s: %s%n", module, bundleName, callerModule, c);
                }

                if (c != null && ResourceBundle.class.isAssignableFrom(c)) {
                    @SuppressWarnings("unchecked")
                    Class<ResourceBundle> bundleClass = (Class<ResourceBundle>) c;
                    Module m = bundleClass.getModule();
                    if (!isAccessible(callerModule, m, bundleClass.getPackageName())) {
                        trace("   %s does not have access to %s/%s%n", callerModule,
                              m.getName(), bundleClass.getPackageName());
                        return null;
                    }

                    return newResourceBundle(bundleClass);
                }
            } catch (ClassNotFoundException e) {}
            return null;
        }


        static boolean isAccessible(Module callerModule, Module module, String pn) {
            if (!module.isNamed() || callerModule == module)
                return true;

            return module.isOpen(pn, callerModule);
        }


        static ResourceBundle loadPropertyResourceBundle(Module callerModule,
                                                         Module module,
                                                         String baseName,
                                                         Locale locale)
            throws IOException
        {
            String bundleName = Control.INSTANCE.toBundleName(baseName, locale);

            PrivilegedAction<InputStream> pa = () -> {
                try {
                    String resourceName = Control.INSTANCE
                        .toResourceName0(bundleName, "properties");
                    if (resourceName == null) {
                        return null;
                    }
                    trace("local in %s %s caller %s%n", module, resourceName, callerModule);

                    // if the package is in the given module but not opened
                    // locate it from the given module first.
                    String pn = toPackageName(bundleName);
                    trace("   %s/%s is accessible to %s : %s%n",
                            module.getName(), pn, callerModule,
                            isAccessible(callerModule, module, pn));
                    if (isAccessible(callerModule, module, pn)) {
                        InputStream in = module.getResourceAsStream(resourceName);
                        if (in != null) {
                            return in;
                        }
                    }

                    ClassLoader loader = module.getClassLoader();
                    trace("loader for %s %s caller %s%n", module, resourceName, callerModule);

                    try {
                        if (loader != null) {
                            return loader.getResourceAsStream(resourceName);
                        } else {
                            URL url = BootLoader.findResource(resourceName);
                            if (url != null) {
                                return url.openStream();
                            }
                        }
                    } catch (Exception e) {}
                    return null;

                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            };

            try (InputStream stream = AccessController.doPrivileged(pa)) {
                if (stream != null) {
                    return new PropertyResourceBundle(stream);
                } else {
                    return null;
                }
            } catch (UncheckedIOException e) {
                throw e.getCause();
            }
        }

        private static String toPackageName(String bundleName) {
            int i = bundleName.lastIndexOf('.');
            return i != -1 ? bundleName.substring(0, i) : "";
        }

    }

    private static final boolean TRACE_ON = Boolean.valueOf(
        GetPropertyAction.privilegedGetProperty("resource.bundle.debug", "false"));

    private static void trace(String format, Object... params) {
        if (TRACE_ON)
            System.out.format(format, params);
    }
}
