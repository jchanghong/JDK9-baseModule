/*
 * Copyright (c) 1994, 2011, Oracle and/or its affiliates. All rights reserved.
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


public class InternalError extends VirtualMachineError {
    private static final long serialVersionUID = -9062593416125562365L;


    public InternalError() {
        super();
    }


    public InternalError(String message) {
        super(message);
    }



    public InternalError(String message, Throwable cause) {
        super(message, cause);
    }


    public InternalError(Throwable cause) {
        super(cause);
    }

}
