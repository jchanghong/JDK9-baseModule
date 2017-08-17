/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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

import java.security.PublicKey;
import java.security.spec.ECPoint;


public interface ECPublicKey extends PublicKey, ECKey {


    static final long serialVersionUID = -3314988629879632826L;


    ECPoint getW();
}
