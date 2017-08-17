/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

package java.security.cert;


public interface CertPathChecker {


    void init(boolean forward) throws CertPathValidatorException;


    boolean isForwardCheckingSupported();


    void check(Certificate cert) throws CertPathValidatorException;
}
