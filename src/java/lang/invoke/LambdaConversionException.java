/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.invoke;


public class LambdaConversionException extends Exception {
    private static final long serialVersionUID = 292L + 8L;


    public LambdaConversionException() {
    }


    public LambdaConversionException(String message) {
        super(message);
    }


    public LambdaConversionException(String message, Throwable cause) {
        super(message, cause);
    }


    public LambdaConversionException(Throwable cause) {
        super(cause);
    }


    public LambdaConversionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
