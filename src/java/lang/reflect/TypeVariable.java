/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.reflect;


public interface TypeVariable<D extends GenericDeclaration> extends Type, AnnotatedElement {

    Type[] getBounds();


    D getGenericDeclaration();


    String getName();


     AnnotatedType[] getAnnotatedBounds();
}
