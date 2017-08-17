/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
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

import sun.net.www.protocol.http.AuthenticatorKeys;



// There are no abstract methods, but to be useful the user must
// subclass.
public abstract
class Authenticator {

    // The system-wide authenticator object.  See setDefault().
    private static volatile Authenticator theAuthenticator;

    private String requestingHost;
    private InetAddress requestingSite;
    private int requestingPort;
    private String requestingProtocol;
    private String requestingPrompt;
    private String requestingScheme;
    private URL requestingURL;
    private RequestorType requestingAuthType;
    private final String key = AuthenticatorKeys.computeKey(this);


    public enum RequestorType {

        PROXY,

        SERVER
    }

    private void reset() {
        requestingHost = null;
        requestingSite = null;
        requestingPort = -1;
        requestingProtocol = null;
        requestingPrompt = null;
        requestingScheme = null;
        requestingURL = null;
        requestingAuthType = RequestorType.SERVER;
    }



    public static synchronized void setDefault(Authenticator a) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            NetPermission setDefaultPermission
                = new NetPermission("setDefaultAuthenticator");
            sm.checkPermission(setDefaultPermission);
        }

        theAuthenticator = a;
    }


    public static Authenticator getDefault() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            NetPermission requestPermission
                = new NetPermission("requestPasswordAuthentication");
            sm.checkPermission(requestPermission);
        }
        return theAuthenticator;
    }


    public static PasswordAuthentication requestPasswordAuthentication(
                                            InetAddress addr,
                                            int port,
                                            String protocol,
                                            String prompt,
                                            String scheme) {

        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            NetPermission requestPermission
                = new NetPermission("requestPasswordAuthentication");
            sm.checkPermission(requestPermission);
        }

        Authenticator a = theAuthenticator;
        if (a == null) {
            return null;
        } else {
            synchronized(a) {
                a.reset();
                a.requestingSite = addr;
                a.requestingPort = port;
                a.requestingProtocol = protocol;
                a.requestingPrompt = prompt;
                a.requestingScheme = scheme;
                return a.getPasswordAuthentication();
            }
        }
    }


    public static PasswordAuthentication requestPasswordAuthentication(
                                            String host,
                                            InetAddress addr,
                                            int port,
                                            String protocol,
                                            String prompt,
                                            String scheme) {

        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            NetPermission requestPermission
                = new NetPermission("requestPasswordAuthentication");
            sm.checkPermission(requestPermission);
        }

        Authenticator a = theAuthenticator;
        if (a == null) {
            return null;
        } else {
            synchronized(a) {
                a.reset();
                a.requestingHost = host;
                a.requestingSite = addr;
                a.requestingPort = port;
                a.requestingProtocol = protocol;
                a.requestingPrompt = prompt;
                a.requestingScheme = scheme;
                return a.getPasswordAuthentication();
            }
        }
    }


    public static PasswordAuthentication requestPasswordAuthentication(
                                    String host,
                                    InetAddress addr,
                                    int port,
                                    String protocol,
                                    String prompt,
                                    String scheme,
                                    URL url,
                                    RequestorType reqType) {

        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            NetPermission requestPermission
                = new NetPermission("requestPasswordAuthentication");
            sm.checkPermission(requestPermission);
        }

        Authenticator a = theAuthenticator;
        if (a == null) {
            return null;
        } else {
            synchronized(a) {
                a.reset();
                a.requestingHost = host;
                a.requestingSite = addr;
                a.requestingPort = port;
                a.requestingProtocol = protocol;
                a.requestingPrompt = prompt;
                a.requestingScheme = scheme;
                a.requestingURL = url;
                a.requestingAuthType = reqType;
                return a.getPasswordAuthentication();
            }
        }
    }


    public static PasswordAuthentication requestPasswordAuthentication(
                                    Authenticator authenticator,
                                    String host,
                                    InetAddress addr,
                                    int port,
                                    String protocol,
                                    String prompt,
                                    String scheme,
                                    URL url,
                                    RequestorType reqType) {

        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            NetPermission requestPermission
                = new NetPermission("requestPasswordAuthentication");
            sm.checkPermission(requestPermission);
        }

        Authenticator a = authenticator == null ? theAuthenticator : authenticator;
        if (a == null) {
            return null;
        } else {
            return a.requestPasswordAuthenticationInstance(host,
                                                           addr,
                                                           port,
                                                           protocol,
                                                           prompt,
                                                           scheme,
                                                           url,
                                                           reqType);
        }
    }


    public PasswordAuthentication
    requestPasswordAuthenticationInstance(String host,
                                          InetAddress addr,
                                          int port,
                                          String protocol,
                                          String prompt,
                                          String scheme,
                                          URL url,
                                          RequestorType reqType) {
        synchronized (this) {
            this.reset();
            this.requestingHost = host;
            this.requestingSite = addr;
            this.requestingPort = port;
            this.requestingProtocol = protocol;
            this.requestingPrompt = prompt;
            this.requestingScheme = scheme;
            this.requestingURL = url;
            this.requestingAuthType = reqType;
            return this.getPasswordAuthentication();
        }
    }


    protected final String getRequestingHost() {
        return requestingHost;
    }


    protected final InetAddress getRequestingSite() {
        return requestingSite;
    }


    protected final int getRequestingPort() {
        return requestingPort;
    }


    protected final String getRequestingProtocol() {
        return requestingProtocol;
    }


    protected final String getRequestingPrompt() {
        return requestingPrompt;
    }


    protected final String getRequestingScheme() {
        return requestingScheme;
    }


    protected PasswordAuthentication getPasswordAuthentication() {
        return null;
    }


    protected URL getRequestingURL () {
        return requestingURL;
    }


    protected RequestorType getRequestorType () {
        return requestingAuthType;
    }

    static String getKey(Authenticator a) {
        return a.key;
    }
    static {
        AuthenticatorKeys.setAuthenticatorKeyAccess(Authenticator::getKey);
    }
}
