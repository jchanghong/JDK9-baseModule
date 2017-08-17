/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.file.attribute;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.concurrent.TimeUnit;



public final class FileTime
    implements Comparable<FileTime>
{

    private final TimeUnit unit;


    private final long value;


    private Instant instant;


    private String valueAsString;


    private FileTime(long value, TimeUnit unit, Instant instant) {
        this.value = value;
        this.unit = unit;
        this.instant = instant;
    }


    public static FileTime from(long value, TimeUnit unit) {
        Objects.requireNonNull(unit, "unit");
        return new FileTime(value, unit, null);
    }


    public static FileTime fromMillis(long value) {
        return new FileTime(value, TimeUnit.MILLISECONDS, null);
    }


    public static FileTime from(Instant instant) {
        Objects.requireNonNull(instant, "instant");
        return new FileTime(0, null, instant);
    }


    public long to(TimeUnit unit) {
        Objects.requireNonNull(unit, "unit");
        if (this.unit != null) {
            return unit.convert(this.value, this.unit);
        } else {
            long secs = unit.convert(instant.getEpochSecond(), TimeUnit.SECONDS);
            if (secs == Long.MIN_VALUE || secs == Long.MAX_VALUE) {
                return secs;
            }
            long nanos = unit.convert(instant.getNano(), TimeUnit.NANOSECONDS);
            long r = secs + nanos;
            // Math.addExact() variant
            if (((secs ^ r) & (nanos ^ r)) < 0) {
                return (secs < 0) ? Long.MIN_VALUE : Long.MAX_VALUE;
            }
            return r;
        }
    }


    public long toMillis() {
        if (unit != null) {
            return unit.toMillis(value);
        } else {
            long secs = instant.getEpochSecond();
            int  nanos = instant.getNano();
            // Math.multiplyExact() variant
            long r = secs * 1000;
            long ax = Math.abs(secs);
            if (((ax | 1000) >>> 31 != 0)) {
                if ((r / 1000) != secs) {
                    return (secs < 0) ? Long.MIN_VALUE : Long.MAX_VALUE;
                }
            }
            return r + nanos / 1000_000;
        }
    }


    private static final long HOURS_PER_DAY      = 24L;
    private static final long MINUTES_PER_HOUR   = 60L;
    private static final long SECONDS_PER_MINUTE = 60L;
    private static final long SECONDS_PER_HOUR   = SECONDS_PER_MINUTE * MINUTES_PER_HOUR;
    private static final long SECONDS_PER_DAY    = SECONDS_PER_HOUR * HOURS_PER_DAY;
    private static final long MILLIS_PER_SECOND  = 1000L;
    private static final long MICROS_PER_SECOND  = 1000_000L;
    private static final long NANOS_PER_SECOND   = 1000_000_000L;
    private static final int  NANOS_PER_MILLI    = 1000_000;
    private static final int  NANOS_PER_MICRO    = 1000;
    // The epoch second of Instant.MIN.
    private static final long MIN_SECOND = -31557014167219200L;
    // The epoch second of Instant.MAX.
    private static final long MAX_SECOND = 31556889864403199L;

    /*
     * Scale d by m, checking for overflow.
     */
    private static long scale(long d, long m, long over) {
        if (d >  over) return Long.MAX_VALUE;
        if (d < -over) return Long.MIN_VALUE;
        return d * m;
    }


    public Instant toInstant() {
        if (instant == null) {
            long secs = 0L;
            int nanos = 0;
            switch (unit) {
                case DAYS:
                    secs = scale(value, SECONDS_PER_DAY,
                                 Long.MAX_VALUE/SECONDS_PER_DAY);
                    break;
                case HOURS:
                    secs = scale(value, SECONDS_PER_HOUR,
                                 Long.MAX_VALUE/SECONDS_PER_HOUR);
                    break;
                case MINUTES:
                    secs = scale(value, SECONDS_PER_MINUTE,
                                 Long.MAX_VALUE/SECONDS_PER_MINUTE);
                    break;
                case SECONDS:
                    secs = value;
                    break;
                case MILLISECONDS:
                    secs = Math.floorDiv(value, MILLIS_PER_SECOND);
                    nanos = (int)Math.floorMod(value, MILLIS_PER_SECOND)
                            * NANOS_PER_MILLI;
                    break;
                case MICROSECONDS:
                    secs = Math.floorDiv(value, MICROS_PER_SECOND);
                    nanos = (int)Math.floorMod(value, MICROS_PER_SECOND)
                            * NANOS_PER_MICRO;
                    break;
                case NANOSECONDS:
                    secs = Math.floorDiv(value, NANOS_PER_SECOND);
                    nanos = (int)Math.floorMod(value, NANOS_PER_SECOND);
                    break;
                default : throw new AssertionError("Unit not handled");
            }
            if (secs <= MIN_SECOND)
                instant = Instant.MIN;
            else if (secs >= MAX_SECOND)
                instant = Instant.MAX;
            else
                instant = Instant.ofEpochSecond(secs, nanos);
        }
        return instant;
    }


    @Override
    public boolean equals(Object obj) {
        return (obj instanceof FileTime) ? compareTo((FileTime)obj) == 0 : false;
    }


    @Override
    public int hashCode() {
        // hashcode of instant representation to satisfy contract with equals
        return toInstant().hashCode();
    }

    private long toDays() {
        if (unit != null) {
            return unit.toDays(value);
        } else {
            return TimeUnit.SECONDS.toDays(toInstant().getEpochSecond());
        }
    }

    private long toExcessNanos(long days) {
        if (unit != null) {
            return unit.toNanos(value - unit.convert(days, TimeUnit.DAYS));
        } else {
            return TimeUnit.SECONDS.toNanos(toInstant().getEpochSecond()
                                            - TimeUnit.DAYS.toSeconds(days));
        }
    }


    @Override
    public int compareTo(FileTime other) {
        // same granularity
        if (unit != null && unit == other.unit) {
            return Long.compare(value, other.value);
        } else {
            // compare using instant representation when unit differs
            long secs = toInstant().getEpochSecond();
            long secsOther = other.toInstant().getEpochSecond();
            int cmp = Long.compare(secs, secsOther);
            if (cmp != 0) {
                return cmp;
            }
            cmp = Long.compare(toInstant().getNano(), other.toInstant().getNano());
            if (cmp != 0) {
                return cmp;
            }
            if (secs != MAX_SECOND && secs != MIN_SECOND) {
                return 0;
            }
            // if both this and other's Instant reps are MIN/MAX,
            // use daysSinceEpoch and nanosOfDays, which will not
            // saturate during calculation.
            long days = toDays();
            long daysOther = other.toDays();
            if (days == daysOther) {
                return Long.compare(toExcessNanos(days), other.toExcessNanos(daysOther));
            }
            return Long.compare(days, daysOther);
        }
    }

    // days in a 400 year cycle = 146097
    // days in a 10,000 year cycle = 146097 * 25
    // seconds per day = 86400
    private static final long DAYS_PER_10000_YEARS = 146097L * 25L;
    private static final long SECONDS_PER_10000_YEARS = 146097L * 25L * 86400L;
    private static final long SECONDS_0000_TO_1970 = ((146097L * 5L) - (30L * 365L + 7L)) * 86400L;

    // append year/month/day/hour/minute/second/nano with width and 0 padding
    private StringBuilder append(StringBuilder sb, int w, int d) {
        while (w > 0) {
            sb.append((char)(d/w + '0'));
            d = d % w;
            w /= 10;
        }
        return sb;
    }


    @Override
    public String toString() {
        if (valueAsString == null) {
            long secs = 0L;
            int  nanos = 0;
            if (instant == null && unit.compareTo(TimeUnit.SECONDS) >= 0) {
                secs = unit.toSeconds(value);
            } else {
                secs = toInstant().getEpochSecond();
                nanos = toInstant().getNano();
            }
            LocalDateTime ldt;
            int year = 0;
            if (secs >= -SECONDS_0000_TO_1970) {
                // current era
                long zeroSecs = secs - SECONDS_PER_10000_YEARS + SECONDS_0000_TO_1970;
                long hi = Math.floorDiv(zeroSecs, SECONDS_PER_10000_YEARS) + 1;
                long lo = Math.floorMod(zeroSecs, SECONDS_PER_10000_YEARS);
                ldt = LocalDateTime.ofEpochSecond(lo - SECONDS_0000_TO_1970, nanos, ZoneOffset.UTC);
                year = ldt.getYear() +  (int)hi * 10000;
            } else {
                // before current era
                long zeroSecs = secs + SECONDS_0000_TO_1970;
                long hi = zeroSecs / SECONDS_PER_10000_YEARS;
                long lo = zeroSecs % SECONDS_PER_10000_YEARS;
                ldt = LocalDateTime.ofEpochSecond(lo - SECONDS_0000_TO_1970, nanos, ZoneOffset.UTC);
                year = ldt.getYear() + (int)hi * 10000;
            }
            if (year <= 0) {
                year = year - 1;
            }
            int fraction = ldt.getNano();
            StringBuilder sb = new StringBuilder(64);
            sb.append(year < 0 ? "-" : "");
            year = Math.abs(year);
            if (year < 10000) {
                append(sb, 1000, Math.abs(year));
            } else {
                sb.append(String.valueOf(year));
            }
            sb.append('-');
            append(sb, 10, ldt.getMonthValue());
            sb.append('-');
            append(sb, 10, ldt.getDayOfMonth());
            sb.append('T');
            append(sb, 10, ldt.getHour());
            sb.append(':');
            append(sb, 10, ldt.getMinute());
            sb.append(':');
            append(sb, 10, ldt.getSecond());
            if (fraction != 0) {
                sb.append('.');
                // adding leading zeros and stripping any trailing zeros
                int w = 100_000_000;
                while (fraction % 10 == 0) {
                    fraction /= 10;
                    w /= 10;
                }
                append(sb, w, fraction);
            }
            sb.append('Z');
            valueAsString = sb.toString();
        }
        return valueAsString;
    }
}
