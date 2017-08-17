/*
 * Copyright (c) 1997, 2015, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.ref;




public class PhantomReference<T> extends Reference<T> {


    public T get() {
        return null;
    }


    public PhantomReference(T referent, ReferenceQueue<? super T> q) {
        super(referent, q);
    }

}
