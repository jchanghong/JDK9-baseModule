/*
 * Copyright (c) 2007, 2015, Oracle and/or its affiliates. All rights reserved.
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
package java.net;

import java.util.Properties;
import sun.security.action.GetPropertyAction;



class DefaultDatagramSocketImplFactory
{
    private static final Class<?> prefixImplClass;

    /* java.net.preferIPv4Stack */
    private static final boolean preferIPv4Stack;

    /* True if exclusive binding is on for Windows */
    private static final boolean exclusiveBind;

    static {
        Class<?> prefixImplClassLocal = null;

        Properties props = GetPropertyAction.privilegedGetProperties();
        preferIPv4Stack = Boolean.parseBoolean(
                props.getProperty("java.net.preferIPv4Stack"));

        String exclBindProp = props.getProperty("sun.net.useExclusiveBind", "");
        exclusiveBind = (exclBindProp.isEmpty())
                ? true
                : Boolean.parseBoolean(exclBindProp);

        // impl.prefix
        String prefix = null;
        try {
            prefix = props.getProperty("impl.prefix");
            if (prefix != null)
                prefixImplClassLocal = Class.forName("java.net."+prefix+"DatagramSocketImpl");
        } catch (Exception e) {
            System.err.println("Can't find class: java.net." +
                                prefix +
                                "DatagramSocketImpl: check impl.prefix property");
        }

        prefixImplClass = prefixImplClassLocal;
    }


    static DatagramSocketImpl createDatagramSocketImpl(boolean isMulticast)
        throws SocketException {
        if (prefixImplClass != null) {
            try {
                @SuppressWarnings("deprecation")
                Object result = prefixImplClass.newInstance();
                return (DatagramSocketImpl) result;
            } catch (Exception e) {
                throw new SocketException("can't instantiate DatagramSocketImpl");
            }
        } else {
            if (!preferIPv4Stack && !isMulticast)
                return new DualStackPlainDatagramSocketImpl(exclusiveBind);
            else
                return new TwoStacksPlainDatagramSocketImpl(exclusiveBind && !isMulticast);
        }
    }
}
