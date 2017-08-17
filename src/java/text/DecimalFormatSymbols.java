/*
 * Copyright (c) 1996, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * (C) Copyright IBM Corp. 1996 - 1998 - All Rights Reserved
 *
 *   The original version of this source code and documentation is copyrighted
 * and owned by Taligent, Inc., a wholly-owned subsidiary of IBM. These
 * materials are provided under terms of a License Agreement between Taligent
 * and Sun. This technology is protected by multiple US and International
 * patents. This notice and attribution to Taligent may not be removed.
 *   Taligent is a registered trademark of Taligent, Inc.
 *
 */

package java.text;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.text.spi.DecimalFormatSymbolsProvider;
import java.util.Currency;
import java.util.Locale;
import sun.util.locale.provider.LocaleProviderAdapter;
import sun.util.locale.provider.LocaleServiceProviderPool;
import sun.util.locale.provider.ResourceBundleBasedAdapter;



public class DecimalFormatSymbols implements Cloneable, Serializable {


    public DecimalFormatSymbols() {
        initialize( Locale.getDefault(Locale.Category.FORMAT) );
    }


    public DecimalFormatSymbols( Locale locale ) {
        initialize( locale );
    }


    public static Locale[] getAvailableLocales() {
        LocaleServiceProviderPool pool =
            LocaleServiceProviderPool.getPool(DecimalFormatSymbolsProvider.class);
        return pool.getAvailableLocales();
    }


    public static final DecimalFormatSymbols getInstance() {
        return getInstance(Locale.getDefault(Locale.Category.FORMAT));
    }


    public static final DecimalFormatSymbols getInstance(Locale locale) {
        LocaleProviderAdapter adapter;
        adapter = LocaleProviderAdapter.getAdapter(DecimalFormatSymbolsProvider.class, locale);
        DecimalFormatSymbolsProvider provider = adapter.getDecimalFormatSymbolsProvider();
        DecimalFormatSymbols dfsyms = provider.getInstance(locale);
        if (dfsyms == null) {
            provider = LocaleProviderAdapter.forJRE().getDecimalFormatSymbolsProvider();
            dfsyms = provider.getInstance(locale);
        }
        return dfsyms;
    }


    public char getZeroDigit() {
        return zeroDigit;
    }


    public void setZeroDigit(char zeroDigit) {
        this.zeroDigit = zeroDigit;
    }


    public char getGroupingSeparator() {
        return groupingSeparator;
    }


    public void setGroupingSeparator(char groupingSeparator) {
        this.groupingSeparator = groupingSeparator;
    }


    public char getDecimalSeparator() {
        return decimalSeparator;
    }


    public void setDecimalSeparator(char decimalSeparator) {
        this.decimalSeparator = decimalSeparator;
    }


    public char getPerMill() {
        return perMill;
    }


    public void setPerMill(char perMill) {
        this.perMill = perMill;
    }


    public char getPercent() {
        return percent;
    }


    public void setPercent(char percent) {
        this.percent = percent;
    }


    public char getDigit() {
        return digit;
    }


    public void setDigit(char digit) {
        this.digit = digit;
    }


    public char getPatternSeparator() {
        return patternSeparator;
    }


    public void setPatternSeparator(char patternSeparator) {
        this.patternSeparator = patternSeparator;
    }


    public String getInfinity() {
        return infinity;
    }


    public void setInfinity(String infinity) {
        this.infinity = infinity;
    }


    public String getNaN() {
        return NaN;
    }


    public void setNaN(String NaN) {
        this.NaN = NaN;
    }


    public char getMinusSign() {
        return minusSign;
    }


    public void setMinusSign(char minusSign) {
        this.minusSign = minusSign;
    }


    public String getCurrencySymbol()
    {
        initializeCurrency(locale);
        return currencySymbol;
    }


    public void setCurrencySymbol(String currency)
    {
        initializeCurrency(locale);
        currencySymbol = currency;
    }


    public String getInternationalCurrencySymbol()
    {
        initializeCurrency(locale);
        return intlCurrencySymbol;
    }


