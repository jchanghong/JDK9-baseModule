/*
 * Copyright (c) 1996, 2017, Oracle and/or its affiliates. All rights reserved.
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

package java.security;

import java.util.*;
import java.util.regex.*;

import java.security.Provider.Service;

import sun.security.jca.*;
import sun.security.jca.GetInstance.Instance;
import sun.security.util.Debug;



public class SecureRandom extends java.util.Random {

    private static final Debug pdebug =
                        Debug.getInstance("provider", "Provider");
    private static final boolean skipDebug =
        Debug.isOn("engine=") && !Debug.isOn("securerandom");


    private Provider provider = null;


    private SecureRandomSpi secureRandomSpi = null;


    private final boolean threadSafe;

    /*
     * The algorithm name of null if unknown.
     *
     * @serial
     * @since 1.5
     */
    private String algorithm;

    // Seed Generator
    private static volatile SecureRandom seedGenerator;


    public SecureRandom() {
        /*
         * This call to our superclass constructor will result in a call
         * to our own {@code setSeed} method, which will return
         * immediately when it is passed zero.
         */
        super(0);
        getDefaultPRNG(false, null);
        this.threadSafe = getThreadSafe();
    }

    private boolean getThreadSafe() {
        if (provider == null || algorithm == null) {
            return false;
        } else {
            return Boolean.parseBoolean(provider.getProperty(
                    "SecureRandom." + algorithm + " ThreadSafe", "false"));
        }
    }


    public SecureRandom(byte[] seed) {
        super(0);
        getDefaultPRNG(true, seed);
        this.threadSafe = getThreadSafe();
    }

    private void getDefaultPRNG(boolean setSeed, byte[] seed) {
        String prng = getPrngAlgorithm();
        if (prng == null) {
            // bummer, get the SUN implementation
            prng = "SHA1PRNG";
            this.secureRandomSpi = new sun.security.provider.SecureRandom();
            this.provider = Providers.getSunProvider();
            if (setSeed) {
                this.secureRandomSpi.engineSetSeed(seed);
            }
        } else {
            try {
                SecureRandom random = SecureRandom.getInstance(prng);
                this.secureRandomSpi = random.getSecureRandomSpi();
                this.provider = random.getProvider();
                if (setSeed) {
                    this.secureRandomSpi.engineSetSeed(seed);
                }
            } catch (NoSuchAlgorithmException nsae) {
                // never happens, because we made sure the algorithm exists
                throw new RuntimeException(nsae);
            }
        }
        // JDK 1.1 based implementations subclass SecureRandom instead of
        // SecureRandomSpi. They will also go through this code path because
        // they must call a SecureRandom constructor as it is their superclass.
        // If we are dealing with such an implementation, do not set the
        // algorithm value as it would be inaccurate.
        if (getClass() == SecureRandom.class) {
            this.algorithm = prng;
        }
    }


    protected SecureRandom(SecureRandomSpi secureRandomSpi,
                           Provider provider) {
        this(secureRandomSpi, provider, null);
    }

    private SecureRandom(SecureRandomSpi secureRandomSpi, Provider provider,
            String algorithm) {
        super(0);
        this.secureRandomSpi = secureRandomSpi;
        this.provider = provider;
        this.algorithm = algorithm;
        this.threadSafe = getThreadSafe();

        if (!skipDebug && pdebug != null) {
            pdebug.println("SecureRandom." + algorithm +
                " algorithm from: " + getProviderName());
        }
    }

    private String getProviderName() {
        return (provider == null) ? "(no provider)" : provider.getName();
    }


    public static SecureRandom getInstance(String algorithm)
            throws NoSuchAlgorithmException {
        Objects.requireNonNull(algorithm, "null algorithm name");
        Instance instance = GetInstance.getInstance("SecureRandom",
                SecureRandomSpi.class, algorithm);
        return new SecureRandom((SecureRandomSpi)instance.impl,
                instance.provider, algorithm);
    }


    public static SecureRandom getInstance(String algorithm, String provider)
            throws NoSuchAlgorithmException, NoSuchProviderException {
        Objects.requireNonNull(algorithm, "null algorithm name");
        Instance instance = GetInstance.getInstance("SecureRandom",
            SecureRandomSpi.class, algorithm, provider);
        return new SecureRandom((SecureRandomSpi)instance.impl,
            instance.provider, algorithm);
    }


    public static SecureRandom getInstance(String algorithm,
            Provider provider) throws NoSuchAlgorithmException {
        Objects.requireNonNull(algorithm, "null algorithm name");
        Instance instance = GetInstance.getInstance("SecureRandom",
            SecureRandomSpi.class, algorithm, provider);
        return new SecureRandom((SecureRandomSpi)instance.impl,
            instance.provider, algorithm);
    }


    public static SecureRandom getInstance(
            String algorithm, SecureRandomParameters params)
            throws NoSuchAlgorithmException {
        Objects.requireNonNull(algorithm, "null algorithm name");
        if (params == null) {
            throw new IllegalArgumentException("params cannot be null");
        }
        Instance instance = GetInstance.getInstance("SecureRandom",
                SecureRandomSpi.class, algorithm, params);
        return new SecureRandom((SecureRandomSpi)instance.impl,
                instance.provider, algorithm);
    }


    public static SecureRandom getInstance(String algorithm,
            SecureRandomParameters params, String provider)
            throws NoSuchAlgorithmException, NoSuchProviderException {
        Objects.requireNonNull(algorithm, "null algorithm name");
        if (params == null) {
            throw new IllegalArgumentException("params cannot be null");
        }
        Instance instance = GetInstance.getInstance("SecureRandom",
                SecureRandomSpi.class, algorithm, params, provider);
        return new SecureRandom((SecureRandomSpi)instance.impl,
                instance.provider, algorithm);
    }


    public static SecureRandom getInstance(String algorithm,
            SecureRandomParameters params, Provider provider)
            throws NoSuchAlgorithmException {
        Objects.requireNonNull(algorithm, "null algorithm name");
        if (params == null) {
            throw new IllegalArgumentException("params cannot be null");
        }
        Instance instance = GetInstance.getInstance("SecureRandom",
                SecureRandomSpi.class, algorithm, params, provider);
        return new SecureRandom((SecureRandomSpi)instance.impl,
                instance.provider, algorithm);
    }


    SecureRandomSpi getSecureRandomSpi() {
        return secureRandomSpi;
    }


    public final Provider getProvider() {
        return provider;
    }


    public String getAlgorithm() {
        return Objects.toString(algorithm, "unknown");
    }


    @Override
    public String toString() {
        return secureRandomSpi.toString();
    }


    public SecureRandomParameters getParameters() {
        return secureRandomSpi.engineGetParameters();
    }


    public void setSeed(byte[] seed) {
        if (threadSafe) {
            secureRandomSpi.engineSetSeed(seed);
        } else {
            synchronized (this) {
                secureRandomSpi.engineSetSeed(seed);
            }
        }
    }


    @Override
    public void setSeed(long seed) {
        /*
         * Ignore call from super constructor (as well as any other calls
         * unfortunate enough to be passing 0).  It's critical that we
         * ignore call from superclass constructor, as digest has not
         * yet been initialized at that point.
         */
        if (seed != 0) {
            setSeed(longToByteArray(seed));
        }
    }


    @Override
    public void nextBytes(byte[] bytes) {
        if (threadSafe) {
            secureRandomSpi.engineNextBytes(bytes);
        } else {
            synchronized (this) {
                secureRandomSpi.engineNextBytes(bytes);
            }
        }
    }


    public void nextBytes(byte[] bytes, SecureRandomParameters params) {
        if (params == null) {
            throw new IllegalArgumentException("params cannot be null");
        }
        if (threadSafe) {
            secureRandomSpi.engineNextBytes(
                    Objects.requireNonNull(bytes), params);
        } else {
            synchronized (this) {
                secureRandomSpi.engineNextBytes(
                        Objects.requireNonNull(bytes), params);
            }
        }
    }


    @Override
    protected final int next(int numBits) {
        int numBytes = (numBits+7)/8;
        byte[] b = new byte[numBytes];
        int next = 0;

        nextBytes(b);
        for (int i = 0; i < numBytes; i++) {
            next = (next << 8) + (b[i] & 0xFF);
        }

        return next >>> (numBytes*8 - numBits);
    }


    public static byte[] getSeed(int numBytes) {
        SecureRandom seedGen = seedGenerator;
        if (seedGen == null) {
            seedGen = new SecureRandom();
            seedGenerator = seedGen;
        }
        return seedGen.generateSeed(numBytes);
    }


    public byte[] generateSeed(int numBytes) {
        if (numBytes < 0) {
            throw new IllegalArgumentException("numBytes cannot be negative");
        }
        if (threadSafe) {
            return secureRandomSpi.engineGenerateSeed(numBytes);
        } else {
            synchronized (this) {
                return secureRandomSpi.engineGenerateSeed(numBytes);
            }
        }
    }


    private static byte[] longToByteArray(long l) {
        byte[] retVal = new byte[8];

        for (int i = 0; i < 8; i++) {
            retVal[i] = (byte) l;
            l >>= 8;
        }

        return retVal;
    }


    private static String getPrngAlgorithm() {
        for (Provider p : Providers.getProviderList().providers()) {
            for (Service s : p.getServices()) {
                if (s.getType().equals("SecureRandom")) {
                    return s.getAlgorithm();
                }
            }
        }
        return null;
    }

    /*
     * Lazily initialize since Pattern.compile() is heavy.
     * Effective Java (2nd Edition), Item 71.
     */
    private static final class StrongPatternHolder {
        /*
         * Entries are alg:prov separated by ,
         * Allow for prepended/appended whitespace between entries.
         *
         * Capture groups:
         *     1 - alg
         *     2 - :prov (optional)
         *     3 - prov (optional)
         *     4 - ,nextEntry (optional)
         *     5 - nextEntry (optional)
         */
        private static Pattern pattern =
            Pattern.compile(
                "\\s*([\\S&&[^:,]]*)(\\:([\\S&&[^,]]*))?\\s*(\\,(.*))?");
    }


    public static SecureRandom getInstanceStrong()
            throws NoSuchAlgorithmException {

        String property = AccessController.doPrivileged(
            new PrivilegedAction<>() {
                @Override
                public String run() {
                    return Security.getProperty(
                        "securerandom.strongAlgorithms");
                }
            });

        if ((property == null) || (property.length() == 0)) {
            throw new NoSuchAlgorithmException(
                "Null/empty securerandom.strongAlgorithms Security Property");
        }

        String remainder = property;
        while (remainder != null) {
            Matcher m;
            if ((m = StrongPatternHolder.pattern.matcher(
                    remainder)).matches()) {

                String alg = m.group(1);
                String prov = m.group(3);

                try {
                    if (prov == null) {
                        return SecureRandom.getInstance(alg);
                    } else {
                        return SecureRandom.getInstance(alg, prov);
                    }
                } catch (NoSuchAlgorithmException |
                        NoSuchProviderException e) {
                }
                remainder = m.group(5);
            } else {
                remainder = null;
            }
        }

        throw new NoSuchAlgorithmException(
            "No strong SecureRandom impls available: " + property);
    }


    public void reseed() {
        if (threadSafe) {
            secureRandomSpi.engineReseed(null);
        } else {
            synchronized (this) {
                secureRandomSpi.engineReseed(null);
            }
        }
    }


    public void reseed(SecureRandomParameters params) {
        if (params == null) {
            throw new IllegalArgumentException("params cannot be null");
        }
        if (threadSafe) {
            secureRandomSpi.engineReseed(params);
        } else {
            synchronized (this) {
                secureRandomSpi.engineReseed(params);
            }
        }
    }

    // Declare serialVersionUID to be compatible with JDK1.1
    static final long serialVersionUID = 4940670005562187L;

    // Retain unused values serialized from JDK1.1

    private byte[] state;

    private MessageDigest digest = null;

    private byte[] randomBytes;

    private int randomBytesUsed;

    private long counter;
}
