/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
import jdk.internal.util.Preconditions;
import jdk.internal.vm.annotation.ForceInline;
import jdk.internal.vm.annotation.Stable;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.lang.invoke.MethodHandleStatics.UNSAFE;
import static java.lang.invoke.MethodHandleStatics.newInternalError;


public abstract class VarHandle {
    final VarForm vform;

    VarHandle(VarForm vform) {
        this.vform = vform;
    }

    RuntimeException unsupported() {
        return new UnsupportedOperationException();
    }

    // Plain accessors


    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    Object get(Object... args);


    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    void set(Object... args);


    // Volatile accessors


    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    Object getVolatile(Object... args);


    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    void setVolatile(Object... args);



    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    Object getOpaque(Object... args);


    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    void setOpaque(Object... args);


    // Lazy accessors


    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    Object getAcquire(Object... args);


    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    void setRelease(Object... args);


    // Compare and set accessors


    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    boolean compareAndSet(Object... args);


    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    Object compareAndExchange(Object... args);


    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    Object compareAndExchangeAcquire(Object... args);


    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    Object compareAndExchangeRelease(Object... args);

    // Weak (spurious failures allowed)


    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    boolean weakCompareAndSetPlain(Object... args);


    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    boolean weakCompareAndSet(Object... args);


    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    boolean weakCompareAndSetAcquire(Object... args);


    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    boolean weakCompareAndSetRelease(Object... args);


    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    Object getAndSet(Object... args);


    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    Object getAndSetAcquire(Object... args);


    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    Object getAndSetRelease(Object... args);

    // Primitive adders
    // Throw UnsupportedOperationException for refs


    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    Object getAndAdd(Object... args);


    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    Object getAndAddAcquire(Object... args);


    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    Object getAndAddRelease(Object... args);


    // Bitwise operations
    // Throw UnsupportedOperationException for refs


    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    Object getAndBitwiseOr(Object... args);


    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    Object getAndBitwiseOrAcquire(Object... args);


    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    Object getAndBitwiseOrRelease(Object... args);


    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    Object getAndBitwiseAnd(Object... args);


    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    Object getAndBitwiseAndAcquire(Object... args);


    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    Object getAndBitwiseAndRelease(Object... args);


    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    Object getAndBitwiseXor(Object... args);


    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    Object getAndBitwiseXorAcquire(Object... args);


    public final native
    @MethodHandle.PolymorphicSignature
    @HotSpotIntrinsicCandidate
    Object getAndBitwiseXorRelease(Object... args);


    enum AccessType {
        GET(Object.class),
        SET(void.class),
        COMPARE_AND_SWAP(boolean.class),
        COMPARE_AND_EXCHANGE(Object.class),
        GET_AND_UPDATE(Object.class);

        final Class<?> returnType;
        final boolean isMonomorphicInReturnType;

        AccessType(Class<?> returnType) {
            this.returnType = returnType;
            isMonomorphicInReturnType = returnType != Object.class;
        }

        MethodType accessModeType(Class<?> receiver, Class<?> value,
                                  Class<?>... intermediate) {
            Class<?>[] ps;
            int i;
            switch (this) {
                case GET:
                    ps = allocateParameters(0, receiver, intermediate);
                    fillParameters(ps, receiver, intermediate);
                    return MethodType.methodType(value, ps);
                case SET:
                    ps = allocateParameters(1, receiver, intermediate);
                    i = fillParameters(ps, receiver, intermediate);
                    ps[i] = value;
                    return MethodType.methodType(void.class, ps);
                case COMPARE_AND_SWAP:
                    ps = allocateParameters(2, receiver, intermediate);
                    i = fillParameters(ps, receiver, intermediate);
                    ps[i++] = value;
                    ps[i] = value;
                    return MethodType.methodType(boolean.class, ps);
                case COMPARE_AND_EXCHANGE:
                    ps = allocateParameters(2, receiver, intermediate);
                    i = fillParameters(ps, receiver, intermediate);
                    ps[i++] = value;
                    ps[i] = value;
                    return MethodType.methodType(value, ps);
                case GET_AND_UPDATE:
                    ps = allocateParameters(1, receiver, intermediate);
                    i = fillParameters(ps, receiver, intermediate);
                    ps[i] = value;
                    return MethodType.methodType(value, ps);
                default:
                    throw new InternalError("Unknown AccessType");
            }
        }

        private static Class<?>[] allocateParameters(int values,
                                                     Class<?> receiver, Class<?>... intermediate) {
            int size = ((receiver != null) ? 1 : 0) + intermediate.length + values;
            return new Class<?>[size];
        }

        private static int fillParameters(Class<?>[] ps,
                                          Class<?> receiver, Class<?>... intermediate) {
            int i = 0;
            if (receiver != null)
                ps[i++] = receiver;
            for (int j = 0; j < intermediate.length; j++)
                ps[i++] = intermediate[j];
            return i;
        }
    }


