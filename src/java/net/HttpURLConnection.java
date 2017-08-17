/*
 * Copyright (c) 1996, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.InputStream;
import java.io.IOException;
import java.security.Permission;
import java.util.Date;


public abstract class HttpURLConnection extends URLConnection {
    /* instance variables */


    protected String method = "GET";


    protected int chunkLength = -1;


    protected int fixedContentLength = -1;


    protected long fixedContentLengthLong = -1;


    public void setAuthenticator(Authenticator auth) {
        throw new UnsupportedOperationException("Supplying an authenticator"
                    + " is not supported by " + this.getClass());
    }


    public String getHeaderFieldKey (int n) {
        return null;
    }


    public void setFixedLengthStreamingMode (int contentLength) {
        if (connected) {
            throw new IllegalStateException ("Already connected");
        }
        if (chunkLength != -1) {
            throw new IllegalStateException ("Chunked encoding streaming mode set");
        }
        if (contentLength < 0) {
            throw new IllegalArgumentException ("invalid content length");
        }
        fixedContentLength = contentLength;
    }


    public void setFixedLengthStreamingMode(long contentLength) {
        if (connected) {
            throw new IllegalStateException("Already connected");
        }
        if (chunkLength != -1) {
            throw new IllegalStateException(
                "Chunked encoding streaming mode set");
        }
        if (contentLength < 0) {
            throw new IllegalArgumentException("invalid content length");
        }
        fixedContentLengthLong = contentLength;
    }

    /* Default chunk size (including chunk header) if not specified;
     * we want to keep this in sync with the one defined in
     * sun.net.www.http.ChunkedOutputStream
     */
    private static final int DEFAULT_CHUNK_SIZE = 4096;


    public void setChunkedStreamingMode (int chunklen) {
        if (connected) {
            throw new IllegalStateException ("Can't set streaming mode: already connected");
        }
        if (fixedContentLength != -1 || fixedContentLengthLong != -1) {
            throw new IllegalStateException ("Fixed length streaming mode set");
        }
        chunkLength = chunklen <=0? DEFAULT_CHUNK_SIZE : chunklen;
    }


    public String getHeaderField(int n) {
        return null;
    }


    protected int responseCode = -1;


    protected String responseMessage = null;

    /* static variables */

    /* do we automatically follow redirects? The default is true. */
    private static boolean followRedirects = true;


    protected boolean instanceFollowRedirects = followRedirects;

    /* valid HTTP methods */
    private static final String[] methods = {
        "GET", "POST", "HEAD", "OPTIONS", "PUT", "DELETE", "TRACE"
    };


    protected HttpURLConnection (URL u) {
        super(u);
    }


    public static void setFollowRedirects(boolean set) {
        SecurityManager sec = System.getSecurityManager();
        if (sec != null) {
            // seems to be the best check here...
            sec.checkSetFactory();
        }
        followRedirects = set;
    }


    public static boolean getFollowRedirects() {
        return followRedirects;
    }


     public void setInstanceFollowRedirects(boolean followRedirects) {
        instanceFollowRedirects = followRedirects;
     }


     public boolean getInstanceFollowRedirects() {
         return instanceFollowRedirects;
     }


    public void setRequestMethod(String method) throws ProtocolException {
        if (connected) {
            throw new ProtocolException("Can't reset method: already connected");
        }
        // This restriction will prevent people from using this class to
        // experiment w/ new HTTP methods using java.  But it should
        // be placed for security - the request String could be
        // arbitrarily long.

        for (int i = 0; i < methods.length; i++) {
            if (methods[i].equals(method)) {
                if (method.equals("TRACE")) {
                    SecurityManager s = System.getSecurityManager();
                    if (s != null) {
                        s.checkPermission(new NetPermission("allowHttpTrace"));
                    }
                }
                this.method = method;
                return;
            }
        }
        throw new ProtocolException("Invalid HTTP method: " + method);
    }


    public String getRequestMethod() {
        return method;
    }


    public int getResponseCode() throws IOException {
        /*
         * We're got the response code already
         */
        if (responseCode != -1) {
            return responseCode;
        }

        /*
         * Ensure that we have connected to the server. Record
         * exception as we need to re-throw it if there isn't
         * a status line.
         */
        Exception exc = null;
        try {
            getInputStream();
        } catch (Exception e) {
            exc = e;
        }

        /*
         * If we can't a status-line then re-throw any exception
         * that getInputStream threw.
         */
        String statusLine = getHeaderField(0);
        if (statusLine == null) {
            if (exc != null) {
                if (exc instanceof RuntimeException)
                    throw (RuntimeException)exc;
                else
                    throw (IOException)exc;
            }
            return -1;
        }

        /*
         * Examine the status-line - should be formatted as per
         * section 6.1 of RFC 2616 :-
         *
         * Status-Line = HTTP-Version SP Status-Code SP Reason-Phrase
         *
         * If status line can't be parsed return -1.
         */
        if (statusLine.startsWith("HTTP/1.")) {
            int codePos = statusLine.indexOf(' ');
            if (codePos > 0) {

                int phrasePos = statusLine.indexOf(' ', codePos+1);
                if (phrasePos > 0 && phrasePos < statusLine.length()) {
                    responseMessage = statusLine.substring(phrasePos+1);
                }

                // deviation from RFC 2616 - don't reject status line
                // if SP Reason-Phrase is not included.
                if (phrasePos < 0)
                    phrasePos = statusLine.length();

                try {
                    responseCode = Integer.parseInt
                            (statusLine.substring(codePos+1, phrasePos));
                    return responseCode;
                } catch (NumberFormatException e) { }
            }
        }
        return -1;
    }


    public String getResponseMessage() throws IOException {
        getResponseCode();
        return responseMessage;
    }

    @SuppressWarnings("deprecation")
    public long getHeaderFieldDate(String name, long Default) {
        String dateString = getHeaderField(name);
        try {
            if (dateString.indexOf("GMT") == -1) {
                dateString = dateString+" GMT";
            }
            return Date.parse(dateString);
        } catch (Exception e) {
        }
        return Default;
    }



    public abstract void disconnect();


    public abstract boolean usingProxy();


    public Permission getPermission() throws IOException {
        int port = url.getPort();
        port = port < 0 ? 80 : port;
        String host = url.getHost() + ":" + port;
        Permission permission = new SocketPermission(host, "connect");
        return permission;
    }


    public InputStream getErrorStream() {
        return null;
    }



    // REMIND: do we want all these??
    // Others not here that we do want??

    /* 2XX: generally "OK" */


    public static final int HTTP_OK = 200;


    public static final int HTTP_CREATED = 201;


    public static final int HTTP_ACCEPTED = 202;


    public static final int HTTP_NOT_AUTHORITATIVE = 203;


    public static final int HTTP_NO_CONTENT = 204;


    public static final int HTTP_RESET = 205;


    public static final int HTTP_PARTIAL = 206;

    /* 3XX: relocation/redirect */


    public static final int HTTP_MULT_CHOICE = 300;


    public static final int HTTP_MOVED_PERM = 301;


    public static final int HTTP_MOVED_TEMP = 302;


    public static final int HTTP_SEE_OTHER = 303;


    public static final int HTTP_NOT_MODIFIED = 304;


    public static final int HTTP_USE_PROXY = 305;

    /* 4XX: client error */


    public static final int HTTP_BAD_REQUEST = 400;


    public static final int HTTP_UNAUTHORIZED = 401;


    public static final int HTTP_PAYMENT_REQUIRED = 402;


    public static final int HTTP_FORBIDDEN = 403;


    public static final int HTTP_NOT_FOUND = 404;


    public static final int HTTP_BAD_METHOD = 405;


    public static final int HTTP_NOT_ACCEPTABLE = 406;


    public static final int HTTP_PROXY_AUTH = 407;


    public static final int HTTP_CLIENT_TIMEOUT = 408;


    public static final int HTTP_CONFLICT = 409;


    public static final int HTTP_GONE = 410;


    public static final int HTTP_LENGTH_REQUIRED = 411;


    public static final int HTTP_PRECON_FAILED = 412;


    public static final int HTTP_ENTITY_TOO_LARGE = 413;


    public static final int HTTP_REQ_TOO_LONG = 414;


    public static final int HTTP_UNSUPPORTED_TYPE = 415;

    /* 5XX: server error */


    @Deprecated
    public static final int HTTP_SERVER_ERROR = 500;


    public static final int HTTP_INTERNAL_ERROR = 500;


    public static final int HTTP_NOT_IMPLEMENTED = 501;


    public static final int HTTP_BAD_GATEWAY = 502;


    public static final int HTTP_UNAVAILABLE = 503;


    public static final int HTTP_GATEWAY_TIMEOUT = 504;


    public static final int HTTP_VERSION = 505;

}
