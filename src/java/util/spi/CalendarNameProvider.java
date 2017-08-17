/*
 * Copyright (c) 2012, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Calendar;
import java.util.Locale;
import java.util.Map;


public abstract class CalendarNameProvider extends LocaleServiceProvider {

    protected CalendarNameProvider() {
    }


    public abstract String getDisplayName(String calendarType,
                                          int field, int value,
                                          int style, Locale locale);


    public abstract Map<String, Integer> getDisplayNames(String calendarType,
                                                         int field, int style,
                                                         Locale locale);
}
