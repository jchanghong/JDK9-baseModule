/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.file;



public class NotLinkException
    extends FileSystemException
{
    static final long serialVersionUID = -388655596416518021L;


    public NotLinkException(String file) {
        super(file);
    }


    public NotLinkException(String file, String other, String reason) {
        super(file, other, reason);
    }
}
