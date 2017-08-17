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

package java.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.PrivilegedAction;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.StringTokenizer;
import java.util.Collections;
import java.util.Map;
import java.util.List;
import java.security.Permission;
import java.security.AccessController;
import sun.security.util.SecurityConstants;
import sun.net.www.MessageHeader;
import sun.security.action.GetPropertyAction;


public abstract class URLConnection {


    protected URL url;


    protected boolean doInput = true;


    protected boolean doOutput = false;

    private static boolean defaultAllowUserInteraction = false;


    protected boolean allowUserInteraction = defaultAllowUserInteraction;

    private static volatile boolean defaultUseCaches = true;


    protected boolean useCaches;

    private static final ConcurrentHashMap<String,Boolean> defaultCaching =
        new ConcurrentHashMap<>();


    protected long ifModifiedSince = 0;


    protected boolean connected = false;


    private int connectTimeout;
    private int readTimeout;


    private MessageHeader requests;


    private static volatile FileNameMap fileNameMap;


    public static FileNameMap getFileNameMap() {
        FileNameMap map = fileNameMap;

        if (map == null) {
            fileNameMap = map = new FileNameMap() {
                private FileNameMap internalMap =
                    sun.net.www.MimeTable.loadTable();

                public String getContentTypeFor(String fileName) {
                    return internalMap.getContentTypeFor(fileName);
                }
            };
        }

        return map;
    }


