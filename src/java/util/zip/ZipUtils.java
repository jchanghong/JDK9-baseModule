/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

package java.util.zip;

import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

import static java.util.zip.ZipConstants.ENDHDR;

class ZipUtils {

    // used to adjust values between Windows and java epoch
    private static final long WINDOWS_EPOCH_IN_MICROSECONDS = -11644473600000000L;

    // used to indicate the corresponding windows time is not available
    public static final long WINDOWS_TIME_NOT_AVAILABLE = Long.MIN_VALUE;


    public static final FileTime winTimeToFileTime(long wtime) {
        return FileTime.from(wtime / 10 + WINDOWS_EPOCH_IN_MICROSECONDS,
                             TimeUnit.MICROSECONDS);
    }


    public static final long fileTimeToWinTime(FileTime ftime) {
        return (ftime.to(TimeUnit.MICROSECONDS) - WINDOWS_EPOCH_IN_MICROSECONDS) * 10;
    }


    public static final long UPPER_UNIXTIME_BOUND = 0x7fffffff;


    public static final FileTime unixTimeToFileTime(long utime) {
        return FileTime.from(utime, TimeUnit.SECONDS);
    }


    public static final long fileTimeToUnixTime(FileTime ftime) {
        return ftime.to(TimeUnit.SECONDS);
    }


    public static long dosToJavaTime(long dtime) {
        LocalDateTime ldt = LocalDateTime.of(
                (int) (((dtime >> 25) & 0x7f) + 1980),
                (int) ((dtime >> 21) & 0x0f),
                (int) ((dtime >> 16) & 0x1f),
                (int) ((dtime >> 11) & 0x1f),
                (int) ((dtime >> 5) & 0x3f),
                (int) ((dtime << 1) & 0x3e));
        return TimeUnit.MILLISECONDS.convert(ldt.toEpochSecond(
                ZoneId.systemDefault().getRules().getOffset(ldt)), TimeUnit.SECONDS);
    }


    public static long extendedDosToJavaTime(long xdostime) {
        long time = dosToJavaTime(xdostime);
        return time + (xdostime >> 32);
    }


    private static long javaToDosTime(long time) {
        Instant instant = Instant.ofEpochMilli(time);
        LocalDateTime ldt = LocalDateTime.ofInstant(
                instant, ZoneId.systemDefault());
        int year = ldt.getYear() - 1980;
        if (year < 0) {
            return (1 << 21) | (1 << 16);
        }
        return (year << 25 |
            ldt.getMonthValue() << 21 |
            ldt.getDayOfMonth() << 16 |
            ldt.getHour() << 11 |
            ldt.getMinute() << 5 |
            ldt.getSecond() >> 1) & 0xffffffffL;
    }


    public static long javaToExtendedDosTime(long time) {
        if (time < 0) {
            return ZipEntry.DOSTIME_BEFORE_1980;
        }
        long dostime = javaToDosTime(time);
        return (dostime != ZipEntry.DOSTIME_BEFORE_1980)
                ? dostime + ((time % 2000) << 32)
                : ZipEntry.DOSTIME_BEFORE_1980;
    }


    public static final int get16(byte b[], int off) {
        return (b[off] & 0xff) | ((b[off + 1] & 0xff) << 8);
    }


    public static final long get32(byte b[], int off) {
        return (get16(b, off) | ((long)get16(b, off+2) << 16)) & 0xffffffffL;
    }


    public static final long get64(byte b[], int off) {
        return get32(b, off) | (get32(b, off+4) << 32);
    }


    public static final int get32S(byte b[], int off) {
        return (get16(b, off) | (get16(b, off+2) << 16));
    }

    // fields access methods
    static final int CH(byte[] b, int n) {
        return b[n] & 0xff ;
    }

    static final int SH(byte[] b, int n) {
        return (b[n] & 0xff) | ((b[n + 1] & 0xff) << 8);
    }

    static final long LG(byte[] b, int n) {
        return ((SH(b, n)) | (SH(b, n + 2) << 16)) & 0xffffffffL;
    }

    static final long LL(byte[] b, int n) {
        return (LG(b, n)) | (LG(b, n + 4) << 32);
    }

