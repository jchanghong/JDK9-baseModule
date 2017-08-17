/*
 * Copyright (c) 1997, 2015, Oracle and/or its affiliates. All rights reserved.
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

package java.util.jar;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Locale;
import sun.util.logging.PlatformLogger;
import java.util.Comparator;


public class Attributes implements Map<Object,Object>, Cloneable {

    protected Map<Object,Object> map;


    public Attributes() {
        this(11);
    }


    public Attributes(int size) {
        map = new LinkedHashMap<>(size);
    }


    public Attributes(Attributes attr) {
        map = new LinkedHashMap<>(attr);
    }



    public Object get(Object name) {
        return map.get(name);
    }


    public String getValue(String name) {
        return (String)get(new Attributes.Name(name));
    }


    public String getValue(Name name) {
        return (String)get(name);
    }


    public Object put(Object name, Object value) {
        return map.put((Attributes.Name)name, (String)value);
    }


    public String putValue(String name, String value) {
        return (String)put(new Name(name), value);
    }


    public Object remove(Object name) {
        return map.remove(name);
    }


    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }


    public boolean containsKey(Object name) {
        return map.containsKey(name);
    }


    public void putAll(Map<?,?> attr) {
        // ## javac bug?
        if (!Attributes.class.isInstance(attr))
            throw new ClassCastException();
        for (Map.Entry<?,?> me : (attr).entrySet())
            put(me.getKey(), me.getValue());
    }


    public void clear() {
        map.clear();
    }


    public int size() {
        return map.size();
    }


    public boolean isEmpty() {
        return map.isEmpty();
    }


    public Set<Object> keySet() {
        return map.keySet();
    }


    public Collection<Object> values() {
        return map.values();
    }


    public Set<Map.Entry<Object,Object>> entrySet() {
        return map.entrySet();
    }


    public boolean equals(Object o) {
        return map.equals(o);
    }


    public int hashCode() {
        return map.hashCode();
    }


    public Object clone() {
        return new Attributes(this);
    }

    /*
     * Writes the current attributes to the specified data output stream.
     * XXX Need to handle UTF8 values and break up lines longer than 72 bytes
     */
     @SuppressWarnings("deprecation")
     void write(DataOutputStream os) throws IOException {
         for (Entry<Object, Object> e : entrySet()) {
             StringBuffer buffer = new StringBuffer(
                                         ((Name) e.getKey()).toString());
             buffer.append(": ");

             String value = (String) e.getValue();
             if (value != null) {
                 byte[] vb = value.getBytes("UTF8");
                 value = new String(vb, 0, 0, vb.length);
             }
             buffer.append(value);

             buffer.append("\r\n");
             Manifest.make72Safe(buffer);
             os.writeBytes(buffer.toString());
         }
        os.writeBytes("\r\n");
    }

    /*
     * Writes the current attributes to the specified data output stream,
     * make sure to write out the MANIFEST_VERSION or SIGNATURE_VERSION
     * attributes first.
     *
     * XXX Need to handle UTF8 values and break up lines longer than 72 bytes
     */
    @SuppressWarnings("deprecation")
    void writeMain(DataOutputStream out) throws IOException
    {
        // write out the *-Version header first, if it exists
        String vername = Name.MANIFEST_VERSION.toString();
        String version = getValue(vername);
        if (version == null) {
            vername = Name.SIGNATURE_VERSION.toString();
            version = getValue(vername);
        }

        if (version != null) {
            out.writeBytes(vername+": "+version+"\r\n");
        }

        // write out all attributes except for the version
        // we wrote out earlier
        for (Entry<Object, Object> e : entrySet()) {
            String name = ((Name) e.getKey()).toString();
            if ((version != null) && !(name.equalsIgnoreCase(vername))) {

                StringBuffer buffer = new StringBuffer(name);
                buffer.append(": ");

                String value = (String) e.getValue();
                if (value != null) {
                    byte[] vb = value.getBytes("UTF8");
                    value = new String(vb, 0, 0, vb.length);
                }
                buffer.append(value);

                buffer.append("\r\n");
                Manifest.make72Safe(buffer);
                out.writeBytes(buffer.toString());
            }
        }
        out.writeBytes("\r\n");
    }

    /*
     * Reads attributes from the specified input stream.
     * XXX Need to handle UTF8 values.
     */
    @SuppressWarnings("deprecation")
    void read(Manifest.FastInputStream is, byte[] lbuf) throws IOException {
        String name = null, value = null;
        byte[] lastline = null;

        int len;
        while ((len = is.readLine(lbuf)) != -1) {
            boolean lineContinued = false;
            if (lbuf[--len] != '\n') {
                throw new IOException("line too long");
            }
            if (len > 0 && lbuf[len-1] == '\r') {
                --len;
            }
            if (len == 0) {
                break;
            }
            int i = 0;
            if (lbuf[0] == ' ') {
                // continuation of previous line
                if (name == null) {
                    throw new IOException("misplaced continuation line");
                }
                lineContinued = true;
                byte[] buf = new byte[lastline.length + len - 1];
                System.arraycopy(lastline, 0, buf, 0, lastline.length);
                System.arraycopy(lbuf, 1, buf, lastline.length, len - 1);
                if (is.peek() == ' ') {
                    lastline = buf;
                    continue;
                }
                value = new String(buf, 0, buf.length, "UTF8");
                lastline = null;
            } else {
                while (lbuf[i++] != ':') {
                    if (i >= len) {
                        throw new IOException("invalid header field");
                    }
                }
                if (lbuf[i++] != ' ') {
                    throw new IOException("invalid header field");
                }
                name = new String(lbuf, 0, 0, i - 2);
                if (is.peek() == ' ') {
                    lastline = new byte[len - i];
                    System.arraycopy(lbuf, i, lastline, 0, len - i);
                    continue;
                }
                value = new String(lbuf, i, len - i, "UTF8");
            }
            try {
                if ((putValue(name, value) != null) && (!lineContinued)) {
                    PlatformLogger.getLogger("java.util.jar").warning(
                                     "Duplicate name in Manifest: " + name
                                     + ".\n"
                                     + "Ensure that the manifest does not "
                                     + "have duplicate entries, and\n"
                                     + "that blank lines separate "
                                     + "individual sections in both your\n"
                                     + "manifest and in the META-INF/MANIFEST.MF "
                                     + "entry in the jar file.");
                }
            } catch (IllegalArgumentException e) {
                throw new IOException("invalid header field name: " + name);
            }
        }
    }


    public static class Name {
        private String name;
        private int hashCode = -1;


        public Name(String name) {
            if (name == null) {
                throw new NullPointerException("name");
            }
            if (!isValid(name)) {
                throw new IllegalArgumentException(name);
            }
            this.name = name.intern();
        }

        private static boolean isValid(String name) {
            int len = name.length();
            if (len > 70 || len == 0) {
                return false;
            }
            for (int i = 0; i < len; i++) {
                if (!isValid(name.charAt(i))) {
                    return false;
                }
            }
            return true;
        }

        private static boolean isValid(char c) {
            return isAlpha(c) || isDigit(c) || c == '_' || c == '-';
        }

        private static boolean isAlpha(char c) {
            return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
        }

        private static boolean isDigit(char c) {
            return c >= '0' && c <= '9';
        }


        public boolean equals(Object o) {
            if (o instanceof Name) {
                Comparator<String> c = String.CASE_INSENSITIVE_ORDER;
                return c.compare(name, ((Name)o).name) == 0;
            } else {
                return false;
            }
        }


        public int hashCode() {
            if (hashCode == -1) {
                hashCode = name.toLowerCase(Locale.ROOT).hashCode();
            }
            return hashCode;
        }


        public String toString() {
            return name;
        }


        public static final Name MANIFEST_VERSION = new Name("Manifest-Version");


        public static final Name SIGNATURE_VERSION = new Name("Signature-Version");


        public static final Name CONTENT_TYPE = new Name("Content-Type");


        public static final Name CLASS_PATH = new Name("Class-Path");


        public static final Name MAIN_CLASS = new Name("Main-Class");


        public static final Name SEALED = new Name("Sealed");


        public static final Name EXTENSION_LIST = new Name("Extension-List");


        public static final Name EXTENSION_NAME = new Name("Extension-Name");


        @Deprecated
        public static final Name EXTENSION_INSTALLATION = new Name("Extension-Installation");


        public static final Name IMPLEMENTATION_TITLE = new Name("Implementation-Title");


        public static final Name IMPLEMENTATION_VERSION = new Name("Implementation-Version");


        public static final Name IMPLEMENTATION_VENDOR = new Name("Implementation-Vendor");


        @Deprecated
        public static final Name IMPLEMENTATION_VENDOR_ID = new Name("Implementation-Vendor-Id");


        @Deprecated
        public static final Name IMPLEMENTATION_URL = new Name("Implementation-URL");


        public static final Name SPECIFICATION_TITLE = new Name("Specification-Title");


        public static final Name SPECIFICATION_VERSION = new Name("Specification-Version");


        public static final Name SPECIFICATION_VENDOR = new Name("Specification-Vendor");


        public static final Name MULTI_RELEASE = new Name("Multi-Release");
    }
}
