/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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




public class MalformedInputException
    extends CharacterCodingException
{

    private static final long serialVersionUID = -3438823399834806194L;

    private int inputLength;


    public MalformedInputException(int inputLength) {
        this.inputLength = inputLength;
    }


    public int getInputLength() {
        return inputLength;
    }


    public String getMessage() {
        return "Input length = " + inputLength;
    }

}
