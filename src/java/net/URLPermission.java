/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ObjectInputStream;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.security.Permission;


public final class URLPermission extends Permission {

    private static final long serialVersionUID = -2702463814894478682L;

    private transient String scheme;
    private transient String ssp;                 // scheme specific part
    private transient String path;
    private transient List<String> methods;
    private transient List<String> requestHeaders;
    private transient Authority authority;

    // serialized field
    private String actions;


    public URLPermission(String url, String actions) {
        super(url);
        init(actions);
    }

    private void init(String actions) {
        parseURI(getName());
        int colon = actions.indexOf(':');
        if (actions.lastIndexOf(':') != colon) {
            throw new IllegalArgumentException(
                "Invalid actions string: \"" + actions + "\"");
        }

        String methods, headers;
        if (colon == -1) {
            methods = actions;
            headers = "";
        } else {
            methods = actions.substring(0, colon);
            headers = actions.substring(colon+1);
        }

        List<String> l = normalizeMethods(methods);
        Collections.sort(l);
        this.methods = Collections.unmodifiableList(l);

        l = normalizeHeaders(headers);
        Collections.sort(l);
        this.requestHeaders = Collections.unmodifiableList(l);

        this.actions = actions();
    }


    public URLPermission(String url) {
        this(url, "*:*");
    }


    public String getActions() {
        return actions;
    }


    public boolean implies(Permission p) {
        if (! (p instanceof URLPermission)) {
            return false;
        }

        URLPermission that = (URLPermission)p;

        if (this.methods.isEmpty() && !that.methods.isEmpty()) {
            return false;
        }

        if (!this.methods.isEmpty() &&
            !this.methods.get(0).equals("*") &&
            Collections.indexOfSubList(this.methods,
                                       that.methods) == -1) {
            return false;
        }

        if (this.requestHeaders.isEmpty() && !that.requestHeaders.isEmpty()) {
            return false;
        }

        if (!this.requestHeaders.isEmpty() &&
            !this.requestHeaders.get(0).equals("*") &&
             Collections.indexOfSubList(this.requestHeaders,
                                        that.requestHeaders) == -1) {
            return false;
        }

        if (!this.scheme.equals(that.scheme)) {
            return false;
        }

        if (this.ssp.equals("*")) {
            return true;
        }

        if (!this.authority.implies(that.authority)) {
            return false;
        }

        if (this.path == null) {
            return that.path == null;
        }
        if (that.path == null) {
            return false;
        }

        if (this.path.endsWith("/-")) {
            String thisprefix = this.path.substring(0, this.path.length() - 1);
            return that.path.startsWith(thisprefix);
            }

        if (this.path.endsWith("/*")) {
            String thisprefix = this.path.substring(0, this.path.length() - 1);
            if (!that.path.startsWith(thisprefix)) {
                return false;
            }
            String thatsuffix = that.path.substring(thisprefix.length());
            // suffix must not contain '/' chars
            if (thatsuffix.indexOf('/') != -1) {
                return false;
            }
            if (thatsuffix.equals("-")) {
                return false;
            }
            return true;
        }
        return this.path.equals(that.path);
    }



    public boolean equals(Object p) {
        if (!(p instanceof URLPermission)) {
            return false;
        }
        URLPermission that = (URLPermission)p;
        if (!this.scheme.equals(that.scheme)) {
            return false;
        }
        if (!this.getActions().equals(that.getActions())) {
            return false;
        }
        if (!this.authority.equals(that.authority)) {
            return false;
        }
        if (this.path != null) {
            return this.path.equals(that.path);
        } else {
            return that.path == null;
        }
    }


    public int hashCode() {
        return getActions().hashCode()
            + scheme.hashCode()
            + authority.hashCode()
            + (path == null ? 0 : path.hashCode());
    }


    private List<String> normalizeMethods(String methods) {
        List<String> l = new ArrayList<>();
        StringBuilder b = new StringBuilder();
        for (int i=0; i<methods.length(); i++) {
            char c = methods.charAt(i);
            if (c == ',') {
                String s = b.toString();
                if (s.length() > 0)
                    l.add(s);
                b = new StringBuilder();
            } else if (c == ' ' || c == '\t') {
                throw new IllegalArgumentException(
                    "White space not allowed in methods: \"" + methods + "\"");
            } else {
                if (c >= 'a' && c <= 'z') {
                    c += 'A' - 'a';
                }
                b.append(c);
            }
        }
        String s = b.toString();
        if (s.length() > 0)
            l.add(s);
        return l;
    }

