/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

package java.util.spi;

import jdk.internal.misc.JavaUtilResourceBundleAccess;
import jdk.internal.misc.SharedSecrets;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import static sun.security.util.SecurityConstants.GET_CLASSLOADER_PERMISSION;


public abstract class AbstractResourceBundleProvider implements ResourceBundleProvider {
    private static final JavaUtilResourceBundleAccess RB_ACCESS =
        SharedSecrets.getJavaUtilResourceBundleAccess();

    private static final String FORMAT_CLASS = "java.class";
    private static final String FORMAT_PROPERTIES = "java.properties";

    private final String[] formats;


    protected AbstractResourceBundleProvider() {
        this(FORMAT_PROPERTIES);
    }


    protected AbstractResourceBundleProvider(String... formats) {
        this.formats = formats.clone();  // defensive copy
        if (this.formats.length == 0) {
            throw new IllegalArgumentException("empty formats");
        }
        for (String f : this.formats) {
            if (!FORMAT_CLASS.equals(f) && !FORMAT_PROPERTIES.equals(f)) {
                throw new IllegalArgumentException(f);
            }
        }
    }


    protected String toBundleName(String baseName, Locale locale) {
        return ResourceBundle.Control.getControl(ResourceBundle.Control.FORMAT_DEFAULT)
            .toBundleName(baseName, locale);
    }


    @Override
    public ResourceBundle getBundle(String baseName, Locale locale) {
        Module module = this.getClass().getModule();
        String bundleName = toBundleName(baseName, locale);
        ResourceBundle bundle = null;

        for (String format : formats) {
            try {
                if (FORMAT_CLASS.equals(format)) {
                    bundle = loadResourceBundle(module, bundleName);
                } else if (FORMAT_PROPERTIES.equals(format)) {
                    bundle = loadPropertyResourceBundle(module, bundleName);
                }
                if (bundle != null) {
                    break;
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return bundle;
    }

    /*
     * Returns the ResourceBundle of .class format if found in the module
     * of this provider.
     */
    private static ResourceBundle loadResourceBundle(Module module, String bundleName)
    {
        PrivilegedAction<Class<?>> pa = () -> Class.forName(module, bundleName);
        Class<?> c = AccessController.doPrivileged(pa, null, GET_CLASSLOADER_PERMISSION);
        if (c != null && ResourceBundle.class.isAssignableFrom(c)) {
            @SuppressWarnings("unchecked")
            Class<ResourceBundle> bundleClass = (Class<ResourceBundle>) c;
            return RB_ACCESS.newResourceBundle(bundleClass);
        }
        return null;
    }

    /*
     * Returns the ResourceBundle of .property format if found in the module
     * of this provider.
     */
    private static ResourceBundle loadPropertyResourceBundle(Module module,
                                                             String bundleName)
        throws IOException
    {
        String resourceName = toResourceName(bundleName, "properties");
        if (resourceName == null) {
            return null;
        }

        PrivilegedAction<InputStream> pa = () -> {
            try {
                return module.getResourceAsStream(resourceName);
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

    private static String toResourceName(String bundleName, String suffix) {
        if (bundleName.contains("://")) {
            return null;
        }
        StringBuilder sb = new StringBuilder(bundleName.length() + 1 + suffix.length());
        sb.append(bundleName.replace('.', '/')).append('.').append(suffix);
        return sb.toString();
    }

}
