/*
 * Copyright (c) 1996, 2004, Oracle and/or its affiliates. All rights reserved.
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

package java.io;

import java.io.ObjectOutput;
import java.io.ObjectInput;


public interface Externalizable extends java.io.Serializable {

    void writeExternal(ObjectOutput out) throws IOException;


    void readExternal(ObjectInput in) throws IOException, ClassNotFoundException;
}
