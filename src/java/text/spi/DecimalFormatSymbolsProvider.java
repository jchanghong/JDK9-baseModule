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

package java.text.spi;

import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.spi.LocaleServiceProvider;


public abstract class DecimalFormatSymbolsProvider extends LocaleServiceProvider {


    protected DecimalFormatSymbolsProvider() {
    }


    public abstract DecimalFormatSymbols getInstance(Locale locale);
}
