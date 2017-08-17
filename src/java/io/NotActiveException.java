/*
 * Copyright (c) 1996, 2005, Oracle and/or its affiliates. All rights reserved.
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

package java.io;


public class NotActiveException extends ObjectStreamException {

    private static final long serialVersionUID = -3893467273049808895L;


    public NotActiveException(String reason) {
        super(reason);
    }


    public NotActiveException() {
        super();
    }
}
