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












public abstract class ShortBuffer
    extends Buffer
    implements Comparable<ShortBuffer>
{

    // These fields are declared here rather than in Heap-X-Buffer in order to
    // reduce the number of virtual method invocations needed to access these
    // values, which is especially costly when coding small buffers.
    //
    final short[] hb;                  // Non-null only for heap buffers
    final int offset;
    boolean isReadOnly;

    // Creates a new buffer with the given mark, position, limit, capacity,
    // backing array, and array offset
    //
    ShortBuffer(int mark, int pos, int lim, int cap,   // package-private
                 short[] hb, int offset)
    {
        super(mark, pos, lim, cap);
        this.hb = hb;
        this.offset = offset;
    }

    // Creates a new buffer with the given mark, position, limit, and capacity
    //
    ShortBuffer(int mark, int pos, int lim, int cap) { // package-private
        this(mark, pos, lim, cap, null, 0);
    }



























    public static ShortBuffer allocate(int capacity) {
        if (capacity < 0)
            throw createCapacityException(capacity);
        return new HeapShortBuffer(capacity, capacity);
    }


    public static ShortBuffer wrap(short[] array,
                                    int offset, int length)
    {
        try {
            return new HeapShortBuffer(array, offset, length);
        } catch (IllegalArgumentException x) {
            throw new IndexOutOfBoundsException();
        }
    }


    public static ShortBuffer wrap(short[] array) {
        return wrap(array, 0, array.length);
    }































































































    @Override
    public abstract ShortBuffer slice();


    @Override
    public abstract ShortBuffer duplicate();


    public abstract ShortBuffer asReadOnlyBuffer();


    // -- Singleton get/put methods --


    public abstract short get();


    public abstract ShortBuffer put(short s);


    public abstract short get(int index);















    public abstract ShortBuffer put(int index, short s);


    // -- Bulk get operations --


    public ShortBuffer get(short[] dst, int offset, int length) {
        checkBounds(offset, length, dst.length);
        if (length > remaining())
            throw new BufferUnderflowException();
        int end = offset + length;
        for (int i = offset; i < end; i++)
            dst[i] = get();
        return this;
    }


    public ShortBuffer get(short[] dst) {
        return get(dst, 0, dst.length);
    }


    // -- Bulk put operations --


    public ShortBuffer put(ShortBuffer src) {
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


    public ShortBuffer put(short[] src, int offset, int length) {
        checkBounds(offset, length, src.length);
        if (length > remaining())
            throw new BufferOverflowException();
        int end = offset + length;
        for (int i = offset; i < end; i++)
            this.put(src[i]);
        return this;
    }


    public final ShortBuffer put(short[] src) {
        return put(src, 0, src.length);
    }































































































    // -- Other stuff --


    public final boolean hasArray() {
        return (hb != null) && !isReadOnly;
    }


    public final short[] array() {
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

    final

    ShortBuffer position(int newPosition) {
        super.position(newPosition);
        return this;
    }
    

    @Override
    public

    final

    ShortBuffer limit(int newLimit) {
        super.limit(newLimit);
        return this;
    }
    

    @Override
    public 

    final

    ShortBuffer mark() {
        super.mark();
        return this;
    }


    @Override
    public 

    final

    ShortBuffer reset() {
        super.reset();
        return this;
    }


    @Override
    public 

    final

    ShortBuffer clear() {
        super.clear();
        return this;
    }


    @Override
    public 

    final

    ShortBuffer flip() {
        super.flip();
        return this;
    }


    @Override
    public 

    final

    ShortBuffer rewind() {
        super.rewind();
        return this;
    }


    public abstract ShortBuffer compact();


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
        if (!(ob instanceof ShortBuffer))
            return false;
        ShortBuffer that = (ShortBuffer)ob;
        if (this.remaining() != that.remaining())
            return false;
        int p = this.position();
        for (int i = this.limit() - 1, j = that.limit() - 1; i >= p; i--, j--)
            if (!equals(this.get(i), that.get(j)))
                return false;
        return true;
    }

    private static boolean equals(short x, short y) {



        return x == y;

    }


    public int compareTo(ShortBuffer that) {
        int n = this.position() + Math.min(this.remaining(), that.remaining());
        for (int i = this.position(), j = that.position(); i < n; i++, j++) {
            int cmp = compare(this.get(i), that.get(j));
            if (cmp != 0)
                return cmp;
        }
        return this.remaining() - that.remaining();
    }

    private static int compare(short x, short y) {






        return Short.compare(x, y);

    }

    // -- Other char stuff --


































































































































































































    // -- Other byte stuff: Access to binary data --




    public abstract ByteOrder order();










































































































































































































}
