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


import jdk.internal.HotSpotIntrinsicCandidate;

import java.util.Arrays;
import java.util.Objects;

import static java.lang.invoke.MethodHandleStatics.*;


public abstract class MethodHandle {


    @java.lang.annotation.Target({java.lang.annotation.ElementType.METHOD})
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @interface PolymorphicSignature { }

    private final MethodType type;
    /*private*/ final LambdaForm form;
    // form is not private so that invokers can easily fetch it
    /*private*/ MethodHandle asTypeCache;
    // asTypeCache is not private so that invokers can easily fetch it
    /*non-public*/ byte customizationCount;
    // customizationCount should be accessible from invokers


    public MethodType type() {
        return type;
    }


    // @param type type (permanently assigned) of the new method handle
    /*non-public*/ MethodHandle(MethodType type, LambdaForm form) {
        this.type = Objects.requireNonNull(type);
        this.form = Objects.requireNonNull(form).uncustomize();

        this.form.prepare();  // TO DO:  Try to delay this step until just before invocation.
    }


    @HotSpotIntrinsicCandidate
    public final native @PolymorphicSignature Object invokeExact(Object... args) throws Throwable;


    @HotSpotIntrinsicCandidate
    public final native @PolymorphicSignature Object invoke(Object... args) throws Throwable;


    @HotSpotIntrinsicCandidate
    /*non-public*/ final native @PolymorphicSignature Object invokeBasic(Object... args) throws Throwable;


    @HotSpotIntrinsicCandidate
    /*non-public*/ static native @PolymorphicSignature Object linkToVirtual(Object... args) throws Throwable;


    @HotSpotIntrinsicCandidate
    /*non-public*/ static native @PolymorphicSignature Object linkToStatic(Object... args) throws Throwable;


    @HotSpotIntrinsicCandidate
    /*non-public*/ static native @PolymorphicSignature Object linkToSpecial(Object... args) throws Throwable;


    @HotSpotIntrinsicCandidate
    /*non-public*/ static native @PolymorphicSignature Object linkToInterface(Object... args) throws Throwable;


    public Object invokeWithArguments(Object... arguments) throws Throwable {
        MethodType invocationType = MethodType.genericMethodType(arguments == null ? 0 : arguments.length);
        return invocationType.invokers().spreadInvoker(0).invokeExact(asType(invocationType), arguments);
    }


    public Object invokeWithArguments(java.util.List<?> arguments) throws Throwable {
        return invokeWithArguments(arguments.toArray());
    }


    public MethodHandle asType(MethodType newType) {
        // Fast path alternative to a heavyweight {@code asType} call.
        // Return 'this' if the conversion will be a no-op.
        if (newType == type) {
            return this;
        }
        // Return 'this.asTypeCache' if the conversion is already memoized.
        MethodHandle atc = asTypeCached(newType);
        if (atc != null) {
            return atc;
        }
        return asTypeUncached(newType);
    }

    private MethodHandle asTypeCached(MethodType newType) {
        MethodHandle atc = asTypeCache;
        if (atc != null && newType == atc.type) {
            return atc;
        }
        return null;
    }


    /*non-public*/ MethodHandle asTypeUncached(MethodType newType) {
        if (!type.isConvertibleTo(newType))
            throw new WrongMethodTypeException("cannot convert "+this+" to "+newType);
        return asTypeCache = MethodHandleImpl.makePairwiseConvert(this, newType, true);
    }


    public MethodHandle asSpreader(Class<?> arrayType, int arrayLength) {
        return asSpreader(type().parameterCount() - arrayLength, arrayType, arrayLength);
    }


    public MethodHandle asSpreader(int spreadArgPos, Class<?> arrayType, int arrayLength) {
        MethodType postSpreadType = asSpreaderChecks(arrayType, spreadArgPos, arrayLength);
        MethodHandle afterSpread = this.asType(postSpreadType);
        BoundMethodHandle mh = afterSpread.rebind();
        LambdaForm lform = mh.editor().spreadArgumentsForm(1 + spreadArgPos, arrayType, arrayLength);
        MethodType preSpreadType = postSpreadType.replaceParameterTypes(spreadArgPos, spreadArgPos + arrayLength, arrayType);
        return mh.copyWith(preSpreadType, lform);
    }


