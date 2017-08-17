/*
 * Copyright (c) 1997, 2017, Oracle and/or its affiliates. All rights reserved.
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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.net.InetAddress;
import java.security.AccessController;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.PrivilegedAction;
import java.security.Security;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Vector;
import java.util.StringJoiner;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentSkipListMap;
import sun.net.util.IPAddressUtil;
import sun.net.PortConfig;
import sun.security.util.RegisteredDomain;
import sun.security.util.SecurityConstants;
import sun.security.util.Debug;




public final class SocketPermission extends Permission
    implements java.io.Serializable
{
    private static final long serialVersionUID = -7204263841984476862L;


    private static final int CONNECT    = 0x1;


    private static final int LISTEN     = 0x2;


    private static final int ACCEPT     = 0x4;


    private static final int RESOLVE    = 0x8;


    private static final int NONE               = 0x0;


    private static final int ALL        = CONNECT|LISTEN|ACCEPT|RESOLVE;

    // various port constants
    private static final int PORT_MIN = 0;
    private static final int PORT_MAX = 65535;
    private static final int PRIV_PORT_MAX = 1023;
    private static final int DEF_EPH_LOW = 49152;

    // the actions mask
    private transient int mask;



    private String actions; // Left null as long as possible, then
                            // created and re-used in the getAction function.

    // hostname part as it is passed
    private transient String hostname;

    // the canonical name of the host
    // in the case of "*.foo.com", cname is ".foo.com".

    private transient String cname;

    // all the IP addresses of the host
    private transient InetAddress[] addresses;

    // true if the hostname is a wildcard (e.g. "*.sun.com")
    private transient boolean wildcard;

    // true if we were initialized with a single numeric IP address
    private transient boolean init_with_ip;

    // true if this SocketPermission represents an invalid/unknown host
    // used for implies when the delayed lookup has already failed
    private transient boolean invalid;

    // port range on host
    private transient int[] portrange;

    private transient boolean defaultDeny = false;

    // true if this SocketPermission represents a hostname
    // that failed our reverse mapping heuristic test
    private transient boolean untrusted;
    private transient boolean trusted;

    // true if the sun.net.trustNameService system property is set
    private static boolean trustNameService;

    private static Debug debug = null;
    private static boolean debugInit = false;

    // lazy initializer
    private static class EphemeralRange {
        static final int low = initEphemeralPorts("low", DEF_EPH_LOW);
            static final int high = initEphemeralPorts("high", PORT_MAX);
    };

    static {
        Boolean tmp = java.security.AccessController.doPrivileged(
                new sun.security.action.GetBooleanAction("sun.net.trustNameService"));
        trustNameService = tmp.booleanValue();
    }

    private static synchronized Debug getDebug() {
        if (!debugInit) {
            debug = Debug.getInstance("access");
            debugInit = true;
        }
        return debug;
    }


    public SocketPermission(String host, String action) {
        super(getHost(host));
        // name initialized to getHost(host); NPE detected in getHost()
        init(getName(), getMask(action));
    }


    SocketPermission(String host, int mask) {
        super(getHost(host));
        // name initialized to getHost(host); NPE detected in getHost()
        init(getName(), mask);
    }

    private void setDeny() {
        defaultDeny = true;
    }

    private static String getHost(String host) {
        if (host.equals("")) {
            return "localhost";
        } else {
            /* IPv6 literal address used in this context should follow
             * the format specified in RFC 2732;
             * if not, we try to solve the unambiguous case
             */
            int ind;
            if (host.charAt(0) != '[') {
                if ((ind = host.indexOf(':')) != host.lastIndexOf(':')) {
                    /* More than one ":", meaning IPv6 address is not
                     * in RFC 2732 format;
                     * We will rectify user errors for all unambiguious cases
                     */
                    StringTokenizer st = new StringTokenizer(host, ":");
                    int tokens = st.countTokens();
                    if (tokens == 9) {
                        // IPv6 address followed by port
                        ind = host.lastIndexOf(':');
                        host = "[" + host.substring(0, ind) + "]" +
                            host.substring(ind);
                    } else if (tokens == 8 && host.indexOf("::") == -1) {
                        // IPv6 address only, not followed by port
                        host = "[" + host + "]";
                    } else {
                        // could be ambiguous
                        throw new IllegalArgumentException("Ambiguous"+
                                                           " hostport part");
                    }
                }
            }
            return host;
        }
    }

    private int[] parsePort(String port)
        throws Exception
    {

        if (port == null || port.equals("") || port.equals("*")) {
            return new int[] {PORT_MIN, PORT_MAX};
        }

        int dash = port.indexOf('-');

        if (dash == -1) {
            int p = Integer.parseInt(port);
            return new int[] {p, p};
        } else {
            String low = port.substring(0, dash);
            String high = port.substring(dash+1);
            int l,h;

            if (low.equals("")) {
                l = PORT_MIN;
            } else {
                l = Integer.parseInt(low);
            }

            if (high.equals("")) {
                h = PORT_MAX;
            } else {
                h = Integer.parseInt(high);
            }
            if (l < 0 || h < 0 || h<l)
                throw new IllegalArgumentException("invalid port range");

            return new int[] {l, h};
        }
    }


    private boolean includesEphemerals() {
        return portrange[0] == 0;
    }


    private void init(String host, int mask) {
        // Set the integer mask that represents the actions

        if ((mask & ALL) != mask)
            throw new IllegalArgumentException("invalid actions mask");

        // always OR in RESOLVE if we allow any of the others
        this.mask = mask | RESOLVE;

        // Parse the host name.  A name has up to three components, the
        // hostname, a port number, or two numbers representing a port
        // range.   "www.sun.com:8080-9090" is a valid host name.

        // With IPv6 an address can be 2010:836B:4179::836B:4179
        // An IPv6 address needs to be enclose in []
        // For ex: [2010:836B:4179::836B:4179]:8080-9090
        // Refer to RFC 2732 for more information.

        int rb = 0 ;
        int start = 0, end = 0;
        int sep = -1;
        String hostport = host;
        if (host.charAt(0) == '[') {
            start = 1;
            rb = host.indexOf(']');
            if (rb != -1) {
                host = host.substring(start, rb);
            } else {
                throw new
                    IllegalArgumentException("invalid host/port: "+host);
            }
            sep = hostport.indexOf(':', rb+1);
        } else {
            start = 0;
            sep = host.indexOf(':', rb);
            end = sep;
            if (sep != -1) {
                host = host.substring(start, end);
            }
        }

        if (sep != -1) {
            String port = hostport.substring(sep+1);
            try {
                portrange = parsePort(port);
            } catch (Exception e) {
                throw new
                    IllegalArgumentException("invalid port range: "+port);
            }
        } else {
            portrange = new int[] { PORT_MIN, PORT_MAX };
        }

        hostname = host;

        // is this a domain wildcard specification
        if (host.lastIndexOf('*') > 0) {
            throw new
               IllegalArgumentException("invalid host wildcard specification");
        } else if (host.startsWith("*")) {
            wildcard = true;
            if (host.equals("*")) {
                cname = "";
            } else if (host.startsWith("*.")) {
                cname = host.substring(1).toLowerCase();
            } else {
              throw new
               IllegalArgumentException("invalid host wildcard specification");
            }
            return;
        } else {
            if (host.length() > 0) {
                // see if we are being initialized with an IP address.
                char ch = host.charAt(0);
                if (ch == ':' || Character.digit(ch, 16) != -1) {
                    byte ip[] = IPAddressUtil.textToNumericFormatV4(host);
                    if (ip == null) {
                        ip = IPAddressUtil.textToNumericFormatV6(host);
                    }
                    if (ip != null) {
                        try {
                            addresses =
                                new InetAddress[]
                                {InetAddress.getByAddress(ip) };
                            init_with_ip = true;
                        } catch (UnknownHostException uhe) {
                            // this shouldn't happen
                            invalid = true;
                        }
                    }
                }
            }
        }
    }


    private static int getMask(String action) {

        if (action == null) {
            throw new NullPointerException("action can't be null");
        }

        if (action.equals("")) {
            throw new IllegalArgumentException("action can't be empty");
        }

        int mask = NONE;

        // Use object identity comparison against known-interned strings for
        // performance benefit (these values are used heavily within the JDK).
        if (action == SecurityConstants.SOCKET_RESOLVE_ACTION) {
            return RESOLVE;
        } else if (action == SecurityConstants.SOCKET_CONNECT_ACTION) {
            return CONNECT;
        } else if (action == SecurityConstants.SOCKET_LISTEN_ACTION) {
            return LISTEN;
        } else if (action == SecurityConstants.SOCKET_ACCEPT_ACTION) {
            return ACCEPT;
        } else if (action == SecurityConstants.SOCKET_CONNECT_ACCEPT_ACTION) {
            return CONNECT|ACCEPT;
        }

        char[] a = action.toCharArray();

        int i = a.length - 1;
        if (i < 0)
            return mask;

        while (i != -1) {
            char c;

            // skip whitespace
            while ((i!=-1) && ((c = a[i]) == ' ' ||
                               c == '\r' ||
                               c == '\n' ||
                               c == '\f' ||
                               c == '\t'))
                i--;

            // check for the known strings
            int matchlen;

            if (i >= 6 && (a[i-6] == 'c' || a[i-6] == 'C') &&
                          (a[i-5] == 'o' || a[i-5] == 'O') &&
                          (a[i-4] == 'n' || a[i-4] == 'N') &&
                          (a[i-3] == 'n' || a[i-3] == 'N') &&
                          (a[i-2] == 'e' || a[i-2] == 'E') &&
                          (a[i-1] == 'c' || a[i-1] == 'C') &&
                          (a[i] == 't' || a[i] == 'T'))
            {
                matchlen = 7;
                mask |= CONNECT;

            } else if (i >= 6 && (a[i-6] == 'r' || a[i-6] == 'R') &&
                                 (a[i-5] == 'e' || a[i-5] == 'E') &&
                                 (a[i-4] == 's' || a[i-4] == 'S') &&
                                 (a[i-3] == 'o' || a[i-3] == 'O') &&
                                 (a[i-2] == 'l' || a[i-2] == 'L') &&
                                 (a[i-1] == 'v' || a[i-1] == 'V') &&
                                 (a[i] == 'e' || a[i] == 'E'))
            {
                matchlen = 7;
                mask |= RESOLVE;

            } else if (i >= 5 && (a[i-5] == 'l' || a[i-5] == 'L') &&
                                 (a[i-4] == 'i' || a[i-4] == 'I') &&
                                 (a[i-3] == 's' || a[i-3] == 'S') &&
                                 (a[i-2] == 't' || a[i-2] == 'T') &&
                                 (a[i-1] == 'e' || a[i-1] == 'E') &&
                                 (a[i] == 'n' || a[i] == 'N'))
            {
                matchlen = 6;
                mask |= LISTEN;

            } else if (i >= 5 && (a[i-5] == 'a' || a[i-5] == 'A') &&
                                 (a[i-4] == 'c' || a[i-4] == 'C') &&
                                 (a[i-3] == 'c' || a[i-3] == 'C') &&
                                 (a[i-2] == 'e' || a[i-2] == 'E') &&
                                 (a[i-1] == 'p' || a[i-1] == 'P') &&
                                 (a[i] == 't' || a[i] == 'T'))
            {
                matchlen = 6;
                mask |= ACCEPT;

            } else {
                // parse error
                throw new IllegalArgumentException(
                        "invalid permission: " + action);
            }

            // make sure we didn't just match the tail of a word
            // like "ackbarfaccept".  Also, skip to the comma.
            boolean seencomma = false;
            while (i >= matchlen && !seencomma) {
                switch(a[i-matchlen]) {
                case ',':
                    seencomma = true;
                    break;
                case ' ': case '\r': case '\n':
                case '\f': case '\t':
                    break;
                default:
                    throw new IllegalArgumentException(
                            "invalid permission: " + action);
                }
                i--;
            }

            // point i at the location of the comma minus one (or -1).
            i -= matchlen;
        }

        return mask;
    }

    private boolean isUntrusted()
        throws UnknownHostException
    {
        if (trusted) return false;
        if (invalid || untrusted) return true;
        try {
            if (!trustNameService && (defaultDeny ||
                sun.net.www.URLConnection.isProxiedHost(hostname))) {
                if (this.cname == null) {
                    this.getCanonName();
                }
                if (!match(cname, hostname)) {
                    // Last chance
                    if (!authorized(hostname, addresses[0].getAddress())) {
                        untrusted = true;
                        Debug debug = getDebug();
                        if (debug != null && Debug.isOn("failure")) {
                            debug.println("socket access restriction: proxied host " + "(" + addresses[0] + ")" + " does not match " + cname + " from reverse lookup");
                        }
                        return true;
                    }
                }
                trusted = true;
            }
        } catch (UnknownHostException uhe) {
            invalid = true;
            throw uhe;
        }
        return false;
    }


    void getCanonName()
        throws UnknownHostException
    {
        if (cname != null || invalid || untrusted) return;

        // attempt to get the canonical name

        try {
            // first get the IP addresses if we don't have them yet
            // this is because we need the IP address to then get
            // FQDN.
            if (addresses == null) {
                getIP();
            }

            // we have to do this check, otherwise we might not
            // get the fully qualified domain name
            if (init_with_ip) {
                cname = addresses[0].getHostName(false).toLowerCase();
            } else {
             cname = InetAddress.getByName(addresses[0].getHostAddress()).
                                              getHostName(false).toLowerCase();
            }
        } catch (UnknownHostException uhe) {
            invalid = true;
            throw uhe;
        }
    }

    private transient String cdomain, hdomain;

    private boolean match(String cname, String hname) {
        String a = cname.toLowerCase();
        String b = hname.toLowerCase();
        if (a.startsWith(b)  &&
            ((a.length() == b.length()) || (a.charAt(b.length()) == '.'))) {
            return true;
        }
        if (cdomain == null) {
            cdomain = RegisteredDomain.from(a)
                                      .map(RegisteredDomain::name)
                                      .orElse(a);
        }
        if (hdomain == null) {
            hdomain = RegisteredDomain.from(b)
                                      .map(RegisteredDomain::name)
                                      .orElse(b);
        }

        return cdomain.length() != 0 && hdomain.length() != 0
                        && cdomain.equals(hdomain);
    }

    private boolean authorized(String cname, byte[] addr) {
        if (addr.length == 4)
            return authorizedIPv4(cname, addr);
        else if (addr.length == 16)
            return authorizedIPv6(cname, addr);
        else
            return false;
    }

    private boolean authorizedIPv4(String cname, byte[] addr) {
        String authHost = "";
        InetAddress auth;

        try {
            authHost = "auth." +
                        (addr[3] & 0xff) + "." + (addr[2] & 0xff) + "." +
                        (addr[1] & 0xff) + "." + (addr[0] & 0xff) +
                        ".in-addr.arpa";
            // Following check seems unnecessary
            // auth = InetAddress.getAllByName0(authHost, false)[0];
            authHost = hostname + '.' + authHost;
            auth = InetAddress.getAllByName0(authHost, false)[0];
            if (auth.equals(InetAddress.getByAddress(addr))) {
                return true;
            }
            Debug debug = getDebug();
            if (debug != null && Debug.isOn("failure")) {
                debug.println("socket access restriction: IP address of " + auth + " != " + InetAddress.getByAddress(addr));
            }
        } catch (UnknownHostException uhe) {
            Debug debug = getDebug();
            if (debug != null && Debug.isOn("failure")) {
                debug.println("socket access restriction: forward lookup failed for " + authHost);
            }
        }
        return false;
    }

    private boolean authorizedIPv6(String cname, byte[] addr) {
        String authHost = "";
        InetAddress auth;

        try {
            StringBuilder sb = new StringBuilder(39);

            for (int i = 15; i >= 0; i--) {
                sb.append(Integer.toHexString(((addr[i]) & 0x0f)));
                sb.append('.');
                sb.append(Integer.toHexString(((addr[i] >> 4) & 0x0f)));
                sb.append('.');
            }
            authHost = "auth." + sb.toString() + "IP6.ARPA";
            //auth = InetAddress.getAllByName0(authHost, false)[0];
            authHost = hostname + '.' + authHost;
            auth = InetAddress.getAllByName0(authHost, false)[0];
            if (auth.equals(InetAddress.getByAddress(addr)))
                return true;
            Debug debug = getDebug();
            if (debug != null && Debug.isOn("failure")) {
                debug.println("socket access restriction: IP address of " + auth + " != " + InetAddress.getByAddress(addr));
            }
        } catch (UnknownHostException uhe) {
            Debug debug = getDebug();
            if (debug != null && Debug.isOn("failure")) {
                debug.println("socket access restriction: forward lookup failed for " + authHost);
            }
        }
        return false;
    }



    void getIP()
        throws UnknownHostException
    {
        if (addresses != null || wildcard || invalid) return;

        try {
            // now get all the IP addresses
            String host;
            if (getName().charAt(0) == '[') {
                // Literal IPv6 address
                host = getName().substring(1, getName().indexOf(']'));
            } else {
                int i = getName().indexOf(':');
                if (i == -1)
                    host = getName();
                else {
                    host = getName().substring(0,i);
                }
            }

            addresses =
                new InetAddress[] {InetAddress.getAllByName0(host, false)[0]};

        } catch (UnknownHostException uhe) {
            invalid = true;
            throw uhe;
        }  catch (IndexOutOfBoundsException iobe) {
            invalid = true;
            throw new UnknownHostException(getName());
        }
    }


    @Override
    public boolean implies(Permission p) {
        int i,j;

        if (!(p instanceof SocketPermission))
            return false;

        if (p == this)
            return true;

        SocketPermission that = (SocketPermission) p;

        return ((this.mask & that.mask) == that.mask) &&
                                        impliesIgnoreMask(that);
    }


    boolean impliesIgnoreMask(SocketPermission that) {
        int i,j;

        if ((that.mask & RESOLVE) != that.mask) {

            // check simple port range
            if ((that.portrange[0] < this.portrange[0]) ||
                    (that.portrange[1] > this.portrange[1])) {

                // if either includes the ephemeral range, do full check
                if (this.includesEphemerals() || that.includesEphemerals()) {
                    if (!inRange(this.portrange[0], this.portrange[1],
                                     that.portrange[0], that.portrange[1]))
                    {
                                return false;
                    }
                } else {
                    return false;
                }
            }
        }

        // allow a "*" wildcard to always match anything
        if (this.wildcard && "".equals(this.cname))
            return true;

        // return if either one of these NetPerm objects are invalid...
        if (this.invalid || that.invalid) {
            return compareHostnames(that);
        }

        try {
            if (this.init_with_ip) { // we only check IP addresses
                if (that.wildcard)
                    return false;

                if (that.init_with_ip) {
                    return (this.addresses[0].equals(that.addresses[0]));
                } else {
                    if (that.addresses == null) {
                        that.getIP();
                    }
                    for (i=0; i < that.addresses.length; i++) {
                        if (this.addresses[0].equals(that.addresses[i]))
                            return true;
                    }
                }
                // since "this" was initialized with an IP address, we
                // don't check any other cases
                return false;
            }

            // check and see if we have any wildcards...
            if (this.wildcard || that.wildcard) {
                // if they are both wildcards, return true iff
                // that's cname ends with this cname (i.e., *.sun.com
                // implies *.eng.sun.com)
                if (this.wildcard && that.wildcard)
                    return (that.cname.endsWith(this.cname));

                // a non-wildcard can't imply a wildcard
                if (that.wildcard)
                    return false;

                // this is a wildcard, lets see if that's cname ends with
                // it...
                if (that.cname == null) {
                    that.getCanonName();
                }
                return (that.cname.endsWith(this.cname));
            }

            // comapare IP addresses
            if (this.addresses == null) {
                this.getIP();
            }

            if (that.addresses == null) {
                that.getIP();
            }

            if (!(that.init_with_ip && this.isUntrusted())) {
                for (j = 0; j < this.addresses.length; j++) {
                    for (i=0; i < that.addresses.length; i++) {
                        if (this.addresses[j].equals(that.addresses[i]))
                            return true;
                    }
                }

                // XXX: if all else fails, compare hostnames?
                // Do we really want this?
                if (this.cname == null) {
                    this.getCanonName();
                }

                if (that.cname == null) {
                    that.getCanonName();
                }

                return (this.cname.equalsIgnoreCase(that.cname));
            }

        } catch (UnknownHostException uhe) {
            return compareHostnames(that);
        }

        // make sure the first thing that is done here is to return
        // false. If not, uncomment the return false in the above catch.

        return false;
    }

    private boolean compareHostnames(SocketPermission that) {
        // we see if the original names/IPs passed in were equal.

        String thisHost = hostname;
        String thatHost = that.hostname;

        if (thisHost == null) {
            return false;
        } else if (this.wildcard) {
            final int cnameLength = this.cname.length();
            return thatHost.regionMatches(true,
                                          (thatHost.length() - cnameLength),
                                          this.cname, 0, cnameLength);
        } else {
            return thisHost.equalsIgnoreCase(thatHost);
        }
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (! (obj instanceof SocketPermission))
            return false;

        SocketPermission that = (SocketPermission) obj;

        //this is (overly?) complex!!!

        // check the mask first
        if (this.mask != that.mask) return false;

        if ((that.mask & RESOLVE) != that.mask) {
            // now check the port range...
            if ((this.portrange[0] != that.portrange[0]) ||
                (this.portrange[1] != that.portrange[1])) {
                return false;
            }
        }

        // short cut. This catches:
        //  "crypto" equal to "crypto", or
        // "1.2.3.4" equal to "1.2.3.4.", or
        //  "*.edu" equal to "*.edu", but it
        //  does not catch "crypto" equal to
        // "crypto.eng.sun.com".

        if (this.getName().equalsIgnoreCase(that.getName())) {
            return true;
        }

        // we now attempt to get the Canonical (FQDN) name and
        // compare that. If this fails, about all we can do is return
        // false.

        try {
            this.getCanonName();
            that.getCanonName();
        } catch (UnknownHostException uhe) {
            return false;
        }

        if (this.invalid || that.invalid)
            return false;

        if (this.cname != null) {
            return this.cname.equalsIgnoreCase(that.cname);
        }

        return false;
    }


    @Override
    public int hashCode() {
        /*
         * If this SocketPermission was initialized with an IP address
         * or a wildcard, use getName().hashCode(), otherwise use
         * the hashCode() of the host name returned from
         * java.net.InetAddress.getHostName method.
         */

        if (init_with_ip || wildcard) {
            return this.getName().hashCode();
        }

        try {
            getCanonName();
        } catch (UnknownHostException uhe) {

        }

        if (invalid || cname == null)
            return this.getName().hashCode();
        else
            return this.cname.hashCode();
    }



    int getMask() {
        return mask;
    }


    private static String getActions(int mask) {
        StringJoiner sj = new StringJoiner(",");
        if ((mask & CONNECT) == CONNECT) {
            sj.add("connect");
        }
        if ((mask & LISTEN) == LISTEN) {
            sj.add("listen");
        }
        if ((mask & ACCEPT) == ACCEPT) {
            sj.add("accept");
        }
        if ((mask & RESOLVE) == RESOLVE) {
            sj.add("resolve");
        }
        return sj.toString();
    }


    @Override
    public String getActions()
    {
        if (actions == null)
            actions = getActions(this.mask);

        return actions;
    }


    @Override
    public PermissionCollection newPermissionCollection() {
        return new SocketPermissionCollection();
    }


    private synchronized void writeObject(java.io.ObjectOutputStream s)
        throws IOException
    {
        // Write out the actions. The superclass takes care of the name
        // call getActions to make sure actions field is initialized
        if (actions == null)
            getActions();
        s.defaultWriteObject();
    }


    private synchronized void readObject(java.io.ObjectInputStream s)
         throws IOException, ClassNotFoundException
    {
        // Read in the action, then initialize the rest
        s.defaultReadObject();
        init(getName(),getMask(actions));
    }


    private static int initEphemeralPorts(String suffix, int defval) {
        return AccessController.doPrivileged(
            new PrivilegedAction<>(){
                public Integer run() {
                    int val = Integer.getInteger(
                            "jdk.net.ephemeralPortRange."+suffix, -1
                    );
                    if (val != -1) {
                        return val;
                    } else {
                        return suffix.equals("low") ?
                            PortConfig.getLower() : PortConfig.getUpper();
                    }
                }
            }
        );
    }


    private static boolean inRange(
        int policyLow, int policyHigh, int targetLow, int targetHigh
    )
    {
        final int ephemeralLow = EphemeralRange.low;
        final int ephemeralHigh = EphemeralRange.high;

        if (targetLow == 0) {
            // check policy includes ephemeral range
            if (!inRange(policyLow, policyHigh, ephemeralLow, ephemeralHigh)) {
                return false;
            }
            if (targetHigh == 0) {
                // nothing left to do
                return true;
            }
            // continue check with first real port number
            targetLow = 1;
        }

        if (policyLow == 0 && policyHigh == 0) {
            // ephemeral range only
            return targetLow >= ephemeralLow && targetHigh <= ephemeralHigh;
        }

        if (policyLow != 0) {
            // simple check of policy only
            return targetLow >= policyLow && targetHigh <= policyHigh;
        }

        // policyLow == 0 which means possibly two ranges to check

        // first check if policy and ephem range overlap/contiguous

        if (policyHigh >= ephemeralLow - 1) {
            return targetHigh <= ephemeralHigh;
        }

        // policy and ephem range do not overlap

        // target range must lie entirely inside policy range or eph range

        return  (targetLow <= policyHigh && targetHigh <= policyHigh) ||
                (targetLow >= ephemeralLow && targetHigh <= ephemeralHigh);
    }
    /*
    public String toString()
    {
        StringBuffer s = new StringBuffer(super.toString() + "\n" +
            "cname = " + cname + "\n" +
            "wildcard = " + wildcard + "\n" +
            "invalid = " + invalid + "\n" +
            "portrange = " + portrange[0] + "," + portrange[1] + "\n");
        if (addresses != null) for (int i=0; i<addresses.length; i++) {
            s.append( addresses[i].getHostAddress());
            s.append("\n");
        } else {
            s.append("(no addresses)\n");
        }

        return s.toString();
    }

    public static void main(String args[]) throws Exception {
        SocketPermission this_ = new SocketPermission(args[0], "connect");
        SocketPermission that_ = new SocketPermission(args[1], "connect");
        System.out.println("-----\n");
        System.out.println("this.implies(that) = " + this_.implies(that_));
        System.out.println("-----\n");
        System.out.println("this = "+this_);
        System.out.println("-----\n");
        System.out.println("that = "+that_);
        System.out.println("-----\n");

        SocketPermissionCollection nps = new SocketPermissionCollection();
        nps.add(this_);
        nps.add(new SocketPermission("www-leland.stanford.edu","connect"));
        nps.add(new SocketPermission("www-sun.com","connect"));
        System.out.println("nps.implies(that) = " + nps.implies(that_));
        System.out.println("-----\n");
    }
    */
}



