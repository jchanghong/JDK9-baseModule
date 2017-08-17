/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.charset.spi;

import java.nio.charset.Charset;
import java.util.Iterator;




public abstract class CharsetProvider {

    private static Void checkPermission() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
            sm.checkPermission(new RuntimePermission("charsetProvider"));
        return null;
    }
    private CharsetProvider(Void ignore) { }


    protected CharsetProvider() {
        this(checkPermission());
    }


    public abstract Iterator<Charset> charsets();


    public abstract Charset charsetForName(String charsetName);

}
