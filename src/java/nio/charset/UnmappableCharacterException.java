/*
 * Copyright (c) 2001, 2013, Oracle and/or its affiliates. All rights reserved.
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




public class UnmappableCharacterException
    extends CharacterCodingException
{

    private static final long serialVersionUID = -7026962371537706123L;

    private int inputLength;


    public UnmappableCharacterException(int inputLength) {
        this.inputLength = inputLength;
    }


    public int getInputLength() {
        return inputLength;
    }


    public String getMessage() {
        return "Input length = " + inputLength;
    }

}