    public void setInternationalCurrencySymbol(String currencyCode)
    {
        initializeCurrency(locale);
        intlCurrencySymbol = currencyCode;
        currency = null;
        if (currencyCode != null) {
            try {
                currency = Currency.getInstance(currencyCode);
                currencySymbol = currency.getSymbol();
            } catch (IllegalArgumentException e) {
            }
        }
    }


    public Currency getCurrency() {
        initializeCurrency(locale);
        return currency;
    }


    public void setCurrency(Currency currency) {
        if (currency == null) {
            throw new NullPointerException();
        }
        initializeCurrency(locale);
        this.currency = currency;
        intlCurrencySymbol = currency.getCurrencyCode();
        currencySymbol = currency.getSymbol(locale);
    }



    public char getMonetaryDecimalSeparator()
    {
        return monetarySeparator;
    }


    public void setMonetaryDecimalSeparator(char sep)
    {
        monetarySeparator = sep;
    }

    //------------------------------------------------------------
    // BEGIN   Package Private methods ... to be made public later
    //------------------------------------------------------------


    char getExponentialSymbol()
    {
        return exponential;
    }


    public String getExponentSeparator()
    {
        return exponentialSeparator;
    }


    void setExponentialSymbol(char exp)
    {
        exponential = exp;
    }


    public void setExponentSeparator(String exp)
    {
        if (exp == null) {
            throw new NullPointerException();
        }
        exponentialSeparator = exp;
    }


    //------------------------------------------------------------
    // END     Package Private methods ... to be made public later
    //------------------------------------------------------------


