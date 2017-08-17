/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Locale;


public abstract class LocaleServiceProvider {

    private static Void checkPermission() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission("localeServiceProvider"));
        }
        return null;
    }
    private LocaleServiceProvider(Void ignore) { }


    protected LocaleServiceProvider() {
        this(checkPermission());
    }


    public abstract Locale[] getAvailableLocales();


    public boolean isSupportedLocale(Locale locale) {
        locale = locale.stripExtensions(); // throws NPE if locale == null
        for (Locale available : getAvailableLocales()) {
            if (locale.equals(available.stripExtensions())) {
                return true;
}
        }
        return false;
    }
}
