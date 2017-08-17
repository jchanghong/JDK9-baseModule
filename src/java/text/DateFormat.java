/*
 * Copyright (c) 1996, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * (C) Copyright Taligent, Inc. 1996 - All Rights Reserved
 * (C) Copyright IBM Corp. 1996 - All Rights Reserved
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

import java.io.InvalidObjectException;
import java.text.spi.DateFormatProvider;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.util.spi.LocaleServiceProvider;
import sun.util.locale.provider.LocaleProviderAdapter;
import sun.util.locale.provider.LocaleServiceProviderPool;


public abstract class DateFormat extends Format {


    protected Calendar calendar;


    protected NumberFormat numberFormat;


    public static final int ERA_FIELD = 0;

    public static final int YEAR_FIELD = 1;

    public static final int MONTH_FIELD = 2;

    public static final int DATE_FIELD = 3;

    public static final int HOUR_OF_DAY1_FIELD = 4;

    public static final int HOUR_OF_DAY0_FIELD = 5;

    public static final int MINUTE_FIELD = 6;

    public static final int SECOND_FIELD = 7;

    public static final int MILLISECOND_FIELD = 8;

    public static final int DAY_OF_WEEK_FIELD = 9;

    public static final int DAY_OF_YEAR_FIELD = 10;

    public static final int DAY_OF_WEEK_IN_MONTH_FIELD = 11;

    public static final int WEEK_OF_YEAR_FIELD = 12;

    public static final int WEEK_OF_MONTH_FIELD = 13;

    public static final int AM_PM_FIELD = 14;

    public static final int HOUR1_FIELD = 15;

    public static final int HOUR0_FIELD = 16;

    public static final int TIMEZONE_FIELD = 17;

    // Proclaim serial compatibility with 1.1 FCS
    private static final long serialVersionUID = 7218322306649953788L;


    public final StringBuffer format(Object obj, StringBuffer toAppendTo,
                                     FieldPosition fieldPosition)
    {
        if (obj instanceof Date)
            return format( (Date)obj, toAppendTo, fieldPosition );
        else if (obj instanceof Number)
            return format( new Date(((Number)obj).longValue()),
                          toAppendTo, fieldPosition );
        else
            throw new IllegalArgumentException("Cannot format given Object as a Date");
    }


    public abstract StringBuffer format(Date date, StringBuffer toAppendTo,
                                        FieldPosition fieldPosition);


    public final String format(Date date)
    {
        return format(date, new StringBuffer(),
                      DontCareFieldPosition.INSTANCE).toString();
    }


    public Date parse(String source) throws ParseException
    {
        ParsePosition pos = new ParsePosition(0);
        Date result = parse(source, pos);
        if (pos.index == 0)
            throw new ParseException("Unparseable date: \"" + source + "\"" ,
                pos.errorIndex);
        return result;
    }


    public abstract Date parse(String source, ParsePosition pos);


    public Object parseObject(String source, ParsePosition pos) {
        return parse(source, pos);
    }


    public static final int FULL = 0;

    public static final int LONG = 1;

    public static final int MEDIUM = 2;

    public static final int SHORT = 3;

    public static final int DEFAULT = MEDIUM;


    public static final DateFormat getTimeInstance()
    {
        return get(DEFAULT, 0, 1, Locale.getDefault(Locale.Category.FORMAT));
    }


    public static final DateFormat getTimeInstance(int style)
    {
        return get(style, 0, 1, Locale.getDefault(Locale.Category.FORMAT));
    }


    public static final DateFormat getTimeInstance(int style,
                                                 Locale aLocale)
    {
        return get(style, 0, 1, aLocale);
    }


    public static final DateFormat getDateInstance()
    {
        return get(0, DEFAULT, 2, Locale.getDefault(Locale.Category.FORMAT));
    }


    public static final DateFormat getDateInstance(int style)
    {
        return get(0, style, 2, Locale.getDefault(Locale.Category.FORMAT));
    }


    public static final DateFormat getDateInstance(int style,
                                                 Locale aLocale)
    {
        return get(0, style, 2, aLocale);
    }


    public static final DateFormat getDateTimeInstance()
    {
        return get(DEFAULT, DEFAULT, 3, Locale.getDefault(Locale.Category.FORMAT));
    }


    public static final DateFormat getDateTimeInstance(int dateStyle,
                                                       int timeStyle)
    {
        return get(timeStyle, dateStyle, 3, Locale.getDefault(Locale.Category.FORMAT));
    }


    public static final DateFormat
        getDateTimeInstance(int dateStyle, int timeStyle, Locale aLocale)
    {
        return get(timeStyle, dateStyle, 3, aLocale);
    }


    public static final DateFormat getInstance() {
        return getDateTimeInstance(SHORT, SHORT);
    }


    public static Locale[] getAvailableLocales()
    {
        LocaleServiceProviderPool pool =
            LocaleServiceProviderPool.getPool(DateFormatProvider.class);
        return pool.getAvailableLocales();
    }


    public void setCalendar(Calendar newCalendar)
    {
        this.calendar = newCalendar;
    }


    public Calendar getCalendar()
    {
        return calendar;
    }


    public void setNumberFormat(NumberFormat newNumberFormat)
    {
        this.numberFormat = newNumberFormat;
    }


    public NumberFormat getNumberFormat()
    {
        return numberFormat;
    }


    public void setTimeZone(TimeZone zone)
    {
        calendar.setTimeZone(zone);
    }


    public TimeZone getTimeZone()
    {
        return calendar.getTimeZone();
    }


    public void setLenient(boolean lenient)
    {
        calendar.setLenient(lenient);
    }


    public boolean isLenient()
    {
        return calendar.isLenient();
    }


    public int hashCode() {
        return numberFormat.hashCode();
        // just enough fields for a reasonable distribution
    }


    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DateFormat other = (DateFormat) obj;
        return (// calendar.equivalentTo(other.calendar) // THIS API DOESN'T EXIST YET!
                calendar.getFirstDayOfWeek() == other.calendar.getFirstDayOfWeek() &&
                calendar.getMinimalDaysInFirstWeek() == other.calendar.getMinimalDaysInFirstWeek() &&
                calendar.isLenient() == other.calendar.isLenient() &&
                calendar.getTimeZone().equals(other.calendar.getTimeZone()) &&
                numberFormat.equals(other.numberFormat));
    }


    public Object clone()
    {
        DateFormat other = (DateFormat) super.clone();
        other.calendar = (Calendar) calendar.clone();
        other.numberFormat = (NumberFormat) numberFormat.clone();
        return other;
    }


    private static DateFormat get(int timeStyle, int dateStyle,
                                  int flags, Locale loc) {
        if ((flags & 1) != 0) {
            if (timeStyle < 0 || timeStyle > 3) {
                throw new IllegalArgumentException("Illegal time style " + timeStyle);
            }
        } else {
            timeStyle = -1;
        }
        if ((flags & 2) != 0) {
            if (dateStyle < 0 || dateStyle > 3) {
                throw new IllegalArgumentException("Illegal date style " + dateStyle);
            }
        } else {
            dateStyle = -1;
        }

        LocaleProviderAdapter adapter = LocaleProviderAdapter.getAdapter(DateFormatProvider.class, loc);
        DateFormat dateFormat = get(adapter, timeStyle, dateStyle, loc);
        if (dateFormat == null) {
            dateFormat = get(LocaleProviderAdapter.forJRE(), timeStyle, dateStyle, loc);
        }
        return dateFormat;
    }

    private static DateFormat get(LocaleProviderAdapter adapter, int timeStyle, int dateStyle, Locale loc) {
        DateFormatProvider provider = adapter.getDateFormatProvider();
        DateFormat dateFormat;
        if (timeStyle == -1) {
            dateFormat = provider.getDateInstance(dateStyle, loc);
        } else {
            if (dateStyle == -1) {
                dateFormat = provider.getTimeInstance(timeStyle, loc);
            } else {
                dateFormat = provider.getDateTimeInstance(dateStyle, timeStyle, loc);
            }
        }
        return dateFormat;
    }


    protected DateFormat() {}


    public static class Field extends Format.Field {

        // Proclaim serial compatibility with 1.4 FCS
        private static final long serialVersionUID = 7441350119349544720L;

        // table of all instances in this class, used by readResolve
        private static final Map<String, Field> instanceMap = new HashMap<>(18);
        // Maps from Calendar constant (such as Calendar.ERA) to Field
        // constant (such as Field.ERA).
        private static final Field[] calendarToFieldMapping =
                                             new Field[Calendar.FIELD_COUNT];


        private int calendarField;


        public static Field ofCalendarField(int calendarField) {
            if (calendarField < 0 || calendarField >=
                        calendarToFieldMapping.length) {
                throw new IllegalArgumentException("Unknown Calendar constant "
                                                   + calendarField);
            }
            return calendarToFieldMapping[calendarField];
        }


        protected Field(String name, int calendarField) {
            super(name);
            this.calendarField = calendarField;
            if (this.getClass() == DateFormat.Field.class) {
                instanceMap.put(name, this);
                if (calendarField >= 0) {
                    // assert(calendarField < Calendar.FIELD_COUNT);
                    calendarToFieldMapping[calendarField] = this;
                }
            }
        }


        public int getCalendarField() {
            return calendarField;
        }


        @Override
        protected Object readResolve() throws InvalidObjectException {
            if (this.getClass() != DateFormat.Field.class) {
                throw new InvalidObjectException("subclass didn't correctly implement readResolve");
            }

            Object instance = instanceMap.get(getName());
            if (instance != null) {
                return instance;
            } else {
                throw new InvalidObjectException("unknown attribute name");
            }
        }

        //
        // The constants
        //


        public static final Field ERA = new Field("era", Calendar.ERA);


        public static final Field YEAR = new Field("year", Calendar.YEAR);


        public static final Field MONTH = new Field("month", Calendar.MONTH);


        public static final Field DAY_OF_MONTH = new
                            Field("day of month", Calendar.DAY_OF_MONTH);


        public static final Field HOUR_OF_DAY1 = new Field("hour of day 1",-1);


        public static final Field HOUR_OF_DAY0 = new
               Field("hour of day", Calendar.HOUR_OF_DAY);


        public static final Field MINUTE =new Field("minute", Calendar.MINUTE);


        public static final Field SECOND =new Field("second", Calendar.SECOND);


        public static final Field MILLISECOND = new
                Field("millisecond", Calendar.MILLISECOND);


        public static final Field DAY_OF_WEEK = new
                Field("day of week", Calendar.DAY_OF_WEEK);


        public static final Field DAY_OF_YEAR = new
                Field("day of year", Calendar.DAY_OF_YEAR);


        public static final Field DAY_OF_WEEK_IN_MONTH =
                     new Field("day of week in month",
                                            Calendar.DAY_OF_WEEK_IN_MONTH);


        public static final Field WEEK_OF_YEAR = new
              Field("week of year", Calendar.WEEK_OF_YEAR);


        public static final Field WEEK_OF_MONTH = new
            Field("week of month", Calendar.WEEK_OF_MONTH);


        public static final Field AM_PM = new
                            Field("am pm", Calendar.AM_PM);


        public static final Field HOUR1 = new Field("hour 1", -1);


        public static final Field HOUR0 = new
                            Field("hour", Calendar.HOUR);


        public static final Field TIME_ZONE = new Field("time zone", -1);
    }
}