    @Override
    public Object clone() {
        try {
            return (DecimalFormatSymbols)super.clone();
            // other fields are bit-copied
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (getClass() != obj.getClass()) return false;
        DecimalFormatSymbols other = (DecimalFormatSymbols) obj;
        return (zeroDigit == other.zeroDigit &&
        groupingSeparator == other.groupingSeparator &&
        decimalSeparator == other.decimalSeparator &&
        percent == other.percent &&
        perMill == other.perMill &&
        digit == other.digit &&
        minusSign == other.minusSign &&
        patternSeparator == other.patternSeparator &&
        infinity.equals(other.infinity) &&
        NaN.equals(other.NaN) &&
        getCurrencySymbol().equals(other.getCurrencySymbol()) && // possible currency init occurs here
        intlCurrencySymbol.equals(other.intlCurrencySymbol) &&
        currency == other.currency &&
        monetarySeparator == other.monetarySeparator &&
        exponentialSeparator.equals(other.exponentialSeparator) &&
        locale.equals(other.locale));
    }


    @Override
    public int hashCode() {
            int result = zeroDigit;
            result = result * 37 + groupingSeparator;
            result = result * 37 + decimalSeparator;
            return result;
    }


    private void initialize( Locale locale ) {
        this.locale = locale;

        // get resource bundle data
        LocaleProviderAdapter adapter = LocaleProviderAdapter.getAdapter(DecimalFormatSymbolsProvider.class, locale);
        // Avoid potential recursions
        if (!(adapter instanceof ResourceBundleBasedAdapter)) {
            adapter = LocaleProviderAdapter.getResourceBundleBased();
        }
        Object[] data = adapter.getLocaleResources(locale).getDecimalFormatSymbolsData();
        String[] numberElements = (String[]) data[0];

        decimalSeparator = numberElements[0].charAt(0);
        groupingSeparator = numberElements[1].charAt(0);
        patternSeparator = numberElements[2].charAt(0);
        percent = numberElements[3].charAt(0);
        zeroDigit = numberElements[4].charAt(0); //different for Arabic,etc.
        digit = numberElements[5].charAt(0);
        minusSign = numberElements[6].charAt(0);
        exponential = numberElements[7].charAt(0);
        exponentialSeparator = numberElements[7]; //string representation new since 1.6
        perMill = numberElements[8].charAt(0);
        infinity  = numberElements[9];
        NaN = numberElements[10];

        // maybe filled with previously cached values, or null.
        intlCurrencySymbol = (String) data[1];
        currencySymbol = (String) data[2];

        // Currently the monetary decimal separator is the same as the
        // standard decimal separator for all locales that we support.
        // If that changes, add a new entry to NumberElements.
        monetarySeparator = decimalSeparator;
    }


    private void initializeCurrency(Locale locale) {
        if (currencyInitialized) {
            return;
        }

        // Try to obtain the currency used in the locale's country.
        // Check for empty country string separately because it's a valid
        // country ID for Locale (and used for the C locale), but not a valid
        // ISO 3166 country code, and exceptions are expensive.
        if (locale.getCountry().length() > 0) {
            try {
                currency = Currency.getInstance(locale);
            } catch (IllegalArgumentException e) {
                // use default values below for compatibility
            }
        }

        if (currency != null) {
            // get resource bundle data
            LocaleProviderAdapter adapter =
                LocaleProviderAdapter.getAdapter(DecimalFormatSymbolsProvider.class, locale);
            // Avoid potential recursions
            if (!(adapter instanceof ResourceBundleBasedAdapter)) {
                adapter = LocaleProviderAdapter.getResourceBundleBased();
            }
            Object[] data = adapter.getLocaleResources(locale).getDecimalFormatSymbolsData();
            intlCurrencySymbol = currency.getCurrencyCode();
            if (data[1] != null && data[1] == intlCurrencySymbol) {
                currencySymbol = (String) data[2];
            } else {
                currencySymbol = currency.getSymbol(locale);
                data[1] = intlCurrencySymbol;
                data[2] = currencySymbol;
            }
        } else {
            // default values
            intlCurrencySymbol = "XXX";
            try {
                currency = Currency.getInstance(intlCurrencySymbol);
            } catch (IllegalArgumentException e) {
            }
            currencySymbol = "\u00A4";
        }

        currencyInitialized = true;
    }


    private void readObject(ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        if (serialVersionOnStream < 1) {
            // Didn't have monetarySeparator or exponential field;
            // use defaults.
            monetarySeparator = decimalSeparator;
            exponential       = 'E';
        }
        if (serialVersionOnStream < 2) {
            // didn't have locale; use root locale
            locale = Locale.ROOT;
        }
        if (serialVersionOnStream < 3) {
            // didn't have exponentialSeparator. Create one using exponential
            exponentialSeparator = Character.toString(exponential);
        }
        serialVersionOnStream = currentSerialVersion;

        if (intlCurrencySymbol != null) {
            try {
                 currency = Currency.getInstance(intlCurrencySymbol);
            } catch (IllegalArgumentException e) {
            }
            currencyInitialized = true;
        }
    }


    private  char    zeroDigit;


    private  char    groupingSeparator;


    private  char    decimalSeparator;


    private  char    perMill;


    private  char    percent;


    private  char    digit;


    private  char    patternSeparator;


    private  String  infinity;


    private  String  NaN;


    private  char    minusSign;


    private  String  currencySymbol;


    private  String  intlCurrencySymbol;


    private  char    monetarySeparator; // Field new in JDK 1.1.6


    private  char    exponential;       // Field new in JDK 1.1.6


    private  String    exponentialSeparator;       // Field new in JDK 1.6


    private Locale locale;

    // currency; only the ISO code is serialized.
    private transient Currency currency;
    private transient volatile boolean currencyInitialized;

    // Proclaim JDK 1.1 FCS compatibility
    static final long serialVersionUID = 5772796243397350300L;

    // The internal serial version which says which version was written
    // - 0 (default) for version up to JDK 1.1.5
    // - 1 for version from JDK 1.1.6, which includes two new fields:
    //     monetarySeparator and exponential.
    // - 2 for version from J2SE 1.4, which includes locale field.
    // - 3 for version from J2SE 1.6, which includes exponentialSeparator field.
    private static final int currentSerialVersion = 3;


    private int serialVersionOnStream = currentSerialVersion;
}
