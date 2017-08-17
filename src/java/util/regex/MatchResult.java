/*
 * Copyright (c) 2003, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.util.regex;


public interface MatchResult {


    public int start();


    public int start(int group);


    public int end();


    public int end(int group);


    public String group();


    public String group(int group);


    public int groupCount();

}
