/*
 * Copyright (c) 2003, 2017, Oracle and/or its affiliates. All rights reserved.
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

package java.lang;

import jdk.internal.math.FloatingDecimal;
import java.util.Arrays;
import java.util.Spliterator;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import static java.lang.String.COMPACT_STRINGS;
import static java.lang.String.UTF16;
import static java.lang.String.LATIN1;
import static java.lang.String.checkIndex;
import static java.lang.String.checkOffset;


abstract class AbstractStringBuilder implements Appendable, CharSequence {

    byte[] value;


    byte coder;


    int count;


    AbstractStringBuilder() {
    }


    AbstractStringBuilder(int capacity) {
        if (COMPACT_STRINGS) {
            value = new byte[capacity];
            coder = LATIN1;
        } else {
            value = StringUTF16.newBytesFor(capacity);
            coder = UTF16;
        }
    }


    @Override
    public int length() {
        return count;
    }


    public int capacity() {
        return value.length >> coder;
    }


    public void ensureCapacity(int minimumCapacity) {
        if (minimumCapacity > 0) {
            ensureCapacityInternal(minimumCapacity);
        }
    }


    private void ensureCapacityInternal(int minimumCapacity) {
        // overflow-conscious code
        int oldCapacity = value.length >> coder;
        if (minimumCapacity - oldCapacity > 0) {
            value = Arrays.copyOf(value,
                    newCapacity(minimumCapacity) << coder);
        }
    }


    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;


    private int newCapacity(int minCapacity) {
        // overflow-conscious code
        int oldCapacity = value.length >> coder;
        int newCapacity = (oldCapacity << 1) + 2;
        if (newCapacity - minCapacity < 0) {
            newCapacity = minCapacity;
        }
        int SAFE_BOUND = MAX_ARRAY_SIZE >> coder;
        return (newCapacity <= 0 || SAFE_BOUND - newCapacity < 0)
            ? hugeCapacity(minCapacity)
            : newCapacity;
    }

    private int hugeCapacity(int minCapacity) {
        int SAFE_BOUND = MAX_ARRAY_SIZE >> coder;
        int UNSAFE_BOUND = Integer.MAX_VALUE >> coder;
        if (UNSAFE_BOUND - minCapacity < 0) { // overflow
            throw new OutOfMemoryError();
        }
        return (minCapacity > SAFE_BOUND)
            ? minCapacity : SAFE_BOUND;
    }


    private void inflate() {
        if (!isLatin1()) {
            return;
        }
        byte[] buf = StringUTF16.newBytesFor(value.length);
        StringLatin1.inflate(value, 0, buf, 0, count);
        this.value = buf;
        this.coder = UTF16;
    }


    public void trimToSize() {
        int length = count << coder;
        if (length < value.length) {
            value = Arrays.copyOf(value, length);
        }
    }


    public void setLength(int newLength) {
        if (newLength < 0) {
            throw new StringIndexOutOfBoundsException(newLength);
        }
        ensureCapacityInternal(newLength);
        if (count < newLength) {
            if (isLatin1()) {
                StringLatin1.fillNull(value, count, newLength);
            } else {
                StringUTF16.fillNull(value, count, newLength);
            }
        }
        count = newLength;
    }


    @Override
    public char charAt(int index) {
        checkIndex(index, count);
        if (isLatin1()) {
            return (char)(value[index] & 0xff);
        }
        return StringUTF16.charAt(value, index);
    }


    public int codePointAt(int index) {
        int count = this.count;
        byte[] value = this.value;
        checkIndex(index, count);
        if (isLatin1()) {
            return value[index] & 0xff;
        }
        return StringUTF16.codePointAtSB(value, index, count);
    }


    public int codePointBefore(int index) {
        int i = index - 1;
        if (i < 0 || i >= count) {
            throw new StringIndexOutOfBoundsException(index);
        }
        if (isLatin1()) {
            return value[i] & 0xff;
        }
        return StringUTF16.codePointBeforeSB(value, index);
    }


    public int codePointCount(int beginIndex, int endIndex) {
        if (beginIndex < 0 || endIndex > count || beginIndex > endIndex) {
            throw new IndexOutOfBoundsException();
        }
        if (isLatin1()) {
            return endIndex - beginIndex;
        }
        return StringUTF16.codePointCountSB(value, beginIndex, endIndex);
    }


    public int offsetByCodePoints(int index, int codePointOffset) {
        if (index < 0 || index > count) {
            throw new IndexOutOfBoundsException();
        }
        return Character.offsetByCodePoints(this,
                                            index, codePointOffset);
    }


    public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin)
    {
        checkRangeSIOOBE(srcBegin, srcEnd, count);  // compatible to old version
        int n = srcEnd - srcBegin;
        checkRange(dstBegin, dstBegin + n, dst.length);
        if (isLatin1()) {
            StringLatin1.getChars(value, srcBegin, srcEnd, dst, dstBegin);
        } else {
            StringUTF16.getChars(value, srcBegin, srcEnd, dst, dstBegin);
        }
    }


    public void setCharAt(int index, char ch) {
        checkIndex(index, count);
        if (isLatin1() && StringLatin1.canEncode(ch)) {
            value[index] = (byte)ch;
        } else {
            if (isLatin1()) {
                inflate();
            }
            StringUTF16.putCharSB(value, index, ch);
        }
    }


    public AbstractStringBuilder append(Object obj) {
        return append(String.valueOf(obj));
    }


    public AbstractStringBuilder append(String str) {
        if (str == null) {
            return appendNull();
        }
        int len = str.length();
        ensureCapacityInternal(count + len);
        putStringAt(count, str);
        count += len;
        return this;
    }

    // Documentation in subclasses because of synchro difference
    public AbstractStringBuilder append(StringBuffer sb) {
        return this.append((AbstractStringBuilder)sb);
    }


    AbstractStringBuilder append(AbstractStringBuilder asb) {
        if (asb == null) {
            return appendNull();
        }
        int len = asb.length();
        ensureCapacityInternal(count + len);
        if (getCoder() != asb.getCoder()) {
            inflate();
        }
        asb.getBytes(value, count, coder);
        count += len;
        return this;
    }

    // Documentation in subclasses because of synchro difference
    @Override
    public AbstractStringBuilder append(CharSequence s) {
        if (s == null) {
            return appendNull();
        }
        if (s instanceof String) {
            return this.append((String)s);
        }
        if (s instanceof AbstractStringBuilder) {
            return this.append((AbstractStringBuilder)s);
        }
        return this.append(s, 0, s.length());
    }

    private AbstractStringBuilder appendNull() {
        ensureCapacityInternal(count + 4);
        int count = this.count;
        byte[] val = this.value;
        if (isLatin1()) {
            val[count++] = 'n';
            val[count++] = 'u';
            val[count++] = 'l';
            val[count++] = 'l';
        } else {
            count = StringUTF16.putCharsAt(val, count, 'n', 'u', 'l', 'l');
        }
        this.count = count;
        return this;
    }


    @Override
    public AbstractStringBuilder append(CharSequence s, int start, int end) {
        if (s == null) {
            s = "null";
        }
        checkRange(start, end, s.length());
        int len = end - start;
        ensureCapacityInternal(count + len);
        appendChars(s, start, end);
        return this;
    }


    public AbstractStringBuilder append(char[] str) {
        int len = str.length;
        ensureCapacityInternal(count + len);
        appendChars(str, 0, len);
        return this;
    }


    public AbstractStringBuilder append(char str[], int offset, int len) {
        int end = offset + len;
        checkRange(offset, end, str.length);
        ensureCapacityInternal(count + len);
        appendChars(str, offset, end);
        return this;
    }


    public AbstractStringBuilder append(boolean b) {
        ensureCapacityInternal(count + (b ? 4 : 5));
        int count = this.count;
        byte[] val = this.value;
        if (isLatin1()) {
            if (b) {
                val[count++] = 't';
                val[count++] = 'r';
                val[count++] = 'u';
                val[count++] = 'e';
            } else {
                val[count++] = 'f';
                val[count++] = 'a';
                val[count++] = 'l';
                val[count++] = 's';
                val[count++] = 'e';
            }
        } else {
            if (b) {
                count = StringUTF16.putCharsAt(val, count, 't', 'r', 'u', 'e');
            } else {
                count = StringUTF16.putCharsAt(val, count, 'f', 'a', 'l', 's', 'e');
            }
        }
        this.count = count;
        return this;
    }


    @Override
    public AbstractStringBuilder append(char c) {
        ensureCapacityInternal(count + 1);
        if (isLatin1() && StringLatin1.canEncode(c)) {
            value[count++] = (byte)c;
        } else {
            if (isLatin1()) {
                inflate();
            }
            StringUTF16.putCharSB(value, count++, c);
        }
        return this;
    }


    public AbstractStringBuilder append(int i) {
        int count = this.count;
        int spaceNeeded = count + Integer.stringSize(i);
        ensureCapacityInternal(spaceNeeded);
        if (isLatin1()) {
            Integer.getChars(i, spaceNeeded, value);
        } else {
            StringUTF16.getChars(i, count, spaceNeeded, value);
        }
        this.count = spaceNeeded;
        return this;
    }


    public AbstractStringBuilder append(long l) {
        int count = this.count;
        int spaceNeeded = count + Long.stringSize(l);
        ensureCapacityInternal(spaceNeeded);
        if (isLatin1()) {
            Long.getChars(l, spaceNeeded, value);
        } else {
            StringUTF16.getChars(l, count, spaceNeeded, value);
        }
        this.count = spaceNeeded;
        return this;
    }


    public AbstractStringBuilder append(float f) {
        FloatingDecimal.appendTo(f,this);
        return this;
    }


    public AbstractStringBuilder append(double d) {
        FloatingDecimal.appendTo(d,this);
        return this;
    }


    public AbstractStringBuilder delete(int start, int end) {
        int count = this.count;
        if (end > count) {
            end = count;
        }
        checkRangeSIOOBE(start, end, count);
        int len = end - start;
        if (len > 0) {
            shift(end, -len);
            this.count = count - len;
        }
        return this;
    }


    public AbstractStringBuilder appendCodePoint(int codePoint) {
        if (Character.isBmpCodePoint(codePoint)) {
            return append((char)codePoint);
        }
        return append(Character.toChars(codePoint));
    }


    public AbstractStringBuilder deleteCharAt(int index) {
        checkIndex(index, count);
        shift(index + 1, -1);
        count--;
        return this;
    }


    public AbstractStringBuilder replace(int start, int end, String str) {
        int count = this.count;
        if (end > count) {
            end = count;
        }
        checkRangeSIOOBE(start, end, count);
        int len = str.length();
        int newCount = count + len - (end - start);
        ensureCapacityInternal(newCount);
        shift(end, newCount - count);
        this.count = newCount;
        putStringAt(start, str);
        return this;
    }


    public String substring(int start) {
        return substring(start, count);
    }


    @Override
    public CharSequence subSequence(int start, int end) {
        return substring(start, end);
    }


    public String substring(int start, int end) {
        checkRangeSIOOBE(start, end, count);
        if (isLatin1()) {
            return StringLatin1.newString(value, start, end - start);
        }
        return StringUTF16.newString(value, start, end - start);
    }

    private void shift(int offset, int n) {
        System.arraycopy(value, offset << coder,
                         value, (offset + n) << coder, (count - offset) << coder);
    }


    public AbstractStringBuilder insert(int index, char[] str, int offset,
                                        int len)
    {
        checkOffset(index, count);
        checkRangeSIOOBE(offset, offset + len, str.length);
        ensureCapacityInternal(count + len);
        shift(index, len);
        count += len;
        putCharsAt(index, str, offset, offset + len);
        return this;
    }


    public AbstractStringBuilder insert(int offset, Object obj) {
        return insert(offset, String.valueOf(obj));
    }


    public AbstractStringBuilder insert(int offset, String str) {
        checkOffset(offset, count);
        if (str == null) {
            str = "null";
        }
        int len = str.length();
        ensureCapacityInternal(count + len);
        shift(offset, len);
        count += len;
        putStringAt(offset, str);
        return this;
    }


    public AbstractStringBuilder insert(int offset, char[] str) {
        checkOffset(offset, count);
        int len = str.length;
        ensureCapacityInternal(count + len);
        shift(offset, len);
        count += len;
        putCharsAt(offset, str, 0, len);
        return this;
    }


    public AbstractStringBuilder insert(int dstOffset, CharSequence s) {
        if (s == null) {
            s = "null";
        }
        if (s instanceof String) {
            return this.insert(dstOffset, (String)s);
        }
        return this.insert(dstOffset, s, 0, s.length());
    }


    public AbstractStringBuilder insert(int dstOffset, CharSequence s,
                                        int start, int end)
    {
        if (s == null) {
            s = "null";
        }
        checkOffset(dstOffset, count);
        checkRange(start, end, s.length());
        int len = end - start;
        ensureCapacityInternal(count + len);
        shift(dstOffset, len);
        count += len;
        putCharsAt(dstOffset, s, start, end);
        return this;
    }


    public AbstractStringBuilder insert(int offset, boolean b) {
        return insert(offset, String.valueOf(b));
    }


    public AbstractStringBuilder insert(int offset, char c) {
        checkOffset(offset, count);
        ensureCapacityInternal(count + 1);
        shift(offset, 1);
        count += 1;
        if (isLatin1() && StringLatin1.canEncode(c)) {
            value[offset] = (byte)c;
        } else {
            if (isLatin1()) {
                inflate();
            }
            StringUTF16.putCharSB(value, offset, c);
        }
        return this;
    }


    public AbstractStringBuilder insert(int offset, int i) {
        return insert(offset, String.valueOf(i));
    }


    public AbstractStringBuilder insert(int offset, long l) {
        return insert(offset, String.valueOf(l));
    }


    public AbstractStringBuilder insert(int offset, float f) {
        return insert(offset, String.valueOf(f));
    }


    public AbstractStringBuilder insert(int offset, double d) {
        return insert(offset, String.valueOf(d));
    }


    public int indexOf(String str) {
        return indexOf(str, 0);
    }


    public int indexOf(String str, int fromIndex) {
        return String.indexOf(value, coder, count, str, fromIndex);
    }


    public int lastIndexOf(String str) {
        return lastIndexOf(str, count);
    }


    public int lastIndexOf(String str, int fromIndex) {
        return String.lastIndexOf(value, coder, count, str, fromIndex);
    }


    public AbstractStringBuilder reverse() {
        byte[] val = this.value;
        int count = this.count;
        int coder = this.coder;
        int n = count - 1;
        if (COMPACT_STRINGS && coder == LATIN1) {
            for (int j = (n-1) >> 1; j >= 0; j--) {
                int k = n - j;
                byte cj = val[j];
                val[j] = val[k];
                val[k] = cj;
            }
        } else {
            StringUTF16.reverse(val, count);
        }
        return this;
    }


    @Override
    public abstract String toString();


    @Override
    public IntStream chars() {
        // Reuse String-based spliterator. This requires a supplier to
        // capture the value and count when the terminal operation is executed
        return StreamSupport.intStream(
                () -> {
                    // The combined set of field reads are not atomic and thread
                    // safe but bounds checks will ensure no unsafe reads from
                    // the byte array
                    byte[] val = this.value;
                    int count = this.count;
                    byte coder = this.coder;
                    return coder == LATIN1
                           ? new StringLatin1.CharsSpliterator(val, 0, count, 0)
                           : new StringUTF16.CharsSpliterator(val, 0, count, 0);
                },
                Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED,
                false);
    }


    @Override
    public IntStream codePoints() {
        // Reuse String-based spliterator. This requires a supplier to
        // capture the value and count when the terminal operation is executed
        return StreamSupport.intStream(
                () -> {
                    // The combined set of field reads are not atomic and thread
                    // safe but bounds checks will ensure no unsafe reads from
                    // the byte array
                    byte[] val = this.value;
                    int count = this.count;
                    byte coder = this.coder;
                    return coder == LATIN1
                           ? new StringLatin1.CharsSpliterator(val, 0, count, 0)
                           : new StringUTF16.CodePointsSpliterator(val, 0, count, 0);
                },
                Spliterator.ORDERED,
                false);
    }


    final byte[] getValue() {
        return value;
    }

    /*
     * Invoker guarantees it is in UTF16 (inflate itself for asb), if two
     * coders are different and the dstBegin has enough space
     *
     * @param dstBegin  the char index, not offset of byte[]
     * @param coder     the coder of dst[]
     */
    void getBytes(byte dst[], int dstBegin, byte coder) {
        if (this.coder == coder) {
            System.arraycopy(value, 0, dst, dstBegin << coder, count << coder);
        } else {        // this.coder == LATIN && coder == UTF16
            StringLatin1.inflate(value, 0, dst, dstBegin, count);
        }
    }

    /* for readObject() */
    void initBytes(char[] value, int off, int len) {
        if (String.COMPACT_STRINGS) {
            this.value = StringUTF16.compress(value, off, len);
            if (this.value != null) {
                this.coder = LATIN1;
                return;
            }
        }
        this.coder = UTF16;
        this.value = StringUTF16.toBytes(value, off, len);
    }

    final byte getCoder() {
        return COMPACT_STRINGS ? coder : UTF16;
    }

    final boolean isLatin1() {
        return COMPACT_STRINGS && coder == LATIN1;
    }

    private final void putCharsAt(int index, char[] s, int off, int end) {
        if (isLatin1()) {
            byte[] val = this.value;
            for (int i = off, j = index; i < end; i++) {
                char c = s[i];
                if (StringLatin1.canEncode(c)) {
                    val[j++] = (byte)c;
                } else {
                    inflate();
                    StringUTF16.putCharsSB(this.value, j, s, i, end);
                    return;
                }
            }
        } else {
            StringUTF16.putCharsSB(this.value, index, s, off, end);
        }
    }

    private final void putCharsAt(int index, CharSequence s, int off, int end) {
        if (isLatin1()) {
            byte[] val = this.value;
            for (int i = off, j = index; i < end; i++) {
                char c = s.charAt(i);
                if (StringLatin1.canEncode(c)) {
                    val[j++] = (byte)c;
                } else {
                    inflate();
                    StringUTF16.putCharsSB(this.value, j, s, i, end);
                    return;
                }
            }
        } else {
            StringUTF16.putCharsSB(this.value, index, s, off, end);
        }
    }

    private final void putStringAt(int index, String str) {
        if (getCoder() != str.coder()) {
            inflate();
        }
        str.getBytes(value, index, coder);
    }

    private final void appendChars(char[] s, int off, int end) {
        int count = this.count;
        if (isLatin1()) {
            byte[] val = this.value;
            for (int i = off, j = count; i < end; i++) {
                char c = s[i];
                if (StringLatin1.canEncode(c)) {
                    val[j++] = (byte)c;
                } else {
                    this.count = count = j;
                    inflate();
                    StringUTF16.putCharsSB(this.value, j, s, i, end);
                    this.count = count + end - i;
                    return;
                }
            }
        } else {
            StringUTF16.putCharsSB(this.value, count, s, off, end);
        }
        this.count = count + end - off;
    }

    private final void appendChars(CharSequence s, int off, int end) {
        if (isLatin1()) {
            byte[] val = this.value;
            for (int i = off, j = count; i < end; i++) {
                char c = s.charAt(i);
                if (StringLatin1.canEncode(c)) {
                    val[j++] = (byte)c;
                } else {
                    count = j;
                    inflate();
                    StringUTF16.putCharsSB(this.value, j, s, i, end);
                    count += end - i;
                    return;
                }
            }
        } else {
            StringUTF16.putCharsSB(this.value, count, s, off, end);
        }
        count += end - off;
    }

    /* IndexOutOfBoundsException, if out of bounds */
    private static void checkRange(int start, int end, int len) {
        if (start < 0 || start > end || end > len) {
            throw new IndexOutOfBoundsException(
                "start " + start + ", end " + end + ", length " + len);
        }
    }

    /* StringIndexOutOfBoundsException, if out of bounds */
    private static void checkRangeSIOOBE(int start, int end, int len) {
        if (start < 0 || start > end || end > len) {
            throw new StringIndexOutOfBoundsException(
                "start " + start + ", end " + end + ", length " + len);
        }
    }
}
