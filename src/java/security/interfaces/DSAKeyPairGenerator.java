/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.security.interfaces;

import java.security.*;


public interface DSAKeyPairGenerator {


   public void initialize(DSAParams params, SecureRandom random)
   throws InvalidParameterException;


    public void initialize(int modlen, boolean genParams, SecureRandom random)
    throws InvalidParameterException;
}
