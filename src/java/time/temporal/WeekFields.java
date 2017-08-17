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

/*
 *
 *
 *
 *
 *
 * Copyright (c) 2011-2012, Stephen Colebourne & Michael Nascimento Santos
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  * Neither the name of JSR-310 nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package java.time.temporal;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.DAY_OF_WEEK;
import static java.time.temporal.ChronoField.DAY_OF_YEAR;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.FOREVER;
import static java.time.temporal.ChronoUnit.MONTHS;
import static java.time.temporal.ChronoUnit.WEEKS;
import static java.time.temporal.ChronoUnit.YEARS;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.Chronology;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import sun.util.locale.provider.CalendarDataUtility;
import sun.util.locale.provider.LocaleProviderAdapter;
import sun.util.locale.provider.LocaleResources;


public final class WeekFields implements Serializable {
    // implementation notes
    // querying week-of-month or week-of-year should return the week value bound within the month/year
    // however, setting the week value should be lenient (use plus/minus weeks)
    // allow week-of-month outer range [0 to 6]
    // allow week-of-year outer range [0 to 54]
    // this is because callers shouldn't be expected to know the details of validity


    private static final ConcurrentMap<String, WeekFields> CACHE = new ConcurrentHashMap<>(4, 0.75f, 2);


    public static final WeekFields ISO = new WeekFields(DayOfWeek.MONDAY, 4);


    public static final WeekFields SUNDAY_START = WeekFields.of(DayOfWeek.SUNDAY, 1);


    public static final TemporalUnit WEEK_BASED_YEARS = IsoFields.WEEK_BASED_YEARS;


    private static final long serialVersionUID = -1177360819670808121L;


    private final DayOfWeek firstDayOfWeek;

    private final int minimalDays;

    private final transient TemporalField dayOfWeek = ComputedDayOfField.ofDayOfWeekField(this);

    private final transient TemporalField weekOfMonth = ComputedDayOfField.ofWeekOfMonthField(this);

    private final transient TemporalField weekOfYear = ComputedDayOfField.ofWeekOfYearField(this);

    private final transient TemporalField weekOfWeekBasedYear = ComputedDayOfField.ofWeekOfWeekBasedYearField(this);

    private final transient TemporalField weekBasedYear = ComputedDayOfField.ofWeekBasedYearField(this);

    //-----------------------------------------------------------------------

    public static WeekFields of(Locale locale) {
        Objects.requireNonNull(locale, "locale");
        locale = new Locale(locale.getLanguage(), locale.getCountry());  // elminate variants

        int calDow = CalendarDataUtility.retrieveFirstDayOfWeek(locale);
        DayOfWeek dow = DayOfWeek.SUNDAY.plus(calDow - 1);
        int minDays = CalendarDataUtility.retrieveMinimalDaysInFirstWeek(locale);
        return WeekFields.of(dow, minDays);
    }


    public static WeekFields of(DayOfWeek firstDayOfWeek, int minimalDaysInFirstWeek) {
        String key = firstDayOfWeek.toString() + minimalDaysInFirstWeek;
        WeekFields rules = CACHE.get(key);
        if (rules == null) {
            rules = new WeekFields(firstDayOfWeek, minimalDaysInFirstWeek);
            CACHE.putIfAbsent(key, rules);
            rules = CACHE.get(key);
        }
        return rules;
    }

    //-----------------------------------------------------------------------

    private WeekFields(DayOfWeek firstDayOfWeek, int minimalDaysInFirstWeek) {
        Objects.requireNonNull(firstDayOfWeek, "firstDayOfWeek");
        if (minimalDaysInFirstWeek < 1 || minimalDaysInFirstWeek > 7) {
            throw new IllegalArgumentException("Minimal number of days is invalid");
        }
        this.firstDayOfWeek = firstDayOfWeek;
        this.minimalDays = minimalDaysInFirstWeek;
    }

    //-----------------------------------------------------------------------

    private void readObject(ObjectInputStream s)
         throws IOException, ClassNotFoundException, InvalidObjectException
    {
        s.defaultReadObject();
        if (firstDayOfWeek == null) {
            throw new InvalidObjectException("firstDayOfWeek is null");
        }

        if (minimalDays < 1 || minimalDays > 7) {
            throw new InvalidObjectException("Minimal number of days is invalid");
        }
    }


    private Object readResolve() throws InvalidObjectException {
        try {
            return WeekFields.of(firstDayOfWeek, minimalDays);
        } catch (IllegalArgumentException iae) {
            throw new InvalidObjectException("Invalid serialized WeekFields: " + iae.getMessage());
        }
    }

    //-----------------------------------------------------------------------

    public DayOfWeek getFirstDayOfWeek() {
        return firstDayOfWeek;
    }


    public int getMinimalDaysInFirstWeek() {
        return minimalDays;
    }

    //-----------------------------------------------------------------------

    public TemporalField dayOfWeek() {
        return dayOfWeek;
    }


    public TemporalField weekOfMonth() {
        return weekOfMonth;
    }


    public TemporalField weekOfYear() {
        return weekOfYear;
    }


    public TemporalField weekOfWeekBasedYear() {
        return weekOfWeekBasedYear;
    }


    public TemporalField weekBasedYear() {
        return weekBasedYear;
    }

    //-----------------------------------------------------------------------

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof WeekFields) {
            return hashCode() == object.hashCode();
        }
        return false;
    }


    @Override
    public int hashCode() {
        return firstDayOfWeek.ordinal() * 7 + minimalDays;
    }

    //-----------------------------------------------------------------------

    @Override
    public String toString() {
        return "WeekFields[" + firstDayOfWeek + ',' + minimalDays + ']';
    }

    //-----------------------------------------------------------------------

    static class ComputedDayOfField implements TemporalField {


        static ComputedDayOfField ofDayOfWeekField(WeekFields weekDef) {
            return new ComputedDayOfField("DayOfWeek", weekDef, DAYS, WEEKS, DAY_OF_WEEK_RANGE);
        }


        static ComputedDayOfField ofWeekOfMonthField(WeekFields weekDef) {
            return new ComputedDayOfField("WeekOfMonth", weekDef, WEEKS, MONTHS, WEEK_OF_MONTH_RANGE);
        }


        static ComputedDayOfField ofWeekOfYearField(WeekFields weekDef) {
            return new ComputedDayOfField("WeekOfYear", weekDef, WEEKS, YEARS, WEEK_OF_YEAR_RANGE);
        }


        static ComputedDayOfField ofWeekOfWeekBasedYearField(WeekFields weekDef) {
            return new ComputedDayOfField("WeekOfWeekBasedYear", weekDef, WEEKS, IsoFields.WEEK_BASED_YEARS, WEEK_OF_WEEK_BASED_YEAR_RANGE);
        }


        static ComputedDayOfField ofWeekBasedYearField(WeekFields weekDef) {
            return new ComputedDayOfField("WeekBasedYear", weekDef, IsoFields.WEEK_BASED_YEARS, FOREVER, ChronoField.YEAR.range());
        }


        private ChronoLocalDate ofWeekBasedYear(Chronology chrono,
                int yowby, int wowby, int dow) {
            ChronoLocalDate date = chrono.date(yowby, 1, 1);
            int ldow = localizedDayOfWeek(date);
            int offset = startOfWeekOffset(1, ldow);

            // Clamp the week of year to keep it in the same year
            int yearLen = date.lengthOfYear();
            int newYearWeek = computeWeek(offset, yearLen + weekDef.getMinimalDaysInFirstWeek());
            wowby = Math.min(wowby, newYearWeek - 1);

            int days = -offset + (dow - 1) + (wowby - 1) * 7;
            return date.plus(days, DAYS);
        }

        private final String name;
        private final WeekFields weekDef;
        private final TemporalUnit baseUnit;
        private final TemporalUnit rangeUnit;
        private final ValueRange range;

        private ComputedDayOfField(String name, WeekFields weekDef, TemporalUnit baseUnit, TemporalUnit rangeUnit, ValueRange range) {
            this.name = name;
            this.weekDef = weekDef;
            this.baseUnit = baseUnit;
            this.rangeUnit = rangeUnit;
            this.range = range;
        }

        private static final ValueRange DAY_OF_WEEK_RANGE = ValueRange.of(1, 7);
        private static final ValueRange WEEK_OF_MONTH_RANGE = ValueRange.of(0, 1, 4, 6);
        private static final ValueRange WEEK_OF_YEAR_RANGE = ValueRange.of(0, 1, 52, 54);
        private static final ValueRange WEEK_OF_WEEK_BASED_YEAR_RANGE = ValueRange.of(1, 52, 53);

        @Override
        public long getFrom(TemporalAccessor temporal) {
            if (rangeUnit == WEEKS) {  // day-of-week
                return localizedDayOfWeek(temporal);
            } else if (rangeUnit == MONTHS) {  // week-of-month
                return localizedWeekOfMonth(temporal);
            } else if (rangeUnit == YEARS) {  // week-of-year
                return localizedWeekOfYear(temporal);
            } else if (rangeUnit == WEEK_BASED_YEARS) {
                return localizedWeekOfWeekBasedYear(temporal);
            } else if (rangeUnit == FOREVER) {
                return localizedWeekBasedYear(temporal);
            } else {
                throw new IllegalStateException("unreachable, rangeUnit: " + rangeUnit + ", this: " + this);
            }
        }

        private int localizedDayOfWeek(TemporalAccessor temporal) {
            int sow = weekDef.getFirstDayOfWeek().getValue();
            int isoDow = temporal.get(DAY_OF_WEEK);
            return Math.floorMod(isoDow - sow, 7) + 1;
        }

        private int localizedDayOfWeek(int isoDow) {
            int sow = weekDef.getFirstDayOfWeek().getValue();
            return Math.floorMod(isoDow - sow, 7) + 1;
        }

        private long localizedWeekOfMonth(TemporalAccessor temporal) {
            int dow = localizedDayOfWeek(temporal);
            int dom = temporal.get(DAY_OF_MONTH);
            int offset = startOfWeekOffset(dom, dow);
            return computeWeek(offset, dom);
        }

        private long localizedWeekOfYear(TemporalAccessor temporal) {
            int dow = localizedDayOfWeek(temporal);
            int doy = temporal.get(DAY_OF_YEAR);
            int offset = startOfWeekOffset(doy, dow);
            return computeWeek(offset, doy);
        }


        private int localizedWeekBasedYear(TemporalAccessor temporal) {
            int dow = localizedDayOfWeek(temporal);
            int year = temporal.get(YEAR);
            int doy = temporal.get(DAY_OF_YEAR);
            int offset = startOfWeekOffset(doy, dow);
            int week = computeWeek(offset, doy);
            if (week == 0) {
                // Day is in end of week of previous year; return the previous year
                return year - 1;
            } else {
                // If getting close to end of year, use higher precision logic
                // Check if date of year is in partial week associated with next year
                ValueRange dayRange = temporal.range(DAY_OF_YEAR);
                int yearLen = (int)dayRange.getMaximum();
                int newYearWeek = computeWeek(offset, yearLen + weekDef.getMinimalDaysInFirstWeek());
                if (week >= newYearWeek) {
                    return year + 1;
                }
            }
            return year;
        }


        private int localizedWeekOfWeekBasedYear(TemporalAccessor temporal) {
            int dow = localizedDayOfWeek(temporal);
            int doy = temporal.get(DAY_OF_YEAR);
            int offset = startOfWeekOffset(doy, dow);
            int week = computeWeek(offset, doy);
            if (week == 0) {
                // Day is in end of week of previous year
                // Recompute from the last day of the previous year
                ChronoLocalDate date = Chronology.from(temporal).date(temporal);
                date = date.minus(doy, DAYS);   // Back down into previous year
                return localizedWeekOfWeekBasedYear(date);
            } else if (week > 50) {
                // If getting close to end of year, use higher precision logic
                // Check if date of year is in partial week associated with next year
                ValueRange dayRange = temporal.range(DAY_OF_YEAR);
                int yearLen = (int)dayRange.getMaximum();
                int newYearWeek = computeWeek(offset, yearLen + weekDef.getMinimalDaysInFirstWeek());
                if (week >= newYearWeek) {
                    // Overlaps with week of following year; reduce to week in following year
                    week = week - newYearWeek + 1;
                }
            }
            return week;
        }


        private int startOfWeekOffset(int day, int dow) {
            // offset of first day corresponding to the day of week in first 7 days (zero origin)
            int weekStart = Math.floorMod(day - dow, 7);
            int offset = -weekStart;
            if (weekStart + 1 > weekDef.getMinimalDaysInFirstWeek()) {
                // The previous week has the minimum days in the current month to be a 'week'
                offset = 7 - weekStart;
            }
            return offset;
        }


        private int computeWeek(int offset, int day) {
            return ((7 + offset + (day - 1)) / 7);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <R extends Temporal> R adjustInto(R temporal, long newValue) {
            // Check the new value and get the old value of the field
            int newVal = range.checkValidIntValue(newValue, this);  // lenient check range
            int currentVal = temporal.get(this);
            if (newVal == currentVal) {
                return temporal;
            }

            if (rangeUnit == FOREVER) {     // replace year of WeekBasedYear
                // Create a new date object with the same chronology,
                // the desired year and the same week and dow.
                int idow = temporal.get(weekDef.dayOfWeek);
                int wowby = temporal.get(weekDef.weekOfWeekBasedYear);
                return (R) ofWeekBasedYear(Chronology.from(temporal), (int)newValue, wowby, idow);
            } else {
                // Compute the difference and add that using the base unit of the field
                return (R) temporal.plus(newVal - currentVal, baseUnit);
            }
        }

        @Override
        public ChronoLocalDate resolve(
                Map<TemporalField, Long> fieldValues, TemporalAccessor partialTemporal, ResolverStyle resolverStyle) {
            final long value = fieldValues.get(this);
            final int newValue = Math.toIntExact(value);  // broad limit makes overflow checking lighter
            // first convert localized day-of-week to ISO day-of-week
            // doing this first handles case where both ISO and localized were parsed and might mismatch
            // day-of-week is always strict as two different day-of-week values makes lenient complex
            if (rangeUnit == WEEKS) {  // day-of-week
                final int checkedValue = range.checkValidIntValue(value, this);  // no leniency as too complex
                final int startDow = weekDef.getFirstDayOfWeek().getValue();
                long isoDow = Math.floorMod((startDow - 1) + (checkedValue - 1), 7) + 1;
                fieldValues.remove(this);
                fieldValues.put(DAY_OF_WEEK, isoDow);
                return null;
            }

            // can only build date if ISO day-of-week is present
            if (fieldValues.containsKey(DAY_OF_WEEK) == false) {
                return null;
            }
            int isoDow = DAY_OF_WEEK.checkValidIntValue(fieldValues.get(DAY_OF_WEEK));
            int dow = localizedDayOfWeek(isoDow);

            // build date
            Chronology chrono = Chronology.from(partialTemporal);
            if (fieldValues.containsKey(YEAR)) {
                int year = YEAR.checkValidIntValue(fieldValues.get(YEAR));  // validate
                if (rangeUnit == MONTHS && fieldValues.containsKey(MONTH_OF_YEAR)) {  // week-of-month
                    long month = fieldValues.get(MONTH_OF_YEAR);  // not validated yet
                    return resolveWoM(fieldValues, chrono, year, month, newValue, dow, resolverStyle);
                }
                if (rangeUnit == YEARS) {  // week-of-year
                    return resolveWoY(fieldValues, chrono, year, newValue, dow, resolverStyle);
                }
            } else if ((rangeUnit == WEEK_BASED_YEARS || rangeUnit == FOREVER) &&
                    fieldValues.containsKey(weekDef.weekBasedYear) &&
                    fieldValues.containsKey(weekDef.weekOfWeekBasedYear)) { // week-of-week-based-year and year-of-week-based-year
                return resolveWBY(fieldValues, chrono, dow, resolverStyle);
            }
            return null;
        }

        private ChronoLocalDate resolveWoM(
                Map<TemporalField, Long> fieldValues, Chronology chrono, int year, long month, long wom, int localDow, ResolverStyle resolverStyle) {
            ChronoLocalDate date;
            if (resolverStyle == ResolverStyle.LENIENT) {
                date = chrono.date(year, 1, 1).plus(Math.subtractExact(month, 1), MONTHS);
                long weeks = Math.subtractExact(wom, localizedWeekOfMonth(date));
                int days = localDow - localizedDayOfWeek(date);  // safe from overflow
                date = date.plus(Math.addExact(Math.multiplyExact(weeks, 7), days), DAYS);
            } else {
                int monthValid = MONTH_OF_YEAR.checkValidIntValue(month);  // validate
                date = chrono.date(year, monthValid, 1);
                int womInt = range.checkValidIntValue(wom, this);  // validate
                int weeks = (int) (womInt - localizedWeekOfMonth(date));  // safe from overflow
                int days = localDow - localizedDayOfWeek(date);  // safe from overflow
                date = date.plus(weeks * 7 + days, DAYS);
                if (resolverStyle == ResolverStyle.STRICT && date.getLong(MONTH_OF_YEAR) != month) {
                    throw new DateTimeException("Strict mode rejected resolved date as it is in a different month");
                }
            }
            fieldValues.remove(this);
            fieldValues.remove(YEAR);
            fieldValues.remove(MONTH_OF_YEAR);
            fieldValues.remove(DAY_OF_WEEK);
            return date;
        }

        private ChronoLocalDate resolveWoY(
                Map<TemporalField, Long> fieldValues, Chronology chrono, int year, long woy, int localDow, ResolverStyle resolverStyle) {
            ChronoLocalDate date = chrono.date(year, 1, 1);
            if (resolverStyle == ResolverStyle.LENIENT) {
                long weeks = Math.subtractExact(woy, localizedWeekOfYear(date));
                int days = localDow - localizedDayOfWeek(date);  // safe from overflow
                date = date.plus(Math.addExact(Math.multiplyExact(weeks, 7), days), DAYS);
            } else {
                int womInt = range.checkValidIntValue(woy, this);  // validate
                int weeks = (int) (womInt - localizedWeekOfYear(date));  // safe from overflow
                int days = localDow - localizedDayOfWeek(date);  // safe from overflow
                date = date.plus(weeks * 7 + days, DAYS);
                if (resolverStyle == ResolverStyle.STRICT && date.getLong(YEAR) != year) {
                    throw new DateTimeException("Strict mode rejected resolved date as it is in a different year");
                }
            }
            fieldValues.remove(this);
            fieldValues.remove(YEAR);
            fieldValues.remove(DAY_OF_WEEK);
            return date;
        }

        private ChronoLocalDate resolveWBY(
                Map<TemporalField, Long> fieldValues, Chronology chrono, int localDow, ResolverStyle resolverStyle) {
            int yowby = weekDef.weekBasedYear.range().checkValidIntValue(
                    fieldValues.get(weekDef.weekBasedYear), weekDef.weekBasedYear);
            ChronoLocalDate date;
            if (resolverStyle == ResolverStyle.LENIENT) {
                date = ofWeekBasedYear(chrono, yowby, 1, localDow);
                long wowby = fieldValues.get(weekDef.weekOfWeekBasedYear);
                long weeks = Math.subtractExact(wowby, 1);
                date = date.plus(weeks, WEEKS);
            } else {
                int wowby = weekDef.weekOfWeekBasedYear.range().checkValidIntValue(
                        fieldValues.get(weekDef.weekOfWeekBasedYear), weekDef.weekOfWeekBasedYear);  // validate
                date = ofWeekBasedYear(chrono, yowby, wowby, localDow);
                if (resolverStyle == ResolverStyle.STRICT && localizedWeekBasedYear(date) != yowby) {
                    throw new DateTimeException("Strict mode rejected resolved date as it is in a different week-based-year");
                }
            }
            fieldValues.remove(this);
            fieldValues.remove(weekDef.weekBasedYear);
            fieldValues.remove(weekDef.weekOfWeekBasedYear);
            fieldValues.remove(DAY_OF_WEEK);
            return date;
        }

        //-----------------------------------------------------------------------
        @Override
        public String getDisplayName(Locale locale) {
            Objects.requireNonNull(locale, "locale");
            if (rangeUnit == YEARS) {  // only have values for week-of-year
                LocaleResources lr = LocaleProviderAdapter.getResourceBundleBased()
                        .getLocaleResources(locale);
                ResourceBundle rb = lr.getJavaTimeFormatData();
                return rb.containsKey("field.week") ? rb.getString("field.week") : name;
            }
            return name;
        }

        @Override
        public TemporalUnit getBaseUnit() {
            return baseUnit;
        }

        @Override
        public TemporalUnit getRangeUnit() {
            return rangeUnit;
        }

        @Override
        public boolean isDateBased() {
            return true;
        }

        @Override
        public boolean isTimeBased() {
            return false;
        }

        @Override
        public ValueRange range() {
            return range;
        }

        //-----------------------------------------------------------------------
        @Override
        public boolean isSupportedBy(TemporalAccessor temporal) {
            if (temporal.isSupported(DAY_OF_WEEK)) {
                if (rangeUnit == WEEKS) {  // day-of-week
                    return true;
                } else if (rangeUnit == MONTHS) {  // week-of-month
                    return temporal.isSupported(DAY_OF_MONTH);
                } else if (rangeUnit == YEARS) {  // week-of-year
                    return temporal.isSupported(DAY_OF_YEAR);
                } else if (rangeUnit == WEEK_BASED_YEARS) {
                    return temporal.isSupported(DAY_OF_YEAR);
                } else if (rangeUnit == FOREVER) {
                    return temporal.isSupported(YEAR);
                }
            }
            return false;
        }

        @Override
        public ValueRange rangeRefinedBy(TemporalAccessor temporal) {
            if (rangeUnit == ChronoUnit.WEEKS) {  // day-of-week
                return range;
            } else if (rangeUnit == MONTHS) {  // week-of-month
                return rangeByWeek(temporal, DAY_OF_MONTH);
            } else if (rangeUnit == YEARS) {  // week-of-year
                return rangeByWeek(temporal, DAY_OF_YEAR);
            } else if (rangeUnit == WEEK_BASED_YEARS) {
                return rangeWeekOfWeekBasedYear(temporal);
            } else if (rangeUnit == FOREVER) {
                return YEAR.range();
            } else {
                throw new IllegalStateException("unreachable, rangeUnit: " + rangeUnit + ", this: " + this);
            }
        }


        private ValueRange rangeByWeek(TemporalAccessor temporal, TemporalField field) {
            int dow = localizedDayOfWeek(temporal);
            int offset = startOfWeekOffset(temporal.get(field), dow);
            ValueRange fieldRange = temporal.range(field);
            return ValueRange.of(computeWeek(offset, (int) fieldRange.getMinimum()),
                    computeWeek(offset, (int) fieldRange.getMaximum()));
        }


        private ValueRange rangeWeekOfWeekBasedYear(TemporalAccessor temporal) {
            if (!temporal.isSupported(DAY_OF_YEAR)) {
                return WEEK_OF_YEAR_RANGE;
            }
            int dow = localizedDayOfWeek(temporal);
            int doy = temporal.get(DAY_OF_YEAR);
            int offset = startOfWeekOffset(doy, dow);
            int week = computeWeek(offset, doy);
            if (week == 0) {
                // Day is in end of week of previous year
                // Recompute from the last day of the previous year
                ChronoLocalDate date = Chronology.from(temporal).date(temporal);
                date = date.minus(doy + 7, DAYS);   // Back down into previous year
                return rangeWeekOfWeekBasedYear(date);
            }
            // Check if day of year is in partial week associated with next year
            ValueRange dayRange = temporal.range(DAY_OF_YEAR);
            int yearLen = (int)dayRange.getMaximum();
            int newYearWeek = computeWeek(offset, yearLen + weekDef.getMinimalDaysInFirstWeek());

            if (week >= newYearWeek) {
                // Overlaps with weeks of following year; recompute from a week in following year
                ChronoLocalDate date = Chronology.from(temporal).date(temporal);
                date = date.plus(yearLen - doy + 1 + 7, ChronoUnit.DAYS);
                return rangeWeekOfWeekBasedYear(date);
            }
            return ValueRange.of(1, newYearWeek-1);
        }

        //-----------------------------------------------------------------------
        @Override
        public String toString() {
            return name + "[" + weekDef.toString() + "]";
        }
    }
}
