/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;
import java.util.Map;


public interface CookieStore {

    public void add(URI uri, HttpCookie cookie);



    public List<HttpCookie> get(URI uri);



    public List<HttpCookie> getCookies();



    public List<URI> getURIs();



    public boolean remove(URI uri, HttpCookie cookie);



    public boolean removeAll();
}
