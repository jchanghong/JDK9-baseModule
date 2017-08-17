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
package java.lang;

import java.lang.module.Configuration;
import java.lang.module.ModuleReference;
import java.net.URI;


class NamedPackage {
    private final String name;
    private final Module module;

    NamedPackage(String pn, Module module) {
        if (pn.isEmpty() && module.isNamed()) {
            throw new InternalError("unnamed package in  " + module);
        }
        this.name = pn.intern();
        this.module = module;
    }


    String packageName() {
        return name;
    }


    Module module() {
        return module;
    }


    URI location() {
        if (module.isNamed() && module.getLayer() != null) {
            Configuration cf = module.getLayer().configuration();
            ModuleReference mref
                = cf.findModule(module.getName()).get().reference();
            return mref.location().orElse(null);
        }
        return null;
    }


    static Package toPackage(String name, Module module) {
        return new Package(name, module);
    }
}
