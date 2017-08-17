/*
 * Copyright (c) 2000, 2004, Oracle and/or its affiliates. All rights reserved.
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

package java.util;


public abstract class EventListenerProxy<T extends EventListener>
        implements EventListener {

    private final T listener;


    public EventListenerProxy(T listener) {
        this.listener = listener;
    }


    public T getListener() {
        return this.listener;
    }
}
