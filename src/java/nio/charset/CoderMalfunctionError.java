/*
 * Copyright (c) 2001, 2007, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.charset;




public class CoderMalfunctionError
    extends Error
{

    private static final long serialVersionUID = -1151412348057794301L;


    public CoderMalfunctionError(Exception cause) {
        super(cause);
    }

}
