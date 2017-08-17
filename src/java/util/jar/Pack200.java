/*
 * Copyright (c) 2003, 2017, Oracle and/or its affiliates. All rights reserved.
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
package java.util.jar;

import java.util.SortedMap;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;
import java.io.IOException;
import sun.security.action.GetPropertyAction;



public abstract class Pack200 {
    private Pack200() {} //prevent instantiation

    // Static methods of the Pack200 class.

    public static synchronized Packer newPacker() {
        return (Packer) newInstance(PACK_PROVIDER);
    }




    public static Unpacker newUnpacker() {
        return (Unpacker) newInstance(UNPACK_PROVIDER);
    }

    // Interfaces

    public interface Packer {

        String SEGMENT_LIMIT    = "pack.segment.limit";


        String KEEP_FILE_ORDER = "pack.keep.file.order";



        String EFFORT           = "pack.effort";


        String DEFLATE_HINT     = "pack.deflate.hint";


        String MODIFICATION_TIME        = "pack.modification.time";


        String PASS_FILE_PFX            = "pack.pass.file.";

        /// Attribute control.


        String UNKNOWN_ATTRIBUTE        = "pack.unknown.attribute";


        String CLASS_ATTRIBUTE_PFX      = "pack.class.attribute.";


        String FIELD_ATTRIBUTE_PFX      = "pack.field.attribute.";


        String METHOD_ATTRIBUTE_PFX     = "pack.method.attribute.";


        String CODE_ATTRIBUTE_PFX       = "pack.code.attribute.";


        String PROGRESS                 = "pack.progress";


        String KEEP  = "keep";


        String PASS  = "pass";


        String STRIP = "strip";


        String ERROR = "error";


        String TRUE = "true";


        String FALSE = "false";


        String LATEST = "latest";


        SortedMap<String,String> properties();


        void pack(JarFile in, OutputStream out) throws IOException ;


        void pack(JarInputStream in, OutputStream out) throws IOException ;
    }


    public interface Unpacker {


        String KEEP  = "keep";


        String TRUE = "true";


        String FALSE = "false";


        String DEFLATE_HINT      = "unpack.deflate.hint";




        String PROGRESS         = "unpack.progress";


        SortedMap<String,String> properties();


        void unpack(InputStream in, JarOutputStream out) throws IOException;


        void unpack(File in, JarOutputStream out) throws IOException;
    }

    // Private stuff....

    private static final String PACK_PROVIDER = "java.util.jar.Pack200.Packer";
    private static final String UNPACK_PROVIDER = "java.util.jar.Pack200.Unpacker";

    private static Class<?> packerImpl;
    private static Class<?> unpackerImpl;

    private static synchronized Object newInstance(String prop) {
        String implName = "(unknown)";
        try {
            Class<?> impl = (PACK_PROVIDER.equals(prop))? packerImpl: unpackerImpl;
            if (impl == null) {
                // The first time, we must decide which class to use.
                implName = GetPropertyAction.privilegedGetProperty(prop,"");
                if (implName != null && !implName.equals(""))
                    impl = Class.forName(implName);
                else if (PACK_PROVIDER.equals(prop))
                    impl = com.sun.java.util.jar.pack.PackerImpl.class;
                else
                    impl = com.sun.java.util.jar.pack.UnpackerImpl.class;
            }
            // We have a class.  Now instantiate it.
            @SuppressWarnings("deprecation")
            Object result = impl.newInstance();
            return result;
        } catch (ClassNotFoundException e) {
            throw new Error("Class not found: " + implName +
                                ":\ncheck property " + prop +
                                " in your properties file.", e);
        } catch (InstantiationException e) {
            throw new Error("Could not instantiate: " + implName +
                                ":\ncheck property " + prop +
                                " in your properties file.", e);
        } catch (IllegalAccessException e) {
            throw new Error("Cannot access class: " + implName +
                                ":\ncheck property " + prop +
                                " in your properties file.", e);
        }
    }

}
