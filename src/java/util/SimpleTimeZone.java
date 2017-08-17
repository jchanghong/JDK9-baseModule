/*
 * Copyright (c) 1996, 2011, Oracle and/or its affiliates. All rights reserved.
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

package java.util;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import sun.util.calendar.CalendarSystem;
import sun.util.calendar.CalendarUtils;
import sun.util.calendar.BaseCalendar;
import sun.util.calendar.Gregorian;



public class SimpleTimeZone extends TimeZone {

    public SimpleTimeZone(int rawOffset, String ID)
    {
        this.rawOffset = rawOffset;
        setID (ID);
        dstSavings = millisPerHour; // In case user sets rules later
    }


    public SimpleTimeZone(int rawOffset, String ID,
                          int startMonth, int startDay, int startDayOfWeek, int startTime,
                          int endMonth, int endDay, int endDayOfWeek, int endTime)
    {
        this(rawOffset, ID,
             startMonth, startDay, startDayOfWeek, startTime, WALL_TIME,
             endMonth, endDay, endDayOfWeek, endTime, WALL_TIME,
             millisPerHour);
    }


    public SimpleTimeZone(int rawOffset, String ID,
                          int startMonth, int startDay, int startDayOfWeek, int startTime,
                          int endMonth, int endDay, int endDayOfWeek, int endTime,
                          int dstSavings)
    {
        this(rawOffset, ID,
             startMonth, startDay, startDayOfWeek, startTime, WALL_TIME,
             endMonth, endDay, endDayOfWeek, endTime, WALL_TIME,
             dstSavings);
    }


    public SimpleTimeZone(int rawOffset, String ID,
                          int startMonth, int startDay, int startDayOfWeek,
                          int startTime, int startTimeMode,
                          int endMonth, int endDay, int endDayOfWeek,
                          int endTime, int endTimeMode,
                          int dstSavings) {

        setID(ID);
        this.rawOffset      = rawOffset;
        this.startMonth     = startMonth;
        this.startDay       = startDay;
        this.startDayOfWeek = startDayOfWeek;
        this.startTime      = startTime;
        this.startTimeMode  = startTimeMode;
        this.endMonth       = endMonth;
        this.endDay         = endDay;
        this.endDayOfWeek   = endDayOfWeek;
        this.endTime        = endTime;
        this.endTimeMode    = endTimeMode;
        this.dstSavings     = dstSavings;

        // this.useDaylight is set by decodeRules
        decodeRules();
        if (dstSavings <= 0) {
            throw new IllegalArgumentException("Illegal daylight saving value: " + dstSavings);
        }
    }


    public void setStartYear(int year)
    {
        startYear = year;
        invalidateCache();
    }


    public void setStartRule(int startMonth, int startDay, int startDayOfWeek, int startTime)
    {
        this.startMonth = startMonth;
        this.startDay = startDay;
        this.startDayOfWeek = startDayOfWeek;
        this.startTime = startTime;
        startTimeMode = WALL_TIME;
        decodeStartRule();
        invalidateCache();
    }


    public void setStartRule(int startMonth, int startDay, int startTime) {
        setStartRule(startMonth, startDay, 0, startTime);
    }


    public void setStartRule(int startMonth, int startDay, int startDayOfWeek,
                             int startTime, boolean after)
    {
        // TODO: this method doesn't check the initial values of dayOfMonth or dayOfWeek.
        if (after) {
            setStartRule(startMonth, startDay, -startDayOfWeek, startTime);
        } else {
            setStartRule(startMonth, -startDay, -startDayOfWeek, startTime);
        }
    }


    public void setEndRule(int endMonth, int endDay, int endDayOfWeek,
                           int endTime)
    {
        this.endMonth = endMonth;
        this.endDay = endDay;
        this.endDayOfWeek = endDayOfWeek;
        this.endTime = endTime;
        this.endTimeMode = WALL_TIME;
        decodeEndRule();
        invalidateCache();
    }


    public void setEndRule(int endMonth, int endDay, int endTime)
    {
        setEndRule(endMonth, endDay, 0, endTime);
    }


    public void setEndRule(int endMonth, int endDay, int endDayOfWeek, int endTime, boolean after)
    {
        if (after) {
            setEndRule(endMonth, endDay, -endDayOfWeek, endTime);
        } else {
            setEndRule(endMonth, -endDay, -endDayOfWeek, endTime);
        }
    }


    public int getOffset(long date) {
        return getOffsets(date, null);
    }


    int getOffsets(long date, int[] offsets) {
        int offset = rawOffset;

      computeOffset:
        if (useDaylight) {
            synchronized (this) {
                if (cacheStart != 0) {
                    if (date >= cacheStart && date < cacheEnd) {
                        offset += dstSavings;
                        break computeOffset;
                    }
                }
            }
            BaseCalendar cal = date >= GregorianCalendar.DEFAULT_GREGORIAN_CUTOVER ?
                gcal : (BaseCalendar) CalendarSystem.forName("julian");
            BaseCalendar.Date cdate = (BaseCalendar.Date) cal.newCalendarDate(TimeZone.NO_TIMEZONE);
            // Get the year in local time
            cal.getCalendarDate(date + rawOffset, cdate);
            int year = cdate.getNormalizedYear();
            if (year >= startYear) {
                // Clear time elements for the transition calculations
                cdate.setTimeOfDay(0, 0, 0, 0);
                offset = getOffset(cal, cdate, year, date);
            }
        }

        if (offsets != null) {
            offsets[0] = rawOffset;
            offsets[1] = offset - rawOffset;
        }
        return offset;
    }


    public int getOffset(int era, int year, int month, int day, int dayOfWeek,
                         int millis)
    {
        if (era != GregorianCalendar.AD && era != GregorianCalendar.BC) {
            throw new IllegalArgumentException("Illegal era " + era);
        }

        int y = year;
        if (era == GregorianCalendar.BC) {
            // adjust y with the GregorianCalendar-style year numbering.
            y = 1 - y;
        }

        // If the year isn't representable with the 64-bit long
        // integer in milliseconds, convert the year to an
        // equivalent year. This is required to pass some JCK test cases
        // which are actually useless though because the specified years
        // can't be supported by the Java time system.
        if (y >= 292278994) {
            y = 2800 + y % 2800;
        } else if (y <= -292269054) {
            // y %= 28 also produces an equivalent year, but positive
            // year numbers would be convenient to use the UNIX cal
            // command.
            y = (int) CalendarUtils.mod((long) y, 28);
        }

        // convert year to its 1-based month value
        int m = month + 1;

        // First, calculate time as a Gregorian date.
        BaseCalendar cal = gcal;
        BaseCalendar.Date cdate = (BaseCalendar.Date) cal.newCalendarDate(TimeZone.NO_TIMEZONE);
        cdate.setDate(y, m, day);
        long time = cal.getTime(cdate); // normalize cdate
        time += millis - rawOffset; // UTC time

        // If the time value represents a time before the default
        // Gregorian cutover, recalculate time using the Julian
        // calendar system. For the Julian calendar system, the
        // normalized year numbering is ..., -2 (BCE 2), -1 (BCE 1),
        // 1, 2 ... which is different from the GregorianCalendar
        // style year numbering (..., -1, 0 (BCE 1), 1, 2, ...).
        if (time < GregorianCalendar.DEFAULT_GREGORIAN_CUTOVER) {
            cal = (BaseCalendar) CalendarSystem.forName("julian");
            cdate = (BaseCalendar.Date) cal.newCalendarDate(TimeZone.NO_TIMEZONE);
            cdate.setNormalizedDate(y, m, day);
            time = cal.getTime(cdate) + millis - rawOffset;
        }

        if ((cdate.getNormalizedYear() != y)
            || (cdate.getMonth() != m)
            || (cdate.getDayOfMonth() != day)
            // The validation should be cdate.getDayOfWeek() ==
            // dayOfWeek. However, we don't check dayOfWeek for
            // compatibility.
            || (dayOfWeek < Calendar.SUNDAY || dayOfWeek > Calendar.SATURDAY)
            || (millis < 0 || millis >= (24*60*60*1000))) {
            throw new IllegalArgumentException();
        }

        if (!useDaylight || year < startYear || era != GregorianCalendar.CE) {
            return rawOffset;
        }

        return getOffset(cal, cdate, y, time);
    }

    private int getOffset(BaseCalendar cal, BaseCalendar.Date cdate, int year, long time) {
        synchronized (this) {
            if (cacheStart != 0) {
                if (time >= cacheStart && time < cacheEnd) {
                    return rawOffset + dstSavings;
                }
                if (year == cacheYear) {
                    return rawOffset;
                }
            }
        }

        long start = getStart(cal, cdate, year);
        long end = getEnd(cal, cdate, year);
        int offset = rawOffset;
        if (start <= end) {
            if (time >= start && time < end) {
                offset += dstSavings;
            }
            synchronized (this) {
                cacheYear = year;
                cacheStart = start;
                cacheEnd = end;
            }
        } else {
            if (time < end) {
                // TODO: support Gregorian cutover. The previous year
                // may be in the other calendar system.
                start = getStart(cal, cdate, year - 1);
                if (time >= start) {
                    offset += dstSavings;
                }
            } else if (time >= start) {
                // TODO: support Gregorian cutover. The next year
                // may be in the other calendar system.
                end = getEnd(cal, cdate, year + 1);
                if (time < end) {
                    offset += dstSavings;
                }
            }
            if (start <= end) {
                synchronized (this) {
                    // The start and end transitions are in multiple years.
                    cacheYear = (long) startYear - 1;
                    cacheStart = start;
                    cacheEnd = end;
                }
            }
        }
        return offset;
    }

    private long getStart(BaseCalendar cal, BaseCalendar.Date cdate, int year) {
        int time = startTime;
        if (startTimeMode != UTC_TIME) {
            time -= rawOffset;
        }
        return getTransition(cal, cdate, startMode, year, startMonth, startDay,
                             startDayOfWeek, time);
    }

    private long getEnd(BaseCalendar cal, BaseCalendar.Date cdate, int year) {
        int time = endTime;
        if (endTimeMode != UTC_TIME) {
            time -= rawOffset;
        }
        if (endTimeMode == WALL_TIME) {
            time -= dstSavings;
        }
        return getTransition(cal, cdate, endMode, year, endMonth, endDay,
                                        endDayOfWeek, time);
    }

    private long getTransition(BaseCalendar cal, BaseCalendar.Date cdate,
                               int mode, int year, int month, int dayOfMonth,
                               int dayOfWeek, int timeOfDay) {
        cdate.setNormalizedYear(year);
        cdate.setMonth(month + 1);
        switch (mode) {
        case DOM_MODE:
            cdate.setDayOfMonth(dayOfMonth);
            break;

        case DOW_IN_MONTH_MODE:
            cdate.setDayOfMonth(1);
            if (dayOfMonth < 0) {
                cdate.setDayOfMonth(cal.getMonthLength(cdate));
            }
            cdate = (BaseCalendar.Date) cal.getNthDayOfWeek(dayOfMonth, dayOfWeek, cdate);
            break;

        case DOW_GE_DOM_MODE:
            cdate.setDayOfMonth(dayOfMonth);
            cdate = (BaseCalendar.Date) cal.getNthDayOfWeek(1, dayOfWeek, cdate);
            break;

        case DOW_LE_DOM_MODE:
            cdate.setDayOfMonth(dayOfMonth);
            cdate = (BaseCalendar.Date) cal.getNthDayOfWeek(-1, dayOfWeek, cdate);
            break;
        }
        return cal.getTime(cdate) + timeOfDay;
    }


    public int getRawOffset()
    {
        // The given date will be taken into account while
        // we have the historical time zone data in place.
        return rawOffset;
    }


    public void setRawOffset(int offsetMillis)
    {
        this.rawOffset = offsetMillis;
    }


    public void setDSTSavings(int millisSavedDuringDST) {
        if (millisSavedDuringDST <= 0) {
            throw new IllegalArgumentException("Illegal daylight saving value: "
                                               + millisSavedDuringDST);
        }
        dstSavings = millisSavedDuringDST;
    }


    public int getDSTSavings() {
        return useDaylight ? dstSavings : 0;
    }


    public boolean useDaylightTime()
    {
        return useDaylight;
    }


    @Override
    public boolean observesDaylightTime() {
        return useDaylightTime();
    }


    public boolean inDaylightTime(Date date)
    {
        return (getOffset(date.getTime()) != rawOffset);
    }


    public Object clone()
    {
        return super.clone();
    }


    public synchronized int hashCode()
    {
        return startMonth ^ startDay ^ startDayOfWeek ^ startTime ^
            endMonth ^ endDay ^ endDayOfWeek ^ endTime ^ rawOffset;
    }


    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SimpleTimeZone)) {
            return false;
        }

        SimpleTimeZone that = (SimpleTimeZone) obj;

        return getID().equals(that.getID()) &&
            hasSameRules(that);
    }


    public boolean hasSameRules(TimeZone other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SimpleTimeZone)) {
            return false;
        }
        SimpleTimeZone that = (SimpleTimeZone) other;
        return rawOffset == that.rawOffset &&
            useDaylight == that.useDaylight &&
            (!useDaylight
             // Only check rules if using DST
             || (dstSavings == that.dstSavings &&
                 startMode == that.startMode &&
                 startMonth == that.startMonth &&
                 startDay == that.startDay &&
                 startDayOfWeek == that.startDayOfWeek &&
                 startTime == that.startTime &&
                 startTimeMode == that.startTimeMode &&
                 endMode == that.endMode &&
                 endMonth == that.endMonth &&
                 endDay == that.endDay &&
                 endDayOfWeek == that.endDayOfWeek &&
                 endTime == that.endTime &&
                 endTimeMode == that.endTimeMode &&
                 startYear == that.startYear));
    }


    public String toString() {
        return getClass().getName() +
            "[id=" + getID() +
            ",offset=" + rawOffset +
            ",dstSavings=" + dstSavings +
            ",useDaylight=" + useDaylight +
            ",startYear=" + startYear +
            ",startMode=" + startMode +
            ",startMonth=" + startMonth +
            ",startDay=" + startDay +
            ",startDayOfWeek=" + startDayOfWeek +
            ",startTime=" + startTime +
            ",startTimeMode=" + startTimeMode +
            ",endMode=" + endMode +
            ",endMonth=" + endMonth +
            ",endDay=" + endDay +
            ",endDayOfWeek=" + endDayOfWeek +
            ",endTime=" + endTime +
            ",endTimeMode=" + endTimeMode + ']';
    }

    // =======================privates===============================


    private int startMonth;


    private int startDay;


    private int startDayOfWeek;


    private int startTime;


    private int startTimeMode;


    private int endMonth;


    private int endDay;


    private int endDayOfWeek;


    private int endTime;


    private int endTimeMode;


    private int startYear;


    private int rawOffset;


    private boolean useDaylight=false; // indicate if this time zone uses DST

    private static final int millisPerHour = 60*60*1000;
    private static final int millisPerDay  = 24*millisPerHour;


    private final byte monthLength[] = staticMonthLength;
    private static final byte staticMonthLength[] = {31,28,31,30,31,30,31,31,30,31,30,31};
    private static final byte staticLeapMonthLength[] = {31,29,31,30,31,30,31,31,30,31,30,31};


    private int startMode;


    private int endMode;


    private int dstSavings;

    private static final Gregorian gcal = CalendarSystem.getGregorianCalendar();


    private transient long cacheYear;
    private transient long cacheStart;
    private transient long cacheEnd;


    private static final int DOM_MODE          = 1; // Exact day of month, "Mar 1"
    private static final int DOW_IN_MONTH_MODE = 2; // Day of week in month, "lastSun"
    private static final int DOW_GE_DOM_MODE   = 3; // Day of week after day of month, "Sun>=15"
    private static final int DOW_LE_DOM_MODE   = 4; // Day of week before day of month, "Sun<=21"


    public static final int WALL_TIME = 0; // Zero for backward compatibility


    public static final int STANDARD_TIME = 1;


    public static final int UTC_TIME = 2;

    // Proclaim compatibility with 1.1
    static final long serialVersionUID = -403250971215465050L;

    // the internal serial version which says which version was written
    // - 0 (default) for version up to JDK 1.1.3
    // - 1 for version from JDK 1.1.4, which includes 3 new fields
    // - 2 for JDK 1.3, which includes 2 new fields
    static final int currentSerialVersion = 2;


    private int serialVersionOnStream = currentSerialVersion;

    private synchronized void invalidateCache() {
        cacheYear = startYear - 1;
        cacheStart = cacheEnd = 0;
    }

    //----------------------------------------------------------------------
    // Rule representation
    //
    // We represent the following flavors of rules:
    //       5        the fifth of the month
    //       lastSun  the last Sunday in the month
    //       lastMon  the last Monday in the month
    //       Sun>=8   first Sunday on or after the eighth
    //       Sun<=25  last Sunday on or before the 25th
    // This is further complicated by the fact that we need to remain
    // backward compatible with the 1.1 FCS.  Finally, we need to minimize
    // API changes.  In order to satisfy these requirements, we support
    // three representation systems, and we translate between them.
    //
    // INTERNAL REPRESENTATION
    // This is the format SimpleTimeZone objects take after construction or
    // streaming in is complete.  Rules are represented directly, using an
    // unencoded format.  We will discuss the start rule only below; the end
    // rule is analogous.
    //   startMode      Takes on enumerated values DAY_OF_MONTH,
    //                  DOW_IN_MONTH, DOW_AFTER_DOM, or DOW_BEFORE_DOM.
    //   startDay       The day of the month, or for DOW_IN_MONTH mode, a
    //                  value indicating which DOW, such as +1 for first,
    //                  +2 for second, -1 for last, etc.
    //   startDayOfWeek The day of the week.  Ignored for DAY_OF_MONTH.
    //
    // ENCODED REPRESENTATION
    // This is the format accepted by the constructor and by setStartRule()
    // and setEndRule().  It uses various combinations of positive, negative,
    // and zero values to encode the different rules.  This representation
    // allows us to specify all the different rule flavors without altering
    // the API.
    //   MODE              startMonth    startDay    startDayOfWeek
    //   DOW_IN_MONTH_MODE >=0           !=0         >0
    //   DOM_MODE          >=0           >0          ==0
    //   DOW_GE_DOM_MODE   >=0           >0          <0
    //   DOW_LE_DOM_MODE   >=0           <0          <0
    //   (no DST)          don't care    ==0         don't care
    //
    // STREAMED REPRESENTATION
    // We must retain binary compatibility with the 1.1 FCS.  The 1.1 code only
    // handles DOW_IN_MONTH_MODE and non-DST mode, the latter indicated by the
    // flag useDaylight.  When we stream an object out, we translate into an
    // approximate DOW_IN_MONTH_MODE representation so the object can be parsed
    // and used by 1.1 code.  Following that, we write out the full
    // representation separately so that contemporary code can recognize and
    // parse it.  The full representation is written in a "packed" format,
    // consisting of a version number, a length, and an array of bytes.  Future
    // versions of this class may specify different versions.  If they wish to
    // include additional data, they should do so by storing them after the
    // packed representation below.
    //----------------------------------------------------------------------


    private void decodeRules()
    {
        decodeStartRule();
        decodeEndRule();
    }


    private void decodeStartRule() {
        useDaylight = (startDay != 0) && (endDay != 0);
        if (startDay != 0) {
            if (startMonth < Calendar.JANUARY || startMonth > Calendar.DECEMBER) {
                throw new IllegalArgumentException(
                        "Illegal start month " + startMonth);
            }
            if (startTime < 0 || startTime > millisPerDay) {
                throw new IllegalArgumentException(
                        "Illegal start time " + startTime);
            }
            if (startDayOfWeek == 0) {
                startMode = DOM_MODE;
            } else {
                if (startDayOfWeek > 0) {
                    startMode = DOW_IN_MONTH_MODE;
                } else {
                    startDayOfWeek = -startDayOfWeek;
                    if (startDay > 0) {
                        startMode = DOW_GE_DOM_MODE;
                    } else {
                        startDay = -startDay;
                        startMode = DOW_LE_DOM_MODE;
                    }
                }
                if (startDayOfWeek > Calendar.SATURDAY) {
                    throw new IllegalArgumentException(
                           "Illegal start day of week " + startDayOfWeek);
                }
            }
            if (startMode == DOW_IN_MONTH_MODE) {
                if (startDay < -5 || startDay > 5) {
                    throw new IllegalArgumentException(
                            "Illegal start day of week in month " + startDay);
                }
            } else if (startDay < 1 || startDay > staticMonthLength[startMonth]) {
                throw new IllegalArgumentException(
                        "Illegal start day " + startDay);
            }
        }
    }


    private void decodeEndRule() {
        useDaylight = (startDay != 0) && (endDay != 0);
        if (endDay != 0) {
            if (endMonth < Calendar.JANUARY || endMonth > Calendar.DECEMBER) {
                throw new IllegalArgumentException(
                        "Illegal end month " + endMonth);
            }
            if (endTime < 0 || endTime > millisPerDay) {
                throw new IllegalArgumentException(
                        "Illegal end time " + endTime);
            }
            if (endDayOfWeek == 0) {
                endMode = DOM_MODE;
            } else {
                if (endDayOfWeek > 0) {
                    endMode = DOW_IN_MONTH_MODE;
                } else {
                    endDayOfWeek = -endDayOfWeek;
                    if (endDay > 0) {
                        endMode = DOW_GE_DOM_MODE;
                    } else {
                        endDay = -endDay;
                        endMode = DOW_LE_DOM_MODE;
                    }
                }
                if (endDayOfWeek > Calendar.SATURDAY) {
                    throw new IllegalArgumentException(
                           "Illegal end day of week " + endDayOfWeek);
                }
            }
            if (endMode == DOW_IN_MONTH_MODE) {
                if (endDay < -5 || endDay > 5) {
                    throw new IllegalArgumentException(
                            "Illegal end day of week in month " + endDay);
                }
            } else if (endDay < 1 || endDay > staticMonthLength[endMonth]) {
                throw new IllegalArgumentException(
                        "Illegal end day " + endDay);
            }
        }
    }


    private void makeRulesCompatible()
    {
        switch (startMode) {
        case DOM_MODE:
            startDay = 1 + (startDay / 7);
            startDayOfWeek = Calendar.SUNDAY;
            break;

        case DOW_GE_DOM_MODE:
            // A day-of-month of 1 is equivalent to DOW_IN_MONTH_MODE
            // that is, Sun>=1 == firstSun.
            if (startDay != 1) {
                startDay = 1 + (startDay / 7);
            }
            break;

        case DOW_LE_DOM_MODE:
            if (startDay >= 30) {
                startDay = -1;
            } else {
                startDay = 1 + (startDay / 7);
            }
            break;
        }

        switch (endMode) {
        case DOM_MODE:
            endDay = 1 + (endDay / 7);
            endDayOfWeek = Calendar.SUNDAY;
            break;

        case DOW_GE_DOM_MODE:
            // A day-of-month of 1 is equivalent to DOW_IN_MONTH_MODE
            // that is, Sun>=1 == firstSun.
            if (endDay != 1) {
                endDay = 1 + (endDay / 7);
            }
            break;

        case DOW_LE_DOM_MODE:
            if (endDay >= 30) {
                endDay = -1;
            } else {
                endDay = 1 + (endDay / 7);
            }
            break;
        }

        /*
         * Adjust the start and end times to wall time.  This works perfectly
         * well unless it pushes into the next or previous day.  If that
         * happens, we attempt to adjust the day rule somewhat crudely.  The day
         * rules have been forced into DOW_IN_MONTH mode already, so we change
         * the day of week to move forward or back by a day.  It's possible to
         * make a more refined adjustment of the original rules first, but in
         * most cases this extra effort will go to waste once we adjust the day
         * rules anyway.
         */
        switch (startTimeMode) {
        case UTC_TIME:
            startTime += rawOffset;
            break;
        }
        while (startTime < 0) {
            startTime += millisPerDay;
            startDayOfWeek = 1 + ((startDayOfWeek+5) % 7); // Back 1 day
        }
        while (startTime >= millisPerDay) {
            startTime -= millisPerDay;
            startDayOfWeek = 1 + (startDayOfWeek % 7); // Forward 1 day
        }

        switch (endTimeMode) {
        case UTC_TIME:
            endTime += rawOffset + dstSavings;
            break;
        case STANDARD_TIME:
            endTime += dstSavings;
        }
        while (endTime < 0) {
            endTime += millisPerDay;
            endDayOfWeek = 1 + ((endDayOfWeek+5) % 7); // Back 1 day
        }
        while (endTime >= millisPerDay) {
            endTime -= millisPerDay;
            endDayOfWeek = 1 + (endDayOfWeek % 7); // Forward 1 day
        }
    }


    private byte[] packRules()
    {
        byte[] rules = new byte[6];
        rules[0] = (byte)startDay;
        rules[1] = (byte)startDayOfWeek;
        rules[2] = (byte)endDay;
        rules[3] = (byte)endDayOfWeek;

        // As of serial version 2, include time modes
        rules[4] = (byte)startTimeMode;
        rules[5] = (byte)endTimeMode;

        return rules;
    }


    private void unpackRules(byte[] rules)
    {
        startDay       = rules[0];
        startDayOfWeek = rules[1];
        endDay         = rules[2];
        endDayOfWeek   = rules[3];

        // As of serial version 2, include time modes
        if (rules.length >= 6) {
            startTimeMode = rules[4];
            endTimeMode   = rules[5];
        }
    }


    private int[] packTimes() {
        int[] times = new int[2];
        times[0] = startTime;
        times[1] = endTime;
        return times;
    }


    private void unpackTimes(int[] times) {
        startTime = times[0];
        endTime = times[1];
    }


    private void writeObject(ObjectOutputStream stream)
         throws IOException
    {
        // Construct a binary rule
        byte[] rules = packRules();
        int[] times = packTimes();

        // Convert to 1.1 FCS rules.  This step may cause us to lose information.
        makeRulesCompatible();

        // Write out the 1.1 FCS rules
        stream.defaultWriteObject();

        // Write out the binary rules in the optional data area of the stream.
        stream.writeInt(rules.length);
        stream.write(rules);
        stream.writeObject(times);

        // Recover the original rules.  This recovers the information lost
        // by makeRulesCompatible.
        unpackRules(rules);
        unpackTimes(times);
    }


    private void readObject(ObjectInputStream stream)
         throws IOException, ClassNotFoundException
    {
        stream.defaultReadObject();

        if (serialVersionOnStream < 1) {
            // Fix a bug in the 1.1 SimpleTimeZone code -- namely,
            // startDayOfWeek and endDayOfWeek were usually uninitialized.  We can't do
            // too much, so we assume SUNDAY, which actually works most of the time.
            if (startDayOfWeek == 0) {
                startDayOfWeek = Calendar.SUNDAY;
            }
            if (endDayOfWeek == 0) {
                endDayOfWeek = Calendar.SUNDAY;
            }

            // The variables dstSavings, startMode, and endMode are post-1.1, so they
            // won't be present if we're reading from a 1.1 stream.  Fix them up.
            startMode = endMode = DOW_IN_MONTH_MODE;
            dstSavings = millisPerHour;
        } else {
            // For 1.1.4, in addition to the 3 new instance variables, we also
            // store the actual rules (which have not be made compatible with 1.1)
            // in the optional area.  Read them in here and parse them.
            int length = stream.readInt();
            byte[] rules = new byte[length];
            stream.readFully(rules);
            unpackRules(rules);
        }

        if (serialVersionOnStream >= 2) {
            int[] times = (int[]) stream.readObject();
            unpackTimes(times);
        }

        serialVersionOnStream = currentSerialVersion;
    }
}
