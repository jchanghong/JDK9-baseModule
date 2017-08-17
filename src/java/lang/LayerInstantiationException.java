/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

package java.lang;


public class LayerInstantiationException extends RuntimeException {
    private static final long serialVersionUID = -906239691613568347L;


    public LayerInstantiationException() {
    }


    public LayerInstantiationException(String msg) {
        super(msg);
    }


    public LayerInstantiationException(Throwable cause) {
        super(cause);
    }


    public LayerInstantiationException(String msg, Throwable cause) {
        super(msg, cause);
    }
}

