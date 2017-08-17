/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.invoke;

import jdk.internal.loader.BootLoader;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.FieldVisitor;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.vm.annotation.Stable;
import sun.invoke.util.ValueConversions;
import sun.invoke.util.Wrapper;

import java.lang.invoke.LambdaForm.NamedFunction;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import static java.lang.invoke.LambdaForm.BasicType;
import static java.lang.invoke.LambdaForm.BasicType.*;
import static java.lang.invoke.MethodHandleStatics.*;
import static jdk.internal.org.objectweb.asm.Opcodes.*;


/*non-public*/ abstract class BoundMethodHandle extends MethodHandle {

    /*non-public*/ BoundMethodHandle(MethodType type, LambdaForm form) {
        super(type, form);
        assert(speciesData() == speciesData(form));
    }

    //
    // BMH API and internals
    //

    static BoundMethodHandle bindSingle(MethodType type, LambdaForm form, BasicType xtype, Object x) {
        // for some type signatures, there exist pre-defined concrete BMH classes
        try {
            switch (xtype) {
            case L_TYPE:
                return bindSingle(type, form, x);  // Use known fast path.
            case I_TYPE:
                return (BoundMethodHandle) SpeciesData.EMPTY.extendWith(I_TYPE).constructor().invokeBasic(type, form, ValueConversions.widenSubword(x));
            case J_TYPE:
                return (BoundMethodHandle) SpeciesData.EMPTY.extendWith(J_TYPE).constructor().invokeBasic(type, form, (long) x);
            case F_TYPE:
                return (BoundMethodHandle) SpeciesData.EMPTY.extendWith(F_TYPE).constructor().invokeBasic(type, form, (float) x);
            case D_TYPE:
                return (BoundMethodHandle) SpeciesData.EMPTY.extendWith(D_TYPE).constructor().invokeBasic(type, form, (double) x);
            default : throw newInternalError("unexpected xtype: " + xtype);
            }
        } catch (Throwable t) {
            throw uncaughtException(t);
        }
    }

    /*non-public*/
    LambdaFormEditor editor() {
        return form.editor();
    }

    static BoundMethodHandle bindSingle(MethodType type, LambdaForm form, Object x) {
        return Species_L.make(type, form, x);
    }

    @Override // there is a default binder in the super class, for 'L' types only
    /*non-public*/
    BoundMethodHandle bindArgumentL(int pos, Object value) {
        return editor().bindArgumentL(this, pos, value);
    }
    /*non-public*/
    BoundMethodHandle bindArgumentI(int pos, int value) {
        return editor().bindArgumentI(this, pos, value);
    }
    /*non-public*/
    BoundMethodHandle bindArgumentJ(int pos, long value) {
        return editor().bindArgumentJ(this, pos, value);
    }
    /*non-public*/
    BoundMethodHandle bindArgumentF(int pos, float value) {
        return editor().bindArgumentF(this, pos, value);
    }
    /*non-public*/
    BoundMethodHandle bindArgumentD(int pos, double value) {
        return editor().bindArgumentD(this, pos, value);
    }

    @Override
    BoundMethodHandle rebind() {
        if (!tooComplex()) {
            return this;
        }
        return makeReinvoker(this);
    }

    private boolean tooComplex() {
        return (fieldCount() > FIELD_COUNT_THRESHOLD ||
                form.expressionCount() > FORM_EXPRESSION_THRESHOLD);
    }
    private static final int FIELD_COUNT_THRESHOLD = 12;      // largest convenient BMH field count
    private static final int FORM_EXPRESSION_THRESHOLD = 24;  // largest convenient BMH expression count


    static BoundMethodHandle makeReinvoker(MethodHandle target) {
        LambdaForm form = DelegatingMethodHandle.makeReinvokerForm(
                target, MethodTypeForm.LF_REBIND,
                Species_L.SPECIES_DATA, Species_L.SPECIES_DATA.getterFunction(0));
        return Species_L.make(target.type(), form, target);
    }


    /*non-public*/ abstract SpeciesData speciesData();

    /*non-public*/ static SpeciesData speciesData(LambdaForm form) {
        Object c = form.names[0].constraint;
        if (c instanceof SpeciesData)
            return (SpeciesData) c;
        // if there is no BMH constraint, then use the null constraint
        return SpeciesData.EMPTY;
    }


    /*non-public*/ abstract int fieldCount();

    @Override
    Object internalProperties() {
        return "\n& BMH="+internalValues();
    }

    @Override
    final String internalValues() {
        int count = speciesData().fieldCount();
        if (count == 1) {
            return "[" + arg(0) + "]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < count; ++i) {
            sb.append("\n  ").append(i).append(": ( ").append(arg(i)).append(" )");
        }
        return sb.append("\n]").toString();
    }

    /*non-public*/ final Object arg(int i) {
        try {
            switch (speciesData().fieldType(i)) {
            case L_TYPE: return          speciesData().getters[i].invokeBasic(this);
            case I_TYPE: return (int)    speciesData().getters[i].invokeBasic(this);
            case J_TYPE: return (long)   speciesData().getters[i].invokeBasic(this);
            case F_TYPE: return (float)  speciesData().getters[i].invokeBasic(this);
            case D_TYPE: return (double) speciesData().getters[i].invokeBasic(this);
            }
        } catch (Throwable ex) {
            throw uncaughtException(ex);
        }
        throw new InternalError("unexpected type: " + speciesData().typeChars+"."+i);
    }

    //
    // cloning API
    //

    /*non-public*/ abstract BoundMethodHandle copyWith(MethodType mt, LambdaForm lf);
    /*non-public*/ abstract BoundMethodHandle copyWithExtendL(MethodType mt, LambdaForm lf, Object narg);
    /*non-public*/ abstract BoundMethodHandle copyWithExtendI(MethodType mt, LambdaForm lf, int    narg);
    /*non-public*/ abstract BoundMethodHandle copyWithExtendJ(MethodType mt, LambdaForm lf, long   narg);
    /*non-public*/ abstract BoundMethodHandle copyWithExtendF(MethodType mt, LambdaForm lf, float  narg);
    /*non-public*/ abstract BoundMethodHandle copyWithExtendD(MethodType mt, LambdaForm lf, double narg);

    //
    // concrete BMH classes required to close bootstrap loops
    //

    private  // make it private to force users to access the enclosing class first
    static final class Species_L extends BoundMethodHandle {
        final Object argL0;
        private Species_L(MethodType mt, LambdaForm lf, Object argL0) {
            super(mt, lf);
            this.argL0 = argL0;
        }
        @Override
        /*non-public*/ SpeciesData speciesData() {
            return SPECIES_DATA;
        }
        @Override
        /*non-public*/ int fieldCount() {
            return 1;
        }
        /*non-public*/ static final SpeciesData SPECIES_DATA = new SpeciesData("L", Species_L.class);
        /*non-public*/ static BoundMethodHandle make(MethodType mt, LambdaForm lf, Object argL0) {
            return new Species_L(mt, lf, argL0);
        }
        @Override
        /*non-public*/ final BoundMethodHandle copyWith(MethodType mt, LambdaForm lf) {
            return new Species_L(mt, lf, argL0);
        }
        @Override
        /*non-public*/ final BoundMethodHandle copyWithExtendL(MethodType mt, LambdaForm lf, Object narg) {
            try {
                return (BoundMethodHandle) SPECIES_DATA.extendWith(L_TYPE).constructor().invokeBasic(mt, lf, argL0, narg);
            } catch (Throwable ex) {
                throw uncaughtException(ex);
            }
        }
        @Override
        /*non-public*/ final BoundMethodHandle copyWithExtendI(MethodType mt, LambdaForm lf, int narg) {
            try {
                return (BoundMethodHandle) SPECIES_DATA.extendWith(I_TYPE).constructor().invokeBasic(mt, lf, argL0, narg);
            } catch (Throwable ex) {
                throw uncaughtException(ex);
            }
        }
        @Override
        /*non-public*/ final BoundMethodHandle copyWithExtendJ(MethodType mt, LambdaForm lf, long narg) {
            try {
                return (BoundMethodHandle) SPECIES_DATA.extendWith(J_TYPE).constructor().invokeBasic(mt, lf, argL0, narg);
            } catch (Throwable ex) {
                throw uncaughtException(ex);
            }
        }
        @Override
        /*non-public*/ final BoundMethodHandle copyWithExtendF(MethodType mt, LambdaForm lf, float narg) {
            try {
                return (BoundMethodHandle) SPECIES_DATA.extendWith(F_TYPE).constructor().invokeBasic(mt, lf, argL0, narg);
            } catch (Throwable ex) {
                throw uncaughtException(ex);
            }
        }
        @Override
        /*non-public*/ final BoundMethodHandle copyWithExtendD(MethodType mt, LambdaForm lf, double narg) {
            try {
                return (BoundMethodHandle) SPECIES_DATA.extendWith(D_TYPE).constructor().invokeBasic(mt, lf, argL0, narg);
            } catch (Throwable ex) {
                throw uncaughtException(ex);
            }
        }
    }

    //
    // BMH species meta-data
    //


    static class SpeciesData {
        private final String                             typeChars;
        private final BasicType[]                        typeCodes;
        private final Class<? extends BoundMethodHandle> clazz;
        // Bootstrapping requires circular relations MH -> BMH -> SpeciesData -> MH
        // Therefore, we need a non-final link in the chain.  Use array elements.
        @Stable private final MethodHandle[]             constructor;
        @Stable private final MethodHandle[]             getters;
        @Stable private final NamedFunction[]            nominalGetters;
        @Stable private final SpeciesData[]              extensions;

        /*non-public*/ int fieldCount() {
            return typeCodes.length;
        }
        /*non-public*/ BasicType fieldType(int i) {
            return typeCodes[i];
        }
        /*non-public*/ char fieldTypeChar(int i) {
            return typeChars.charAt(i);
        }
        String fieldSignature() {
            return typeChars;
        }
        public Class<? extends BoundMethodHandle> fieldHolder() {
            return clazz;
        }
        public String toString() {
            return "SpeciesData<"+fieldSignature()+">";
        }


        NamedFunction getterFunction(int i) {
            NamedFunction nf = nominalGetters[i];
            assert(nf.memberDeclaringClassOrNull() == fieldHolder());
            assert(nf.returnType() == fieldType(i));
            return nf;
        }

        NamedFunction[] getterFunctions() {
            return nominalGetters;
        }

        MethodHandle[] getterHandles() { return getters; }

        MethodHandle constructor() {
            return constructor[0];
        }

        static final SpeciesData EMPTY = new SpeciesData("", BoundMethodHandle.class);

        SpeciesData(String types, Class<? extends BoundMethodHandle> clazz) {
            this.typeChars = types;
            this.typeCodes = basicTypes(types);
            this.clazz = clazz;
            if (!INIT_DONE) {
                this.constructor = new MethodHandle[1];  // only one ctor
                this.getters = new MethodHandle[types.length()];
                this.nominalGetters = new NamedFunction[types.length()];
            } else {
                this.constructor = Factory.makeCtors(clazz, types, null);
                this.getters = Factory.makeGetters(clazz, types, null);
                this.nominalGetters = Factory.makeNominalGetters(types, null, this.getters);
            }
            this.extensions = new SpeciesData[ARG_TYPE_LIMIT];
        }

        private void initForBootstrap() {
            assert(!INIT_DONE);
            if (constructor() == null) {
                String types = typeChars;
                CACHE.put(types, this);
                Factory.makeCtors(clazz, types, this.constructor);
                Factory.makeGetters(clazz, types, this.getters);
                Factory.makeNominalGetters(types, this.nominalGetters, this.getters);
            }
        }

        private static final ConcurrentMap<String, SpeciesData> CACHE = new ConcurrentHashMap<>();
        private static final boolean INIT_DONE;  // set after <clinit> finishes...

        SpeciesData extendWith(byte type) {
            return extendWith(BasicType.basicType(type));
        }

        SpeciesData extendWith(BasicType type) {
            int ord = type.ordinal();
            SpeciesData d = extensions[ord];
            if (d != null)  return d;
            extensions[ord] = d = get(typeChars+type.basicTypeChar());
            return d;
        }

        private static SpeciesData get(String types) {
            return CACHE.computeIfAbsent(types, new Function<String, SpeciesData>() {
                @Override
                public SpeciesData apply(String types) {
                    Class<? extends BoundMethodHandle> bmhcl = Factory.getConcreteBMHClass(types);
                    // SpeciesData instantiation may throw VirtualMachineError because of
                    // code cache overflow...
                    SpeciesData speciesData = new SpeciesData(types, bmhcl);
                    // CHM.computeIfAbsent ensures only one SpeciesData will be set
                    // successfully on the concrete BMH class if ever
                    Factory.setSpeciesDataToConcreteBMHClass(bmhcl, speciesData);
                    // the concrete BMH class is published via SpeciesData instance
                    // returned here only after it's SPECIES_DATA field is set
                    return speciesData;
                }
            });
        }


        static boolean speciesDataCachePopulated() {
            Class<BoundMethodHandle> rootCls = BoundMethodHandle.class;
            for (Class<?> c : rootCls.getDeclaredClasses()) {
                if (rootCls.isAssignableFrom(c)) {
                    final Class<? extends BoundMethodHandle> cbmh = c.asSubclass(BoundMethodHandle.class);
                    SpeciesData d = Factory.getSpeciesDataFromConcreteBMHClass(cbmh);
                    assert(d != null) : cbmh.getName();
                    assert(d.clazz == cbmh);
                    assert(CACHE.get(d.typeChars) == d);
                }
            }
            return true;
        }

        static {
            // Pre-fill the BMH species-data cache with EMPTY and all BMH's inner subclasses.
            EMPTY.initForBootstrap();
            Species_L.SPECIES_DATA.initForBootstrap();
            // check that all static SpeciesData instances have been initialized
            assert speciesDataCachePopulated();
            // Note:  Do not simplify this, because INIT_DONE must not be
            // a compile-time constant during bootstrapping.
            INIT_DONE = Boolean.TRUE;
        }
    }

    static SpeciesData getSpeciesData(String types) {
        return SpeciesData.get(types);
    }


    static class Factory {

        static final String JLO_SIG  = "Ljava/lang/Object;";
        static final String JLS_SIG  = "Ljava/lang/String;";
        static final String JLC_SIG  = "Ljava/lang/Class;";
        static final String MH       = "java/lang/invoke/MethodHandle";
        static final String MH_SIG   = "L"+MH+";";
        static final String BMH      = "java/lang/invoke/BoundMethodHandle";
        static final String BMH_SIG  = "L"+BMH+";";
        static final String SPECIES_DATA     = "java/lang/invoke/BoundMethodHandle$SpeciesData";
        static final String SPECIES_DATA_SIG = "L"+SPECIES_DATA+";";
        static final String STABLE_SIG       = "Ljdk/internal/vm/annotation/Stable;";

        static final String SPECIES_PREFIX_NAME = "Species_";
        static final String SPECIES_PREFIX_PATH = BMH + "$" + SPECIES_PREFIX_NAME;
        static final String SPECIES_CLASS_PREFIX = SPECIES_PREFIX_PATH.replace('/', '.');

        static final String BMHSPECIES_DATA_EWI_SIG = "(B)" + SPECIES_DATA_SIG;
        static final String BMHSPECIES_DATA_GFC_SIG = "(" + JLS_SIG + JLC_SIG + ")" + SPECIES_DATA_SIG;
        static final String MYSPECIES_DATA_SIG = "()" + SPECIES_DATA_SIG;
        static final String VOID_SIG   = "()V";
        static final String INT_SIG    = "()I";

        static final String SIG_INCIPIT = "(Ljava/lang/invoke/MethodType;Ljava/lang/invoke/LambdaForm;";

        static final String[] E_THROWABLE = new String[] { "java/lang/Throwable" };

        static final ConcurrentMap<String, Class<? extends BoundMethodHandle>> CLASS_CACHE = new ConcurrentHashMap<>();


        static Class<? extends BoundMethodHandle> getConcreteBMHClass(String types) {
            // CHM.computeIfAbsent ensures generateConcreteBMHClass is called
            // only once per key.
            return CLASS_CACHE.computeIfAbsent(
                types, new Function<String, Class<? extends BoundMethodHandle>>() {
                    @Override
                    public Class<? extends BoundMethodHandle> apply(String types) {
                        String shortTypes = LambdaForm.shortenSignature(types);
                        String className = SPECIES_CLASS_PREFIX + shortTypes;
                        Class<?> c = BootLoader.loadClassOrNull(className);
                        if (TRACE_RESOLVE) {
                            System.out.println("[BMH_RESOLVE] " + shortTypes +
                                    (c != null ? " (success)" : " (fail)") );
                        }
                        if (c != null) {
                            return c.asSubclass(BoundMethodHandle.class);
                        } else {
                            // Not pregenerated, generate the class
                            return generateConcreteBMHClass(shortTypes, types);
                        }
                    }
                });
        }


        static Class<? extends BoundMethodHandle> generateConcreteBMHClass(String shortTypes,
                String types) {
            final String className  = speciesInternalClassName(shortTypes);
            byte[] classFile = generateConcreteBMHClassBytes(shortTypes, types, className);

            // load class
            InvokerBytecodeGenerator.maybeDump(className, classFile);
            Class<? extends BoundMethodHandle> bmhClass =
                UNSAFE.defineClass(className, classFile, 0, classFile.length,
                                   BoundMethodHandle.class.getClassLoader(), null)
                    .asSubclass(BoundMethodHandle.class);

            return bmhClass;
        }

        static String speciesInternalClassName(String shortTypes) {
            return SPECIES_PREFIX_PATH + shortTypes;
        }

        static byte[] generateConcreteBMHClassBytes(final String shortTypes,
                final String types, final String className) {
            final String sourceFile = SPECIES_PREFIX_NAME + shortTypes;

            final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS + ClassWriter.COMPUTE_FRAMES);
            final int NOT_ACC_PUBLIC = 0;  // not ACC_PUBLIC
            cw.visit(V1_6, NOT_ACC_PUBLIC + ACC_FINAL + ACC_SUPER, className, null, BMH, null);
            cw.visitSource(sourceFile, null);

            // emit static types and SPECIES_DATA fields
            FieldVisitor fw = cw.visitField(NOT_ACC_PUBLIC + ACC_STATIC, "SPECIES_DATA", SPECIES_DATA_SIG, null, null);
            fw.visitAnnotation(STABLE_SIG, true);
            fw.visitEnd();

            // emit bound argument fields
            for (int i = 0; i < types.length(); ++i) {
                final char t = types.charAt(i);
                final String fieldName = makeFieldName(types, i);
                final String fieldDesc = t == 'L' ? JLO_SIG : String.valueOf(t);
                cw.visitField(ACC_FINAL, fieldName, fieldDesc, null, null).visitEnd();
            }

            MethodVisitor mv;

            // emit constructor
            mv = cw.visitMethod(ACC_PRIVATE, "<init>", makeSignature(types, true), null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0); // this
            mv.visitVarInsn(ALOAD, 1); // type
            mv.visitVarInsn(ALOAD, 2); // form

            mv.visitMethodInsn(INVOKESPECIAL, BMH, "<init>", makeSignature("", true), false);

            for (int i = 0, j = 0; i < types.length(); ++i, ++j) {
                // i counts the arguments, j counts corresponding argument slots
                char t = types.charAt(i);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(typeLoadOp(t), j + 3); // parameters start at 3
                mv.visitFieldInsn(PUTFIELD, className, makeFieldName(types, i), typeSig(t));
                if (t == 'J' || t == 'D') {
                    ++j; // adjust argument register access
                }
            }

            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            // emit implementation of speciesData()
            mv = cw.visitMethod(NOT_ACC_PUBLIC + ACC_FINAL, "speciesData", MYSPECIES_DATA_SIG, null, null);
            mv.visitCode();
            mv.visitFieldInsn(GETSTATIC, className, "SPECIES_DATA", SPECIES_DATA_SIG);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            // emit implementation of fieldCount()
            mv = cw.visitMethod(NOT_ACC_PUBLIC + ACC_FINAL, "fieldCount", INT_SIG, null, null);
            mv.visitCode();
            int fc = types.length();
            if (fc <= (ICONST_5 - ICONST_0)) {
                mv.visitInsn(ICONST_0 + fc);
            } else {
                mv.visitIntInsn(SIPUSH, fc);
            }
            mv.visitInsn(IRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
            // emit make()  ...factory method wrapping constructor
            mv = cw.visitMethod(NOT_ACC_PUBLIC + ACC_STATIC, "make", makeSignature(types, false), null, null);
            mv.visitCode();
            // make instance
            mv.visitTypeInsn(NEW, className);
            mv.visitInsn(DUP);
            // load mt, lf
            mv.visitVarInsn(ALOAD, 0);  // type
            mv.visitVarInsn(ALOAD, 1);  // form
            // load factory method arguments
            for (int i = 0, j = 0; i < types.length(); ++i, ++j) {
                // i counts the arguments, j counts corresponding argument slots
                char t = types.charAt(i);
                mv.visitVarInsn(typeLoadOp(t), j + 2); // parameters start at 3
                if (t == 'J' || t == 'D') {
                    ++j; // adjust argument register access
                }
            }

            // finally, invoke the constructor and return
            mv.visitMethodInsn(INVOKESPECIAL, className, "<init>", makeSignature(types, true), false);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            // emit copyWith()
            mv = cw.visitMethod(NOT_ACC_PUBLIC + ACC_FINAL, "copyWith", makeSignature("", false), null, null);
            mv.visitCode();
            // make instance
            mv.visitTypeInsn(NEW, className);
            mv.visitInsn(DUP);
            // load mt, lf
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ALOAD, 2);
            // put fields on the stack
            emitPushFields(types, className, mv);
            // finally, invoke the constructor and return
            mv.visitMethodInsn(INVOKESPECIAL, className, "<init>", makeSignature(types, true), false);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            // for each type, emit copyWithExtendT()
            for (BasicType type : BasicType.ARG_TYPES) {
                int ord = type.ordinal();
                char btChar = type.basicTypeChar();
                mv = cw.visitMethod(NOT_ACC_PUBLIC + ACC_FINAL, "copyWithExtend" + btChar, makeSignature(String.valueOf(btChar), false), null, E_THROWABLE);
                mv.visitCode();
                // return SPECIES_DATA.extendWith(t).constructor().invokeBasic(mt, lf, argL0, ..., narg)
                // obtain constructor
                mv.visitFieldInsn(GETSTATIC, className, "SPECIES_DATA", SPECIES_DATA_SIG);
                int iconstInsn = ICONST_0 + ord;
                assert(iconstInsn <= ICONST_5);
                mv.visitInsn(iconstInsn);
                mv.visitMethodInsn(INVOKEVIRTUAL, SPECIES_DATA, "extendWith", BMHSPECIES_DATA_EWI_SIG, false);
                mv.visitMethodInsn(INVOKEVIRTUAL, SPECIES_DATA, "constructor", "()" + MH_SIG, false);
                // load mt, lf
                mv.visitVarInsn(ALOAD, 1);
                mv.visitVarInsn(ALOAD, 2);
                // put fields on the stack
                emitPushFields(types, className, mv);
                // put narg on stack
                mv.visitVarInsn(typeLoadOp(btChar), 3);
                // finally, invoke the constructor and return
                mv.visitMethodInsn(INVOKEVIRTUAL, MH, "invokeBasic", makeSignature(types + btChar, false), false);
                mv.visitInsn(ARETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }

            cw.visitEnd();

            return cw.toByteArray();
        }

        private static int typeLoadOp(char t) {
            switch (t) {
            case 'L': return ALOAD;
            case 'I': return ILOAD;
            case 'J': return LLOAD;
            case 'F': return FLOAD;
            case 'D': return DLOAD;
            default : throw newInternalError("unrecognized type " + t);
            }
        }

        private static void emitPushFields(String types, String className, MethodVisitor mv) {
            for (int i = 0; i < types.length(); ++i) {
                char tc = types.charAt(i);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, makeFieldName(types, i), typeSig(tc));
            }
        }

        static String typeSig(char t) {
            return t == 'L' ? JLO_SIG : String.valueOf(t);
        }

        //
        // Getter MH generation.
        //

        private static MethodHandle makeGetter(Class<?> cbmhClass, String types, int index) {
            String fieldName = makeFieldName(types, index);
            Class<?> fieldType = Wrapper.forBasicType(types.charAt(index)).primitiveType();
            try {
                return LOOKUP.findGetter(cbmhClass, fieldName, fieldType);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw newInternalError(e);
            }
        }

        static MethodHandle[] makeGetters(Class<?> cbmhClass, String types, MethodHandle[] mhs) {
            if (mhs == null)  mhs = new MethodHandle[types.length()];
            for (int i = 0; i < mhs.length; ++i) {
                mhs[i] = makeGetter(cbmhClass, types, i);
                assert(mhs[i].internalMemberName().getDeclaringClass() == cbmhClass);
            }
            return mhs;
        }

        static MethodHandle[] makeCtors(Class<? extends BoundMethodHandle> cbmh, String types, MethodHandle mhs[]) {
            if (mhs == null)  mhs = new MethodHandle[1];
            if (types.equals(""))  return mhs;  // hack for empty BMH species
            mhs[0] = makeCbmhCtor(cbmh, types);
            return mhs;
        }

        static NamedFunction[] makeNominalGetters(String types, NamedFunction[] nfs, MethodHandle[] getters) {
            if (nfs == null)  nfs = new NamedFunction[types.length()];
            for (int i = 0; i < nfs.length; ++i) {
                nfs[i] = new NamedFunction(getters[i]);
            }
            return nfs;
        }

        //
        // Auxiliary methods.
        //

        static SpeciesData getSpeciesDataFromConcreteBMHClass(Class<? extends BoundMethodHandle> cbmh) {
            try {
                Field F_SPECIES_DATA = cbmh.getDeclaredField("SPECIES_DATA");
                return (SpeciesData) F_SPECIES_DATA.get(null);
            } catch (ReflectiveOperationException ex) {
                throw newInternalError(ex);
            }
        }

        static void setSpeciesDataToConcreteBMHClass(Class<? extends BoundMethodHandle> cbmh, SpeciesData speciesData) {
            try {
                Field F_SPECIES_DATA = cbmh.getDeclaredField("SPECIES_DATA");
                // ## FIXME: annotation parser can't create proxy classes until module system is fully initialzed
                // assert F_SPECIES_DATA.getDeclaredAnnotation(Stable.class) != null;
                F_SPECIES_DATA.set(null, speciesData);
            } catch (ReflectiveOperationException ex) {
                throw newInternalError(ex);
            }
        }


        private static String makeFieldName(String types, int index) {
            assert index >= 0 && index < types.length();
            return "arg" + types.charAt(index) + index;
        }

        private static String makeSignature(String types, boolean ctor) {
            StringBuilder buf = new StringBuilder(SIG_INCIPIT);
            for (char c : types.toCharArray()) {
                buf.append(typeSig(c));
            }
            return buf.append(')').append(ctor ? "V" : BMH_SIG).toString();
        }

        static MethodHandle makeCbmhCtor(Class<? extends BoundMethodHandle> cbmh, String types) {
            try {
                return LOOKUP.findStatic(cbmh, "make", MethodType.fromDescriptor(makeSignature(types, false), null));
            } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | TypeNotPresentException e) {
                throw newInternalError(e);
            }
        }
    }

    static final Lookup LOOKUP = Lookup.IMPL_LOOKUP;


    static final SpeciesData SPECIES_DATA = SpeciesData.EMPTY;

    private static final SpeciesData[] SPECIES_DATA_CACHE = new SpeciesData[6];
    private static SpeciesData checkCache(int size, String types) {
        int idx = size - 1;
        SpeciesData data = SPECIES_DATA_CACHE[idx];
        if (data != null)  return data;
        SPECIES_DATA_CACHE[idx] = data = getSpeciesData(types);
        return data;
    }
    static SpeciesData speciesData_L()      { return checkCache(1, "L"); }
    static SpeciesData speciesData_LL()     { return checkCache(2, "LL"); }
    static SpeciesData speciesData_LLL()    { return checkCache(3, "LLL"); }
    static SpeciesData speciesData_LLLL()   { return checkCache(4, "LLLL"); }
    static SpeciesData speciesData_LLLLL()  { return checkCache(5, "LLLLL"); }
}
