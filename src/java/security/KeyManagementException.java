/*
 * Copyright (c) 1996, 2013, Oracle and/or its affiliates. All rights reserved.
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



public class KeyManagementException extends KeyException {

    private static final long serialVersionUID = 947674216157062695L;


    public KeyManagementException() {
        super();
    }


   public KeyManagementException(String msg) {
        super(msg);
    }


    public KeyManagementException(String message, Throwable cause) {
        super(message, cause);
    }


    public KeyManagementException(Throwable cause) {
        super(cause);
    }
}
