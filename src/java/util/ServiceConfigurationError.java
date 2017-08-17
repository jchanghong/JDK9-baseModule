/*
 * Copyright (c) 2005, 2017, Oracle and/or its affiliates. All rights reserved.
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

package java.util;




public class ServiceConfigurationError
    extends Error
{

    private static final long serialVersionUID = 74132770414881L;


    public ServiceConfigurationError(String msg) {
        super(msg);
    }


    public ServiceConfigurationError(String msg, Throwable cause) {
        super(msg, cause);
    }

}
