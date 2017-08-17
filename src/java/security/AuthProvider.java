/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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

package java.security;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import javax.security.auth.callback.CallbackHandler;


public abstract class AuthProvider extends Provider {

    private static final long serialVersionUID = 4197859053084546461L;


    @Deprecated(since="9")
    protected AuthProvider(String name, double version, String info) {
        super(name, Double.toString(version), info);
    }


    protected AuthProvider(String name, String versionStr, String info) {
        super(name, versionStr, info);
    }


    public abstract void login(Subject subject, CallbackHandler handler)
        throws LoginException;


    public abstract void logout() throws LoginException;


    public abstract void setCallbackHandler(CallbackHandler handler);
}