    public enum AccessMode {

        GET("get", AccessType.GET),

        SET("set", AccessType.SET),

        GET_VOLATILE("getVolatile", AccessType.GET),

        SET_VOLATILE("setVolatile", AccessType.SET),

        GET_ACQUIRE("getAcquire", AccessType.GET),

        SET_RELEASE("setRelease", AccessType.SET),

        GET_OPAQUE("getOpaque", AccessType.GET),

        SET_OPAQUE("setOpaque", AccessType.SET),

        COMPARE_AND_SET("compareAndSet", AccessType.COMPARE_AND_SWAP),

        COMPARE_AND_EXCHANGE("compareAndExchange", AccessType.COMPARE_AND_EXCHANGE),

        COMPARE_AND_EXCHANGE_ACQUIRE("compareAndExchangeAcquire", AccessType.COMPARE_AND_EXCHANGE),

        COMPARE_AND_EXCHANGE_RELEASE("compareAndExchangeRelease", AccessType.COMPARE_AND_EXCHANGE),

        WEAK_COMPARE_AND_SET_PLAIN("weakCompareAndSetPlain", AccessType.COMPARE_AND_SWAP),

        WEAK_COMPARE_AND_SET("weakCompareAndSet", AccessType.COMPARE_AND_SWAP),

        WEAK_COMPARE_AND_SET_ACQUIRE("weakCompareAndSetAcquire", AccessType.COMPARE_AND_SWAP),

        WEAK_COMPARE_AND_SET_RELEASE("weakCompareAndSetRelease", AccessType.COMPARE_AND_SWAP),

        GET_AND_SET("getAndSet", AccessType.GET_AND_UPDATE),

        GET_AND_SET_ACQUIRE("getAndSetAcquire", AccessType.GET_AND_UPDATE),

        GET_AND_SET_RELEASE("getAndSetRelease", AccessType.GET_AND_UPDATE),

        GET_AND_ADD("getAndAdd", AccessType.GET_AND_UPDATE),

        GET_AND_ADD_ACQUIRE("getAndAddAcquire", AccessType.GET_AND_UPDATE),

        GET_AND_ADD_RELEASE("getAndAddRelease", AccessType.GET_AND_UPDATE),

        GET_AND_BITWISE_OR("getAndBitwiseOr", AccessType.GET_AND_UPDATE),

        GET_AND_BITWISE_OR_RELEASE("getAndBitwiseOrRelease", AccessType.GET_AND_UPDATE),

        GET_AND_BITWISE_OR_ACQUIRE("getAndBitwiseOrAcquire", AccessType.GET_AND_UPDATE),

        GET_AND_BITWISE_AND("getAndBitwiseAnd", AccessType.GET_AND_UPDATE),

        GET_AND_BITWISE_AND_RELEASE("getAndBitwiseAndRelease", AccessType.GET_AND_UPDATE),

        GET_AND_BITWISE_AND_ACQUIRE("getAndBitwiseAndAcquire", AccessType.GET_AND_UPDATE),

        GET_AND_BITWISE_XOR("getAndBitwiseXor", AccessType.GET_AND_UPDATE),

        GET_AND_BITWISE_XOR_RELEASE("getAndBitwiseXorRelease", AccessType.GET_AND_UPDATE),

        GET_AND_BITWISE_XOR_ACQUIRE("getAndBitwiseXorAcquire", AccessType.GET_AND_UPDATE),
        ;

        static final Map<String, AccessMode> methodNameToAccessMode;
        static {
            // Initial capacity of # values is sufficient to avoid resizes
            // for the smallest table size (32)
            methodNameToAccessMode = new HashMap<>(AccessMode.values().length);
            for (AccessMode am : AccessMode.values()) {
                methodNameToAccessMode.put(am.methodName, am);
            }
        }

        final String methodName;
        final AccessType at;

        AccessMode(final String methodName, AccessType at) {
            this.methodName = methodName;
            this.at = at;
        }


        public String methodName() {
            return methodName;
        }


        public static AccessMode valueFromMethodName(String methodName) {
            AccessMode am = methodNameToAccessMode.get(methodName);
            if (am != null) return am;
            throw new IllegalArgumentException("No AccessMode value for method name " + methodName);
        }

        @ForceInline
        static MemberName getMemberName(int ordinal, VarForm vform) {
            return vform.memberName_table[ordinal];
        }
    }

    static final class AccessDescriptor {
        final MethodType symbolicMethodTypeErased;
        final MethodType symbolicMethodTypeInvoker;
        final Class<?> returnType;
        final int type;
        final int mode;