final class SocketPermissionCollection extends PermissionCollection
    implements Serializable
{
    // Not serialized; see serialization section at end of class
    // A ConcurrentSkipListMap is used to preserve order, so that most
    // recently added permissions are checked first (see JDK-4301064).
    private transient ConcurrentSkipListMap<String, SocketPermission> perms;


    public SocketPermissionCollection() {
        perms = new ConcurrentSkipListMap<>(new SPCComparator());
    }


    @Override
    public void add(Permission permission) {
        if (! (permission instanceof SocketPermission))
            throw new IllegalArgumentException("invalid permission: "+
                                               permission);
        if (isReadOnly())
            throw new SecurityException(
                "attempt to add a Permission to a readonly PermissionCollection");

        SocketPermission sp = (SocketPermission)permission;

        // Add permission to map if it is absent, or replace with new
        // permission if applicable. NOTE: cannot use lambda for
        // remappingFunction parameter until JDK-8076596 is fixed.
        perms.merge(sp.getName(), sp,
            new java.util.function.BiFunction<>() {
                @Override
                public SocketPermission apply(SocketPermission existingVal,
                                              SocketPermission newVal) {
                    int oldMask = existingVal.getMask();
                    int newMask = newVal.getMask();
                    if (oldMask != newMask) {
                        int effective = oldMask | newMask;
                        if (effective == newMask) {
                            return newVal;
                        }
                        if (effective != oldMask) {
                            return new SocketPermission(sp.getName(),
                                                        effective);
                        }
                    }
                    return existingVal;
                }
            }
        );
    }


    @Override
    public boolean implies(Permission permission)
    {
        if (! (permission instanceof SocketPermission))
                return false;

        SocketPermission np = (SocketPermission) permission;

        int desired = np.getMask();
        int effective = 0;
        int needed = desired;

        //System.out.println("implies "+np);
        for (SocketPermission x : perms.values()) {
            //System.out.println("  trying "+x);
            if (((needed & x.getMask()) != 0) && x.impliesIgnoreMask(np)) {
                effective |=  x.getMask();
                if ((effective & desired) == desired) {
                    return true;
                }
                needed = (desired ^ effective);
            }
        }
        return false;
    }


    @Override
    @SuppressWarnings("unchecked")
    public Enumeration<Permission> elements() {
        return (Enumeration)Collections.enumeration(perms.values());
    }

    private static final long serialVersionUID = 2787186408602843674L;

    // Need to maintain serialization interoperability with earlier releases,
    // which had the serializable field:

    //
    // The SocketPermissions for this set.
    // @serial
    //
    // private Vector permissions;


    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("permissions", Vector.class),
    };


    /*
     * Writes the contents of the perms field out as a Vector for
     * serialization compatibility with earlier releases.
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        // Don't call out.defaultWriteObject()

        // Write out Vector
        Vector<SocketPermission> permissions = new Vector<>(perms.values());

        ObjectOutputStream.PutField pfields = out.putFields();
        pfields.put("permissions", permissions);
        out.writeFields();
    }

    /*
     * Reads in a Vector of SocketPermissions and saves them in the perms field.
     */
    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        // Don't call in.defaultReadObject()

        // Read in serialized fields
        ObjectInputStream.GetField gfields = in.readFields();

        // Get the one we want
        @SuppressWarnings("unchecked")
        Vector<SocketPermission> permissions = (Vector<SocketPermission>)gfields.get("permissions", null);
        perms = new ConcurrentSkipListMap<>(new SPCComparator());
        for (SocketPermission sp : permissions) {
            perms.put(sp.getName(), sp);
        }
    }


    private static class SPCComparator implements Comparator<String> {
        @Override
        public int compare(String s1, String s2) {
            if (s1.equals(s2)) {
                return 0;
            }
            return -1;
        }
    }
}