    private List<String> normalizeHeaders(String headers) {
        List<String> l = new ArrayList<>();
        StringBuilder b = new StringBuilder();
        boolean capitalizeNext = true;
        for (int i=0; i<headers.length(); i++) {
            char c = headers.charAt(i);
            if (c >= 'a' && c <= 'z') {
                if (capitalizeNext) {
                    c += 'A' - 'a';
                    capitalizeNext = false;
                }
                b.append(c);
            } else if (c == ' ' || c == '\t') {
                throw new IllegalArgumentException(
                    "White space not allowed in headers: \"" + headers + "\"");
            } else if (c == '-') {
                    capitalizeNext = true;
                b.append(c);
            } else if (c == ',') {
                String s = b.toString();
                if (s.length() > 0)
                    l.add(s);
                b = new StringBuilder();
                capitalizeNext = true;
            } else {
                capitalizeNext = false;
                b.append(c);
            }
        }
        String s = b.toString();
        if (s.length() > 0)
            l.add(s);
        return l;
    }

    private void parseURI(String url) {
        int len = url.length();
        int delim = url.indexOf(':');
        if (delim == -1 || delim + 1 == len) {
            throw new IllegalArgumentException(
                "Invalid URL string: \"" + url + "\"");
        }
        scheme = url.substring(0, delim).toLowerCase();
        this.ssp = url.substring(delim + 1);

        if (!ssp.startsWith("//")) {
            if (!ssp.equals("*")) {
                throw new IllegalArgumentException(
                    "Invalid URL string: \"" + url + "\"");
            }
            this.authority = new Authority(scheme, "*");
            return;
        }
        String authpath = ssp.substring(2);

        delim = authpath.indexOf('/');
        String auth;
        if (delim == -1) {
            this.path = "";
            auth = authpath;
        } else {
            auth = authpath.substring(0, delim);
            this.path = authpath.substring(delim);
        }
        this.authority = new Authority(scheme, auth.toLowerCase());
    }

    private String actions() {
        // The colon separator is optional when the request headers list is
        // empty.This implementation chooses to include it even when the request
        // headers list is empty.
        return String.join(",", methods) + ":" + String.join(",", requestHeaders);
    }


    private void readObject(ObjectInputStream s)
        throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField fields = s.readFields();
        String actions = (String)fields.get("actions", null);

        init(actions);
    }

    static class Authority {
        HostPortrange p;

        Authority(String scheme, String authority) {
            int at = authority.indexOf('@');
            if (at == -1) {
                    p = new HostPortrange(scheme, authority);
            } else {
                    p = new HostPortrange(scheme, authority.substring(at+1));
            }
        }

        boolean implies(Authority other) {
            return impliesHostrange(other) && impliesPortrange(other);
        }

        private boolean impliesHostrange(Authority that) {
            String thishost = this.p.hostname();
            String thathost = that.p.hostname();

            if (p.wildcard() && thishost.equals("")) {
                // this "*" implies all others
                return true;
            }
            if (that.p.wildcard() && thathost.equals("")) {
                // that "*" can only be implied by this "*"
                return false;
            }
            if (thishost.equals(thathost)) {
                // covers all cases of literal IP addresses and fixed
                // domain names.
                return true;
            }
            if (this.p.wildcard()) {
                // this "*.foo.com" implies "bub.bar.foo.com"
                return thathost.endsWith(thishost);
            }
            return false;
        }

        private boolean impliesPortrange(Authority that) {
            int[] thisrange = this.p.portrange();
            int[] thatrange = that.p.portrange();
            if (thisrange[0] == -1) {
                /* port not specified non http/s URL */
                return true;
            }
            return thisrange[0] <= thatrange[0] &&
                        thisrange[1] >= thatrange[1];
        }

        boolean equals(Authority that) {
            return this.p.equals(that.p);
        }

        public int hashCode() {
            return p.hashCode();
        }
    }
}
