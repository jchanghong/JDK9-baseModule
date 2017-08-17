/*
 * Copyright (c) 2008, 2014, Oracle and/or its affiliates. All rights reserved.
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


public enum PKIXReason implements CertPathValidatorException.Reason {

    NAME_CHAINING,


    INVALID_KEY_USAGE,


    INVALID_POLICY,


    NO_TRUST_ANCHOR,


    UNRECOGNIZED_CRIT_EXT,


    NOT_CA_CERT,


    PATH_TOO_LONG,


    INVALID_NAME
}
