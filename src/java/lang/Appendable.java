/*
 * Copyright (c) 2003, 2004, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;


public interface Appendable {


    Appendable append(CharSequence csq) throws IOException;


    Appendable append(CharSequence csq, int start, int end) throws IOException;


    Appendable append(char c) throws IOException;
}