    static final long GETSIG(byte[] b) {
        return LG(b, 0);
    }

    // local file (LOC) header fields
    static final long LOCSIG(byte[] b) { return LG(b, 0); } // signature
    static final int  LOCVER(byte[] b) { return SH(b, 4); } // version needed to extract
    static final int  LOCFLG(byte[] b) { return SH(b, 6); } // general purpose bit flags
    static final int  LOCHOW(byte[] b) { return SH(b, 8); } // compression method
    static final long LOCTIM(byte[] b) { return LG(b, 10);} // modification time
    static final long LOCCRC(byte[] b) { return LG(b, 14);} // crc of uncompressed data
    static final long LOCSIZ(byte[] b) { return LG(b, 18);} // compressed data size
    static final long LOCLEN(byte[] b) { return LG(b, 22);} // uncompressed data size
    static final int  LOCNAM(byte[] b) { return SH(b, 26);} // filename length
    static final int  LOCEXT(byte[] b) { return SH(b, 28);} // extra field length

    // extra local (EXT) header fields
    static final long EXTCRC(byte[] b) { return LG(b, 4);}  // crc of uncompressed data
    static final long EXTSIZ(byte[] b) { return LG(b, 8);}  // compressed size
    static final long EXTLEN(byte[] b) { return LG(b, 12);} // uncompressed size

    // end of central directory header (END) fields
    static final int  ENDSUB(byte[] b) { return SH(b, 8); }  // number of entries on this disk
    static final int  ENDTOT(byte[] b) { return SH(b, 10);}  // total number of entries
    static final long ENDSIZ(byte[] b) { return LG(b, 12);}  // central directory size
    static final long ENDOFF(byte[] b) { return LG(b, 16);}  // central directory offset
    static final int  ENDCOM(byte[] b) { return SH(b, 20);}  // size of zip file comment
    static final int  ENDCOM(byte[] b, int off) { return SH(b, off + 20);}

    // zip64 end of central directory recoder fields
    static final long ZIP64_ENDTOD(byte[] b) { return LL(b, 24);}  // total number of entries on disk
    static final long ZIP64_ENDTOT(byte[] b) { return LL(b, 32);}  // total number of entries
    static final long ZIP64_ENDSIZ(byte[] b) { return LL(b, 40);}  // central directory size
    static final long ZIP64_ENDOFF(byte[] b) { return LL(b, 48);}  // central directory offset
    static final long ZIP64_LOCOFF(byte[] b) { return LL(b, 8);}   // zip64 end offset

    // central directory header (CEN) fields
    static final long CENSIG(byte[] b, int pos) { return LG(b, pos + 0); }
    static final int  CENVEM(byte[] b, int pos) { return SH(b, pos + 4); }
    static final int  CENVER(byte[] b, int pos) { return SH(b, pos + 6); }
    static final int  CENFLG(byte[] b, int pos) { return SH(b, pos + 8); }
    static final int  CENHOW(byte[] b, int pos) { return SH(b, pos + 10);}
    static final long CENTIM(byte[] b, int pos) { return LG(b, pos + 12);}
    static final long CENCRC(byte[] b, int pos) { return LG(b, pos + 16);}
    static final long CENSIZ(byte[] b, int pos) { return LG(b, pos + 20);}
    static final long CENLEN(byte[] b, int pos) { return LG(b, pos + 24);}
    static final int  CENNAM(byte[] b, int pos) { return SH(b, pos + 28);}
    static final int  CENEXT(byte[] b, int pos) { return SH(b, pos + 30);}
    static final int  CENCOM(byte[] b, int pos) { return SH(b, pos + 32);}
    static final int  CENDSK(byte[] b, int pos) { return SH(b, pos + 34);}
    static final int  CENATT(byte[] b, int pos) { return SH(b, pos + 36);}
    static final long CENATX(byte[] b, int pos) { return LG(b, pos + 38);}
    static final long CENOFF(byte[] b, int pos) { return LG(b, pos + 42);}

    // The END header is followed by a variable length comment of size < 64k.
    static final long END_MAXLEN = 0xFFFF + ENDHDR;
    static final int READBLOCKSZ = 128;
}
