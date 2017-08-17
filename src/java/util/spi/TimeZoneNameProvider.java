/*
 * Copyright (c) 2005, 2012, Oracle and/or its affiliates. All rights reserved.
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


public abstract class TimeZoneNameProvider extends LocaleServiceProvider {


    protected TimeZoneNameProvider() {
    }


    public abstract String getDisplayName(String ID, boolean daylight, int style, Locale locale);


    public String getGenericDisplayName(String ID, int style, Locale locale) {
        return null;
    }
}
