/*
 * Copyright (c) 2007, 2014, Oracle and/or its affiliates. All rights reserved.
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


public enum CRLReason {

    UNSPECIFIED,


    KEY_COMPROMISE,


    CA_COMPROMISE,


    AFFILIATION_CHANGED,


    SUPERSEDED,


    CESSATION_OF_OPERATION,


    CERTIFICATE_HOLD,


    UNUSED,


    REMOVE_FROM_CRL,


    PRIVILEGE_WITHDRAWN,


    AA_COMPROMISE
}
