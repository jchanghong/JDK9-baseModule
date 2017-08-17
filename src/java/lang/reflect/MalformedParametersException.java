/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.reflect;


public class MalformedParametersException extends RuntimeException {


    private static final long serialVersionUID = 20130919L;


    public MalformedParametersException() {}


    public MalformedParametersException(String reason) {
        super(reason);
    }
}
