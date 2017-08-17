/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Locale;
import java.util.Objects;


public class DrbgParameters {

    private DrbgParameters() {
        // This class should not be instantiated
    }


    public enum Capability {


        PR_AND_RESEED,


        RESEED_ONLY,


        NONE;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ROOT);
        }


        public boolean supportsReseeding() {
            return this != NONE;
        }


        public boolean supportsPredictionResistance() {
            return this == PR_AND_RESEED;
        }
    }


    public static final class Instantiation
            implements SecureRandomParameters {

        private final int strength;
        private final Capability capability;
        private final byte[] personalizationString;


        public int getStrength() {
            return strength;
        }


        public Capability getCapability() {
            return capability;
        }


        public byte[] getPersonalizationString() {
            return (personalizationString == null) ?
                    null : personalizationString.clone();
        }

        private Instantiation(int strength, Capability capability,
                              byte[] personalizationString) {
            if (strength < -1) {
                throw new IllegalArgumentException(
                        "Illegal security strength: " + strength);
            }
            this.strength = strength;
            this.capability = capability;
            this.personalizationString = (personalizationString == null) ?
                    null : personalizationString.clone();
        }


        @Override
        public String toString() {
            // I don't care what personalizationString looks like
            return strength + "," + capability + "," + personalizationString;
        }
    }


    public static final class NextBytes
            implements SecureRandomParameters {
        private final int strength;
        private final boolean predictionResistance;
        private final byte[] additionalInput;


        public int getStrength() {
            return strength;
        }


        public boolean getPredictionResistance() {
            return predictionResistance;
        }


        public byte[] getAdditionalInput() {
            return additionalInput == null? null: additionalInput.clone();
        }

        private NextBytes(int strength, boolean predictionResistance,
                          byte[] additionalInput) {
            if (strength < -1) {
                throw new IllegalArgumentException(
                        "Illegal security strength: " + strength);
            }
            this.strength = strength;
            this.predictionResistance = predictionResistance;
            this.additionalInput = (additionalInput == null) ?
                    null : additionalInput.clone();
        }
    }


    public static final class Reseed implements SecureRandomParameters {

        private final byte[] additionalInput;
        private final boolean predictionResistance;


        public boolean getPredictionResistance() {
            return predictionResistance;
        }


        public byte[] getAdditionalInput() {
            return additionalInput == null ? null : additionalInput.clone();
        }

        private Reseed(boolean predictionResistance, byte[] additionalInput) {
            this.predictionResistance = predictionResistance;
            this.additionalInput = (additionalInput == null) ?
                    null : additionalInput.clone();
        }
    }


    public static Instantiation instantiation(int strength,
                                              Capability capability,
                                              byte[] personalizationString) {
        return new Instantiation(strength, Objects.requireNonNull(capability),
                personalizationString);
    }


    public static NextBytes nextBytes(int strength,
                                      boolean predictionResistance,
                                      byte[] additionalInput) {
        return new NextBytes(strength, predictionResistance, additionalInput);
    }


    public static Reseed reseed(
            boolean predictionResistance, byte[] additionalInput) {
        return new Reseed(predictionResistance, additionalInput);
    }
}
