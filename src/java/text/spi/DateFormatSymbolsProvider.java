/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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

package java.text.spi;

import java.text.DateFormatSymbols;
import java.util.Locale;
import java.util.spi.LocaleServiceProvider;


public abstract class DateFormatSymbolsProvider extends LocaleServiceProvider {


    protected DateFormatSymbolsProvider() {
    }


    public abstract DateFormatSymbols getInstance(Locale locale);
}
