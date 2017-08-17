/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.channels;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.io.IOException;


public abstract class MembershipKey {


    protected MembershipKey() {
    }


    public abstract boolean isValid();


    public abstract void drop();


    public abstract MembershipKey block(InetAddress source) throws IOException;


    public abstract MembershipKey unblock(InetAddress source);


    public abstract MulticastChannel channel();


    public abstract InetAddress group();


    public abstract NetworkInterface networkInterface();


    public abstract InetAddress sourceAddress();
}
