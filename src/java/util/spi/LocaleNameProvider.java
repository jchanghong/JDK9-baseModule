/*
 * Copyright (c) 2005, 2011, Oracle and/or its affiliates. All rights reserved.
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


public abstract class LocaleNameProvider extends LocaleServiceProvider {


    protected LocaleNameProvider() {
    }


    public abstract String getDisplayLanguage(String languageCode, Locale locale);


    public String getDisplayScript(String scriptCode, Locale locale) {
        return null;
    }


    public abstract String getDisplayCountry(String countryCode, Locale locale);


    public abstract String getDisplayVariant(String variant, Locale locale);
}
