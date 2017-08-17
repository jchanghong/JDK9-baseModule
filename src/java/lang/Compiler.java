/*
 * Copyright (c) 1995, 2016, Oracle and/or its affiliates. All rights reserved.
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


@Deprecated(since="9", forRemoval=true)
public final class Compiler  {
    private Compiler() {}               // don't make instances


    public static boolean compileClass(Class<?> clazz) {
        return false;
    }


    public static boolean compileClasses(String string) {
        return false;
    }


    public static Object command(Object any) {
        return null;
    }


    public static void enable() { }


    public static void disable() { }
}
