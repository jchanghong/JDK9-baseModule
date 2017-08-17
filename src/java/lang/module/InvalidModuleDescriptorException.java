/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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


public class InvalidModuleDescriptorException extends RuntimeException {
    private static final long serialVersionUID = 4863390386809347380L;


    public InvalidModuleDescriptorException() {
    }


    public InvalidModuleDescriptorException(String msg) {
        super(msg);
    }
}
