/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
package java.lang;

import java.lang.StackWalker.StackFrame;
import java.util.EnumSet;
import java.util.Set;

import static java.lang.StackWalker.ExtendedOption.LOCALS_AND_OPERANDS;


/* package-private */
interface LiveStackFrame extends StackFrame {

    public Object[] getMonitors();


    public Object[] getLocals();


    public Object[] getStack();


    public abstract class PrimitiveSlot {

        public abstract int size();


        public int intValue() {
            throw new UnsupportedOperationException("this " + size() + "-byte primitive");
        }


        public long longValue() {
            throw new UnsupportedOperationException("this " + size() + "-byte primitive");
        }
    }



    public static StackWalker getStackWalker() {
        return getStackWalker(EnumSet.noneOf(StackWalker.Option.class));
    }


    public static StackWalker getStackWalker(Set<StackWalker.Option> options) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission("liveStackFrames"));
        }
        return StackWalker.newInstance(options, LOCALS_AND_OPERANDS);
    }
}
