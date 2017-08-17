/*
 * Copyright (c) 1996, 2017, Oracle and/or its affiliates. All rights reserved.
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

package java.security.acl;

import java.util.Enumeration;
import java.security.Principal;


@Deprecated(since="9")
public interface Group extends Principal {


    public boolean addMember(Principal user);


    public boolean removeMember(Principal user);


    public boolean isMember(Principal member);



    public Enumeration<? extends Principal> members();

}