    public static void setFileNameMap(FileNameMap map) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) sm.checkSetFactory();
        fileNameMap = map;
    }


    public abstract void connect() throws IOException;


    public void setConnectTimeout(int timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout can not be negative");
        }
        connectTimeout = timeout;
    }


    public int getConnectTimeout() {
        return connectTimeout;
    }


    public void setReadTimeout(int timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout can not be negative");
        }
        readTimeout = timeout;
    }


    public int getReadTimeout() {
        return readTimeout;
    }


    protected URLConnection(URL url) {
        this.url = url;
        if (url == null) {
            this.useCaches = defaultUseCaches;
        } else {
            this.useCaches = getDefaultUseCaches(url.getProtocol());
        }
    }


    public URL getURL() {
        return url;
    }


    public int getContentLength() {
        long l = getContentLengthLong();
        if (l > Integer.MAX_VALUE)
            return -1;
        return (int) l;
    }


    public long getContentLengthLong() {
        return getHeaderFieldLong("content-length", -1);
    }


    public String getContentType() {
        return getHeaderField("content-type");
    }


    public String getContentEncoding() {
        return getHeaderField("content-encoding");
    }


    public long getExpiration() {
        return getHeaderFieldDate("expires", 0);
    }


    public long getDate() {
        return getHeaderFieldDate("date", 0);
    }


    public long getLastModified() {
        return getHeaderFieldDate("last-modified", 0);
    }


    public String getHeaderField(String name) {
        return null;
    }


    public Map<String,List<String>> getHeaderFields() {
        return Collections.emptyMap();
    }


    public int getHeaderFieldInt(String name, int Default) {
        String value = getHeaderField(name);
        try {
            return Integer.parseInt(value);
        } catch (Exception e) { }
        return Default;
    }


    public long getHeaderFieldLong(String name, long Default) {
        String value = getHeaderField(name);
        try {
            return Long.parseLong(value);
        } catch (Exception e) { }
        return Default;
    }


    @SuppressWarnings("deprecation")
    public long getHeaderFieldDate(String name, long Default) {
        String value = getHeaderField(name);
        try {
            return Date.parse(value);
        } catch (Exception e) { }
        return Default;
    }


    public String getHeaderFieldKey(int n) {
        return null;
    }


    public String getHeaderField(int n) {
        return null;
    }


    public Object getContent() throws IOException {
        // Must call getInputStream before GetHeaderField gets called
        // so that FileNotFoundException has a chance to be thrown up
        // from here without being caught.
        getInputStream();
        return getContentHandler().getContent(this);
    }


    public Object getContent(Class<?>[] classes) throws IOException {
        // Must call getInputStream before GetHeaderField gets called
        // so that FileNotFoundException has a chance to be thrown up
        // from here without being caught.
        getInputStream();
        return getContentHandler().getContent(this, classes);
    }


    public Permission getPermission() throws IOException {
        return SecurityConstants.ALL_PERMISSION;
    }


    public InputStream getInputStream() throws IOException {
        throw new UnknownServiceException("protocol doesn't support input");
    }


    public OutputStream getOutputStream() throws IOException {
        throw new UnknownServiceException("protocol doesn't support output");
    }


    public String toString() {
        return this.getClass().getName() + ":" + url;
    }


    public void setDoInput(boolean doinput) {
        checkConnected();
        doInput = doinput;
    }


    public boolean getDoInput() {
        return doInput;
    }


    public void setDoOutput(boolean dooutput) {
        checkConnected();
        doOutput = dooutput;
    }


    public boolean getDoOutput() {
        return doOutput;
    }


    public void setAllowUserInteraction(boolean allowuserinteraction) {
        checkConnected();
        allowUserInteraction = allowuserinteraction;
    }


    public boolean getAllowUserInteraction() {
        return allowUserInteraction;
    }


    public static void setDefaultAllowUserInteraction(boolean defaultallowuserinteraction) {
        defaultAllowUserInteraction = defaultallowuserinteraction;
    }


    public static boolean getDefaultAllowUserInteraction() {
        return defaultAllowUserInteraction;
    }


    public void setUseCaches(boolean usecaches) {
        checkConnected();
        useCaches = usecaches;
    }


    public boolean getUseCaches() {
        return useCaches;
    }


    public void setIfModifiedSince(long ifmodifiedsince) {
        checkConnected();
        ifModifiedSince = ifmodifiedsince;
    }


    public long getIfModifiedSince() {
        return ifModifiedSince;
    }


    public boolean getDefaultUseCaches() {
        return defaultUseCaches;
    }


    public void setDefaultUseCaches(boolean defaultusecaches) {
        defaultUseCaches = defaultusecaches;
    }


    public static void setDefaultUseCaches(String protocol, boolean defaultVal) {
        protocol = protocol.toLowerCase(Locale.US);
        defaultCaching.put(protocol, defaultVal);
    }


    public static boolean getDefaultUseCaches(String protocol) {
        Boolean protoDefault = defaultCaching.get(protocol.toLowerCase(Locale.US));
        if (protoDefault != null) {
            return protoDefault.booleanValue();
        } else {
            return defaultUseCaches;
        }
    }


    public void setRequestProperty(String key, String value) {
        checkConnected();
        if (key == null)
            throw new NullPointerException ("key is null");

        if (requests == null)
            requests = new MessageHeader();

        requests.set(key, value);
    }


    public void addRequestProperty(String key, String value) {
        checkConnected();
        if (key == null)
            throw new NullPointerException ("key is null");

        if (requests == null)
            requests = new MessageHeader();

        requests.add(key, value);
    }



    public String getRequestProperty(String key) {
        checkConnected();

        if (requests == null)
            return null;

        return requests.findValue(key);
    }


    public Map<String,List<String>> getRequestProperties() {
        checkConnected();

        if (requests == null)
            return Collections.emptyMap();

        return requests.getHeaders(null);
    }


    @Deprecated
    public static void setDefaultRequestProperty(String key, String value) {
    }


    @Deprecated
    public static String getDefaultRequestProperty(String key) {
        return null;
    }


    private static volatile ContentHandlerFactory factory;


    public static synchronized void setContentHandlerFactory(ContentHandlerFactory fac) {
        if (factory != null) {
            throw new Error("factory already defined");
        }
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkSetFactory();
        }
        factory = fac;
    }

    private static final Hashtable<String, ContentHandler> handlers = new Hashtable<>();


    private ContentHandler getContentHandler() throws UnknownServiceException {
        String contentType = stripOffParameters(getContentType());
        if (contentType == null) {
            throw new UnknownServiceException("no content-type");
        }

        ContentHandler handler = handlers.get(contentType);
        if (handler != null)
            return handler;

        if (factory != null) {
            handler = factory.createContentHandler(contentType);
            if (handler != null)
                return handler;
        }

        handler = lookupContentHandlerViaProvider(contentType);

        if (handler != null) {
            ContentHandler h = handlers.putIfAbsent(contentType, handler);
            return Objects.requireNonNullElse(h, handler);
        }

        try {
            handler = lookupContentHandlerClassFor(contentType);
        } catch (Exception e) {
            e.printStackTrace();
            handler = UnknownContentHandler.INSTANCE;
        }

        assert handler != null;

        ContentHandler h = handlers.putIfAbsent(contentType, handler);
        return Objects.requireNonNullElse(h, handler);
    }

    /*
     * Media types are in the format: type/subtype*(; parameter).
     * For looking up the content handler, we should ignore those
     * parameters.
     */
    private String stripOffParameters(String contentType)
    {
        if (contentType == null)
            return null;
        int index = contentType.indexOf(';');

        if (index > 0)
            return contentType.substring(0, index);
        else
            return contentType;
    }

    private static final String contentClassPrefix = "sun.net.www.content";
    private static final String contentPathProp = "java.content.handler.pkgs";


    private ContentHandler lookupContentHandlerClassFor(String contentType) {
        String contentHandlerClassName = typeToPackageName(contentType);

        String contentHandlerPkgPrefixes = getContentHandlerPkgPrefixes();

        StringTokenizer packagePrefixIter =
            new StringTokenizer(contentHandlerPkgPrefixes, "|");

        while (packagePrefixIter.hasMoreTokens()) {
            String packagePrefix = packagePrefixIter.nextToken().trim();

            try {
                String clsName = packagePrefix + "." + contentHandlerClassName;
                Class<?> cls = null;
                try {
                    cls = Class.forName(clsName);
                } catch (ClassNotFoundException e) {
                    ClassLoader cl = ClassLoader.getSystemClassLoader();
                    if (cl != null) {
                        cls = cl.loadClass(clsName);
                    }
                }
                if (cls != null) {
                    @SuppressWarnings("deprecation")
                    Object tmp = cls.newInstance();
                    return (ContentHandler) tmp;
                }
            } catch(Exception ignored) { }
        }

        return UnknownContentHandler.INSTANCE;
    }

    private ContentHandler lookupContentHandlerViaProvider(String contentType) {
        return AccessController.doPrivileged(
                new PrivilegedAction<>() {
                    @Override
                    public ContentHandler run() {
                        ClassLoader cl = ClassLoader.getSystemClassLoader();
                        ServiceLoader<ContentHandlerFactory> sl =
                                ServiceLoader.load(ContentHandlerFactory.class, cl);

                        Iterator<ContentHandlerFactory> iterator = sl.iterator();

                        ContentHandler handler = null;
                        while (iterator.hasNext()) {
                            ContentHandlerFactory f;
                            try {
                                f = iterator.next();
                            } catch (ServiceConfigurationError e) {
                                if (e.getCause() instanceof SecurityException) {
                                    continue;
                                }
                                throw e;
                            }
                            handler = f.createContentHandler(contentType);
                            if (handler != null) {
                                break;
                            }
                        }
                        return handler;
                    }
                });
    }


    private String typeToPackageName(String contentType) {
        // make sure we canonicalize the class name: all lower case
        contentType = contentType.toLowerCase();
        int len = contentType.length();
        char nm[] = new char[len];
        contentType.getChars(0, len, nm, 0);
        for (int i = 0; i < len; i++) {
            char c = nm[i];
            if (c == '/') {
                nm[i] = '.';
            } else if (!('A' <= c && c <= 'Z' ||
                       'a' <= c && c <= 'z' ||
                       '0' <= c && c <= '9')) {
                nm[i] = '_';
            }
        }
        return new String(nm);
    }



    private String getContentHandlerPkgPrefixes() {
        String packagePrefixList =
                GetPropertyAction.privilegedGetProperty(contentPathProp, "");

        if (packagePrefixList != "") {
            packagePrefixList += "|";
        }

        return packagePrefixList + contentClassPrefix;
    }


    public static String guessContentTypeFromName(String fname) {
        return getFileNameMap().getContentTypeFor(fname);
    }


    public static String guessContentTypeFromStream(InputStream is)
                        throws IOException {
        // If we can't read ahead safely, just give up on guessing
        if (!is.markSupported())
            return null;

        is.mark(16);
        int c1 = is.read();
        int c2 = is.read();
        int c3 = is.read();
        int c4 = is.read();
        int c5 = is.read();
        int c6 = is.read();
        int c7 = is.read();
        int c8 = is.read();
        int c9 = is.read();
        int c10 = is.read();
        int c11 = is.read();
        int c12 = is.read();
        int c13 = is.read();
        int c14 = is.read();
        int c15 = is.read();
        int c16 = is.read();
        is.reset();

        if (c1 == 0xCA && c2 == 0xFE && c3 == 0xBA && c4 == 0xBE) {
            return "application/java-vm";
        }

        if (c1 == 0xAC && c2 == 0xED) {
            // next two bytes are version number, currently 0x00 0x05
            return "application/x-java-serialized-object";
        }

        if (c1 == '<') {
            if (c2 == '!'
                || ((c2 == 'h' && (c3 == 't' && c4 == 'm' && c5 == 'l' ||
                                   c3 == 'e' && c4 == 'a' && c5 == 'd') ||
                (c2 == 'b' && c3 == 'o' && c4 == 'd' && c5 == 'y'))) ||
                ((c2 == 'H' && (c3 == 'T' && c4 == 'M' && c5 == 'L' ||
                                c3 == 'E' && c4 == 'A' && c5 == 'D') ||
                (c2 == 'B' && c3 == 'O' && c4 == 'D' && c5 == 'Y')))) {
                return "text/html";
            }

            if (c2 == '?' && c3 == 'x' && c4 == 'm' && c5 == 'l' && c6 == ' ') {
                return "application/xml";
            }
        }

        // big and little (identical) endian UTF-8 encodings, with BOM
        if (c1 == 0xef &&  c2 == 0xbb &&  c3 == 0xbf) {
            if (c4 == '<' &&  c5 == '?' &&  c6 == 'x') {
                return "application/xml";
            }
        }

        // big and little endian UTF-16 encodings, with byte order mark
        if (c1 == 0xfe && c2 == 0xff) {
            if (c3 == 0 && c4 == '<' && c5 == 0 && c6 == '?' &&
                c7 == 0 && c8 == 'x') {
                return "application/xml";
            }
        }

        if (c1 == 0xff && c2 == 0xfe) {
            if (c3 == '<' && c4 == 0 && c5 == '?' && c6 == 0 &&
                c7 == 'x' && c8 == 0) {
                return "application/xml";
            }
        }

        // big and little endian UTF-32 encodings, with BOM
        if (c1 == 0x00 &&  c2 == 0x00 &&  c3 == 0xfe &&  c4 == 0xff) {
            if (c5  == 0 && c6  == 0 && c7  == 0 && c8  == '<' &&
                c9  == 0 && c10 == 0 && c11 == 0 && c12 == '?' &&
                c13 == 0 && c14 == 0 && c15 == 0 && c16 == 'x') {
                return "application/xml";
            }
        }

        if (c1 == 0xff &&  c2 == 0xfe &&  c3 == 0x00 &&  c4 == 0x00) {
            if (c5  == '<' && c6  == 0 && c7  == 0 && c8  == 0 &&
                c9  == '?' && c10 == 0 && c11 == 0 && c12 == 0 &&
                c13 == 'x' && c14 == 0 && c15 == 0 && c16 == 0) {
                return "application/xml";
            }
        }

        if (c1 == 'G' && c2 == 'I' && c3 == 'F' && c4 == '8') {
            return "image/gif";
        }

        if (c1 == '#' && c2 == 'd' && c3 == 'e' && c4 == 'f') {
            return "image/x-bitmap";
        }

        if (c1 == '!' && c2 == ' ' && c3 == 'X' && c4 == 'P' &&
                        c5 == 'M' && c6 == '2') {
            return "image/x-pixmap";
        }

        if (c1 == 137 && c2 == 80 && c3 == 78 &&
                c4 == 71 && c5 == 13 && c6 == 10 &&
                c7 == 26 && c8 == 10) {
            return "image/png";
        }

        if (c1 == 0xFF && c2 == 0xD8 && c3 == 0xFF) {
            if (c4 == 0xE0 || c4 == 0xEE) {
                return "image/jpeg";
            }


            if ((c4 == 0xE1) &&
                (c7 == 'E' && c8 == 'x' && c9 == 'i' && c10 =='f' &&
                 c11 == 0)) {
                return "image/jpeg";
            }
        }

        if ((c1 == 0x49 && c2 == 0x49 && c3 == 0x2a && c4 == 0x00)
            || (c1 == 0x4d && c2 == 0x4d && c3 == 0x00 && c4 == 0x2a)) {
            return "image/tiff";
        }

        if (c1 == 0xD0 && c2 == 0xCF && c3 == 0x11 && c4 == 0xE0 &&
            c5 == 0xA1 && c6 == 0xB1 && c7 == 0x1A && c8 == 0xE1) {

            /* Above is signature of Microsoft Structured Storage.
             * Below this, could have tests for various SS entities.
             * For now, just test for FlashPix.
             */
            if (checkfpx(is)) {
                return "image/vnd.fpx";
            }
        }

        if (c1 == 0x2E && c2 == 0x73 && c3 == 0x6E && c4 == 0x64) {
            return "audio/basic";  // .au format, big endian
        }

        if (c1 == 0x64 && c2 == 0x6E && c3 == 0x73 && c4 == 0x2E) {
            return "audio/basic";  // .au format, little endian
        }

        if (c1 == 'R' && c2 == 'I' && c3 == 'F' && c4 == 'F') {
            /* I don't know if this is official but evidence
             * suggests that .wav files start with "RIFF" - brown
             */
            return "audio/x-wav";
        }
        return null;
    }


    private static boolean checkfpx(InputStream is) throws IOException {

        /* Test for FlashPix image data in Microsoft Structured Storage format.
         * In general, should do this with calls to an SS implementation.
         * Lacking that, need to dig via offsets to get to the FlashPix
         * ClassID.  Details:
         *
         * Offset to Fpx ClsID from beginning of stream should be:
         *
         * FpxClsidOffset = rootEntryOffset + clsidOffset
         *
         * where: clsidOffset = 0x50.
         *        rootEntryOffset = headerSize + sectorSize*sectDirStart
         *                          + 128*rootEntryDirectory
         *
         *        where:  headerSize = 0x200 (always)
         *                sectorSize = 2 raised to power of uSectorShift,
         *                             which is found in the header at
         *                             offset 0x1E.
         *                sectDirStart = found in the header at offset 0x30.
         *                rootEntryDirectory = in general, should search for
         *                                     directory labelled as root.
         *                                     We will assume value of 0 (i.e.,
         *                                     rootEntry is in first directory)
         */

        // Mark the stream so we can reset it. 0x100 is enough for the first
        // few reads, but the mark will have to be reset and set again once
        // the offset to the root directory entry is computed. That offset
        // can be very large and isn't know until the stream has been read from
        is.mark(0x100);

        // Get the byte ordering located at 0x1E. 0xFE is Intel,
        // 0xFF is other
        long toSkip = (long)0x1C;
        long posn;

        if ((posn = skipForward(is, toSkip)) < toSkip) {
          is.reset();
          return false;
        }

        int c[] = new int[16];
        if (readBytes(c, 2, is) < 0) {
            is.reset();
            return false;
        }

        int byteOrder = c[0];

        posn+=2;
        int uSectorShift;
        if (readBytes(c, 2, is) < 0) {
            is.reset();
            return false;
        }

        if(byteOrder == 0xFE) {
            uSectorShift = c[0];
            uSectorShift += c[1] << 8;
        }
        else {
            uSectorShift = c[0] << 8;
            uSectorShift += c[1];
        }

        posn += 2;
        toSkip = (long)0x30 - posn;
        long skipped = 0;
        if ((skipped = skipForward(is, toSkip)) < toSkip) {
          is.reset();
          return false;
        }
        posn += skipped;

        if (readBytes(c, 4, is) < 0) {
            is.reset();
            return false;
        }

        int sectDirStart;
        if(byteOrder == 0xFE) {
            sectDirStart = c[0];
            sectDirStart += c[1] << 8;
            sectDirStart += c[2] << 16;
            sectDirStart += c[3] << 24;
        } else {
            sectDirStart =  c[0] << 24;
            sectDirStart += c[1] << 16;
            sectDirStart += c[2] << 8;
            sectDirStart += c[3];
        }
        posn += 4;
        is.reset(); // Reset back to the beginning

        toSkip = 0x200L + (long)(1<<uSectorShift)*sectDirStart + 0x50L;

        // Sanity check!
        if (toSkip < 0) {
            return false;
        }

        /*
         * How far can we skip? Is there any performance problem here?
         * This skip can be fairly long, at least 0x4c650 in at least
         * one case. Have to assume that the skip will fit in an int.
         * Leave room to read whole root dir
         */
        is.mark((int)toSkip+0x30);

        if ((skipForward(is, toSkip)) < toSkip) {
            is.reset();
            return false;
        }

        /* should be at beginning of ClassID, which is as follows
         * (in Intel byte order):
         *    00 67 61 56 54 C1 CE 11 85 53 00 AA 00 A1 F9 5B
         *
         * This is stored from Windows as long,short,short,char[8]
         * so for byte order changes, the order only changes for
         * the first 8 bytes in the ClassID.
         *
         * Test against this, ignoring second byte (Intel) since
         * this could change depending on part of Fpx file we have.
         */

        if (readBytes(c, 16, is) < 0) {
            is.reset();
            return false;
        }

        // intel byte order
        if (byteOrder == 0xFE &&
            c[0] == 0x00 && c[2] == 0x61 && c[3] == 0x56 &&
            c[4] == 0x54 && c[5] == 0xC1 && c[6] == 0xCE &&
            c[7] == 0x11 && c[8] == 0x85 && c[9] == 0x53 &&
            c[10]== 0x00 && c[11]== 0xAA && c[12]== 0x00 &&
            c[13]== 0xA1 && c[14]== 0xF9 && c[15]== 0x5B) {
            is.reset();
            return true;
        }

        // non-intel byte order
        else if (c[3] == 0x00 && c[1] == 0x61 && c[0] == 0x56 &&
            c[5] == 0x54 && c[4] == 0xC1 && c[7] == 0xCE &&
            c[6] == 0x11 && c[8] == 0x85 && c[9] == 0x53 &&
            c[10]== 0x00 && c[11]== 0xAA && c[12]== 0x00 &&
            c[13]== 0xA1 && c[14]== 0xF9 && c[15]== 0x5B) {
            is.reset();
            return true;
        }
        is.reset();
        return false;
    }


    private static int readBytes(int c[], int len, InputStream is)
                throws IOException {

        byte buf[] = new byte[len];
        if (is.read(buf, 0, len) < len) {
            return -1;
        }

        // fill the passed in int array
        for (int i = 0; i < len; i++) {
             c[i] = buf[i] & 0xff;
        }
        return 0;
    }



    private static long skipForward(InputStream is, long toSkip)
                throws IOException {

        long eachSkip = 0;
        long skipped = 0;

        while (skipped != toSkip) {
            eachSkip = is.skip(toSkip - skipped);

            // check if EOF is reached
            if (eachSkip <= 0) {
                if (is.read() == -1) {
                    return skipped ;
                } else {
                    skipped++;
                }
            }
            skipped += eachSkip;
        }
        return skipped;
    }

    private void checkConnected() {
        if (connected)
            throw new IllegalStateException("Already connected");
    }
}

class UnknownContentHandler extends ContentHandler {
    static final ContentHandler INSTANCE = new UnknownContentHandler();

    public Object getContent(URLConnection uc) throws IOException {
        return uc.getInputStream();
    }
}
