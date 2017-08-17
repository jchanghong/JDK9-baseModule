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

import static java.lang.invoke.MethodHandleStatics.*;
import static java.lang.invoke.MethodHandles.Lookup.IMPL_LOOKUP;

import jdk.internal.vm.annotation.Stable;


abstract
public class CallSite {

    // The actual payload of this call site:
    /*package-private*/
    MethodHandle target;    // Note: This field is known to the JVM.  Do not change.


    /*package-private*/
    CallSite(MethodType type) {
        target = makeUninitializedCallSite(type);
    }


    /*package-private*/
    CallSite(MethodHandle target) {
        target.type();  // null check
        this.target = target;
    }


    /*package-private*/
    CallSite(MethodType targetType, MethodHandle createTargetHook) throws Throwable {
        this(targetType);
        ConstantCallSite selfCCS = (ConstantCallSite) this;
        MethodHandle boundTarget = (MethodHandle) createTargetHook.invokeWithArguments(selfCCS);
        checkTargetChange(this.target, boundTarget);
        this.target = boundTarget;
    }


    private final MethodHandleNatives.CallSiteContext context = MethodHandleNatives.CallSiteContext.make(this);


    public MethodType type() {
        // warning:  do not call getTarget here, because CCS.getTarget can throw IllegalStateException
        return target.type();
    }


    public abstract MethodHandle getTarget();


    public abstract void setTarget(MethodHandle newTarget);

    void checkTargetChange(MethodHandle oldTarget, MethodHandle newTarget) {
        MethodType oldType = oldTarget.type();
        MethodType newType = newTarget.type();  // null check!
        if (!newType.equals(oldType))
            throw wrongTargetType(newTarget, oldType);
    }

    private static WrongMethodTypeException wrongTargetType(MethodHandle target, MethodType type) {
        return new WrongMethodTypeException(String.valueOf(target)+" should be of type "+type);
    }


    public abstract MethodHandle dynamicInvoker();

    /*non-public*/ MethodHandle makeDynamicInvoker() {
        MethodHandle getTarget = getTargetHandle().bindArgumentL(0, this);
        MethodHandle invoker = MethodHandles.exactInvoker(this.type());
        return MethodHandles.foldArguments(invoker, getTarget);
    }

    private static @Stable MethodHandle GET_TARGET;
    private static MethodHandle getTargetHandle() {
        MethodHandle handle = GET_TARGET;
        if (handle != null) {
            return handle;
        }
        try {
            return GET_TARGET = IMPL_LOOKUP.
                    findVirtual(CallSite.class, "getTarget",
                                MethodType.methodType(MethodHandle.class));
        } catch (ReflectiveOperationException e) {
            throw newInternalError(e);
        }
    }

    private static @Stable MethodHandle THROW_UCS;
    private static MethodHandle uninitializedCallSiteHandle() {
        MethodHandle handle = THROW_UCS;
        if (handle != null) {
            return handle;
        }
        try {
            return THROW_UCS = IMPL_LOOKUP.
                findStatic(CallSite.class, "uninitializedCallSite",
                           MethodType.methodType(Object.class, Object[].class));
        } catch (ReflectiveOperationException e) {
            throw newInternalError(e);
        }
    }


    private static Object uninitializedCallSite(Object... ignore) {
        throw new IllegalStateException("uninitialized call site");
    }

    private MethodHandle makeUninitializedCallSite(MethodType targetType) {
        MethodType basicType = targetType.basicType();
        MethodHandle invoker = basicType.form().cachedMethodHandle(MethodTypeForm.MH_UNINIT_CS);
        if (invoker == null) {
            invoker = uninitializedCallSiteHandle().asType(basicType);
            invoker = basicType.form().setCachedMethodHandle(MethodTypeForm.MH_UNINIT_CS, invoker);
        }
        // unchecked view is OK since no values will be received or returned
        return invoker.viewAsType(targetType, false);
    }

    // unsafe stuff:
    private static @Stable long TARGET_OFFSET;
    private static long getTargetOffset() {
        long offset = TARGET_OFFSET;
        if (offset > 0) {
            return offset;
        }
        try {
            offset = TARGET_OFFSET = UNSAFE.objectFieldOffset(CallSite.class.getDeclaredField("target"));
            assert(offset > 0);
            return offset;
        } catch (Exception ex) { throw newInternalError(ex); }
    }

