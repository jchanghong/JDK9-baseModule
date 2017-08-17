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
public interface AclEntry extends Cloneable {


    public boolean setPrincipal(Principal user);


    public Principal getPrincipal();


    public void setNegativePermissions();


    public boolean isNegative();


    public boolean addPermission(Permission permission);


    public boolean removePermission(Permission permission);


    public boolean checkPermission(Permission permission);


    public Enumeration<Permission> permissions();


    public String toString();


    public Object clone();
}
