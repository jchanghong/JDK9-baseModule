/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.invoke;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;


public class MutableCallSite extends CallSite {

    public MutableCallSite(MethodType type) {
        super(type);
    }


    public MutableCallSite(MethodHandle target) {
        super(target);
    }


    @Override public final MethodHandle getTarget() {
        return target;
    }


    @Override public void setTarget(MethodHandle newTarget) {
        checkTargetChange(this.target, newTarget);
        setTargetNormal(newTarget);
    }


    @Override
    public final MethodHandle dynamicInvoker() {
        return makeDynamicInvoker();
    }


    public static void syncAll(MutableCallSite[] sites) {
        if (sites.length == 0)  return;
        STORE_BARRIER.lazySet(0);
        for (MutableCallSite site : sites) {
            Objects.requireNonNull(site); // trigger NPE on first null
        }
        // FIXME: NYI
    }
    private static final AtomicInteger STORE_BARRIER = new AtomicInteger();
}