        public AccessDescriptor(MethodType symbolicMethodType, int type, int mode) {
            this.symbolicMethodTypeErased = symbolicMethodType.erase();
            this.symbolicMethodTypeInvoker = symbolicMethodType.insertParameterTypes(0, VarHandle.class);
            this.returnType = symbolicMethodType.returnType();
            this.type = type;
            this.mode = mode;
        }
    }


    public final Class<?> varType() {
        MethodType typeSet = accessModeType(AccessMode.SET);
        return typeSet.parameterType(typeSet.parameterCount() - 1);
    }


    public final List<Class<?>> coordinateTypes() {
        MethodType typeGet = accessModeType(AccessMode.GET);
        return typeGet.parameterList();
    }


    public final MethodType accessModeType(AccessMode accessMode) {
        TypesAndInvokers tis = getTypesAndInvokers();
        MethodType mt = tis.methodType_table[accessMode.at.ordinal()];
        if (mt == null) {
            mt = tis.methodType_table[accessMode.at.ordinal()] =
                    accessModeTypeUncached(accessMode);
        }
        return mt;
    }
    abstract MethodType accessModeTypeUncached(AccessMode accessMode);


    public final boolean isAccessModeSupported(AccessMode accessMode) {
        return AccessMode.getMemberName(accessMode.ordinal(), vform) != null;
    }


    public final MethodHandle toMethodHandle(AccessMode accessMode) {
        MemberName mn = AccessMode.getMemberName(accessMode.ordinal(), vform);
        if (mn != null) {
            MethodHandle mh = getMethodHandle(accessMode.ordinal());
            return mh.bindTo(this);
        }
        else {
            // Ensure an UnsupportedOperationException is thrown
            return MethodHandles.varHandleInvoker(accessMode, accessModeType(accessMode)).
                    bindTo(this);
        }
    }

    @Stable
    TypesAndInvokers typesAndInvokers;

    static class TypesAndInvokers {
        final @Stable
        MethodType[] methodType_table =
                new MethodType[VarHandle.AccessType.values().length];

        final @Stable
        MethodHandle[] methodHandle_table =
                new MethodHandle[AccessMode.values().length];
    }

    @ForceInline
    private final TypesAndInvokers getTypesAndInvokers() {
        TypesAndInvokers tis = typesAndInvokers;
        if (tis == null) {
            tis = typesAndInvokers = new TypesAndInvokers();
        }
        return tis;
    }

    @ForceInline
    final MethodHandle getMethodHandle(int mode) {
        TypesAndInvokers tis = getTypesAndInvokers();
        MethodHandle mh = tis.methodHandle_table[mode];
        if (mh == null) {
            mh = tis.methodHandle_table[mode] = getMethodHandleUncached(mode);
        }
        return mh;
    }
    private final MethodHandle getMethodHandleUncached(int mode) {
        MethodType mt = accessModeType(AccessMode.values()[mode]).
                insertParameterTypes(0, VarHandle.class);
        MemberName mn = vform.getMemberName(mode);
        DirectMethodHandle dmh = DirectMethodHandle.make(mn);
        // Such a method handle must not be publically exposed directly
        // otherwise it can be cracked, it must be transformed or rebound
        // before exposure
        MethodHandle mh = dmh.copyWith(mt, dmh.form);
        assert mh.type().erase() == mn.getMethodType().erase();
        return mh;
    }


    /*non-public*/
    final void updateVarForm(VarForm newVForm) {
        if (vform == newVForm) return;
        UNSAFE.putObject(this, VFORM_OFFSET, newVForm);
        UNSAFE.fullFence();
    }

    static final BiFunction<String, List<Integer>, ArrayIndexOutOfBoundsException>
            AIOOBE_SUPPLIER = Preconditions.outOfBoundsExceptionFormatter(
            new Function<String, ArrayIndexOutOfBoundsException>() {
                @Override
                public ArrayIndexOutOfBoundsException apply(String s) {
                    return new ArrayIndexOutOfBoundsException(s);
                }
            });

    private static final long VFORM_OFFSET;

    static {
        try {
            VFORM_OFFSET = UNSAFE.objectFieldOffset(VarHandle.class.getDeclaredField("vform"));
        }
        catch (ReflectiveOperationException e) {
            throw newInternalError(e);
        }

        // The VarHandleGuards must be initialized to ensure correct
        // compilation of the guard methods
        UNSAFE.ensureClassInitialized(VarHandleGuards.class);
    }


    // Fence methods


    @ForceInline
    public static void fullFence() {
        UNSAFE.fullFence();
    }


    @ForceInline
    public static void acquireFence() {
        UNSAFE.loadFence();
    }


    @ForceInline
    public static void releaseFence() {
        UNSAFE.storeFence();
    }


    @ForceInline
    public static void loadLoadFence() {
        UNSAFE.loadLoadFence();
    }


    @ForceInline
    public static void storeStoreFence() {
        UNSAFE.storeStoreFence();
    }
}