    private MethodType asSpreaderChecks(Class<?> arrayType, int pos, int arrayLength) {
        spreadArrayChecks(arrayType, arrayLength);
        int nargs = type().parameterCount();
        if (nargs < arrayLength || arrayLength < 0)
            throw newIllegalArgumentException("bad spread array length");
        if (pos < 0 || pos + arrayLength > nargs) {
            throw newIllegalArgumentException("bad spread position");
        }
        Class<?> arrayElement = arrayType.getComponentType();
        MethodType mtype = type();
        boolean match = true, fail = false;
        for (int i = pos; i < pos + arrayLength; i++) {
            Class<?> ptype = mtype.parameterType(i);
            if (ptype != arrayElement) {
                match = false;
                if (!MethodType.canConvert(arrayElement, ptype)) {
                    fail = true;
                    break;
                }
            }
        }
        if (match)  return mtype;
        MethodType needType = mtype.asSpreaderType(arrayType, pos, arrayLength);
        if (!fail)  return needType;
        // elicit an error:
        this.asType(needType);
        throw newInternalError("should not return");
    }

    private void spreadArrayChecks(Class<?> arrayType, int arrayLength) {
        Class<?> arrayElement = arrayType.getComponentType();
        if (arrayElement == null)
            throw newIllegalArgumentException("not an array type", arrayType);
        if ((arrayLength & 0x7F) != arrayLength) {
            if ((arrayLength & 0xFF) != arrayLength)
                throw newIllegalArgumentException("array length is not legal", arrayLength);
            assert(arrayLength >= 128);
            if (arrayElement == long.class ||
                arrayElement == double.class)
                throw newIllegalArgumentException("array length is not legal for long[] or double[]", arrayLength);
        }
    }

     public MethodHandle withVarargs(boolean makeVarargs) {
        if (!makeVarargs) {
            return asFixedArity();
        } else if (!isVarargsCollector()) {
            return asVarargsCollector(type().lastParameterType());
        } else {
            return this;
        }
    }


    public MethodHandle asCollector(Class<?> arrayType, int arrayLength) {
        return asCollector(type().parameterCount() - 1, arrayType, arrayLength);
    }


    public MethodHandle asCollector(int collectArgPos, Class<?> arrayType, int arrayLength) {
        asCollectorChecks(arrayType, collectArgPos, arrayLength);
        BoundMethodHandle mh = rebind();
        MethodType resultType = type().asCollectorType(arrayType, collectArgPos, arrayLength);
        MethodHandle newArray = MethodHandleImpl.varargsArray(arrayType, arrayLength);
        LambdaForm lform = mh.editor().collectArgumentArrayForm(1 + collectArgPos, newArray);
        if (lform != null) {
            return mh.copyWith(resultType, lform);
        }
        lform = mh.editor().collectArgumentsForm(1 + collectArgPos, newArray.type().basicType());
        return mh.copyWithExtendL(resultType, lform, newArray);
    }


    /*non-public*/ boolean asCollectorChecks(Class<?> arrayType, int pos, int arrayLength) {
        spreadArrayChecks(arrayType, arrayLength);
        int nargs = type().parameterCount();
        if (pos < 0 || pos >= nargs) {
            throw newIllegalArgumentException("bad collect position");
        }
        if (nargs != 0) {
            Class<?> param = type().parameterType(pos);
            if (param == arrayType)  return true;
            if (param.isAssignableFrom(arrayType))  return false;
        }
        throw newIllegalArgumentException("array type not assignable to argument", this, arrayType);
    }


    public MethodHandle asVarargsCollector(Class<?> arrayType) {
        Objects.requireNonNull(arrayType);
        boolean lastMatch = asCollectorChecks(arrayType, type().parameterCount() - 1, 0);
        if (isVarargsCollector() && lastMatch)
            return this;
        return MethodHandleImpl.makeVarargsCollector(this, arrayType);
    }


    public boolean isVarargsCollector() {
        return false;
    }


    public MethodHandle asFixedArity() {
        assert(!isVarargsCollector());
        return this;
    }


    public MethodHandle bindTo(Object x) {
        x = type.leadingReferenceParameter().cast(x);  // throw CCE if needed
        return bindArgumentL(0, x);
    }


