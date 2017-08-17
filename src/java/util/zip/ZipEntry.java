/*
 * Copyright (c) 1995, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static java.util.zip.ZipUtils.*;
import java.nio.file.attribute.FileTime;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.ZoneId;

import static java.util.zip.ZipConstants64.*;


public
class ZipEntry implements ZipConstants, Cloneable {

    String name;        // entry name
    long xdostime = -1; // last modification time (in extended DOS time,
                        // where milliseconds lost in conversion might
                        // be encoded into the upper half)
    FileTime mtime;     // last modification time, from extra field data
    FileTime atime;     // last access time, from extra field data
    FileTime ctime;     // creation time, from extra field data
    long crc = -1;      // crc-32 of entry data
    long size = -1;     // uncompressed size of entry data
    long csize = -1;    // compressed size of entry data
    int method = -1;    // compression method
    int flag = 0;       // general purpose flag
    byte[] extra;       // optional extra field data for entry
    String comment;     // optional comment string for entry


    public static final int STORED = 0;


    public static final int DEFLATED = 8;


    static final long DOSTIME_BEFORE_1980 = (1 << 21) | (1 << 16);


    private static final long UPPER_DOSTIME_BOUND =
            128L * 365 * 24 * 60 * 60 * 1000;


    public ZipEntry(String name) {
        Objects.requireNonNull(name, "name");
        if (name.length() > 0xFFFF) {
            throw new IllegalArgumentException("entry name too long");
        }
        this.name = name;
    }


    public ZipEntry(ZipEntry e) {
        Objects.requireNonNull(e, "entry");
        name = e.name;
        xdostime = e.xdostime;
        mtime = e.mtime;
        atime = e.atime;
        ctime = e.ctime;
        crc = e.crc;
        size = e.size;
        csize = e.csize;
        method = e.method;
        flag = e.flag;
        extra = e.extra;
        comment = e.comment;
    }


    ZipEntry() {}


    public String getName() {
        return name;
    }


    public void setTime(long time) {
        this.xdostime = javaToExtendedDosTime(time);
        // Avoid setting the mtime field if time is in the valid
        // range for a DOS time
        if (xdostime != DOSTIME_BEFORE_1980 && time <= UPPER_DOSTIME_BOUND) {
            this.mtime = null;
        } else {
            this.mtime = FileTime.from(time, TimeUnit.MILLISECONDS);
        }
    }


    public long getTime() {
        if (mtime != null) {
            return mtime.toMillis();
        }
        return (xdostime != -1) ? extendedDosToJavaTime(xdostime) : -1;
    }


    public void setTimeLocal(LocalDateTime time) {
        int year = time.getYear() - 1980;
        if (year < 0) {
            this.xdostime = DOSTIME_BEFORE_1980;
        } else {
            this.xdostime = ((year << 25 |
                time.getMonthValue() << 21 |
                time.getDayOfMonth() << 16 |
                time.getHour() << 11 |
                time.getMinute() << 5 |
                time.getSecond() >> 1) & 0xffffffffL)
                + ((long)(((time.getSecond() & 0x1) * 1000) +
                      time.getNano() / 1000_000) << 32);
        }
        if (xdostime != DOSTIME_BEFORE_1980 && year <= 0x7f) {
            this.mtime = null;
        } else {
            this.mtime = FileTime.from(
                ZonedDateTime.of(time, ZoneId.systemDefault()).toInstant());
        }
    }


    public LocalDateTime getTimeLocal() {
        if (mtime != null) {
            return LocalDateTime.ofInstant(mtime.toInstant(), ZoneId.systemDefault());
        }
        int ms = (int)(xdostime >> 32);
        return LocalDateTime.of((int)(((xdostime >> 25) & 0x7f) + 1980),
                             (int)((xdostime >> 21) & 0x0f),
                             (int)((xdostime >> 16) & 0x1f),
                             (int)((xdostime >> 11) & 0x1f),
                             (int)((xdostime >> 5) & 0x3f),
                             (int)((xdostime << 1) & 0x3e) + ms / 1000,
                             (ms % 1000) * 1000_000);
    }



    public ZipEntry setLastModifiedTime(FileTime time) {
        this.mtime = Objects.requireNonNull(time, "lastModifiedTime");
        this.xdostime = javaToExtendedDosTime(time.to(TimeUnit.MILLISECONDS));
        return this;
    }


    public FileTime getLastModifiedTime() {
        if (mtime != null)
            return mtime;
        if (xdostime == -1)
            return null;
        return FileTime.from(getTime(), TimeUnit.MILLISECONDS);
    }


    public ZipEntry setLastAccessTime(FileTime time) {
        this.atime = Objects.requireNonNull(time, "lastAccessTime");
        return this;
    }


    public FileTime getLastAccessTime() {
        return atime;
    }


    public ZipEntry setCreationTime(FileTime time) {
        this.ctime = Objects.requireNonNull(time, "creationTime");
        return this;
    }


    public FileTime getCreationTime() {
        return ctime;
    }


    public void setSize(long size) {
        if (size < 0) {
            throw new IllegalArgumentException("invalid entry size");
        }
        this.size = size;
    }


    public long getSize() {
        return size;
    }


    public long getCompressedSize() {
        return csize;
    }


    public void setCompressedSize(long csize) {
        this.csize = csize;
    }


    public void setCrc(long crc) {
        if (crc < 0 || crc > 0xFFFFFFFFL) {
            throw new IllegalArgumentException("invalid entry crc-32");
        }
        this.crc = crc;
    }


    public long getCrc() {
        return crc;
    }


    public void setMethod(int method) {
        if (method != STORED && method != DEFLATED) {
            throw new IllegalArgumentException("invalid compression method");
        }
        this.method = method;
    }


    public int getMethod() {
        return method;
    }


    public void setExtra(byte[] extra) {
        setExtra0(extra, false);
    }


    void setExtra0(byte[] extra, boolean doZIP64) {
        if (extra != null) {
            if (extra.length > 0xFFFF) {
                throw new IllegalArgumentException("invalid extra field length");
            }
            // extra fields are in "HeaderID(2)DataSize(2)Data... format
            int off = 0;
            int len = extra.length;
            while (off + 4 < len) {
                int tag = get16(extra, off);
                int sz = get16(extra, off + 2);
                off += 4;
                if (off + sz > len)         // invalid data
                    break;
                switch (tag) {
                case EXTID_ZIP64:
                    if (doZIP64) {
                        // LOC extra zip64 entry MUST include BOTH original
                        // and compressed file size fields.
                        // If invalid zip64 extra fields, simply skip. Even
                        // it's rare, it's possible the entry size happens to
                        // be the magic value and it "accidently" has some
                        // bytes in extra match the id.
                        if (sz >= 16) {
                            size = get64(extra, off);
                            csize = get64(extra, off + 8);
                        }
                    }
                    break;
                case EXTID_NTFS:
                    if (sz < 32) // reserved  4 bytes + tag 2 bytes + size 2 bytes
                        break;   // m[a|c]time 24 bytes
                    int pos = off + 4;               // reserved 4 bytes
                    if (get16(extra, pos) !=  0x0001 || get16(extra, pos + 2) != 24)
                        break;
                    long wtime = get64(extra, pos + 4);
                    if (wtime != WINDOWS_TIME_NOT_AVAILABLE) {
                        mtime = winTimeToFileTime(wtime);
                    }
                    wtime = get64(extra, pos + 12);
                    if (wtime != WINDOWS_TIME_NOT_AVAILABLE) {
                        atime = winTimeToFileTime(wtime);
                    }
                    wtime = get64(extra, pos + 20);
                    if (wtime != WINDOWS_TIME_NOT_AVAILABLE) {
                        ctime = winTimeToFileTime(wtime);
                    }
                    break;
                case EXTID_EXTT:
                    int flag = Byte.toUnsignedInt(extra[off]);
                    int sz0 = 1;
                    // The CEN-header extra field contains the modification
                    // time only, or no timestamp at all. 'sz' is used to
                    // flag its presence or absence. But if mtime is present
                    // in LOC it must be present in CEN as well.
                    if ((flag & 0x1) != 0 && (sz0 + 4) <= sz) {
                        mtime = unixTimeToFileTime(get32S(extra, off + sz0));
                        sz0 += 4;
                    }
                    if ((flag & 0x2) != 0 && (sz0 + 4) <= sz) {
                        atime = unixTimeToFileTime(get32S(extra, off + sz0));
                        sz0 += 4;
                    }
                    if ((flag & 0x4) != 0 && (sz0 + 4) <= sz) {
                        ctime = unixTimeToFileTime(get32S(extra, off + sz0));
                        sz0 += 4;
                    }
                    break;
                 default:
                }
                off += sz;
            }
        }
        this.extra = extra;
    }


    public byte[] getExtra() {
        return extra;
    }


    public void setComment(String comment) {
        this.comment = comment;
    }


    public String getComment() {
        return comment;
    }


    public boolean isDirectory() {
        return name.endsWith("/");
    }


    public String toString() {
        return getName();
    }


    public int hashCode() {
        return name.hashCode();
    }


    public Object clone() {
        try {
            ZipEntry e = (ZipEntry)super.clone();
            e.extra = (extra == null) ? null : extra.clone();
            return e;
        } catch (CloneNotSupportedException e) {
            // This should never happen, since we are Cloneable
            throw new InternalError(e);
        }
    }
}
