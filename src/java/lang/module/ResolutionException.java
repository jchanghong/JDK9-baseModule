/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.module;


public class ResolutionException extends RuntimeException {
    private static final long serialVersionUID = -1031186845316729450L;


    public ResolutionException() { }


    public ResolutionException(String msg) {
        super(msg);
    }


    public ResolutionException(Throwable cause) {
        super(cause);
    }


    public ResolutionException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
