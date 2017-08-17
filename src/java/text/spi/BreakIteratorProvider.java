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

import java.text.BreakIterator;
import java.util.Locale;
import java.util.spi.LocaleServiceProvider;


public abstract class BreakIteratorProvider extends LocaleServiceProvider {


    protected BreakIteratorProvider() {
    }


    public abstract BreakIterator getWordInstance(Locale locale);


    public abstract BreakIterator getLineInstance(Locale locale);


    public abstract BreakIterator getCharacterInstance(Locale locale);


    public abstract BreakIterator getSentenceInstance(Locale locale);
}
