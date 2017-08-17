/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.text;

import java.io.InvalidObjectException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;



public interface AttributedCharacterIterator extends CharacterIterator {



    public static class Attribute implements Serializable {


        private String name;

        // table of all instances in this class, used by readResolve
        private static final Map<String, Attribute> instanceMap = new HashMap<>(7);


        protected Attribute(String name) {
            this.name = name;
            if (this.getClass() == Attribute.class) {
                instanceMap.put(name, this);
            }
        }


        public final boolean equals(Object obj) {
            return super.equals(obj);
        }


        public final int hashCode() {
            return super.hashCode();
        }


        public String toString() {
            return getClass().getName() + "(" + name + ")";
        }


        protected String getName() {
            return name;
        }


        protected Object readResolve() throws InvalidObjectException {
            if (this.getClass() != Attribute.class) {
                throw new InvalidObjectException("subclass didn't correctly implement readResolve");
            }

            Attribute instance = instanceMap.get(getName());
            if (instance != null) {
                return instance;
            } else {
                throw new InvalidObjectException("unknown attribute name");
            }
        }


        public static final Attribute LANGUAGE = new Attribute("language");


        public static final Attribute READING = new Attribute("reading");


        public static final Attribute INPUT_METHOD_SEGMENT = new Attribute("input_method_segment");

        // make sure the serial version doesn't change between compiler versions
        private static final long serialVersionUID = -9142742483513960612L;

    };


    public int getRunStart();


    public int getRunStart(Attribute attribute);


    public int getRunStart(Set<? extends Attribute> attributes);


    public int getRunLimit();


    public int getRunLimit(Attribute attribute);


    public int getRunLimit(Set<? extends Attribute> attributes);


    public Map<Attribute,Object> getAttributes();


    public Object getAttribute(Attribute attribute);


    public Set<Attribute> getAllAttributeKeys();
};
