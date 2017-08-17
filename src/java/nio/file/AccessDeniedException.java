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



public class AccessDeniedException
    extends FileSystemException
{
    private static final long serialVersionUID = 4943049599949219617L;


    public AccessDeniedException(String file) {
        super(file);
    }


    public AccessDeniedException(String file, String other, String reason) {
        super(file, other, reason);
    }
}
