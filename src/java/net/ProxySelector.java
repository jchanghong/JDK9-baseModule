/*
 * Copyright (c) 2003, 2013, Oracle and/or its affiliates. All rights reserved.
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
import java.util.List;
import sun.security.util.SecurityConstants;


public abstract class ProxySelector {

    private static ProxySelector theProxySelector;

    static {
        try {
            Class<?> c = Class.forName("sun.net.spi.DefaultProxySelector");
            if (c != null && ProxySelector.class.isAssignableFrom(c)) {
                @SuppressWarnings("deprecation")
                ProxySelector tmp = (ProxySelector) c.newInstance();
                theProxySelector = tmp;
            }
        } catch (Exception e) {
            theProxySelector = null;
        }
    }


    public static ProxySelector getDefault() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(SecurityConstants.GET_PROXYSELECTOR_PERMISSION);
        }
        return theProxySelector;
    }


    public static void setDefault(ProxySelector ps) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(SecurityConstants.SET_PROXYSELECTOR_PERMISSION);
        }
        theProxySelector = ps;
    }


    public abstract List<Proxy> select(URI uri);


    public abstract void connectFailed(URI uri, SocketAddress sa, IOException ioe);


    public static ProxySelector of(InetSocketAddress proxyAddress) {
        return new StaticProxySelector(proxyAddress);
    }

    static class StaticProxySelector extends ProxySelector {
        private static final List<Proxy> NO_PROXY_LIST = List.of(Proxy.NO_PROXY);
        final List<Proxy> list;

        StaticProxySelector(InetSocketAddress address){
            Proxy p;
            if (address == null) {
                p = Proxy.NO_PROXY;
            } else {
                p = new Proxy(Proxy.Type.HTTP, address);
            }
            list = List.of(p);
        }

        @Override
        public void connectFailed(URI uri, SocketAddress sa, IOException e) {
            /* ignore */
        }

        @Override
        public synchronized List<Proxy> select(URI uri) {
            String scheme = uri.getScheme().toLowerCase();
            if (scheme.equals("http") || scheme.equals("https")) {
                return list;
            } else {
                return NO_PROXY_LIST;
            }
        }
    }
}