    /*package-private*/
    void setTargetNormal(MethodHandle newTarget) {
        MethodHandleNatives.setCallSiteTargetNormal(this, newTarget);
    }
    /*package-private*/
    MethodHandle getTargetVolatile() {
        return (MethodHandle) UNSAFE.getObjectVolatile(this, getTargetOffset());
    }
    /*package-private*/
    void setTargetVolatile(MethodHandle newTarget) {
        MethodHandleNatives.setCallSiteTargetVolatile(this, newTarget);
    }

    // this implements the upcall from the JVM, MethodHandleNatives.linkCallSite:
    static CallSite makeSite(MethodHandle bootstrapMethod,
                             // Callee information:
                             String name, MethodType type,
                             // Extra arguments for BSM, if any:
                             Object info,
                             // Caller information:
                             Class<?> callerClass) {
        MethodHandles.Lookup caller = IMPL_LOOKUP.in(callerClass);
        CallSite site;
        try {
            Object binding;
            info = maybeReBox(info);
            if (info == null) {
                binding = bootstrapMethod.invoke(caller, name, type);
            } else if (!info.getClass().isArray()) {
                binding = bootstrapMethod.invoke(caller, name, type, info);
            } else {
                Object[] argv = (Object[]) info;
                maybeReBoxElements(argv);
                switch (argv.length) {
                    case 0:
                        binding = bootstrapMethod.invoke(caller, name, type);
                        break;
                    case 1:
                        binding = bootstrapMethod.invoke(caller, name, type,
                                                         argv[0]);
                        break;
                    case 2:
                        binding = bootstrapMethod.invoke(caller, name, type,
                                                         argv[0], argv[1]);
                        break;
                    case 3:
                        binding = bootstrapMethod.invoke(caller, name, type,
                                                         argv[0], argv[1], argv[2]);
                        break;
                    case 4:
                        binding = bootstrapMethod.invoke(caller, name, type,
                                                         argv[0], argv[1], argv[2], argv[3]);
                        break;
                    case 5:
                        binding = bootstrapMethod.invoke(caller, name, type,
                                                         argv[0], argv[1], argv[2], argv[3], argv[4]);
                        break;
                    case 6:
                        binding = bootstrapMethod.invoke(caller, name, type,
                                                         argv[0], argv[1], argv[2], argv[3], argv[4], argv[5]);
                        break;
                    default:
                        final int NON_SPREAD_ARG_COUNT = 3;  // (caller, name, type)
                        if (NON_SPREAD_ARG_COUNT + argv.length > MethodType.MAX_MH_ARITY)
                            throw new BootstrapMethodError("too many bootstrap method arguments");

                        MethodType invocationType = MethodType.genericMethodType(NON_SPREAD_ARG_COUNT + argv.length);
                        MethodHandle typedBSM = bootstrapMethod.asType(invocationType);
                        MethodHandle spreader = invocationType.invokers().spreadInvoker(NON_SPREAD_ARG_COUNT);
                        binding = spreader.invokeExact(typedBSM, (Object) caller, (Object) name, (Object) type, argv);
                }
            }
            if (binding instanceof CallSite) {
                site = (CallSite) binding;
            } else {
                // See the "Linking Exceptions" section for the invokedynamic
                // instruction in JVMS 6.5.
                // Throws a runtime exception defining the cause that is then
                // in the "catch (Throwable ex)" a few lines below wrapped in
                // BootstrapMethodError
                throw new ClassCastException("bootstrap method failed to produce a CallSite");
            }
            if (!site.getTarget().type().equals(type)) {
                // See the "Linking Exceptions" section for the invokedynamic
                // instruction in JVMS 6.5.
                // Throws a runtime exception defining the cause that is then
                // in the "catch (Throwable ex)" a few lines below wrapped in
                // BootstrapMethodError
                throw wrongTargetType(site.getTarget(), type);
            }
        } catch (Error e) {
            // Pass through an Error, including BootstrapMethodError, any other
            // form of linkage error, such as IllegalAccessError if the bootstrap
            // method is inaccessible, or say ThreadDeath/OutOfMemoryError
            // See the "Linking Exceptions" section for the invokedynamic
            // instruction in JVMS 6.5.
            throw e;
        } catch (Throwable ex) {
            // Wrap anything else in BootstrapMethodError
            throw new BootstrapMethodError("call site initialization exception", ex);
        }
        return site;
    }

    private static Object maybeReBox(Object x) {
        if (x instanceof Integer) {
            int xi = (int) x;
            if (xi == (byte) xi)
                x = xi;  // must rebox; see JLS 5.1.7
        }
        return x;
    }
    private static void maybeReBoxElements(Object[] xa) {
        for (int i = 0; i < xa.length; i++) {
            xa[i] = maybeReBox(xa[i]);
        }
    }
}