    @Override
    public String toString() {
        if (DEBUG_METHOD_HANDLE_NAMES)  return "MethodHandle"+debugString();
        return standardString();
    }
    String standardString() {
        return "MethodHandle"+type;
    }

    String debugString() {
        return type+" : "+internalForm()+internalProperties();
    }

    //// Implementation methods.
    //// Sub-classes can override these default implementations.
    //// All these methods assume arguments are already validated.

    // Other transforms to do:  convert, explicitCast, permute, drop, filter, fold, GWT, catch

    BoundMethodHandle bindArgumentL(int pos, Object value) {
        return rebind().bindArgumentL(pos, value);
    }

    /*non-public*/
    MethodHandle setVarargs(MemberName member) throws IllegalAccessException {
        if (!member.isVarargs())  return this;
        try {
            return this.withVarargs(true);
        } catch (IllegalArgumentException ex) {
            throw member.makeAccessException("cannot make variable arity", null);
        }
    }

    /*non-public*/
    MethodHandle viewAsType(MethodType newType, boolean strict) {
        // No actual conversions, just a new view of the same method.
        // Note that this operation must not produce a DirectMethodHandle,
        // because retyped DMHs, like any transformed MHs,
        // cannot be cracked into MethodHandleInfo.
        assert viewAsTypeChecks(newType, strict);
        BoundMethodHandle mh = rebind();
        return mh.copyWith(newType, mh.form);
    }

    /*non-public*/
    boolean viewAsTypeChecks(MethodType newType, boolean strict) {
        if (strict) {
            assert(type().isViewableAs(newType, true))
                : Arrays.asList(this, newType);
        } else {
            assert(type().basicType().isViewableAs(newType.basicType(), true))
                : Arrays.asList(this, newType);
        }
        return true;
    }

    // Decoding

    /*non-public*/
    LambdaForm internalForm() {
        return form;
    }

    /*non-public*/
    MemberName internalMemberName() {
        return null;  // DMH returns DMH.member
    }

    /*non-public*/
    Class<?> internalCallerClass() {
        return null;  // caller-bound MH for @CallerSensitive method returns caller
    }

    /*non-public*/
    MethodHandleImpl.Intrinsic intrinsicName() {
        // no special intrinsic meaning to most MHs
        return MethodHandleImpl.Intrinsic.NONE;
    }

    /*non-public*/
    MethodHandle withInternalMemberName(MemberName member, boolean isInvokeSpecial) {
        if (member != null) {
            return MethodHandleImpl.makeWrappedMember(this, member, isInvokeSpecial);
        } else if (internalMemberName() == null) {
            // The required internaMemberName is null, and this MH (like most) doesn't have one.
            return this;
        } else {
            // The following case is rare. Mask the internalMemberName by wrapping the MH in a BMH.
            MethodHandle result = rebind();
            assert (result.internalMemberName() == null);
            return result;
        }
    }

    /*non-public*/
    boolean isInvokeSpecial() {
        return false;  // DMH.Special returns true
    }

    /*non-public*/
    Object internalValues() {
        return null;
    }

    /*non-public*/
    Object internalProperties() {
        // Override to something to follow this.form, like "\n& FOO=bar"
        return "";
    }

    //// Method handle implementation methods.
    //// Sub-classes can override these default implementations.
    //// All these methods assume arguments are already validated.

    /*non-public*/
    abstract MethodHandle copyWith(MethodType mt, LambdaForm lf);


    abstract BoundMethodHandle rebind();


    /*non-public*/
    void updateForm(LambdaForm newForm) {
        assert(newForm.customized == null || newForm.customized == this);
        if (form == newForm)  return;
        newForm.prepare();  // as in MethodHandle.<init>
        UNSAFE.putObject(this, FORM_OFFSET, newForm);
        UNSAFE.fullFence();
    }


    /*non-public*/
    void customize() {
        if (form.customized == null) {
            LambdaForm newForm = form.customize(this);
            updateForm(newForm);
        } else {
            assert(form.customized == this);
        }
    }

    private static final long FORM_OFFSET;
    static {
        try {
            FORM_OFFSET = UNSAFE.objectFieldOffset(MethodHandle.class.getDeclaredField("form"));
        } catch (ReflectiveOperationException ex) {
            throw newInternalError(ex);
        }
    }
}
