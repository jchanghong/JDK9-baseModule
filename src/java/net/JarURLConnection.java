/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
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
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.security.Permission;
import sun.net.www.ParseUtil;


public abstract class JarURLConnection extends URLConnection {

    private URL jarFileURL;
    private String entryName;


    protected URLConnection jarFileURLConnection;



    protected JarURLConnection(URL url) throws MalformedURLException {
        super(url);
        parseSpecs(url);
    }

    /* get the specs for a given url out of the cache, and compute and
     * cache them if they're not there.
     */
    private void parseSpecs(URL url) throws MalformedURLException {
        String spec = url.getFile();

        int separator = spec.indexOf("!/");
        /*
         * REMIND: we don't handle nested JAR URLs
         */
        if (separator == -1) {
            throw new MalformedURLException("no !/ found in url spec:" + spec);
        }

        jarFileURL = new URL(spec.substring(0, separator++));
        /*
         * The url argument may have had a runtime fragment appended, so
         * we need to add a runtime fragment to the jarFileURL to enable
         * runtime versioning when the underlying jar file is opened.
         */
        if ("runtime".equals(url.getRef())) {
            jarFileURL = new URL(jarFileURL, "#runtime");
        }
        entryName = null;

        /* if ! is the last letter of the innerURL, entryName is null */
        if (++separator != spec.length()) {
            entryName = spec.substring(separator, spec.length());
            entryName = ParseUtil.decode (entryName);
        }
    }


    public URL getJarFileURL() {
        return jarFileURL;
    }


    public String getEntryName() {
        return entryName;
    }


    public abstract JarFile getJarFile() throws IOException;


    public Manifest getManifest() throws IOException {
        return getJarFile().getManifest();
    }


    public JarEntry getJarEntry() throws IOException {
        return getJarFile().getJarEntry(entryName);
    }


    public Attributes getAttributes() throws IOException {
        JarEntry e = getJarEntry();
        return e != null ? e.getAttributes() : null;
    }


    public Attributes getMainAttributes() throws IOException {
        Manifest man = getManifest();
        return man != null ? man.getMainAttributes() : null;
    }


    public java.security.cert.Certificate[] getCertificates()
         throws IOException
    {
        JarEntry e = getJarEntry();
        return e != null ? e.getCertificates() : null;
    }
}
