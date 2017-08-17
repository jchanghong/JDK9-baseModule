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

// -- This file was mechanically generated: Do not edit! -- //

package java.nio;












public abstract class ByteBuffer
    extends Buffer
    implements Comparable<ByteBuffer>
{

    // These fields are declared here rather than in Heap-X-Buffer in order to
    // reduce the number of virtual method invocations needed to access these
    // values, which is especially costly when coding small buffers.
    //
    final byte[] hb;                  // Non-null only for heap buffers
    final int offset;
    boolean isReadOnly;

    // Creates a new buffer with the given mark, position, limit, capacity,
    // backing array, and array offset
    //
    ByteBuffer(int mark, int pos, int lim, int cap,   // package-private
                 byte[] hb, int offset)
    {
        super(mark, pos, lim, cap);
        this.hb = hb;
        this.offset = offset;
    }

    // Creates a new buffer with the given mark, position, limit, and capacity
    //
    ByteBuffer(int mark, int pos, int lim, int cap) { // package-private
        this(mark, pos, lim, cap, null, 0);
    }




    public static ByteBuffer allocateDirect(int capacity) {
        return new DirectByteBuffer(capacity);
    }




    public static ByteBuffer allocate(int capacity) {
        if (capacity < 0)
            throw createCapacityException(capacity);
        return new HeapByteBuffer(capacity, capacity);
    }


    public static ByteBuffer wrap(byte[] array,
                                    int offset, int length)
    {
        try {
            return new HeapByteBuffer(array, offset, length);
        } catch (IllegalArgumentException x) {
            throw new IndexOutOfBoundsException();
        }
    }


    public static ByteBuffer wrap(byte[] array) {
        return wrap(array, 0, array.length);
    }































































































    @Override
    public abstract ByteBuffer slice();


    @Override
    public abstract ByteBuffer duplicate();


    public abstract ByteBuffer asReadOnlyBuffer();


    // -- Singleton get/put methods --


    public abstract byte get();


    public abstract ByteBuffer put(byte b);


    public abstract byte get(int index);















    public abstract ByteBuffer put(int index, byte b);


    // -- Bulk get operations --


    public ByteBuffer get(byte[] dst, int offset, int length) {
        checkBounds(offset, length, dst.length);
        if (length > remaining())
            throw new BufferUnderflowException();
        int end = offset + length;
        for (int i = offset; i < end; i++)
            dst[i] = get();
        return this;
    }


    public ByteBuffer get(byte[] dst) {
        return get(dst, 0, dst.length);
    }


    // -- Bulk put operations --


    public ByteBuffer put(ByteBuffer src) {
        if (src == this)
            throw createSameBufferException();
        if (isReadOnly())
            throw new ReadOnlyBufferException();
        int n = src.remaining();
        if (n > remaining())
            throw new BufferOverflowException();
        for (int i = 0; i < n; i++)
            put(src.get());
        return this;
    }


    public ByteBuffer put(byte[] src, int offset, int length) {
        checkBounds(offset, length, src.length);
        if (length > remaining())
            throw new BufferOverflowException();
        int end = offset + length;
        for (int i = offset; i < end; i++)
            this.put(src[i]);
        return this;
    }


    public final ByteBuffer put(byte[] src) {
        return put(src, 0, src.length);
    }































































































    // -- Other stuff --


    public final boolean hasArray() {
        return (hb != null) && !isReadOnly;
    }


    public final byte[] array() {
        if (hb == null)
            throw new UnsupportedOperationException();
        if (isReadOnly)
            throw new ReadOnlyBufferException();
        return hb;
    }


    public final int arrayOffset() {
        if (hb == null)
            throw new UnsupportedOperationException();
        if (isReadOnly)
            throw new ReadOnlyBufferException();
        return offset;
    }

    // -- Covariant return type overrides


    @Override
    public



    ByteBuffer position(int newPosition) {
        super.position(newPosition);
        return this;
    }
    

    @Override
    public



    ByteBuffer limit(int newLimit) {
        super.limit(newLimit);
        return this;
    }
    

    @Override
    public 



    ByteBuffer mark() {
        super.mark();
        return this;
    }


    @Override
    public 



    ByteBuffer reset() {
        super.reset();
        return this;
    }


    @Override
    public 



    ByteBuffer clear() {
        super.clear();
        return this;
    }


    @Override
    public 



    ByteBuffer flip() {
        super.flip();
        return this;
    }


    @Override
    public 



    ByteBuffer rewind() {
        super.rewind();
        return this;
    }


    public abstract ByteBuffer compact();


    public abstract boolean isDirect();




    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getClass().getName());
        sb.append("[pos=");
        sb.append(position());
        sb.append(" lim=");
        sb.append(limit());
        sb.append(" cap=");
        sb.append(capacity());
        sb.append("]");
        return sb.toString();
    }







    public int hashCode() {
        int h = 1;
        int p = position();
        for (int i = limit() - 1; i >= p; i--)



            h = 31 * h + (int)get(i);

        return h;
    }


    public boolean equals(Object ob) {
        if (this == ob)
            return true;
        if (!(ob instanceof ByteBuffer))
            return false;
        ByteBuffer that = (ByteBuffer)ob;
        if (this.remaining() != that.remaining())
            return false;
        int p = this.position();
        for (int i = this.limit() - 1, j = that.limit() - 1; i >= p; i--, j--)
            if (!equals(this.get(i), that.get(j)))
                return false;
        return true;
    }

    private static boolean equals(byte x, byte y) {



        return x == y;

    }


    public int compareTo(ByteBuffer that) {
        int n = this.position() + Math.min(this.remaining(), that.remaining());
        for (int i = this.position(), j = that.position(); i < n; i++, j++) {
            int cmp = compare(this.get(i), that.get(j));
            if (cmp != 0)
                return cmp;
        }
        return this.remaining() - that.remaining();
    }

    private static int compare(byte x, byte y) {






        return Byte.compare(x, y);

    }

    // -- Other char stuff --


































































































































































































    // -- Other byte stuff: Access to binary data --





















    boolean bigEndian                                   // package-private
        = true;
    boolean nativeByteOrder                             // package-private
        = (Bits.byteOrder() == ByteOrder.BIG_ENDIAN);


    public final ByteOrder order() {
        return bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
    }


    public final ByteBuffer order(ByteOrder bo) {
        bigEndian = (bo == ByteOrder.BIG_ENDIAN);
        nativeByteOrder =
            (bigEndian == (Bits.byteOrder() == ByteOrder.BIG_ENDIAN));
        return this;
    }


    public final int alignmentOffset(int index, int unitSize) {
        if (index < 0)
            throw new IllegalArgumentException("Index less than zero: " + index);
        if (unitSize < 1 || (unitSize & (unitSize - 1)) != 0)
            throw new IllegalArgumentException("Unit size not a power of two: " + unitSize);
        if (unitSize > 8 && !isDirect())
            throw new UnsupportedOperationException("Unit size unsupported for non-direct buffers: " + unitSize);

        return (int) ((address + index) % unitSize);
    }


    public final ByteBuffer alignedSlice(int unitSize) {
        int pos = position();
        int lim = limit();

        int pos_mod = alignmentOffset(pos, unitSize);
        int lim_mod = alignmentOffset(lim, unitSize);

        // Round up the position to align with unit size
        int aligned_pos = (pos_mod > 0)
            ? pos + (unitSize - pos_mod)
            : pos;

        // Round down the limit to align with unit size
        int aligned_lim = lim - lim_mod;

        if (aligned_pos > lim || aligned_lim < pos) {
            aligned_pos = aligned_lim = pos;
        }

        return slice(aligned_pos, aligned_lim);
    }

    abstract ByteBuffer slice(int pos, int lim);

    // Unchecked accessors, for use by ByteBufferAs-X-Buffer classes
    //
    abstract byte _get(int i);                          // package-private
    abstract void _put(int i, byte b);                  // package-private



    public abstract char getChar();


    public abstract ByteBuffer putChar(char value);


    public abstract char getChar(int index);


    public abstract ByteBuffer putChar(int index, char value);


    public abstract CharBuffer asCharBuffer();



    public abstract short getShort();


    public abstract ByteBuffer putShort(short value);


    public abstract short getShort(int index);


    public abstract ByteBuffer putShort(int index, short value);


    public abstract ShortBuffer asShortBuffer();



    public abstract int getInt();


    public abstract ByteBuffer putInt(int value);


    public abstract int getInt(int index);


    public abstract ByteBuffer putInt(int index, int value);


    public abstract IntBuffer asIntBuffer();



    public abstract long getLong();


    public abstract ByteBuffer putLong(long value);


    public abstract long getLong(int index);


    public abstract ByteBuffer putLong(int index, long value);


    public abstract LongBuffer asLongBuffer();



    public abstract float getFloat();


    public abstract ByteBuffer putFloat(float value);


    public abstract float getFloat(int index);


    public abstract ByteBuffer putFloat(int index, float value);


    public abstract FloatBuffer asFloatBuffer();



    public abstract double getDouble();


    public abstract ByteBuffer putDouble(double value);


    public abstract double getDouble(int index);


    public abstract ByteBuffer putDouble(int index, double value);


    public abstract DoubleBuffer asDoubleBuffer();

}
