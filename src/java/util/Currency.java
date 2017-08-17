/*
 * Copyright (c) 2000, 2016, Oracle and/or its affiliates. All rights reserved.
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

package java.util;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.spi.CurrencyNameProvider;
import sun.util.locale.provider.LocaleServiceProviderPool;
import sun.util.logging.PlatformLogger;



public final class Currency implements Serializable {

    private static final long serialVersionUID = -158308464356906721L;


    private final String currencyCode;


    private final transient int defaultFractionDigits;


    private final transient int numericCode;


    // class data: instance map

    private static ConcurrentMap<String, Currency> instances = new ConcurrentHashMap<>(7);
    private static HashSet<Currency> available;

    // Class data: currency data obtained from currency.data file.
    // Purpose:
    // - determine valid country codes
    // - determine valid currency codes
    // - map country codes to currency codes
    // - obtain default fraction digits for currency codes
    //
    // sc = special case; dfd = default fraction digits
    // Simple countries are those where the country code is a prefix of the
    // currency code, and there are no known plans to change the currency.
    //
    // table formats:
    // - mainTable:
    //   - maps country code to 32-bit int
    //   - 26*26 entries, corresponding to [A-Z]*[A-Z]
    //   - \u007F -> not valid country
    //   - bits 20-31: unused
    //   - bits 10-19: numeric code (0 to 1023)
    //   - bit 9: 1 - special case, bits 0-4 indicate which one
    //            0 - simple country, bits 0-4 indicate final char of currency code
    //   - bits 5-8: fraction digits for simple countries, 0 for special cases
    //   - bits 0-4: final char for currency code for simple country, or ID of special case
    // - special case IDs:
    //   - 0: country has no currency
    //   - other: index into specialCasesList

    static int formatVersion;
    static int dataVersion;
    static int[] mainTable;
    static List<SpecialCaseEntry> specialCasesList;
    static List<OtherCurrencyEntry> otherCurrenciesList;

    // handy constants - must match definitions in GenerateCurrencyData
    // magic number
    private static final int MAGIC_NUMBER = 0x43757244;
    // number of characters from A to Z
    private static final int A_TO_Z = ('Z' - 'A') + 1;
    // entry for invalid country codes
    private static final int INVALID_COUNTRY_ENTRY = 0x0000007F;
    // entry for countries without currency
    private static final int COUNTRY_WITHOUT_CURRENCY_ENTRY = 0x00000200;
    // mask for simple case country entries
    private static final int SIMPLE_CASE_COUNTRY_MASK = 0x00000000;
    // mask for simple case country entry final character
    private static final int SIMPLE_CASE_COUNTRY_FINAL_CHAR_MASK = 0x0000001F;
    // mask for simple case country entry default currency digits
    private static final int SIMPLE_CASE_COUNTRY_DEFAULT_DIGITS_MASK = 0x000001E0;
    // shift count for simple case country entry default currency digits
    private static final int SIMPLE_CASE_COUNTRY_DEFAULT_DIGITS_SHIFT = 5;
    // maximum number for simple case country entry default currency digits
    private static final int SIMPLE_CASE_COUNTRY_MAX_DEFAULT_DIGITS = 9;
    // mask for special case country entries
    private static final int SPECIAL_CASE_COUNTRY_MASK = 0x00000200;
    // mask for special case country index
    private static final int SPECIAL_CASE_COUNTRY_INDEX_MASK = 0x0000001F;
    // delta from entry index component in main table to index into special case tables
    private static final int SPECIAL_CASE_COUNTRY_INDEX_DELTA = 1;
    // mask for distinguishing simple and special case countries
    private static final int COUNTRY_TYPE_MASK = SIMPLE_CASE_COUNTRY_MASK | SPECIAL_CASE_COUNTRY_MASK;
    // mask for the numeric code of the currency
    private static final int NUMERIC_CODE_MASK = 0x000FFC00;
    // shift count for the numeric code of the currency
    private static final int NUMERIC_CODE_SHIFT = 10;

    // Currency data format version
    private static final int VALID_FORMAT_VERSION = 3;

    static {
        AccessController.doPrivileged(new PrivilegedAction<>() {
            @Override
            public Void run() {
                try {
                    try (InputStream in = getClass().getResourceAsStream("/java/util/currency.data")) {
                        if (in == null) {
                            throw new InternalError("Currency data not found");
                        }
                        DataInputStream dis = new DataInputStream(new BufferedInputStream(in));
                        if (dis.readInt() != MAGIC_NUMBER) {
                            throw new InternalError("Currency data is possibly corrupted");
                        }
                        formatVersion = dis.readInt();
                        if (formatVersion != VALID_FORMAT_VERSION) {
                            throw new InternalError("Currency data format is incorrect");
                        }
                        dataVersion = dis.readInt();
                        mainTable = readIntArray(dis, A_TO_Z * A_TO_Z);
                        int scCount = dis.readInt();
                        specialCasesList = readSpecialCases(dis, scCount);
                        int ocCount = dis.readInt();
                        otherCurrenciesList = readOtherCurrencies(dis, ocCount);
                    }
                } catch (IOException e) {
                    throw new InternalError(e);
                }

                // look for the properties file for overrides
                String propsFile = System.getProperty("java.util.currency.data");
                if (propsFile == null) {
                    propsFile = System.getProperty("java.home") + File.separator + "lib" +
                        File.separator + "currency.properties";
                }
                try {
                    File propFile = new File(propsFile);
                    if (propFile.exists()) {
                        Properties props = new Properties();
                        try (FileReader fr = new FileReader(propFile)) {
                            props.load(fr);
                        }
                        Set<String> keys = props.stringPropertyNames();
                        Pattern propertiesPattern =
                            Pattern.compile("([A-Z]{3})\\s*,\\s*(\\d{3})\\s*,\\s*" +
                                "(\\d+)\\s*,?\\s*(\\d{4}-\\d{2}-\\d{2}T\\d{2}:" +
                                "\\d{2}:\\d{2})?");
                        for (String key : keys) {
                           replaceCurrencyData(propertiesPattern,
                               key.toUpperCase(Locale.ROOT),
                               props.getProperty(key).toUpperCase(Locale.ROOT));
                        }
                    }
                } catch (IOException e) {
                    info("currency.properties is ignored because of an IOException", e);
                }
                return null;
            }
        });
    }


    private static final int SYMBOL = 0;
    private static final int DISPLAYNAME = 1;



    private Currency(String currencyCode, int defaultFractionDigits, int numericCode) {
        this.currencyCode = currencyCode;
        this.defaultFractionDigits = defaultFractionDigits;
        this.numericCode = numericCode;
    }


    public static Currency getInstance(String currencyCode) {
        return getInstance(currencyCode, Integer.MIN_VALUE, 0);
    }

    private static Currency getInstance(String currencyCode, int defaultFractionDigits,
        int numericCode) {
        // Try to look up the currency code in the instances table.
        // This does the null pointer check as a side effect.
        // Also, if there already is an entry, the currencyCode must be valid.
        Currency instance = instances.get(currencyCode);
        if (instance != null) {
            return instance;
        }

        if (defaultFractionDigits == Integer.MIN_VALUE) {
            // Currency code not internally generated, need to verify first
            // A currency code must have 3 characters and exist in the main table
            // or in the list of other currencies.
            boolean found = false;
            if (currencyCode.length() != 3) {
                throw new IllegalArgumentException();
            }
            char char1 = currencyCode.charAt(0);
            char char2 = currencyCode.charAt(1);
            int tableEntry = getMainTableEntry(char1, char2);
            if ((tableEntry & COUNTRY_TYPE_MASK) == SIMPLE_CASE_COUNTRY_MASK
                    && tableEntry != INVALID_COUNTRY_ENTRY
                    && currencyCode.charAt(2) - 'A' == (tableEntry & SIMPLE_CASE_COUNTRY_FINAL_CHAR_MASK)) {
                defaultFractionDigits = (tableEntry & SIMPLE_CASE_COUNTRY_DEFAULT_DIGITS_MASK) >> SIMPLE_CASE_COUNTRY_DEFAULT_DIGITS_SHIFT;
                numericCode = (tableEntry & NUMERIC_CODE_MASK) >> NUMERIC_CODE_SHIFT;
                found = true;
            } else { //special case
                int[] fractionAndNumericCode = SpecialCaseEntry.findEntry(currencyCode);
                if (fractionAndNumericCode != null) {
                    defaultFractionDigits = fractionAndNumericCode[0];
                    numericCode = fractionAndNumericCode[1];
                    found = true;
                }
            }

            if (!found) {
                OtherCurrencyEntry ocEntry = OtherCurrencyEntry.findEntry(currencyCode);
                if (ocEntry == null) {
                    throw new IllegalArgumentException();
                }
                defaultFractionDigits = ocEntry.fraction;
                numericCode = ocEntry.numericCode;
            }
        }

        Currency currencyVal =
            new Currency(currencyCode, defaultFractionDigits, numericCode);
        instance = instances.putIfAbsent(currencyCode, currencyVal);
        return (instance != null ? instance : currencyVal);
    }


    public static Currency getInstance(Locale locale) {
        String country = locale.getCountry();
        if (country == null) {
            throw new NullPointerException();
        }

        if (country.length() != 2) {
            throw new IllegalArgumentException();
        }

        char char1 = country.charAt(0);
        char char2 = country.charAt(1);
        int tableEntry = getMainTableEntry(char1, char2);
        if ((tableEntry & COUNTRY_TYPE_MASK) == SIMPLE_CASE_COUNTRY_MASK
                    && tableEntry != INVALID_COUNTRY_ENTRY) {
            char finalChar = (char) ((tableEntry & SIMPLE_CASE_COUNTRY_FINAL_CHAR_MASK) + 'A');
            int defaultFractionDigits = (tableEntry & SIMPLE_CASE_COUNTRY_DEFAULT_DIGITS_MASK) >> SIMPLE_CASE_COUNTRY_DEFAULT_DIGITS_SHIFT;
            int numericCode = (tableEntry & NUMERIC_CODE_MASK) >> NUMERIC_CODE_SHIFT;
            StringBuilder sb = new StringBuilder(country);
            sb.append(finalChar);
            return getInstance(sb.toString(), defaultFractionDigits, numericCode);
        } else {
            // special cases
            if (tableEntry == INVALID_COUNTRY_ENTRY) {
                throw new IllegalArgumentException();
            }
            if (tableEntry == COUNTRY_WITHOUT_CURRENCY_ENTRY) {
                return null;
            } else {
                int index = SpecialCaseEntry.toIndex(tableEntry);
                SpecialCaseEntry scEntry = specialCasesList.get(index);
                if (scEntry.cutOverTime == Long.MAX_VALUE
                        || System.currentTimeMillis() < scEntry.cutOverTime) {
                    return getInstance(scEntry.oldCurrency,
                            scEntry.oldCurrencyFraction,
                            scEntry.oldCurrencyNumericCode);
                } else {
                    return getInstance(scEntry.newCurrency,
                            scEntry.newCurrencyFraction,
                            scEntry.newCurrencyNumericCode);
                }
            }
        }
    }


    public static Set<Currency> getAvailableCurrencies() {
        synchronized(Currency.class) {
            if (available == null) {
                available = new HashSet<>(256);

                // Add simple currencies first
                for (char c1 = 'A'; c1 <= 'Z'; c1 ++) {
                    for (char c2 = 'A'; c2 <= 'Z'; c2 ++) {
                        int tableEntry = getMainTableEntry(c1, c2);
                        if ((tableEntry & COUNTRY_TYPE_MASK) == SIMPLE_CASE_COUNTRY_MASK
                             && tableEntry != INVALID_COUNTRY_ENTRY) {
                            char finalChar = (char) ((tableEntry & SIMPLE_CASE_COUNTRY_FINAL_CHAR_MASK) + 'A');
                            int defaultFractionDigits = (tableEntry & SIMPLE_CASE_COUNTRY_DEFAULT_DIGITS_MASK) >> SIMPLE_CASE_COUNTRY_DEFAULT_DIGITS_SHIFT;
                            int numericCode = (tableEntry & NUMERIC_CODE_MASK) >> NUMERIC_CODE_SHIFT;
                            StringBuilder sb = new StringBuilder();
                            sb.append(c1);
                            sb.append(c2);
                            sb.append(finalChar);
                            available.add(getInstance(sb.toString(), defaultFractionDigits, numericCode));
                        } else if ((tableEntry & COUNTRY_TYPE_MASK) == SPECIAL_CASE_COUNTRY_MASK
                                && tableEntry != INVALID_COUNTRY_ENTRY
                                && tableEntry != COUNTRY_WITHOUT_CURRENCY_ENTRY) {
                            int index = SpecialCaseEntry.toIndex(tableEntry);
                            SpecialCaseEntry scEntry = specialCasesList.get(index);

                            if (scEntry.cutOverTime == Long.MAX_VALUE
                                    || System.currentTimeMillis() < scEntry.cutOverTime) {
                                available.add(getInstance(scEntry.oldCurrency,
                                        scEntry.oldCurrencyFraction,
                                        scEntry.oldCurrencyNumericCode));
                            } else {
                                available.add(getInstance(scEntry.newCurrency,
                                        scEntry.newCurrencyFraction,
                                        scEntry.newCurrencyNumericCode));
                            }
                        }
                    }
                }

                // Now add other currencies
                for (OtherCurrencyEntry entry : otherCurrenciesList) {
                    available.add(getInstance(entry.currencyCode));
                }
            }
        }

        @SuppressWarnings("unchecked")
        Set<Currency> result = (Set<Currency>) available.clone();
        return result;
    }


    public String getCurrencyCode() {
        return currencyCode;
    }


    public String getSymbol() {
        return getSymbol(Locale.getDefault(Locale.Category.DISPLAY));
    }


    public String getSymbol(Locale locale) {
        LocaleServiceProviderPool pool =
            LocaleServiceProviderPool.getPool(CurrencyNameProvider.class);
        String symbol = pool.getLocalizedObject(
                                CurrencyNameGetter.INSTANCE,
                                locale, currencyCode, SYMBOL);
        if (symbol != null) {
            return symbol;
        }

        // use currency code as symbol of last resort
        return currencyCode;
    }


    public int getDefaultFractionDigits() {
        return defaultFractionDigits;
    }


    public int getNumericCode() {
        return numericCode;
    }


    public String getNumericCodeAsString() {
        /* numeric code could be returned as a 3 digit string simply by using
           String.format("%03d",numericCode); which uses regex to parse the format,
           "%03d" in this case. Parsing a regex gives an extra performance overhead,
           so String.format() approach is avoided in this scenario.
        */
        if (numericCode < 100) {
            StringBuilder sb = new StringBuilder();
            sb.append('0');
            if (numericCode < 10) {
                sb.append('0');
            }
            return sb.append(numericCode).toString();
        }
        return String.valueOf(numericCode);
    }


    public String getDisplayName() {
        return getDisplayName(Locale.getDefault(Locale.Category.DISPLAY));
    }


    public String getDisplayName(Locale locale) {
        LocaleServiceProviderPool pool =
            LocaleServiceProviderPool.getPool(CurrencyNameProvider.class);
        String result = pool.getLocalizedObject(
                                CurrencyNameGetter.INSTANCE,
                                locale, currencyCode, DISPLAYNAME);
        if (result != null) {
            return result;
        }

        // use currency code as symbol of last resort
        return currencyCode;
    }


    @Override
    public String toString() {
        return currencyCode;
    }


    private Object readResolve() {
        return getInstance(currencyCode);
    }


    private static int getMainTableEntry(char char1, char char2) {
        if (char1 < 'A' || char1 > 'Z' || char2 < 'A' || char2 > 'Z') {
            throw new IllegalArgumentException();
        }
        return mainTable[(char1 - 'A') * A_TO_Z + (char2 - 'A')];
    }


    private static void setMainTableEntry(char char1, char char2, int entry) {
        if (char1 < 'A' || char1 > 'Z' || char2 < 'A' || char2 > 'Z') {
            throw new IllegalArgumentException();
        }
        mainTable[(char1 - 'A') * A_TO_Z + (char2 - 'A')] = entry;
    }


    private static class CurrencyNameGetter
        implements LocaleServiceProviderPool.LocalizedObjectGetter<CurrencyNameProvider,
                                                                   String> {
        private static final CurrencyNameGetter INSTANCE = new CurrencyNameGetter();

        @Override
        public String getObject(CurrencyNameProvider currencyNameProvider,
                                Locale locale,
                                String key,
                                Object... params) {
            assert params.length == 1;
            int type = (Integer)params[0];

            switch(type) {
            case SYMBOL:
                return currencyNameProvider.getSymbol(key, locale);
            case DISPLAYNAME:
                return currencyNameProvider.getDisplayName(key, locale);
            default:
                assert false; // shouldn't happen
            }

            return null;
        }
    }

    private static int[] readIntArray(DataInputStream dis, int count) throws IOException {
        int[] ret = new int[count];
        for (int i = 0; i < count; i++) {
            ret[i] = dis.readInt();
        }

        return ret;
    }

    private static List<SpecialCaseEntry> readSpecialCases(DataInputStream dis,
            int count)
            throws IOException {

        List<SpecialCaseEntry> list = new ArrayList<>(count);
        long cutOverTime;
        String oldCurrency;
        String newCurrency;
        int oldCurrencyFraction;
        int newCurrencyFraction;
        int oldCurrencyNumericCode;
        int newCurrencyNumericCode;

        for (int i = 0; i < count; i++) {
            cutOverTime = dis.readLong();
            oldCurrency = dis.readUTF();
            newCurrency = dis.readUTF();
            oldCurrencyFraction = dis.readInt();
            newCurrencyFraction = dis.readInt();
            oldCurrencyNumericCode = dis.readInt();
            newCurrencyNumericCode = dis.readInt();
            SpecialCaseEntry sc = new SpecialCaseEntry(cutOverTime,
                    oldCurrency, newCurrency,
                    oldCurrencyFraction, newCurrencyFraction,
                    oldCurrencyNumericCode, newCurrencyNumericCode);
            list.add(sc);
        }
        return list;
    }

    private static List<OtherCurrencyEntry> readOtherCurrencies(DataInputStream dis,
            int count)
            throws IOException {

        List<OtherCurrencyEntry> list = new ArrayList<>(count);
        String currencyCode;
        int fraction;
        int numericCode;

        for (int i = 0; i < count; i++) {
            currencyCode = dis.readUTF();
            fraction = dis.readInt();
            numericCode = dis.readInt();
            OtherCurrencyEntry oc = new OtherCurrencyEntry(currencyCode,
                    fraction,
                    numericCode);
            list.add(oc);
        }
        return list;
    }


    private static void replaceCurrencyData(Pattern pattern, String ctry, String curdata) {

        if (ctry.length() != 2) {
            // ignore invalid country code
            info("currency.properties entry for " + ctry +
                    " is ignored because of the invalid country code.", null);
            return;
        }

        Matcher m = pattern.matcher(curdata);
        if (!m.find() || (m.group(4) == null && countOccurrences(curdata, ',') >= 3)) {
            // format is not recognized.  ignore the data
            // if group(4) date string is null and we've 4 values, bad date value
            info("currency.properties entry for " + ctry +
                    " ignored because the value format is not recognized.", null);
            return;
        }

        try {
            if (m.group(4) != null && !isPastCutoverDate(m.group(4))) {
                info("currency.properties entry for " + ctry +
                        " ignored since cutover date has not passed :" + curdata, null);
                return;
            }
        } catch (ParseException ex) {
            info("currency.properties entry for " + ctry +
                        " ignored since exception encountered :" + ex.getMessage(), null);
            return;
        }

        String code = m.group(1);
        int numeric = Integer.parseInt(m.group(2));
        int entry = numeric << NUMERIC_CODE_SHIFT;
        int fraction = Integer.parseInt(m.group(3));
        if (fraction > SIMPLE_CASE_COUNTRY_MAX_DEFAULT_DIGITS) {
            info("currency.properties entry for " + ctry +
                " ignored since the fraction is more than " +
                SIMPLE_CASE_COUNTRY_MAX_DEFAULT_DIGITS + ":" + curdata, null);
            return;
        }

        int index = SpecialCaseEntry.indexOf(code, fraction, numeric);

        /* if a country switches from simple case to special case or
         * one special case to other special case which is not present
         * in the sc arrays then insert the new entry in special case arrays
         */
        if (index == -1 && (ctry.charAt(0) != code.charAt(0)
                || ctry.charAt(1) != code.charAt(1))) {

            specialCasesList.add(new SpecialCaseEntry(code, fraction, numeric));
            index = specialCasesList.size() - 1;
        }

        if (index == -1) {
            // simple case
            entry |= (fraction << SIMPLE_CASE_COUNTRY_DEFAULT_DIGITS_SHIFT)
                    | (code.charAt(2) - 'A');
        } else {
            // special case
            entry = SPECIAL_CASE_COUNTRY_MASK
                    | (index + SPECIAL_CASE_COUNTRY_INDEX_DELTA);
        }
        setMainTableEntry(ctry.charAt(0), ctry.charAt(1), entry);
    }

    private static boolean isPastCutoverDate(String s) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        format.setLenient(false);
        long time = format.parse(s.trim()).getTime();
        return System.currentTimeMillis() > time;

    }

    private static int countOccurrences(String value, char match) {
        int count = 0;
        for (char c : value.toCharArray()) {
            if (c == match) {
               ++count;
            }
        }
        return count;
    }

    private static void info(String message, Throwable t) {
        PlatformLogger logger = PlatformLogger.getLogger("java.util.Currency");
        if (logger.isLoggable(PlatformLogger.Level.INFO)) {
            if (t != null) {
                logger.info(message, t);
            } else {
                logger.info(message);
            }
        }
    }

    /* Used to represent a special case currency entry
     * - cutOverTime: cut-over time in millis as returned by
     *   System.currentTimeMillis for special case countries that are changing
     *   currencies; Long.MAX_VALUE for countries that are not changing currencies
     * - oldCurrency: old currencies for special case countries
     * - newCurrency: new currencies for special case countries that are
     *   changing currencies; null for others
     * - oldCurrencyFraction: default fraction digits for old currencies
     * - newCurrencyFraction: default fraction digits for new currencies, 0 for
     *   countries that are not changing currencies
     * - oldCurrencyNumericCode: numeric code for old currencies
     * - newCurrencyNumericCode: numeric code for new currencies, 0 for countries
     *   that are not changing currencies
    */
    private static class SpecialCaseEntry {

        final private long cutOverTime;
        final private String oldCurrency;
        final private String newCurrency;
        final private int oldCurrencyFraction;
        final private int newCurrencyFraction;
        final private int oldCurrencyNumericCode;
        final private int newCurrencyNumericCode;

        private SpecialCaseEntry(long cutOverTime, String oldCurrency, String newCurrency,
                int oldCurrencyFraction, int newCurrencyFraction,
                int oldCurrencyNumericCode, int newCurrencyNumericCode) {
            this.cutOverTime = cutOverTime;
            this.oldCurrency = oldCurrency;
            this.newCurrency = newCurrency;
            this.oldCurrencyFraction = oldCurrencyFraction;
            this.newCurrencyFraction = newCurrencyFraction;
            this.oldCurrencyNumericCode = oldCurrencyNumericCode;
            this.newCurrencyNumericCode = newCurrencyNumericCode;
        }

        private SpecialCaseEntry(String currencyCode, int fraction,
                int numericCode) {
            this(Long.MAX_VALUE, currencyCode, "", fraction, 0, numericCode, 0);
        }

        //get the index of the special case entry
        private static int indexOf(String code, int fraction, int numeric) {
            int size = specialCasesList.size();
            for (int index = 0; index < size; index++) {
                SpecialCaseEntry scEntry = specialCasesList.get(index);
                if (scEntry.oldCurrency.equals(code)
                        && scEntry.oldCurrencyFraction == fraction
                        && scEntry.oldCurrencyNumericCode == numeric
                        && scEntry.cutOverTime == Long.MAX_VALUE) {
                    return index;
                }
            }
            return -1;
        }

        // get the fraction and numericCode of the sc currencycode
        private static int[] findEntry(String code) {
            int[] fractionAndNumericCode = null;
            int size = specialCasesList.size();
            for (int index = 0; index < size; index++) {
                SpecialCaseEntry scEntry = specialCasesList.get(index);
                if (scEntry.oldCurrency.equals(code) && (scEntry.cutOverTime == Long.MAX_VALUE
                        || System.currentTimeMillis() < scEntry.cutOverTime)) {
                    //consider only when there is no new currency or cutover time is not passed
                    fractionAndNumericCode = new int[2];
                    fractionAndNumericCode[0] = scEntry.oldCurrencyFraction;
                    fractionAndNumericCode[1] = scEntry.oldCurrencyNumericCode;
                    break;
                } else if (scEntry.newCurrency.equals(code)
                        && System.currentTimeMillis() >= scEntry.cutOverTime) {
                    //consider only if the cutover time is passed
                    fractionAndNumericCode = new int[2];
                    fractionAndNumericCode[0] = scEntry.newCurrencyFraction;
                    fractionAndNumericCode[1] = scEntry.newCurrencyNumericCode;
                    break;
                }
            }
            return fractionAndNumericCode;
        }

        // convert the special case entry to sc arrays index
        private static int toIndex(int tableEntry) {
            return (tableEntry & SPECIAL_CASE_COUNTRY_INDEX_MASK) - SPECIAL_CASE_COUNTRY_INDEX_DELTA;
        }

    }

    /* Used to represent Other currencies
     * - currencyCode: currency codes that are not the main currency
     *   of a simple country
     * - otherCurrenciesDFD: decimal format digits for other currencies
     * - otherCurrenciesNumericCode: numeric code for other currencies
     */
    private static class OtherCurrencyEntry {

        final private String currencyCode;
        final private int fraction;
        final private int numericCode;

        private OtherCurrencyEntry(String currencyCode, int fraction,
                int numericCode) {
            this.currencyCode = currencyCode;
            this.fraction = fraction;
            this.numericCode = numericCode;
        }

        //get the instance of the other currency code
        private static OtherCurrencyEntry findEntry(String code) {
            int size = otherCurrenciesList.size();
            for (int index = 0; index < size; index++) {
                OtherCurrencyEntry ocEntry = otherCurrenciesList.get(index);
                if (ocEntry.currencyCode.equalsIgnoreCase(code)) {
                    return ocEntry;
                }
            }
            return null;
        }

    }

}


