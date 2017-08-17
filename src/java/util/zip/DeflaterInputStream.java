/*
 * Copyright (c) 2006, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.IOException;



public class DeflaterInputStream extends FilterInputStream {

    protected final Deflater def;


    protected final byte[] buf;


    private byte[] rbuf = new byte[1];


    private boolean usesDefaultDeflater = false;


    private boolean reachEOF = false;


    private void ensureOpen() throws IOException {
        if (in == null) {
            throw new IOException("Stream closed");
        }
    }


    public DeflaterInputStream(InputStream in) {
        this(in, new Deflater());
        usesDefaultDeflater = true;
    }


    public DeflaterInputStream(InputStream in, Deflater defl) {
        this(in, defl, 512);
    }


    public DeflaterInputStream(InputStream in, Deflater defl, int bufLen) {
        super(in);

        // Sanity checks
        if (in == null)
            throw new NullPointerException("Null input");
        if (defl == null)
            throw new NullPointerException("Null deflater");
        if (bufLen < 1)
            throw new IllegalArgumentException("Buffer size < 1");

        // Initialize
        def = defl;
        buf = new byte[bufLen];
    }


    public void close() throws IOException {
        if (in != null) {
            try {
                // Clean up
                if (usesDefaultDeflater) {
                    def.end();
                }

                in.close();
            } finally {
                in = null;
            }
        }
    }


    public int read() throws IOException {
        // Read a single byte of compressed data
        int len = read(rbuf, 0, 1);
        if (len <= 0)
            return -1;
        return (rbuf[0] & 0xFF);
    }


    public int read(byte[] b, int off, int len) throws IOException {
        // Sanity checks
        ensureOpen();
        if (b == null) {
            throw new NullPointerException("Null buffer for read");
        } else if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        // Read and compress (deflate) input data bytes
        int cnt = 0;
        while (len > 0 && !def.finished()) {
            int n;

            // Read data from the input stream
            if (def.needsInput()) {
                n = in.read(buf, 0, buf.length);
                if (n < 0) {
                    // End of the input stream reached
                    def.finish();
                } else if (n > 0) {
                    def.setInput(buf, 0, n);
                }
            }

            // Compress the input data, filling the read buffer
            n = def.deflate(b, off, len);
            cnt += n;
            off += n;
            len -= n;
        }
        if (cnt == 0 && def.finished()) {
            reachEOF = true;
            cnt = -1;
        }

        return cnt;
    }


    public long skip(long n) throws IOException {
        if (n < 0) {
            throw new IllegalArgumentException("negative skip length");
        }
        ensureOpen();

        // Skip bytes by repeatedly decompressing small blocks
        if (rbuf.length < 512)
            rbuf = new byte[512];

        int total = (int)Math.min(n, Integer.MAX_VALUE);
        long cnt = 0;
        while (total > 0) {
            // Read a small block of uncompressed bytes
            int len = read(rbuf, 0, (total <= rbuf.length ? total : rbuf.length));

            if (len < 0) {
                break;
            }
            cnt += len;
            total -= len;
        }
        return cnt;
    }


    public int available() throws IOException {
        ensureOpen();
        if (reachEOF) {
            return 0;
        }
        return 1;
    }


    public boolean markSupported() {
        return false;
    }


    public void mark(int limit) {
        // Operation not supported
    }


    public void reset() throws IOException {
        throw new IOException("mark/reset not supported");
    }
}
