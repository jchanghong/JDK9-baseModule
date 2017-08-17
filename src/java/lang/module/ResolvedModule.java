/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.module;

import java.util.Objects;
import java.util.Set;


public final class ResolvedModule {

    private final Configuration cf;
    private final ModuleReference mref;

    ResolvedModule(Configuration cf, ModuleReference mref) {
        this.cf = Objects.requireNonNull(cf);
        this.mref = Objects.requireNonNull(mref);
    }


    public Configuration configuration() {
        return cf;
    }


    public ModuleReference reference() {
        return mref;
    }


    ModuleDescriptor descriptor() {
        return reference().descriptor();
    }


    public String name() {
        return reference().descriptor().name();
    }


    public Set<ResolvedModule> reads() {
        return cf.reads(this);
    }


    @Override
    public int hashCode() {
        return cf.hashCode() ^ mref.hashCode();
    }


    @Override
    public boolean equals(Object ob) {
        if (!(ob instanceof ResolvedModule))
            return false;

        ResolvedModule that = (ResolvedModule) ob;
        return Objects.equals(this.cf, that.cf)
                && Objects.equals(this.mref, that.mref);
    }


    @Override
    public String toString() {
        return System.identityHashCode(cf) + "/" + name();
    }

}
