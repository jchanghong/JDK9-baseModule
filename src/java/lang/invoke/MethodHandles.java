/*
 * Copyright (c) 2008, 2017, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.misc.SharedSecrets;
import jdk.internal.module.IllegalAccessLogger;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.reflect.CallerSensitive;
import jdk.internal.reflect.Reflection;
import jdk.internal.vm.annotation.ForceInline;
import sun.invoke.util.ValueConversions;
import sun.invoke.util.VerifyAccess;
import sun.invoke.util.Wrapper;
import sun.reflect.misc.ReflectUtil;
import sun.security.util.SecurityConstants;

import java.lang.invoke.LambdaForm.BasicType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ReflectPermission;
import java.nio.ByteOrder;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.invoke.MethodHandleImpl.Intrinsic;
import static java.lang.invoke.MethodHandleNatives.Constants.*;
import static java.lang.invoke.MethodHandleStatics.newIllegalArgumentException;
import static java.lang.invoke.MethodType.methodType;


public class MethodHandles {

    private MethodHandles() { }  // do not instantiate

    static final MemberName.Factory IMPL_NAMES = MemberName.getFactory();

    // See IMPL_LOOKUP below.

    //// Method handle creation from ordinary methods.


    @CallerSensitive
    @ForceInline // to ensure Reflection.getCallerClass optimization
    public static Lookup lookup() {
        return new Lookup(Reflection.getCallerClass());
    }


    @CallerSensitive
    private static Lookup reflected$lookup() {
        Class<?> caller = Reflection.getCallerClass();
        if (caller.getClassLoader() == null) {
            throw newIllegalArgumentException("illegal lookupClass: "+caller);
        }
        return new Lookup(caller);
    }


    public static Lookup publicLookup() {
        return Lookup.PUBLIC_LOOKUP;
    }


    public static Lookup privateLookupIn(Class<?> targetClass, Lookup lookup) throws IllegalAccessException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) sm.checkPermission(ACCESS_PERMISSION);
        if (targetClass.isPrimitive())
            throw new IllegalArgumentException(targetClass + " is a primitive class");
        if (targetClass.isArray())
            throw new IllegalArgumentException(targetClass + " is an array class");
        Module targetModule = targetClass.getModule();
        Module callerModule = lookup.lookupClass().getModule();
        if (!callerModule.canRead(targetModule))
            throw new IllegalAccessException(callerModule + " does not read " + targetModule);
        if (targetModule.isNamed()) {
            String pn = targetClass.getPackageName();
            assert pn.length() > 0 : "unnamed package cannot be in named module";
            if (!targetModule.isOpen(pn, callerModule))
                throw new IllegalAccessException(targetModule + " does not open " + pn + " to " + callerModule);
        }
        if ((lookup.lookupModes() & Lookup.MODULE) == 0)
            throw new IllegalAccessException("lookup does not have MODULE lookup mode");
        if (!callerModule.isNamed() && targetModule.isNamed()) {
            IllegalAccessLogger logger = IllegalAccessLogger.illegalAccessLogger();
            if (logger != null) {
                logger.logIfOpenedForIllegalAccess(lookup, targetClass);
            }
        }
        return new Lookup(targetClass);
    }


    public static <T extends Member> T
    reflectAs(Class<T> expected, MethodHandle target) {
        SecurityManager smgr = System.getSecurityManager();
        if (smgr != null)  smgr.checkPermission(ACCESS_PERMISSION);
        Lookup lookup = Lookup.IMPL_LOOKUP;  // use maximally privileged lookup
        return lookup.revealDirect(target).reflectAs(expected, lookup);
    }
    // Copied from AccessibleObject, as used by Method.setAccessible, etc.:
    private static final java.security.Permission ACCESS_PERMISSION =
        new ReflectPermission("suppressAccessChecks");


    public static final
    class Lookup {

        private final Class<?> lookupClass;


        private final int allowedModes;


        public static final int PUBLIC = Modifier.PUBLIC;


        public static final int PRIVATE = Modifier.PRIVATE;


        public static final int PROTECTED = Modifier.PROTECTED;


        public static final int PACKAGE = Modifier.STATIC;


        public static final int MODULE = PACKAGE << 1;


        public static final int UNCONDITIONAL = PACKAGE << 2;

        private static final int ALL_MODES = (PUBLIC | PRIVATE | PROTECTED | PACKAGE | MODULE | UNCONDITIONAL);
        private static final int FULL_POWER_MODES = (ALL_MODES & ~UNCONDITIONAL);
        private static final int TRUSTED   = -1;

        private static int fixmods(int mods) {
            mods &= (ALL_MODES - PACKAGE - MODULE - UNCONDITIONAL);
            return (mods != 0) ? mods : (PACKAGE | MODULE | UNCONDITIONAL);
        }


        public Class<?> lookupClass() {
            return lookupClass;
        }

        // This is just for calling out to MethodHandleImpl.
        private Class<?> lookupClassOrNull() {
            return (allowedModes == TRUSTED) ? null : lookupClass;
        }


        public int lookupModes() {
            return allowedModes & ALL_MODES;
        }


        Lookup(Class<?> lookupClass) {
            this(lookupClass, FULL_POWER_MODES);
            // make sure we haven't accidentally picked up a privileged class:
            checkUnprivilegedlookupClass(lookupClass);
        }

        private Lookup(Class<?> lookupClass, int allowedModes) {
            this.lookupClass = lookupClass;
            this.allowedModes = allowedModes;
        }


        public Lookup in(Class<?> requestedLookupClass) {
            Objects.requireNonNull(requestedLookupClass);
            if (allowedModes == TRUSTED)  // IMPL_LOOKUP can make any lookup at all
                return new Lookup(requestedLookupClass, FULL_POWER_MODES);
            if (requestedLookupClass == this.lookupClass)
                return this;  // keep same capabilities
            int newModes = (allowedModes & FULL_POWER_MODES);
            if (!VerifyAccess.isSameModule(this.lookupClass, requestedLookupClass)) {
                // Need to drop all access when teleporting from a named module to another
                // module. The exception is publicLookup where PUBLIC is not lost.
                if (this.lookupClass.getModule().isNamed()
                    && (this.allowedModes & UNCONDITIONAL) == 0)
                    newModes = 0;
                else
                    newModes &= ~(MODULE|PACKAGE|PRIVATE|PROTECTED);
            }
            if ((newModes & PACKAGE) != 0
                && !VerifyAccess.isSamePackage(this.lookupClass, requestedLookupClass)) {
                newModes &= ~(PACKAGE|PRIVATE|PROTECTED);
            }
            // Allow nestmate lookups to be created without special privilege:
            if ((newModes & PRIVATE) != 0
                && !VerifyAccess.isSamePackageMember(this.lookupClass, requestedLookupClass)) {
                newModes &= ~(PRIVATE|PROTECTED);
            }
            if ((newModes & PUBLIC) != 0
                && !VerifyAccess.isClassAccessible(requestedLookupClass, this.lookupClass, allowedModes)) {
                // The requested class it not accessible from the lookup class.
                // No permissions.
                newModes = 0;
            }

            checkUnprivilegedlookupClass(requestedLookupClass);
            return new Lookup(requestedLookupClass, newModes);
        }



        public Lookup dropLookupMode(int modeToDrop) {
            int oldModes = lookupModes();
            int newModes = oldModes & ~(modeToDrop | PROTECTED | UNCONDITIONAL);
            switch (modeToDrop) {
                case PUBLIC: newModes &= ~(ALL_MODES); break;
                case MODULE: newModes &= ~(PACKAGE | PRIVATE); break;
                case PACKAGE: newModes &= ~(PRIVATE); break;
                case PROTECTED:
                case PRIVATE:
                case UNCONDITIONAL: break;
                default: throw new IllegalArgumentException(modeToDrop + " is not a valid mode to drop");
            }
            if (newModes == oldModes) return this;  // return self if no change
            return new Lookup(lookupClass(), newModes);
        }


        public Class<?> defineClass(byte[] bytes) throws IllegalAccessException {
            SecurityManager sm = System.getSecurityManager();
            if (sm != null)
                sm.checkPermission(new RuntimePermission("defineClass"));
            if ((lookupModes() & PACKAGE) == 0)
                throw new IllegalAccessException("Lookup does not have PACKAGE access");
            assert (lookupModes() & (MODULE|PUBLIC)) != 0;

            // parse class bytes to get class name (in internal form)
            bytes = bytes.clone();
            String name;
            try {
                ClassReader reader = new ClassReader(bytes);
                name = reader.getClassName();
            } catch (RuntimeException e) {
                // ASM exceptions are poorly specified
                ClassFormatError cfe = new ClassFormatError();
                cfe.initCause(e);
                throw cfe;
            }

            // get package and class name in binary form
            String cn, pn;
            int index = name.lastIndexOf('/');
            if (index == -1) {
                cn = name;
                pn = "";
            } else {
                cn = name.replace('/', '.');
                pn = cn.substring(0, index);
            }
            if (!pn.equals(lookupClass.getPackageName())) {
                throw new IllegalArgumentException("Class not in same package as lookup class");
            }

            // invoke the class loader's defineClass method
            ClassLoader loader = lookupClass.getClassLoader();
            ProtectionDomain pd = (loader != null) ? lookupClassProtectionDomain() : null;
            String source = "__Lookup_defineClass__";
            Class<?> clazz = SharedSecrets.getJavaLangAccess().defineClass(loader, cn, bytes, pd, source);
            assert clazz.getClassLoader() == lookupClass.getClassLoader()
                    && clazz.getPackageName().equals(lookupClass.getPackageName())
                    && protectionDomain(clazz) == lookupClassProtectionDomain();
            return clazz;
        }

        private ProtectionDomain lookupClassProtectionDomain() {
            ProtectionDomain pd = cachedProtectionDomain;
            if (pd == null) {
                cachedProtectionDomain = pd = protectionDomain(lookupClass);
            }
            return pd;
        }

        private ProtectionDomain protectionDomain(Class<?> clazz) {
            PrivilegedAction<ProtectionDomain> pa = clazz::getProtectionDomain;
            return AccessController.doPrivileged(pa);
        }

        // cached protection domain
        private volatile ProtectionDomain cachedProtectionDomain;


        // Make sure outer class is initialized first.
        static { IMPL_NAMES.getClass(); }


        static final Lookup IMPL_LOOKUP = new Lookup(Object.class, TRUSTED);


        static final Lookup PUBLIC_LOOKUP = new Lookup(Object.class, (PUBLIC|UNCONDITIONAL));

        private static void checkUnprivilegedlookupClass(Class<?> lookupClass) {
            String name = lookupClass.getName();
            if (name.startsWith("java.lang.invoke."))
                throw newIllegalArgumentException("illegal lookupClass: "+lookupClass);
        }


        @Override
        public String toString() {
            String cname = lookupClass.getName();
            switch (allowedModes) {
            case 0:  // no privileges
                return cname + "/noaccess";
            case PUBLIC:
                return cname + "/public";
            case PUBLIC|UNCONDITIONAL:
                return cname  + "/publicLookup";
            case PUBLIC|MODULE:
                return cname + "/module";
            case PUBLIC|MODULE|PACKAGE:
                return cname + "/package";
            case FULL_POWER_MODES & ~PROTECTED:
                return cname + "/private";
            case FULL_POWER_MODES:
                return cname;
            case TRUSTED:
                return "/trusted";  // internal only; not exported
            default:  // Should not happen, but it's a bitfield...
                cname = cname + "/" + Integer.toHexString(allowedModes);
                assert(false) : cname;
                return cname;
            }
        }


        public
        MethodHandle findStatic(Class<?> refc, String name, MethodType type) throws NoSuchMethodException, IllegalAccessException {
            MemberName method = resolveOrFail(REF_invokeStatic, refc, name, type);
            return getDirectMethod(REF_invokeStatic, refc, method, findBoundCallerClass(method));
        }


        public MethodHandle findVirtual(Class<?> refc, String name, MethodType type) throws NoSuchMethodException, IllegalAccessException {
            if (refc == MethodHandle.class) {
                MethodHandle mh = findVirtualForMH(name, type);
                if (mh != null)  return mh;
            } else if (refc == VarHandle.class) {
                MethodHandle mh = findVirtualForVH(name, type);
                if (mh != null)  return mh;
            }
            byte refKind = (refc.isInterface() ? REF_invokeInterface : REF_invokeVirtual);
            MemberName method = resolveOrFail(refKind, refc, name, type);
            return getDirectMethod(refKind, refc, method, findBoundCallerClass(method));
        }
        private MethodHandle findVirtualForMH(String name, MethodType type) {
            // these names require special lookups because of the implicit MethodType argument
            if ("invoke".equals(name))
                return invoker(type);
            if ("invokeExact".equals(name))
                return exactInvoker(type);
            assert(!MemberName.isMethodHandleInvokeName(name));
            return null;
        }
        private MethodHandle findVirtualForVH(String name, MethodType type) {
            try {
                return varHandleInvoker(VarHandle.AccessMode.valueFromMethodName(name), type);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }


        public MethodHandle findConstructor(Class<?> refc, MethodType type) throws NoSuchMethodException, IllegalAccessException {
            if (refc.isArray()) {
                throw new NoSuchMethodException("no constructor for array class: " + refc.getName());
            }
            String name = "<init>";
            MemberName ctor = resolveOrFail(REF_newInvokeSpecial, refc, name, type);
            return getDirectConstructor(refc, ctor);
        }


        public Class<?> findClass(String targetName) throws ClassNotFoundException, IllegalAccessException {
            Class<?> targetClass = Class.forName(targetName, false, lookupClass.getClassLoader());
            return accessClass(targetClass);
        }


        public Class<?> accessClass(Class<?> targetClass) throws IllegalAccessException {
            if (!VerifyAccess.isClassAccessible(targetClass, lookupClass, allowedModes)) {
                throw new MemberName(targetClass).makeAccessException("access violation", this);
            }
            checkSecurityManager(targetClass, null);
            return targetClass;
        }


        public MethodHandle findSpecial(Class<?> refc, String name, MethodType type,
                                        Class<?> specialCaller) throws NoSuchMethodException, IllegalAccessException {
            checkSpecialCaller(specialCaller, refc);
            Lookup specialLookup = this.in(specialCaller);
            MemberName method = specialLookup.resolveOrFail(REF_invokeSpecial, refc, name, type);
            return specialLookup.getDirectMethod(REF_invokeSpecial, refc, method, findBoundCallerClass(method));
        }


        public MethodHandle findGetter(Class<?> refc, String name, Class<?> type) throws NoSuchFieldException, IllegalAccessException {
            MemberName field = resolveOrFail(REF_getField, refc, name, type);
            return getDirectField(REF_getField, refc, field);
        }


        public MethodHandle findSetter(Class<?> refc, String name, Class<?> type) throws NoSuchFieldException, IllegalAccessException {
            MemberName field = resolveOrFail(REF_putField, refc, name, type);
            return getDirectField(REF_putField, refc, field);
        }


        public VarHandle findVarHandle(Class<?> recv, String name, Class<?> type) throws NoSuchFieldException, IllegalAccessException {
            MemberName getField = resolveOrFail(REF_getField, recv, name, type);
            MemberName putField = resolveOrFail(REF_putField, recv, name, type);
            return getFieldVarHandle(REF_getField, REF_putField, recv, getField, putField);
        }


        public MethodHandle findStaticGetter(Class<?> refc, String name, Class<?> type) throws NoSuchFieldException, IllegalAccessException {
            MemberName field = resolveOrFail(REF_getStatic, refc, name, type);
            return getDirectField(REF_getStatic, refc, field);
        }


        public MethodHandle findStaticSetter(Class<?> refc, String name, Class<?> type) throws NoSuchFieldException, IllegalAccessException {
            MemberName field = resolveOrFail(REF_putStatic, refc, name, type);
            return getDirectField(REF_putStatic, refc, field);
        }


        public VarHandle findStaticVarHandle(Class<?> decl, String name, Class<?> type) throws NoSuchFieldException, IllegalAccessException {
            MemberName getField = resolveOrFail(REF_getStatic, decl, name, type);
            MemberName putField = resolveOrFail(REF_putStatic, decl, name, type);
            return getFieldVarHandle(REF_getStatic, REF_putStatic, decl, getField, putField);
        }


        public MethodHandle bind(Object receiver, String name, MethodType type) throws NoSuchMethodException, IllegalAccessException {
            Class<? extends Object> refc = receiver.getClass(); // may get NPE
            MemberName method = resolveOrFail(REF_invokeSpecial, refc, name, type);
            MethodHandle mh = getDirectMethodNoRestrictInvokeSpecial(refc, method, findBoundCallerClass(method));
            if (!mh.type().leadingReferenceParameter().isAssignableFrom(receiver.getClass())) {
                throw new IllegalAccessException("The restricted defining class " +
                                                 mh.type().leadingReferenceParameter().getName() +
                                                 " is not assignable from receiver class " +
                                                 receiver.getClass().getName());
            }
            return mh.bindArgumentL(0, receiver).setVarargs(method);
        }


        public MethodHandle unreflect(Method m) throws IllegalAccessException {
            if (m.getDeclaringClass() == MethodHandle.class) {
                MethodHandle mh = unreflectForMH(m);
                if (mh != null)  return mh;
            }
            if (m.getDeclaringClass() == VarHandle.class) {
                MethodHandle mh = unreflectForVH(m);
                if (mh != null)  return mh;
            }
            MemberName method = new MemberName(m);
            byte refKind = method.getReferenceKind();
            if (refKind == REF_invokeSpecial)
                refKind = REF_invokeVirtual;
            assert(method.isMethod());
            @SuppressWarnings("deprecation")
            Lookup lookup = m.isAccessible() ? IMPL_LOOKUP : this;
            return lookup.getDirectMethodNoSecurityManager(refKind, method.getDeclaringClass(), method, findBoundCallerClass(method));
        }
        private MethodHandle unreflectForMH(Method m) {
            // these names require special lookups because they throw UnsupportedOperationException
            if (MemberName.isMethodHandleInvokeName(m.getName()))
                return MethodHandleImpl.fakeMethodHandleInvoke(new MemberName(m));
            return null;
        }
        private MethodHandle unreflectForVH(Method m) {
            // these names require special lookups because they throw UnsupportedOperationException
            if (MemberName.isVarHandleMethodInvokeName(m.getName()))
                return MethodHandleImpl.fakeVarHandleInvoke(new MemberName(m));
            return null;
        }


        public MethodHandle unreflectSpecial(Method m, Class<?> specialCaller) throws IllegalAccessException {
            checkSpecialCaller(specialCaller, null);
            Lookup specialLookup = this.in(specialCaller);
            MemberName method = new MemberName(m, true);
            assert(method.isMethod());
            // ignore m.isAccessible:  this is a new kind of access
            return specialLookup.getDirectMethodNoSecurityManager(REF_invokeSpecial, method.getDeclaringClass(), method, findBoundCallerClass(method));
        }


        public MethodHandle unreflectConstructor(Constructor<?> c) throws IllegalAccessException {
            MemberName ctor = new MemberName(c);
            assert(ctor.isConstructor());
            @SuppressWarnings("deprecation")
            Lookup lookup = c.isAccessible() ? IMPL_LOOKUP : this;
            return lookup.getDirectConstructorNoSecurityManager(ctor.getDeclaringClass(), ctor);
        }


        public MethodHandle unreflectGetter(Field f) throws IllegalAccessException {
            return unreflectField(f, false);
        }
        private MethodHandle unreflectField(Field f, boolean isSetter) throws IllegalAccessException {
            MemberName field = new MemberName(f, isSetter);
            assert(isSetter
                    ? MethodHandleNatives.refKindIsSetter(field.getReferenceKind())
                    : MethodHandleNatives.refKindIsGetter(field.getReferenceKind()));
            @SuppressWarnings("deprecation")
            Lookup lookup = f.isAccessible() ? IMPL_LOOKUP : this;
            return lookup.getDirectFieldNoSecurityManager(field.getReferenceKind(), f.getDeclaringClass(), field);
        }


        public MethodHandle unreflectSetter(Field f) throws IllegalAccessException {
            return unreflectField(f, true);
        }


        public VarHandle unreflectVarHandle(Field f) throws IllegalAccessException {
            MemberName getField = new MemberName(f, false);
            MemberName putField = new MemberName(f, true);
            return getFieldVarHandleNoSecurityManager(getField.getReferenceKind(), putField.getReferenceKind(),
                                                      f.getDeclaringClass(), getField, putField);
        }


        public MethodHandleInfo revealDirect(MethodHandle target) {
            MemberName member = target.internalMemberName();
            if (member == null || (!member.isResolved() &&
                                   !member.isMethodHandleInvoke() &&
                                   !member.isVarHandleMethodInvoke()))
                throw newIllegalArgumentException("not a direct method handle");
            Class<?> defc = member.getDeclaringClass();
            byte refKind = member.getReferenceKind();
            assert(MethodHandleNatives.refKindIsValid(refKind));
            if (refKind == REF_invokeSpecial && !target.isInvokeSpecial())
                // Devirtualized method invocation is usually formally virtual.
                // To avoid creating extra MemberName objects for this common case,
                // we encode this extra degree of freedom using MH.isInvokeSpecial.
                refKind = REF_invokeVirtual;
            if (refKind == REF_invokeVirtual && defc.isInterface())
                // Symbolic reference is through interface but resolves to Object method (toString, etc.)
                refKind = REF_invokeInterface;
            // Check SM permissions and member access before cracking.
            try {
                checkAccess(refKind, defc, member);
                checkSecurityManager(defc, member);
            } catch (IllegalAccessException ex) {
                throw new IllegalArgumentException(ex);
            }
            if (allowedModes != TRUSTED && member.isCallerSensitive()) {
                Class<?> callerClass = target.internalCallerClass();
                if (!hasPrivateAccess() || callerClass != lookupClass())
                    throw new IllegalArgumentException("method handle is caller sensitive: "+callerClass);
            }
            // Produce the handle to the results.
            return new InfoFromMemberName(this, member, refKind);
        }

        /// Helper methods, all package-private.

        MemberName resolveOrFail(byte refKind, Class<?> refc, String name, Class<?> type) throws NoSuchFieldException, IllegalAccessException {
            checkSymbolicClass(refc);  // do this before attempting to resolve
            Objects.requireNonNull(name);
            Objects.requireNonNull(type);
            return IMPL_NAMES.resolveOrFail(refKind, new MemberName(refc, name, type, refKind), lookupClassOrNull(),
                                            NoSuchFieldException.class);
        }

        MemberName resolveOrFail(byte refKind, Class<?> refc, String name, MethodType type) throws NoSuchMethodException, IllegalAccessException {
            checkSymbolicClass(refc);  // do this before attempting to resolve
            Objects.requireNonNull(name);
            Objects.requireNonNull(type);
            checkMethodName(refKind, name);  // NPE check on name
            return IMPL_NAMES.resolveOrFail(refKind, new MemberName(refc, name, type, refKind), lookupClassOrNull(),
                                            NoSuchMethodException.class);
        }

        MemberName resolveOrFail(byte refKind, MemberName member) throws ReflectiveOperationException {
            checkSymbolicClass(member.getDeclaringClass());  // do this before attempting to resolve
            Objects.requireNonNull(member.getName());
            Objects.requireNonNull(member.getType());
            return IMPL_NAMES.resolveOrFail(refKind, member, lookupClassOrNull(),
                                            ReflectiveOperationException.class);
        }

        void checkSymbolicClass(Class<?> refc) throws IllegalAccessException {
            Objects.requireNonNull(refc);
            Class<?> caller = lookupClassOrNull();
            if (caller != null && !VerifyAccess.isClassAccessible(refc, caller, allowedModes))
                throw new MemberName(refc).makeAccessException("symbolic reference class is not accessible", this);
        }


        void checkMethodName(byte refKind, String name) throws NoSuchMethodException {
            if (name.startsWith("<") && refKind != REF_newInvokeSpecial)
                throw new NoSuchMethodException("illegal method name: "+name);
        }



        Class<?> findBoundCallerClass(MemberName m) throws IllegalAccessException {
            Class<?> callerClass = null;
            if (MethodHandleNatives.isCallerSensitive(m)) {
                // Only lookups with private access are allowed to resolve caller-sensitive methods
                if (hasPrivateAccess()) {
                    callerClass = lookupClass;
                } else {
                    throw new IllegalAccessException("Attempt to lookup caller-sensitive method using restricted lookup object");
                }
            }
            return callerClass;
        }


        public boolean hasPrivateAccess() {
            return (allowedModes & PRIVATE) != 0;
        }


        void checkSecurityManager(Class<?> refc, MemberName m) {
            SecurityManager smgr = System.getSecurityManager();
            if (smgr == null)  return;
            if (allowedModes == TRUSTED)  return;

            // Step 1:
            boolean fullPowerLookup = hasPrivateAccess();
            if (!fullPowerLookup ||
                !VerifyAccess.classLoaderIsAncestor(lookupClass, refc)) {
                ReflectUtil.checkPackageAccess(refc);
            }

            if (m == null) {  // findClass or accessClass
                // Step 2b:
                if (!fullPowerLookup) {
                    smgr.checkPermission(SecurityConstants.GET_CLASSLOADER_PERMISSION);
                }
                return;
            }

            // Step 2a:
            if (m.isPublic()) return;
            if (!fullPowerLookup) {
                smgr.checkPermission(SecurityConstants.CHECK_MEMBER_ACCESS_PERMISSION);
            }

            // Step 3:
            Class<?> defc = m.getDeclaringClass();
            if (!fullPowerLookup && defc != refc) {
                ReflectUtil.checkPackageAccess(defc);
            }
        }

        void checkMethod(byte refKind, Class<?> refc, MemberName m) throws IllegalAccessException {
            boolean wantStatic = (refKind == REF_invokeStatic);
            String message;
            if (m.isConstructor())
                message = "expected a method, not a constructor";
            else if (!m.isMethod())
                message = "expected a method";
            else if (wantStatic != m.isStatic())
                message = wantStatic ? "expected a static method" : "expected a non-static method";
            else
                { checkAccess(refKind, refc, m); return; }
            throw m.makeAccessException(message, this);
        }

        void checkField(byte refKind, Class<?> refc, MemberName m) throws IllegalAccessException {
            boolean wantStatic = !MethodHandleNatives.refKindHasReceiver(refKind);
            String message;
            if (wantStatic != m.isStatic())
                message = wantStatic ? "expected a static field" : "expected a non-static field";
            else
                { checkAccess(refKind, refc, m); return; }
            throw m.makeAccessException(message, this);
        }


        void checkAccess(byte refKind, Class<?> refc, MemberName m) throws IllegalAccessException {
            assert(m.referenceKindIsConsistentWith(refKind) &&
                   MethodHandleNatives.refKindIsValid(refKind) &&
                   (MethodHandleNatives.refKindIsField(refKind) == m.isField()));
            int allowedModes = this.allowedModes;
            if (allowedModes == TRUSTED)  return;
            int mods = m.getModifiers();
            if (Modifier.isProtected(mods) &&
                    refKind == REF_invokeVirtual &&
                    m.getDeclaringClass() == Object.class &&
                    m.getName().equals("clone") &&
                    refc.isArray()) {
                // The JVM does this hack also.
                // (See ClassVerifier::verify_invoke_instructions
                // and LinkResolver::check_method_accessability.)
                // Because the JVM does not allow separate methods on array types,
                // there is no separate method for int[].clone.
                // All arrays simply inherit Object.clone.
                // But for access checking logic, we make Object.clone
                // (normally protected) appear to be public.
                // Later on, when the DirectMethodHandle is created,
                // its leading argument will be restricted to the
                // requested array type.
                // N.B. The return type is not adjusted, because
                // that is *not* the bytecode behavior.
                mods ^= Modifier.PROTECTED | Modifier.PUBLIC;
            }
            if (Modifier.isProtected(mods) && refKind == REF_newInvokeSpecial) {
                // cannot "new" a protected ctor in a different package
                mods ^= Modifier.PROTECTED;
            }
            if (Modifier.isFinal(mods) &&
                    MethodHandleNatives.refKindIsSetter(refKind))
                throw m.makeAccessException("unexpected set of a final field", this);
            int requestedModes = fixmods(mods);  // adjust 0 => PACKAGE
            if ((requestedModes & allowedModes) != 0) {
                if (VerifyAccess.isMemberAccessible(refc, m.getDeclaringClass(),
                                                    mods, lookupClass(), allowedModes))
                    return;
            } else {
                // Protected members can also be checked as if they were package-private.
                if ((requestedModes & PROTECTED) != 0 && (allowedModes & PACKAGE) != 0
                        && VerifyAccess.isSamePackage(m.getDeclaringClass(), lookupClass()))
                    return;
            }
            throw m.makeAccessException(accessFailedMessage(refc, m), this);
        }

        String accessFailedMessage(Class<?> refc, MemberName m) {
            Class<?> defc = m.getDeclaringClass();
            int mods = m.getModifiers();
            // check the class first:
            boolean classOK = (Modifier.isPublic(defc.getModifiers()) &&
                               (defc == refc ||
                                Modifier.isPublic(refc.getModifiers())));
            if (!classOK && (allowedModes & PACKAGE) != 0) {
                classOK = (VerifyAccess.isClassAccessible(defc, lookupClass(), FULL_POWER_MODES) &&
                           (defc == refc ||
                            VerifyAccess.isClassAccessible(refc, lookupClass(), FULL_POWER_MODES)));
            }
            if (!classOK)
                return "class is not public";
            if (Modifier.isPublic(mods))
                return "access to public member failed";  // (how?, module not readable?)
            if (Modifier.isPrivate(mods))
                return "member is private";
            if (Modifier.isProtected(mods))
                return "member is protected";
            return "member is private to package";
        }

        private static final boolean ALLOW_NESTMATE_ACCESS = false;

        private void checkSpecialCaller(Class<?> specialCaller, Class<?> refc) throws IllegalAccessException {
            int allowedModes = this.allowedModes;
            if (allowedModes == TRUSTED)  return;
            if (!hasPrivateAccess()
                || (specialCaller != lookupClass()
                       // ensure non-abstract methods in superinterfaces can be special-invoked
                    && !(refc != null && refc.isInterface() && refc.isAssignableFrom(specialCaller))
                    && !(ALLOW_NESTMATE_ACCESS &&
                         VerifyAccess.isSamePackageMember(specialCaller, lookupClass()))))
                throw new MemberName(specialCaller).
                    makeAccessException("no private access for invokespecial", this);
        }

        private boolean restrictProtectedReceiver(MemberName method) {
            // The accessing class only has the right to use a protected member
            // on itself or a subclass.  Enforce that restriction, from JVMS 5.4.4, etc.
            if (!method.isProtected() || method.isStatic()
                || allowedModes == TRUSTED
                || method.getDeclaringClass() == lookupClass()
                || VerifyAccess.isSamePackage(method.getDeclaringClass(), lookupClass())
                || (ALLOW_NESTMATE_ACCESS &&
                    VerifyAccess.isSamePackageMember(method.getDeclaringClass(), lookupClass())))
                return false;
            return true;
        }
        private MethodHandle restrictReceiver(MemberName method, DirectMethodHandle mh, Class<?> caller) throws IllegalAccessException {
            assert(!method.isStatic());
            // receiver type of mh is too wide; narrow to caller
            if (!method.getDeclaringClass().isAssignableFrom(caller)) {
                throw method.makeAccessException("caller class must be a subclass below the method", caller);
            }
            MethodType rawType = mh.type();
            if (caller.isAssignableFrom(rawType.parameterType(0))) return mh; // no need to restrict; already narrow
            MethodType narrowType = rawType.changeParameterType(0, caller);
            assert(!mh.isVarargsCollector());  // viewAsType will lose varargs-ness
            assert(mh.viewAsTypeChecks(narrowType, true));
            return mh.copyWith(narrowType, mh.form);
        }


        private MethodHandle getDirectMethod(byte refKind, Class<?> refc, MemberName method, Class<?> callerClass) throws IllegalAccessException {
            final boolean doRestrict    = true;
            final boolean checkSecurity = true;
            return getDirectMethodCommon(refKind, refc, method, checkSecurity, doRestrict, callerClass);
        }

        private MethodHandle getDirectMethodNoRestrictInvokeSpecial(Class<?> refc, MemberName method, Class<?> callerClass) throws IllegalAccessException {
            final boolean doRestrict    = false;
            final boolean checkSecurity = true;
            return getDirectMethodCommon(REF_invokeSpecial, refc, method, checkSecurity, doRestrict, callerClass);
        }

        private MethodHandle getDirectMethodNoSecurityManager(byte refKind, Class<?> refc, MemberName method, Class<?> callerClass) throws IllegalAccessException {
            final boolean doRestrict    = true;
            final boolean checkSecurity = false;  // not needed for reflection or for linking CONSTANT_MH constants
            return getDirectMethodCommon(refKind, refc, method, checkSecurity, doRestrict, callerClass);
        }

        private MethodHandle getDirectMethodCommon(byte refKind, Class<?> refc, MemberName method,
                                                   boolean checkSecurity,
                                                   boolean doRestrict, Class<?> callerClass) throws IllegalAccessException {
            checkMethod(refKind, refc, method);
            // Optionally check with the security manager; this isn't needed for unreflect* calls.
            if (checkSecurity)
                checkSecurityManager(refc, method);
            assert(!method.isMethodHandleInvoke());

            if (refKind == REF_invokeSpecial &&
                refc != lookupClass() &&
                !refc.isInterface() &&
                refc != lookupClass().getSuperclass() &&
                refc.isAssignableFrom(lookupClass())) {
                assert(!method.getName().equals("<init>"));  // not this code path
                // Per JVMS 6.5, desc. of invokespecial instruction:
                // If the method is in a superclass of the LC,
                // and if our original search was above LC.super,
                // repeat the search (symbolic lookup) from LC.super
                // and continue with the direct superclass of that class,
                // and so forth, until a match is found or no further superclasses exist.
                // FIXME: MemberName.resolve should handle this instead.
                Class<?> refcAsSuper = lookupClass();
                MemberName m2;
                do {
                    refcAsSuper = refcAsSuper.getSuperclass();
                    m2 = new MemberName(refcAsSuper,
                                        method.getName(),
                                        method.getMethodType(),
                                        REF_invokeSpecial);
                    m2 = IMPL_NAMES.resolveOrNull(refKind, m2, lookupClassOrNull());
                } while (m2 == null &&         // no method is found yet
                         refc != refcAsSuper); // search up to refc
                if (m2 == null)  throw new InternalError(method.toString());
                method = m2;
                refc = refcAsSuper;
                // redo basic checks
                checkMethod(refKind, refc, method);
            }

            DirectMethodHandle dmh = DirectMethodHandle.make(refKind, refc, method);
            MethodHandle mh = dmh;
            // Optionally narrow the receiver argument to refc using restrictReceiver.
            if ((doRestrict && refKind == REF_invokeSpecial) ||
                    (MethodHandleNatives.refKindHasReceiver(refKind) && restrictProtectedReceiver(method))) {
                mh = restrictReceiver(method, dmh, lookupClass());
            }
            mh = maybeBindCaller(method, mh, callerClass);
            mh = mh.setVarargs(method);
            return mh;
        }
        private MethodHandle maybeBindCaller(MemberName method, MethodHandle mh,
                                             Class<?> callerClass)
                                             throws IllegalAccessException {
            if (allowedModes == TRUSTED || !MethodHandleNatives.isCallerSensitive(method))
                return mh;
            Class<?> hostClass = lookupClass;
            if (!hasPrivateAccess())  // caller must have private access
                hostClass = callerClass;  // callerClass came from a security manager style stack walk
            MethodHandle cbmh = MethodHandleImpl.bindCaller(mh, hostClass);
            // Note: caller will apply varargs after this step happens.
            return cbmh;
        }

        private MethodHandle getDirectField(byte refKind, Class<?> refc, MemberName field) throws IllegalAccessException {
            final boolean checkSecurity = true;
            return getDirectFieldCommon(refKind, refc, field, checkSecurity);
        }

        private MethodHandle getDirectFieldNoSecurityManager(byte refKind, Class<?> refc, MemberName field) throws IllegalAccessException {
            final boolean checkSecurity = false;  // not needed for reflection or for linking CONSTANT_MH constants
            return getDirectFieldCommon(refKind, refc, field, checkSecurity);
        }

        private MethodHandle getDirectFieldCommon(byte refKind, Class<?> refc, MemberName field,
                                                  boolean checkSecurity) throws IllegalAccessException {
            checkField(refKind, refc, field);
            // Optionally check with the security manager; this isn't needed for unreflect* calls.
            if (checkSecurity)
                checkSecurityManager(refc, field);
            DirectMethodHandle dmh = DirectMethodHandle.make(refc, field);
            boolean doRestrict = (MethodHandleNatives.refKindHasReceiver(refKind) &&
                                    restrictProtectedReceiver(field));
            if (doRestrict)
                return restrictReceiver(field, dmh, lookupClass());
            return dmh;
        }
        private VarHandle getFieldVarHandle(byte getRefKind, byte putRefKind,
                                            Class<?> refc, MemberName getField, MemberName putField)
                throws IllegalAccessException {
            final boolean checkSecurity = true;
            return getFieldVarHandleCommon(getRefKind, putRefKind, refc, getField, putField, checkSecurity);
        }
        private VarHandle getFieldVarHandleNoSecurityManager(byte getRefKind, byte putRefKind,
                                                             Class<?> refc, MemberName getField, MemberName putField)
                throws IllegalAccessException {
            final boolean checkSecurity = false;
            return getFieldVarHandleCommon(getRefKind, putRefKind, refc, getField, putField, checkSecurity);
        }
        private VarHandle getFieldVarHandleCommon(byte getRefKind, byte putRefKind,
                                                  Class<?> refc, MemberName getField, MemberName putField,
                                                  boolean checkSecurity) throws IllegalAccessException {
            assert getField.isStatic() == putField.isStatic();
            assert getField.isGetter() && putField.isSetter();
            assert MethodHandleNatives.refKindIsStatic(getRefKind) == MethodHandleNatives.refKindIsStatic(putRefKind);
            assert MethodHandleNatives.refKindIsGetter(getRefKind) && MethodHandleNatives.refKindIsSetter(putRefKind);

            checkField(getRefKind, refc, getField);
            if (checkSecurity)
                checkSecurityManager(refc, getField);

            if (!putField.isFinal()) {
                // A VarHandle does not support updates to final fields, any
                // such VarHandle to a final field will be read-only and
                // therefore the following write-based accessibility checks are
                // only required for non-final fields
                checkField(putRefKind, refc, putField);
                if (checkSecurity)
                    checkSecurityManager(refc, putField);
            }

            boolean doRestrict = (MethodHandleNatives.refKindHasReceiver(getRefKind) &&
                                  restrictProtectedReceiver(getField));
            if (doRestrict) {
                assert !getField.isStatic();
                // receiver type of VarHandle is too wide; narrow to caller
                if (!getField.getDeclaringClass().isAssignableFrom(lookupClass())) {
                    throw getField.makeAccessException("caller class must be a subclass below the method", lookupClass());
                }
                refc = lookupClass();
            }
            return VarHandles.makeFieldHandle(getField, refc, getField.getFieldType(), this.allowedModes == TRUSTED);
        }

        private MethodHandle getDirectConstructor(Class<?> refc, MemberName ctor) throws IllegalAccessException {
            final boolean checkSecurity = true;
            return getDirectConstructorCommon(refc, ctor, checkSecurity);
        }

        private MethodHandle getDirectConstructorNoSecurityManager(Class<?> refc, MemberName ctor) throws IllegalAccessException {
            final boolean checkSecurity = false;  // not needed for reflection or for linking CONSTANT_MH constants
            return getDirectConstructorCommon(refc, ctor, checkSecurity);
        }

        private MethodHandle getDirectConstructorCommon(Class<?> refc, MemberName ctor,
                                                  boolean checkSecurity) throws IllegalAccessException {
            assert(ctor.isConstructor());
            checkAccess(REF_newInvokeSpecial, refc, ctor);
            // Optionally check with the security manager; this isn't needed for unreflect* calls.
            if (checkSecurity)
                checkSecurityManager(refc, ctor);
            assert(!MethodHandleNatives.isCallerSensitive(ctor));  // maybeBindCaller not relevant here
            return DirectMethodHandle.make(ctor).setVarargs(ctor);
        }


        /*non-public*/
        MethodHandle linkMethodHandleConstant(byte refKind, Class<?> defc, String name, Object type) throws ReflectiveOperationException {
            if (!(type instanceof Class || type instanceof MethodType))
                throw new InternalError("unresolved MemberName");
            MemberName member = new MemberName(refKind, defc, name, type);
            MethodHandle mh = LOOKASIDE_TABLE.get(member);
            if (mh != null) {
                checkSymbolicClass(defc);
                return mh;
            }
            // Treat MethodHandle.invoke and invokeExact specially.
            if (defc == MethodHandle.class && refKind == REF_invokeVirtual) {
                mh = findVirtualForMH(member.getName(), member.getMethodType());
                if (mh != null) {
                    return mh;
                }
            }
            MemberName resolved = resolveOrFail(refKind, member);
            mh = getDirectMethodForConstant(refKind, defc, resolved);
            if (mh instanceof DirectMethodHandle
                    && canBeCached(refKind, defc, resolved)) {
                MemberName key = mh.internalMemberName();
                if (key != null) {
                    key = key.asNormalOriginal();
                }
                if (member.equals(key)) {  // better safe than sorry
                    LOOKASIDE_TABLE.put(key, (DirectMethodHandle) mh);
                }
            }
            return mh;
        }
        private
        boolean canBeCached(byte refKind, Class<?> defc, MemberName member) {
            if (refKind == REF_invokeSpecial) {
                return false;
            }
            if (!Modifier.isPublic(defc.getModifiers()) ||
                    !Modifier.isPublic(member.getDeclaringClass().getModifiers()) ||
                    !member.isPublic() ||
                    member.isCallerSensitive()) {
                return false;
            }
            ClassLoader loader = defc.getClassLoader();
            if (!jdk.internal.misc.VM.isSystemDomainLoader(loader)) {
                ClassLoader sysl = ClassLoader.getSystemClassLoader();
                boolean found = false;
                while (sysl != null) {
                    if (loader == sysl) { found = true; break; }
                    sysl = sysl.getParent();
                }
                if (!found) {
                    return false;
                }
            }
            try {
                MemberName resolved2 = publicLookup().resolveOrFail(refKind,
                    new MemberName(refKind, defc, member.getName(), member.getType()));
                checkSecurityManager(defc, resolved2);
            } catch (ReflectiveOperationException | SecurityException ex) {
                return false;
            }
            return true;
        }
        private
        MethodHandle getDirectMethodForConstant(byte refKind, Class<?> defc, MemberName member)
                throws ReflectiveOperationException {
            if (MethodHandleNatives.refKindIsField(refKind)) {
                return getDirectFieldNoSecurityManager(refKind, defc, member);
            } else if (MethodHandleNatives.refKindIsMethod(refKind)) {
                return getDirectMethodNoSecurityManager(refKind, defc, member, lookupClass);
            } else if (refKind == REF_newInvokeSpecial) {
                return getDirectConstructorNoSecurityManager(defc, member);
            }
            // oops
            throw newIllegalArgumentException("bad MethodHandle constant #"+member);
        }

        static ConcurrentHashMap<MemberName, DirectMethodHandle> LOOKASIDE_TABLE = new ConcurrentHashMap<>();
    }


    public static
    MethodHandle arrayConstructor(Class<?> arrayClass) throws IllegalArgumentException {
        if (!arrayClass.isArray()) {
            throw newIllegalArgumentException("not an array class: " + arrayClass.getName());
        }
        MethodHandle ani = MethodHandleImpl.getConstantHandle(MethodHandleImpl.MH_Array_newInstance).
                bindTo(arrayClass.getComponentType());
        return ani.asType(ani.type().changeReturnType(arrayClass));
    }


    public static
    MethodHandle arrayLength(Class<?> arrayClass) throws IllegalArgumentException {
        return MethodHandleImpl.makeArrayElementAccessor(arrayClass, MethodHandleImpl.ArrayAccess.LENGTH);
    }


    public static
    MethodHandle arrayElementGetter(Class<?> arrayClass) throws IllegalArgumentException {
        return MethodHandleImpl.makeArrayElementAccessor(arrayClass, MethodHandleImpl.ArrayAccess.GET);
    }


    public static
    MethodHandle arrayElementSetter(Class<?> arrayClass) throws IllegalArgumentException {
        return MethodHandleImpl.makeArrayElementAccessor(arrayClass, MethodHandleImpl.ArrayAccess.SET);
    }


    public static
    VarHandle arrayElementVarHandle(Class<?> arrayClass) throws IllegalArgumentException {
        return VarHandles.makeArrayElementHandle(arrayClass);
    }


    public static
    VarHandle byteArrayViewVarHandle(Class<?> viewArrayClass,
                                     ByteOrder byteOrder) throws IllegalArgumentException {
        Objects.requireNonNull(byteOrder);
        return VarHandles.byteArrayViewHandle(viewArrayClass,
                                              byteOrder == ByteOrder.BIG_ENDIAN);
    }


    public static
    VarHandle byteBufferViewVarHandle(Class<?> viewArrayClass,
                                      ByteOrder byteOrder) throws IllegalArgumentException {
        Objects.requireNonNull(byteOrder);
        return VarHandles.makeByteBufferViewHandle(viewArrayClass,
                                                   byteOrder == ByteOrder.BIG_ENDIAN);
    }


    /// method handle invocation (reflective style)


    public static
    MethodHandle spreadInvoker(MethodType type, int leadingArgCount) {
        if (leadingArgCount < 0 || leadingArgCount > type.parameterCount())
            throw newIllegalArgumentException("bad argument count", leadingArgCount);
        type = type.asSpreaderType(Object[].class, leadingArgCount, type.parameterCount() - leadingArgCount);
        return type.invokers().spreadInvoker(leadingArgCount);
    }


    public static
    MethodHandle exactInvoker(MethodType type) {
        return type.invokers().exactInvoker();
    }


    public static
    MethodHandle invoker(MethodType type) {
        return type.invokers().genericInvoker();
    }


    static public
    MethodHandle varHandleExactInvoker(VarHandle.AccessMode accessMode, MethodType type) {
        return type.invokers().varHandleMethodExactInvoker(accessMode);
    }


    static public
    MethodHandle varHandleInvoker(VarHandle.AccessMode accessMode, MethodType type) {
        return type.invokers().varHandleMethodInvoker(accessMode);
    }

    static /*non-public*/
    MethodHandle basicInvoker(MethodType type) {
        return type.invokers().basicInvoker();
    }

     /// method handle modification (creation from other method handles)


    public static
    MethodHandle explicitCastArguments(MethodHandle target, MethodType newType) {
        explicitCastArgumentsChecks(target, newType);
        // use the asTypeCache when possible:
        MethodType oldType = target.type();
        if (oldType == newType)  return target;
        if (oldType.explicitCastEquivalentToAsType(newType)) {
            return target.asFixedArity().asType(newType);
        }
        return MethodHandleImpl.makePairwiseConvert(target, newType, false);
    }

    private static void explicitCastArgumentsChecks(MethodHandle target, MethodType newType) {
        if (target.type().parameterCount() != newType.parameterCount()) {
            throw new WrongMethodTypeException("cannot explicitly cast " + target + " to " + newType);
        }
    }


    public static
    MethodHandle permuteArguments(MethodHandle target, MethodType newType, int... reorder) {
        reorder = reorder.clone();  // get a private copy
        MethodType oldType = target.type();
        permuteArgumentChecks(reorder, newType, oldType);
        // first detect dropped arguments and handle them separately
        int[] originalReorder = reorder;
        BoundMethodHandle result = target.rebind();
        LambdaForm form = result.form;
        int newArity = newType.parameterCount();
        // Normalize the reordering into a real permutation,
        // by removing duplicates and adding dropped elements.
        // This somewhat improves lambda form caching, as well
        // as simplifying the transform by breaking it up into steps.
        for (int ddIdx; (ddIdx = findFirstDupOrDrop(reorder, newArity)) != 0; ) {
            if (ddIdx > 0) {
                // We found a duplicated entry at reorder[ddIdx].
                // Example:  (x,y,z)->asList(x,y,z)
                // permuted by [1*,0,1] => (a0,a1)=>asList(a1,a0,a1)
                // permuted by [0,1,0*] => (a0,a1)=>asList(a0,a1,a0)
                // The starred element corresponds to the argument
                // deleted by the dupArgumentForm transform.
                int srcPos = ddIdx, dstPos = srcPos, dupVal = reorder[srcPos];
                boolean killFirst = false;
                for (int val; (val = reorder[--dstPos]) != dupVal; ) {
                    // Set killFirst if the dup is larger than an intervening position.
                    // This will remove at least one inversion from the permutation.
                    if (dupVal > val) killFirst = true;
                }
                if (!killFirst) {
                    srcPos = dstPos;
                    dstPos = ddIdx;
                }
                form = form.editor().dupArgumentForm(1 + srcPos, 1 + dstPos);
                assert (reorder[srcPos] == reorder[dstPos]);
                oldType = oldType.dropParameterTypes(dstPos, dstPos + 1);
                // contract the reordering by removing the element at dstPos
                int tailPos = dstPos + 1;
                System.arraycopy(reorder, tailPos, reorder, dstPos, reorder.length - tailPos);
                reorder = Arrays.copyOf(reorder, reorder.length - 1);
            } else {
                int dropVal = ~ddIdx, insPos = 0;
                while (insPos < reorder.length && reorder[insPos] < dropVal) {
                    // Find first element of reorder larger than dropVal.
                    // This is where we will insert the dropVal.
                    insPos += 1;
                }
                Class<?> ptype = newType.parameterType(dropVal);
                form = form.editor().addArgumentForm(1 + insPos, BasicType.basicType(ptype));
                oldType = oldType.insertParameterTypes(insPos, ptype);
                // expand the reordering by inserting an element at insPos
                int tailPos = insPos + 1;
                reorder = Arrays.copyOf(reorder, reorder.length + 1);
                System.arraycopy(reorder, insPos, reorder, tailPos, reorder.length - tailPos);
                reorder[insPos] = dropVal;
            }
            assert (permuteArgumentChecks(reorder, newType, oldType));
        }
        assert (reorder.length == newArity);  // a perfect permutation
        // Note:  This may cache too many distinct LFs. Consider backing off to varargs code.
        form = form.editor().permuteArgumentsForm(1, reorder);
        if (newType == result.type() && form == result.internalForm())
            return result;
        return result.copyWith(newType, form);
    }


    private static int findFirstDupOrDrop(int[] reorder, int newArity) {
        final int BIT_LIMIT = 63;  // max number of bits in bit mask
        if (newArity < BIT_LIMIT) {
            long mask = 0;
            for (int i = 0; i < reorder.length; i++) {
                int arg = reorder[i];
                if (arg >= newArity) {
                    return reorder.length;
                }
                long bit = 1L << arg;
                if ((mask & bit) != 0) {
                    return i;  // >0 indicates a dup
                }
                mask |= bit;
            }
            if (mask == (1L << newArity) - 1) {
                assert(Long.numberOfTrailingZeros(Long.lowestOneBit(~mask)) == newArity);
                return 0;
            }
            // find first zero
            long zeroBit = Long.lowestOneBit(~mask);
            int zeroPos = Long.numberOfTrailingZeros(zeroBit);
            assert(zeroPos <= newArity);
            if (zeroPos == newArity) {
                return 0;
            }
            return ~zeroPos;
        } else {
            // same algorithm, different bit set
            BitSet mask = new BitSet(newArity);
            for (int i = 0; i < reorder.length; i++) {
                int arg = reorder[i];
                if (arg >= newArity) {
                    return reorder.length;
                }
                if (mask.get(arg)) {
                    return i;  // >0 indicates a dup
                }
                mask.set(arg);
            }
            int zeroPos = mask.nextClearBit(0);
            assert(zeroPos <= newArity);
            if (zeroPos == newArity) {
                return 0;
            }
            return ~zeroPos;
        }
    }

    private static boolean permuteArgumentChecks(int[] reorder, MethodType newType, MethodType oldType) {
        if (newType.returnType() != oldType.returnType())
            throw newIllegalArgumentException("return types do not match",
                    oldType, newType);
        if (reorder.length == oldType.parameterCount()) {
            int limit = newType.parameterCount();
            boolean bad = false;
            for (int j = 0; j < reorder.length; j++) {
                int i = reorder[j];
                if (i < 0 || i >= limit) {
                    bad = true; break;
                }
                Class<?> src = newType.parameterType(i);
                Class<?> dst = oldType.parameterType(j);
                if (src != dst)
                    throw newIllegalArgumentException("parameter types do not match after reorder",
                            oldType, newType);
            }
            if (!bad)  return true;
        }
        throw newIllegalArgumentException("bad reorder array: "+Arrays.toString(reorder));
    }


    public static
    MethodHandle constant(Class<?> type, Object value) {
        if (type.isPrimitive()) {
            if (type == void.class)
                throw newIllegalArgumentException("void type");
            Wrapper w = Wrapper.forPrimitiveType(type);
            value = w.convert(value, type);
            if (w.zero().equals(value))
                return zero(w, type);
            return insertArguments(identity(type), 0, value);
        } else {
            if (value == null)
                return zero(Wrapper.OBJECT, type);
            return identity(type).bindTo(value);
        }
    }


    public static
    MethodHandle identity(Class<?> type) {
        Wrapper btw = (type.isPrimitive() ? Wrapper.forPrimitiveType(type) : Wrapper.OBJECT);
        int pos = btw.ordinal();
        MethodHandle ident = IDENTITY_MHS[pos];
        if (ident == null) {
            ident = setCachedMethodHandle(IDENTITY_MHS, pos, makeIdentity(btw.primitiveType()));
        }
        if (ident.type().returnType() == type)
            return ident;
        // something like identity(Foo.class); do not bother to intern these
        assert (btw == Wrapper.OBJECT);
        return makeIdentity(type);
    }


    public static MethodHandle zero(Class<?> type) {
        Objects.requireNonNull(type);
        return type.isPrimitive() ?  zero(Wrapper.forPrimitiveType(type), type) : zero(Wrapper.OBJECT, type);
    }

    private static MethodHandle identityOrVoid(Class<?> type) {
        return type == void.class ? zero(type) : identity(type);
    }


    public static  MethodHandle empty(MethodType type) {
        Objects.requireNonNull(type);
        return dropArguments(zero(type.returnType()), 0, type.parameterList());
    }

    private static final MethodHandle[] IDENTITY_MHS = new MethodHandle[Wrapper.COUNT];
    private static MethodHandle makeIdentity(Class<?> ptype) {
        MethodType mtype = methodType(ptype, ptype);
        LambdaForm lform = LambdaForm.identityForm(BasicType.basicType(ptype));
        return MethodHandleImpl.makeIntrinsic(mtype, lform, Intrinsic.IDENTITY);
    }

    private static MethodHandle zero(Wrapper btw, Class<?> rtype) {
        int pos = btw.ordinal();
        MethodHandle zero = ZERO_MHS[pos];
        if (zero == null) {
            zero = setCachedMethodHandle(ZERO_MHS, pos, makeZero(btw.primitiveType()));
        }
        if (zero.type().returnType() == rtype)
            return zero;
        assert(btw == Wrapper.OBJECT);
        return makeZero(rtype);
    }
    private static final MethodHandle[] ZERO_MHS = new MethodHandle[Wrapper.COUNT];
    private static MethodHandle makeZero(Class<?> rtype) {
        MethodType mtype = methodType(rtype);
        LambdaForm lform = LambdaForm.zeroForm(BasicType.basicType(rtype));
        return MethodHandleImpl.makeIntrinsic(mtype, lform, Intrinsic.ZERO);
    }

    private static synchronized MethodHandle setCachedMethodHandle(MethodHandle[] cache, int pos, MethodHandle value) {
        // Simulate a CAS, to avoid racy duplication of results.
        MethodHandle prev = cache[pos];
        if (prev != null) return prev;
        return cache[pos] = value;
    }


    public static
    MethodHandle insertArguments(MethodHandle target, int pos, Object... values) {
        int insCount = values.length;
        Class<?>[] ptypes = insertArgumentsChecks(target, insCount, pos);
        if (insCount == 0)  return target;
        BoundMethodHandle result = target.rebind();
        for (int i = 0; i < insCount; i++) {
            Object value = values[i];
            Class<?> ptype = ptypes[pos+i];
            if (ptype.isPrimitive()) {
                result = insertArgumentPrimitive(result, pos, ptype, value);
            } else {
                value = ptype.cast(value);  // throw CCE if needed
                result = result.bindArgumentL(pos, value);
            }
        }
        return result;
    }

    private static BoundMethodHandle insertArgumentPrimitive(BoundMethodHandle result, int pos,
                                                             Class<?> ptype, Object value) {
        Wrapper w = Wrapper.forPrimitiveType(ptype);
        // perform unboxing and/or primitive conversion
        value = w.convert(value, ptype);
        switch (w) {
        case INT:     return result.bindArgumentI(pos, (int)value);
        case LONG:    return result.bindArgumentJ(pos, (long)value);
        case FLOAT:   return result.bindArgumentF(pos, (float)value);
        case DOUBLE:  return result.bindArgumentD(pos, (double)value);
        default:      return result.bindArgumentI(pos, ValueConversions.widenSubword(value));
        }
    }

    private static Class<?>[] insertArgumentsChecks(MethodHandle target, int insCount, int pos) throws RuntimeException {
        MethodType oldType = target.type();
        int outargs = oldType.parameterCount();
        int inargs  = outargs - insCount;
        if (inargs < 0)
            throw newIllegalArgumentException("too many values to insert");
        if (pos < 0 || pos > inargs)
            throw newIllegalArgumentException("no argument type to append");
        return oldType.ptypes();
    }


    public static
    MethodHandle dropArguments(MethodHandle target, int pos, List<Class<?>> valueTypes) {
        return dropArguments0(target, pos, copyTypes(valueTypes.toArray()));
    }

    private static List<Class<?>> copyTypes(Object[] array) {
        return Arrays.asList(Arrays.copyOf(array, array.length, Class[].class));
    }

    private static
    MethodHandle dropArguments0(MethodHandle target, int pos, List<Class<?>> valueTypes) {
        MethodType oldType = target.type();  // get NPE
        int dropped = dropArgumentChecks(oldType, pos, valueTypes);
        MethodType newType = oldType.insertParameterTypes(pos, valueTypes);
        if (dropped == 0)  return target;
        BoundMethodHandle result = target.rebind();
        LambdaForm lform = result.form;
        int insertFormArg = 1 + pos;
        for (Class<?> ptype : valueTypes) {
            lform = lform.editor().addArgumentForm(insertFormArg++, BasicType.basicType(ptype));
        }
        result = result.copyWith(newType, lform);
        return result;
    }

    private static int dropArgumentChecks(MethodType oldType, int pos, List<Class<?>> valueTypes) {
        int dropped = valueTypes.size();
        MethodType.checkSlotCount(dropped);
        int outargs = oldType.parameterCount();
        int inargs  = outargs + dropped;
        if (pos < 0 || pos > outargs)
            throw newIllegalArgumentException("no argument type to remove"
                    + Arrays.asList(oldType, pos, valueTypes, inargs, outargs)
                    );
        return dropped;
    }


    public static
    MethodHandle dropArguments(MethodHandle target, int pos, Class<?>... valueTypes) {
        return dropArguments0(target, pos, copyTypes(valueTypes));
    }

    // private version which allows caller some freedom with error handling
    private static MethodHandle dropArgumentsToMatch(MethodHandle target, int skip, List<Class<?>> newTypes, int pos,
                                      boolean nullOnFailure) {
        newTypes = copyTypes(newTypes.toArray());
        List<Class<?>> oldTypes = target.type().parameterList();
        int match = oldTypes.size();
        if (skip != 0) {
            if (skip < 0 || skip > match) {
                throw newIllegalArgumentException("illegal skip", skip, target);
            }
            oldTypes = oldTypes.subList(skip, match);
            match -= skip;
        }
        List<Class<?>> addTypes = newTypes;
        int add = addTypes.size();
        if (pos != 0) {
            if (pos < 0 || pos > add) {
                throw newIllegalArgumentException("illegal pos", pos, newTypes);
            }
            addTypes = addTypes.subList(pos, add);
            add -= pos;
            assert(addTypes.size() == add);
        }
        // Do not add types which already match the existing arguments.
        if (match > add || !oldTypes.equals(addTypes.subList(0, match))) {
            if (nullOnFailure) {
                return null;
            }
            throw newIllegalArgumentException("argument lists do not match", oldTypes, newTypes);
        }
        addTypes = addTypes.subList(match, add);
        add -= match;
        assert(addTypes.size() == add);
        // newTypes:     (   P*[pos], M*[match], A*[add] )
        // target: ( S*[skip],        M*[match]  )
        MethodHandle adapter = target;
        if (add > 0) {
            adapter = dropArguments0(adapter, skip+ match, addTypes);
        }
        // adapter: (S*[skip],        M*[match], A*[add] )
        if (pos > 0) {
            adapter = dropArguments0(adapter, skip, newTypes.subList(0, pos));
        }
        // adapter: (S*[skip], P*[pos], M*[match], A*[add] )
        return adapter;
    }


    public static
    MethodHandle dropArgumentsToMatch(MethodHandle target, int skip, List<Class<?>> newTypes, int pos) {
        Objects.requireNonNull(target);
        Objects.requireNonNull(newTypes);
        return dropArgumentsToMatch(target, skip, newTypes, pos, false);
    }


    public static
    MethodHandle filterArguments(MethodHandle target, int pos, MethodHandle... filters) {
        filterArgumentsCheckArity(target, pos, filters);
        MethodHandle adapter = target;
        int curPos = pos-1;  // pre-incremented
        for (MethodHandle filter : filters) {
            curPos += 1;
            if (filter == null)  continue;  // ignore null elements of filters
            adapter = filterArgument(adapter, curPos, filter);
        }
        return adapter;
    }

    /*non-public*/ static
    MethodHandle filterArgument(MethodHandle target, int pos, MethodHandle filter) {
        filterArgumentChecks(target, pos, filter);
        MethodType targetType = target.type();
        MethodType filterType = filter.type();
        BoundMethodHandle result = target.rebind();
        Class<?> newParamType = filterType.parameterType(0);
        LambdaForm lform = result.editor().filterArgumentForm(1 + pos, BasicType.basicType(newParamType));
        MethodType newType = targetType.changeParameterType(pos, newParamType);
        result = result.copyWithExtendL(newType, lform, filter);
        return result;
    }

    private static void filterArgumentsCheckArity(MethodHandle target, int pos, MethodHandle[] filters) {
        MethodType targetType = target.type();
        int maxPos = targetType.parameterCount();
        if (pos + filters.length > maxPos)
            throw newIllegalArgumentException("too many filters");
    }

    private static void filterArgumentChecks(MethodHandle target, int pos, MethodHandle filter) throws RuntimeException {
        MethodType targetType = target.type();
        MethodType filterType = filter.type();
        if (filterType.parameterCount() != 1
            || filterType.returnType() != targetType.parameterType(pos))
            throw newIllegalArgumentException("target and filter types do not match", targetType, filterType);
    }


    public static
    MethodHandle collectArguments(MethodHandle target, int pos, MethodHandle filter) {
        MethodType newType = collectArgumentsChecks(target, pos, filter);
        MethodType collectorType = filter.type();
        BoundMethodHandle result = target.rebind();
        LambdaForm lform;
        if (collectorType.returnType().isArray() && filter.intrinsicName() == Intrinsic.NEW_ARRAY) {
            lform = result.editor().collectArgumentArrayForm(1 + pos, filter);
            if (lform != null) {
                return result.copyWith(newType, lform);
            }
        }
        lform = result.editor().collectArgumentsForm(1 + pos, collectorType.basicType());
        return result.copyWithExtendL(newType, lform, filter);
    }

    private static MethodType collectArgumentsChecks(MethodHandle target, int pos, MethodHandle filter) throws RuntimeException {
        MethodType targetType = target.type();
        MethodType filterType = filter.type();
        Class<?> rtype = filterType.returnType();
        List<Class<?>> filterArgs = filterType.parameterList();
        if (rtype == void.class) {
            return targetType.insertParameterTypes(pos, filterArgs);
        }
        if (rtype != targetType.parameterType(pos)) {
            throw newIllegalArgumentException("target and filter types do not match", targetType, filterType);
        }
        return targetType.dropParameterTypes(pos, pos+1).insertParameterTypes(pos, filterArgs);
    }


    public static
    MethodHandle filterReturnValue(MethodHandle target, MethodHandle filter) {
        MethodType targetType = target.type();
        MethodType filterType = filter.type();
        filterReturnValueChecks(targetType, filterType);
        BoundMethodHandle result = target.rebind();
        BasicType rtype = BasicType.basicType(filterType.returnType());
        LambdaForm lform = result.editor().filterReturnForm(rtype, false);
        MethodType newType = targetType.changeReturnType(filterType.returnType());
        result = result.copyWithExtendL(newType, lform, filter);
        return result;
    }

    private static void filterReturnValueChecks(MethodType targetType, MethodType filterType) throws RuntimeException {
        Class<?> rtype = targetType.returnType();
        int filterValues = filterType.parameterCount();
        if (filterValues == 0
                ? (rtype != void.class)
                : (rtype != filterType.parameterType(0) || filterValues != 1))
            throw newIllegalArgumentException("target and filter types do not match", targetType, filterType);
    }


    public static
    MethodHandle foldArguments(MethodHandle target, MethodHandle combiner) {
        return foldArguments(target, 0, combiner);
    }


    public static MethodHandle foldArguments(MethodHandle target, int pos, MethodHandle combiner) {
        MethodType targetType = target.type();
        MethodType combinerType = combiner.type();
        Class<?> rtype = foldArgumentChecks(pos, targetType, combinerType);
        BoundMethodHandle result = target.rebind();
        boolean dropResult = rtype == void.class;
        LambdaForm lform = result.editor().foldArgumentsForm(1 + pos, dropResult, combinerType.basicType());
        MethodType newType = targetType;
        if (!dropResult) {
            newType = newType.dropParameterTypes(pos, pos + 1);
        }
        result = result.copyWithExtendL(newType, lform, combiner);
        return result;
    }


    static MethodHandle foldArguments(MethodHandle target, int pos, MethodHandle combiner, int ... argPositions) {
        MethodType targetType = target.type();
        MethodType combinerType = combiner.type();
        Class<?> rtype = foldArgumentChecks(pos, targetType, combinerType, argPositions);
        BoundMethodHandle result = target.rebind();
        boolean dropResult = rtype == void.class;
        LambdaForm lform = result.editor().foldArgumentsForm(1 + pos, dropResult, combinerType.basicType(), argPositions);
        MethodType newType = targetType;
        if (!dropResult) {
            newType = newType.dropParameterTypes(pos, pos + 1);
        }
        result = result.copyWithExtendL(newType, lform, combiner);
        return result;
    }

    private static Class<?> foldArgumentChecks(int foldPos, MethodType targetType, MethodType combinerType) {
        int foldArgs   = combinerType.parameterCount();
        Class<?> rtype = combinerType.returnType();
        int foldVals = rtype == void.class ? 0 : 1;
        int afterInsertPos = foldPos + foldVals;
        boolean ok = (targetType.parameterCount() >= afterInsertPos + foldArgs);
        if (ok) {
            for (int i = 0; i < foldArgs; i++) {
                if (combinerType.parameterType(i) != targetType.parameterType(i + afterInsertPos)) {
                    ok = false;
                    break;
                }
            }
        }
        if (ok && foldVals != 0 && combinerType.returnType() != targetType.parameterType(foldPos))
            ok = false;
        if (!ok)
            throw misMatchedTypes("target and combiner types", targetType, combinerType);
        return rtype;
    }

    private static Class<?> foldArgumentChecks(int foldPos, MethodType targetType, MethodType combinerType, int ... argPos) {
        int foldArgs = combinerType.parameterCount();
        if (argPos.length != foldArgs) {
            throw newIllegalArgumentException("combiner and argument map must be equal size", combinerType, argPos.length);
        }
        Class<?> rtype = combinerType.returnType();
        int foldVals = rtype == void.class ? 0 : 1;
        boolean ok = true;
        for (int i = 0; i < foldArgs; i++) {
            int arg = argPos[i];
            if (arg < 0 || arg > targetType.parameterCount()) {
                throw newIllegalArgumentException("arg outside of target parameterRange", targetType, arg);
            }
            if (combinerType.parameterType(i) != targetType.parameterType(arg)) {
                throw newIllegalArgumentException("target argument type at position " + arg
                        + " must match combiner argument type at index " + i + ": " + targetType
                        + " -> " + combinerType + ", map: " + Arrays.toString(argPos));
            }
        }
        if (ok && foldVals != 0 && combinerType.returnType() != targetType.parameterType(foldPos)) {
            ok = false;
        }
        if (!ok)
            throw misMatchedTypes("target and combiner types", targetType, combinerType);
        return rtype;
    }


    public static
    MethodHandle guardWithTest(MethodHandle test,
                               MethodHandle target,
                               MethodHandle fallback) {
        MethodType gtype = test.type();
        MethodType ttype = target.type();
        MethodType ftype = fallback.type();
        if (!ttype.equals(ftype))
            throw misMatchedTypes("target and fallback types", ttype, ftype);
        if (gtype.returnType() != boolean.class)
            throw newIllegalArgumentException("guard type is not a predicate "+gtype);
        List<Class<?>> targs = ttype.parameterList();
        test = dropArgumentsToMatch(test, 0, targs, 0, true);
        if (test == null) {
            throw misMatchedTypes("target and test types", ttype, gtype);
        }
        return MethodHandleImpl.makeGuardWithTest(test, target, fallback);
    }

    static <T> RuntimeException misMatchedTypes(String what, T t1, T t2) {
        return newIllegalArgumentException(what + " must match: " + t1 + " != " + t2);
    }


    public static
    MethodHandle catchException(MethodHandle target,
                                Class<? extends Throwable> exType,
                                MethodHandle handler) {
        MethodType ttype = target.type();
        MethodType htype = handler.type();
        if (!Throwable.class.isAssignableFrom(exType))
            throw new ClassCastException(exType.getName());
        if (htype.parameterCount() < 1 ||
            !htype.parameterType(0).isAssignableFrom(exType))
            throw newIllegalArgumentException("handler does not accept exception type "+exType);
        if (htype.returnType() != ttype.returnType())
            throw misMatchedTypes("target and handler return types", ttype, htype);
        handler = dropArgumentsToMatch(handler, 1, ttype.parameterList(), 0, true);
        if (handler == null) {
            throw misMatchedTypes("target and handler types", ttype, htype);
        }
        return MethodHandleImpl.makeGuardWithCatch(target, exType, handler);
    }


    public static
    MethodHandle throwException(Class<?> returnType, Class<? extends Throwable> exType) {
        if (!Throwable.class.isAssignableFrom(exType))
            throw new ClassCastException(exType.getName());
        return MethodHandleImpl.throwException(methodType(returnType, exType));
    }


    public static MethodHandle loop(MethodHandle[]... clauses) {
        // Step 0: determine clause structure.
        loopChecks0(clauses);

        List<MethodHandle> init = new ArrayList<>();
        List<MethodHandle> step = new ArrayList<>();
        List<MethodHandle> pred = new ArrayList<>();
        List<MethodHandle> fini = new ArrayList<>();

        Stream.of(clauses).filter(c -> Stream.of(c).anyMatch(Objects::nonNull)).forEach(clause -> {
            init.add(clause[0]); // all clauses have at least length 1
            step.add(clause.length <= 1 ? null : clause[1]);
            pred.add(clause.length <= 2 ? null : clause[2]);
            fini.add(clause.length <= 3 ? null : clause[3]);
        });

        assert Stream.of(init, step, pred, fini).map(List::size).distinct().count() == 1;
        final int nclauses = init.size();

        // Step 1A: determine iteration variables (V...).
        final List<Class<?>> iterationVariableTypes = new ArrayList<>();
        for (int i = 0; i < nclauses; ++i) {
            MethodHandle in = init.get(i);
            MethodHandle st = step.get(i);
            if (in == null && st == null) {
                iterationVariableTypes.add(void.class);
            } else if (in != null && st != null) {
                loopChecks1a(i, in, st);
                iterationVariableTypes.add(in.type().returnType());
            } else {
                iterationVariableTypes.add(in == null ? st.type().returnType() : in.type().returnType());
            }
        }
        final List<Class<?>> commonPrefix = iterationVariableTypes.stream().filter(t -> t != void.class).
                collect(Collectors.toList());

        // Step 1B: determine loop parameters (A...).
        final List<Class<?>> commonSuffix = buildCommonSuffix(init, step, pred, fini, commonPrefix.size());
        loopChecks1b(init, commonSuffix);

        // Step 1C: determine loop return type.
        // Step 1D: check other types.
        final Class<?> loopReturnType = fini.stream().filter(Objects::nonNull).map(MethodHandle::type).
                map(MethodType::returnType).findFirst().orElse(void.class);
        loopChecks1cd(pred, fini, loopReturnType);

        // Step 2: determine parameter lists.
        final List<Class<?>> commonParameterSequence = new ArrayList<>(commonPrefix);
        commonParameterSequence.addAll(commonSuffix);
        loopChecks2(step, pred, fini, commonParameterSequence);

        // Step 3: fill in omitted functions.
        for (int i = 0; i < nclauses; ++i) {
            Class<?> t = iterationVariableTypes.get(i);
            if (init.get(i) == null) {
                init.set(i, empty(methodType(t, commonSuffix)));
            }
            if (step.get(i) == null) {
                step.set(i, dropArgumentsToMatch(identityOrVoid(t), 0, commonParameterSequence, i));
            }
            if (pred.get(i) == null) {
                pred.set(i, dropArguments0(constant(boolean.class, true), 0, commonParameterSequence));
            }
            if (fini.get(i) == null) {
                fini.set(i, empty(methodType(t, commonParameterSequence)));
            }
        }

        // Step 4: fill in missing parameter types.
        // Also convert all handles to fixed-arity handles.
        List<MethodHandle> finit = fixArities(fillParameterTypes(init, commonSuffix));
        List<MethodHandle> fstep = fixArities(fillParameterTypes(step, commonParameterSequence));
        List<MethodHandle> fpred = fixArities(fillParameterTypes(pred, commonParameterSequence));
        List<MethodHandle> ffini = fixArities(fillParameterTypes(fini, commonParameterSequence));

        assert finit.stream().map(MethodHandle::type).map(MethodType::parameterList).
                allMatch(pl -> pl.equals(commonSuffix));
        assert Stream.of(fstep, fpred, ffini).flatMap(List::stream).map(MethodHandle::type).map(MethodType::parameterList).
                allMatch(pl -> pl.equals(commonParameterSequence));

        return MethodHandleImpl.makeLoop(loopReturnType, commonSuffix, finit, fstep, fpred, ffini);
    }

    private static void loopChecks0(MethodHandle[][] clauses) {
        if (clauses == null || clauses.length == 0) {
            throw newIllegalArgumentException("null or no clauses passed");
        }
        if (Stream.of(clauses).anyMatch(Objects::isNull)) {
            throw newIllegalArgumentException("null clauses are not allowed");
        }
        if (Stream.of(clauses).anyMatch(c -> c.length > 4)) {
            throw newIllegalArgumentException("All loop clauses must be represented as MethodHandle arrays with at most 4 elements.");
        }
    }

    private static void loopChecks1a(int i, MethodHandle in, MethodHandle st) {
        if (in.type().returnType() != st.type().returnType()) {
            throw misMatchedTypes("clause " + i + ": init and step return types", in.type().returnType(),
                    st.type().returnType());
        }
    }

    private static List<Class<?>> longestParameterList(Stream<MethodHandle> mhs, int skipSize) {
        final List<Class<?>> empty = List.of();
        final List<Class<?>> longest = mhs.filter(Objects::nonNull).
                // take only those that can contribute to a common suffix because they are longer than the prefix
                        map(MethodHandle::type).
                        filter(t -> t.parameterCount() > skipSize).
                        map(MethodType::parameterList).
                        reduce((p, q) -> p.size() >= q.size() ? p : q).orElse(empty);
        return longest.size() == 0 ? empty : longest.subList(skipSize, longest.size());
    }

    private static List<Class<?>> longestParameterList(List<List<Class<?>>> lists) {
        final List<Class<?>> empty = List.of();
        return lists.stream().reduce((p, q) -> p.size() >= q.size() ? p : q).orElse(empty);
    }

    private static List<Class<?>> buildCommonSuffix(List<MethodHandle> init, List<MethodHandle> step, List<MethodHandle> pred, List<MethodHandle> fini, int cpSize) {
        final List<Class<?>> longest1 = longestParameterList(Stream.of(step, pred, fini).flatMap(List::stream), cpSize);
        final List<Class<?>> longest2 = longestParameterList(init.stream(), 0);
        return longestParameterList(Arrays.asList(longest1, longest2));
    }

    private static void loopChecks1b(List<MethodHandle> init, List<Class<?>> commonSuffix) {
        if (init.stream().filter(Objects::nonNull).map(MethodHandle::type).
                anyMatch(t -> !t.effectivelyIdenticalParameters(0, commonSuffix))) {
            throw newIllegalArgumentException("found non-effectively identical init parameter type lists: " + init +
                    " (common suffix: " + commonSuffix + ")");
        }
    }

    private static void loopChecks1cd(List<MethodHandle> pred, List<MethodHandle> fini, Class<?> loopReturnType) {
        if (fini.stream().filter(Objects::nonNull).map(MethodHandle::type).map(MethodType::returnType).
                anyMatch(t -> t != loopReturnType)) {
            throw newIllegalArgumentException("found non-identical finalizer return types: " + fini + " (return type: " +
                    loopReturnType + ")");
        }

        if (!pred.stream().filter(Objects::nonNull).findFirst().isPresent()) {
            throw newIllegalArgumentException("no predicate found", pred);
        }
        if (pred.stream().filter(Objects::nonNull).map(MethodHandle::type).map(MethodType::returnType).
                anyMatch(t -> t != boolean.class)) {
            throw newIllegalArgumentException("predicates must have boolean return type", pred);
        }
    }

    private static void loopChecks2(List<MethodHandle> step, List<MethodHandle> pred, List<MethodHandle> fini, List<Class<?>> commonParameterSequence) {
        if (Stream.of(step, pred, fini).flatMap(List::stream).filter(Objects::nonNull).map(MethodHandle::type).
                anyMatch(t -> !t.effectivelyIdenticalParameters(0, commonParameterSequence))) {
            throw newIllegalArgumentException("found non-effectively identical parameter type lists:\nstep: " + step +
                    "\npred: " + pred + "\nfini: " + fini + " (common parameter sequence: " + commonParameterSequence + ")");
        }
    }

    private static List<MethodHandle> fillParameterTypes(List<MethodHandle> hs, final List<Class<?>> targetParams) {
        return hs.stream().map(h -> {
            int pc = h.type().parameterCount();
            int tpsize = targetParams.size();
            return pc < tpsize ? dropArguments0(h, pc, targetParams.subList(pc, tpsize)) : h;
        }).collect(Collectors.toList());
    }

    private static List<MethodHandle> fixArities(List<MethodHandle> hs) {
        return hs.stream().map(MethodHandle::asFixedArity).collect(Collectors.toList());
    }


    public static MethodHandle whileLoop(MethodHandle init, MethodHandle pred, MethodHandle body) {
        whileLoopChecks(init, pred, body);
        MethodHandle fini = identityOrVoid(body.type().returnType());
        MethodHandle[] checkExit = { null, null, pred, fini };
        MethodHandle[] varBody = { init, body };
        return loop(checkExit, varBody);
    }


    public static MethodHandle doWhileLoop(MethodHandle init, MethodHandle body, MethodHandle pred) {
        whileLoopChecks(init, pred, body);
        MethodHandle fini = identityOrVoid(body.type().returnType());
        MethodHandle[] clause = {init, body, pred, fini };
        return loop(clause);
    }

    private static void whileLoopChecks(MethodHandle init, MethodHandle pred, MethodHandle body) {
        Objects.requireNonNull(pred);
        Objects.requireNonNull(body);
        MethodType bodyType = body.type();
        Class<?> returnType = bodyType.returnType();
        List<Class<?>> innerList = bodyType.parameterList();
        List<Class<?>> outerList = innerList;
        if (returnType == void.class) {
            // OK
        } else if (innerList.size() == 0 || innerList.get(0) != returnType) {
            // leading V argument missing => error
            MethodType expected = bodyType.insertParameterTypes(0, returnType);
            throw misMatchedTypes("body function", bodyType, expected);
        } else {
            outerList = innerList.subList(1, innerList.size());
        }
        MethodType predType = pred.type();
        if (predType.returnType() != boolean.class ||
                !predType.effectivelyIdenticalParameters(0, innerList)) {
            throw misMatchedTypes("loop predicate", predType, methodType(boolean.class, innerList));
        }
        if (init != null) {
            MethodType initType = init.type();
            if (initType.returnType() != returnType ||
                    !initType.effectivelyIdenticalParameters(0, outerList)) {
                throw misMatchedTypes("loop initializer", initType, methodType(returnType, outerList));
            }
        }
    }


    public static MethodHandle countedLoop(MethodHandle iterations, MethodHandle init, MethodHandle body) {
        return countedLoop(empty(iterations.type()), iterations, init, body);
    }


    public static MethodHandle countedLoop(MethodHandle start, MethodHandle end, MethodHandle init, MethodHandle body) {
        countedLoopChecks(start, end, init, body);
        Class<?> counterType = start.type().returnType();  // int, but who's counting?
        Class<?> limitType   = end.type().returnType();    // yes, int again
        Class<?> returnType  = body.type().returnType();
        MethodHandle incr = MethodHandleImpl.getConstantHandle(MethodHandleImpl.MH_countedLoopStep);
        MethodHandle pred = MethodHandleImpl.getConstantHandle(MethodHandleImpl.MH_countedLoopPred);
        MethodHandle retv = null;
        if (returnType != void.class) {
            incr = dropArguments(incr, 1, returnType);  // (limit, v, i) => (limit, i)
            pred = dropArguments(pred, 1, returnType);  // ditto
            retv = dropArguments(identity(returnType), 0, counterType);
        }
        body = dropArguments(body, 0, counterType);  // ignore the limit variable
        MethodHandle[]
            loopLimit  = { end, null, pred, retv }, // limit = end(); i < limit || return v
            bodyClause = { init, body },            // v = init(); v = body(v, i)
            indexVar   = { start, incr };           // i = start(); i = i + 1
        return loop(loopLimit, bodyClause, indexVar);
    }

    private static void countedLoopChecks(MethodHandle start, MethodHandle end, MethodHandle init, MethodHandle body) {
        Objects.requireNonNull(start);
        Objects.requireNonNull(end);
        Objects.requireNonNull(body);
        Class<?> counterType = start.type().returnType();
        if (counterType != int.class) {
            MethodType expected = start.type().changeReturnType(int.class);
            throw misMatchedTypes("start function", start.type(), expected);
        } else if (end.type().returnType() != counterType) {
            MethodType expected = end.type().changeReturnType(counterType);
            throw misMatchedTypes("end function", end.type(), expected);
        }
        MethodType bodyType = body.type();
        Class<?> returnType = bodyType.returnType();
        List<Class<?>> innerList = bodyType.parameterList();
        // strip leading V value if present
        int vsize = (returnType == void.class ? 0 : 1);
        if (vsize != 0 && (innerList.size() == 0 || innerList.get(0) != returnType)) {
            // argument list has no "V" => error
            MethodType expected = bodyType.insertParameterTypes(0, returnType);
            throw misMatchedTypes("body function", bodyType, expected);
        } else if (innerList.size() <= vsize || innerList.get(vsize) != counterType) {
            // missing I type => error
            MethodType expected = bodyType.insertParameterTypes(vsize, counterType);
            throw misMatchedTypes("body function", bodyType, expected);
        }
        List<Class<?>> outerList = innerList.subList(vsize + 1, innerList.size());
        if (outerList.isEmpty()) {
            // special case; take lists from end handle
            outerList = end.type().parameterList();
            innerList = bodyType.insertParameterTypes(vsize + 1, outerList).parameterList();
        }
        MethodType expected = methodType(counterType, outerList);
        if (!start.type().effectivelyIdenticalParameters(0, outerList)) {
            throw misMatchedTypes("start parameter types", start.type(), expected);
        }
        if (end.type() != start.type() &&
            !end.type().effectivelyIdenticalParameters(0, outerList)) {
            throw misMatchedTypes("end parameter types", end.type(), expected);
        }
        if (init != null) {
            MethodType initType = init.type();
            if (initType.returnType() != returnType ||
                !initType.effectivelyIdenticalParameters(0, outerList)) {
                throw misMatchedTypes("loop initializer", initType, methodType(returnType, outerList));
            }
        }
    }


    public static MethodHandle iteratedLoop(MethodHandle iterator, MethodHandle init, MethodHandle body) {
        Class<?> iterableType = iteratedLoopChecks(iterator, init, body);
        Class<?> returnType = body.type().returnType();
        MethodHandle hasNext = MethodHandleImpl.getConstantHandle(MethodHandleImpl.MH_iteratePred);
        MethodHandle nextRaw = MethodHandleImpl.getConstantHandle(MethodHandleImpl.MH_iterateNext);
        MethodHandle startIter;
        MethodHandle nextVal;
        {
            MethodType iteratorType;
            if (iterator == null) {
                // derive argument type from body, if available, else use Iterable
                startIter = MethodHandleImpl.getConstantHandle(MethodHandleImpl.MH_initIterator);
                iteratorType = startIter.type().changeParameterType(0, iterableType);
            } else {
                // force return type to the internal iterator class
                iteratorType = iterator.type().changeReturnType(Iterator.class);
                startIter = iterator;
            }
            Class<?> ttype = body.type().parameterType(returnType == void.class ? 0 : 1);
            MethodType nextValType = nextRaw.type().changeReturnType(ttype);

            // perform the asType transforms under an exception transformer, as per spec.:
            try {
                startIter = startIter.asType(iteratorType);
                nextVal = nextRaw.asType(nextValType);
            } catch (WrongMethodTypeException ex) {
                throw new IllegalArgumentException(ex);
            }
        }

        MethodHandle retv = null, step = body;
        if (returnType != void.class) {
            // the simple thing first:  in (I V A...), drop the I to get V
            retv = dropArguments(identity(returnType), 0, Iterator.class);
            // body type signature (V T A...), internal loop types (I V A...)
            step = swapArguments(body, 0, 1);  // swap V <-> T
        }

        MethodHandle[]
            iterVar    = { startIter, null, hasNext, retv },
            bodyClause = { init, filterArgument(step, 0, nextVal) };
        return loop(iterVar, bodyClause);
    }

    private static Class<?> iteratedLoopChecks(MethodHandle iterator, MethodHandle init, MethodHandle body) {
        Objects.requireNonNull(body);
        MethodType bodyType = body.type();
        Class<?> returnType = bodyType.returnType();
        List<Class<?>> internalParamList = bodyType.parameterList();
        // strip leading V value if present
        int vsize = (returnType == void.class ? 0 : 1);
        if (vsize != 0 && (internalParamList.size() == 0 || internalParamList.get(0) != returnType)) {
            // argument list has no "V" => error
            MethodType expected = bodyType.insertParameterTypes(0, returnType);
            throw misMatchedTypes("body function", bodyType, expected);
        } else if (internalParamList.size() <= vsize) {
            // missing T type => error
            MethodType expected = bodyType.insertParameterTypes(vsize, Object.class);
            throw misMatchedTypes("body function", bodyType, expected);
        }
        List<Class<?>> externalParamList = internalParamList.subList(vsize + 1, internalParamList.size());
        Class<?> iterableType = null;
        if (iterator != null) {
            // special case; if the body handle only declares V and T then
            // the external parameter list is obtained from iterator handle
            if (externalParamList.isEmpty()) {
                externalParamList = iterator.type().parameterList();
            }
            MethodType itype = iterator.type();
            if (!Iterator.class.isAssignableFrom(itype.returnType())) {
                throw newIllegalArgumentException("iteratedLoop first argument must have Iterator return type");
            }
            if (!itype.effectivelyIdenticalParameters(0, externalParamList)) {
                MethodType expected = methodType(itype.returnType(), externalParamList);
                throw misMatchedTypes("iterator parameters", itype, expected);
            }
        } else {
            if (externalParamList.isEmpty()) {
                // special case; if the iterator handle is null and the body handle
                // only declares V and T then the external parameter list consists
                // of Iterable
                externalParamList = Arrays.asList(Iterable.class);
                iterableType = Iterable.class;
            } else {
                // special case; if the iterator handle is null and the external
                // parameter list is not empty then the first parameter must be
                // assignable to Iterable
                iterableType = externalParamList.get(0);
                if (!Iterable.class.isAssignableFrom(iterableType)) {
                    throw newIllegalArgumentException(
                            "inferred first loop argument must inherit from Iterable: " + iterableType);
                }
            }
        }
        if (init != null) {
            MethodType initType = init.type();
            if (initType.returnType() != returnType ||
                    !initType.effectivelyIdenticalParameters(0, externalParamList)) {
                throw misMatchedTypes("loop initializer", initType, methodType(returnType, externalParamList));
            }
        }
        return iterableType;  // help the caller a bit
    }

    /*non-public*/ static MethodHandle swapArguments(MethodHandle mh, int i, int j) {
        // there should be a better way to uncross my wires
        int arity = mh.type().parameterCount();
        int[] order = new int[arity];
        for (int k = 0; k < arity; k++)  order[k] = k;
        order[i] = j; order[j] = i;
        Class<?>[] types = mh.type().parameterArray();
        Class<?> ti = types[i]; types[i] = types[j]; types[j] = ti;
        MethodType swapType = methodType(mh.type().returnType(), types);
        return permuteArguments(mh, swapType, order);
    }


    public static MethodHandle tryFinally(MethodHandle target, MethodHandle cleanup) {
        List<Class<?>> targetParamTypes = target.type().parameterList();
        List<Class<?>> cleanupParamTypes = cleanup.type().parameterList();
        Class<?> rtype = target.type().returnType();

        tryFinallyChecks(target, cleanup);

        // Match parameter lists: if the cleanup has a shorter parameter list than the target, add ignored arguments.
        // The cleanup parameter list (minus the leading Throwable and result parameters) must be a sublist of the
        // target parameter list.
        cleanup = dropArgumentsToMatch(cleanup, (rtype == void.class ? 1 : 2), targetParamTypes, 0);

        // Use asFixedArity() to avoid unnecessary boxing of last argument for VarargsCollector case.
        return MethodHandleImpl.makeTryFinally(target.asFixedArity(), cleanup.asFixedArity(), rtype, targetParamTypes);
    }

    private static void tryFinallyChecks(MethodHandle target, MethodHandle cleanup) {
        Class<?> rtype = target.type().returnType();
        if (rtype != cleanup.type().returnType()) {
            throw misMatchedTypes("target and return types", cleanup.type().returnType(), rtype);
        }
        MethodType cleanupType = cleanup.type();
        if (!Throwable.class.isAssignableFrom(cleanupType.parameterType(0))) {
            throw misMatchedTypes("cleanup first argument and Throwable", cleanup.type(), Throwable.class);
        }
        if (rtype != void.class && cleanupType.parameterType(1) != rtype) {
            throw misMatchedTypes("cleanup second argument and target return type", cleanup.type(), rtype);
        }
        // The cleanup parameter list (minus the leading Throwable and result parameters) must be a sublist of the
        // target parameter list.
        int cleanupArgIndex = rtype == void.class ? 1 : 2;
        if (!cleanupType.effectivelyIdenticalParameters(cleanupArgIndex, target.type().parameterList())) {
            throw misMatchedTypes("cleanup parameters after (Throwable,result) and target parameter list prefix",
                    cleanup.type(), target.type());
        }
    }

}
