/*
 * Copyright (c) 1997, 2017, Oracle and/or its affiliates. All rights reserved.
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

package java.security.spec;



public class PKCS8EncodedKeySpec extends EncodedKeySpec {


    public PKCS8EncodedKeySpec(byte[] encodedKey) {
        super(encodedKey);
    }


    public PKCS8EncodedKeySpec(byte[] encodedKey, String algorithm) {
        super(encodedKey, algorithm);
    }


    public byte[] getEncoded() {
        return super.getEncoded();
    }


    public final String getFormat() {
        return "PKCS#8";
    }
}
