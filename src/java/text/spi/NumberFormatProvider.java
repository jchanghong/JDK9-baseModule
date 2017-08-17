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

import java.text.NumberFormat;
import java.util.Locale;
import java.util.spi.LocaleServiceProvider;


public abstract class NumberFormatProvider extends LocaleServiceProvider {


    protected NumberFormatProvider() {
    }


    public abstract NumberFormat getCurrencyInstance(Locale locale);


    public abstract NumberFormat getIntegerInstance(Locale locale);


    public abstract NumberFormat getNumberInstance(Locale locale);


    public abstract NumberFormat getPercentInstance(Locale locale);
}
