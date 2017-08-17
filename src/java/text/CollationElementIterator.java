/*
 * Copyright (c) 1996, 2013, Oracle and/or its affiliates. All rights reserved.
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

/*
 * (C) Copyright Taligent, Inc. 1996, 1997 - All Rights Reserved
 * (C) Copyright IBM Corp. 1996-1998 - All Rights Reserved
 *
 *   The original version of this source code and documentation is copyrighted
 * and owned by Taligent, Inc., a wholly-owned subsidiary of IBM. These
 * materials are provided under terms of a License Agreement between Taligent
 * and Sun. This technology is protected by multiple US and International
 * patents. This notice and attribution to Taligent may not be removed.
 *   Taligent is a registered trademark of Taligent, Inc.
 *
 */

package java.text;

import java.lang.Character;
import java.util.Vector;
import sun.text.CollatorUtilities;
import sun.text.normalizer.NormalizerBase;


public final class CollationElementIterator
{

    public static final int NULLORDER = 0xffffffff;


    CollationElementIterator(String sourceText, RuleBasedCollator owner) {
        this.owner = owner;
        ordering = owner.getTables();
        if ( sourceText.length() != 0 ) {
            NormalizerBase.Mode mode =
                CollatorUtilities.toNormalizerMode(owner.getDecomposition());
            text = new NormalizerBase(sourceText, mode);
        }
    }


    CollationElementIterator(CharacterIterator sourceText, RuleBasedCollator owner) {
        this.owner = owner;
        ordering = owner.getTables();
        NormalizerBase.Mode mode =
            CollatorUtilities.toNormalizerMode(owner.getDecomposition());
        text = new NormalizerBase(sourceText, mode);
    }


    public void reset()
    {
        if (text != null) {
            text.reset();
            NormalizerBase.Mode mode =
                CollatorUtilities.toNormalizerMode(owner.getDecomposition());
            text.setMode(mode);
        }
        buffer = null;
        expIndex = 0;
        swapOrder = 0;
    }


    public int next()
    {
        if (text == null) {
            return NULLORDER;
        }
        NormalizerBase.Mode textMode = text.getMode();
        // convert the owner's mode to something the Normalizer understands
        NormalizerBase.Mode ownerMode =
            CollatorUtilities.toNormalizerMode(owner.getDecomposition());
        if (textMode != ownerMode) {
            text.setMode(ownerMode);
        }

        // if buffer contains any decomposed char values
        // return their strength orders before continuing in
        // the Normalizer's CharacterIterator.
        if (buffer != null) {
            if (expIndex < buffer.length) {
                return strengthOrder(buffer[expIndex++]);
            } else {
                buffer = null;
                expIndex = 0;
            }
        } else if (swapOrder != 0) {
            if (Character.isSupplementaryCodePoint(swapOrder)) {
                char[] chars = Character.toChars(swapOrder);
                swapOrder = chars[1];
                return chars[0] << 16;
            }
            int order = swapOrder << 16;
            swapOrder = 0;
            return order;
        }
        int ch  = text.next();

        // are we at the end of Normalizer's text?
        if (ch == NormalizerBase.DONE) {
            return NULLORDER;
        }

        int value = ordering.getUnicodeOrder(ch);
        if (value == RuleBasedCollator.UNMAPPED) {
            swapOrder = ch;
            return UNMAPPEDCHARVALUE;
        }
        else if (value >= RuleBasedCollator.CONTRACTCHARINDEX) {
            value = nextContractChar(ch);
        }
        if (value >= RuleBasedCollator.EXPANDCHARINDEX) {
            buffer = ordering.getExpandValueList(value);
            expIndex = 0;
            value = buffer[expIndex++];
        }

        if (ordering.isSEAsianSwapping()) {
            int consonant;
            if (isThaiPreVowel(ch)) {
                consonant = text.next();
                if (isThaiBaseConsonant(consonant)) {
                    buffer = makeReorderedBuffer(consonant, value, buffer, true);
                    value = buffer[0];
                    expIndex = 1;
                } else if (consonant != NormalizerBase.DONE) {
                    text.previous();
                }
            }
            if (isLaoPreVowel(ch)) {
                consonant = text.next();
                if (isLaoBaseConsonant(consonant)) {
                    buffer = makeReorderedBuffer(consonant, value, buffer, true);
                    value = buffer[0];
                    expIndex = 1;
                } else if (consonant != NormalizerBase.DONE) {
                    text.previous();
                }
            }
        }

        return strengthOrder(value);
    }


    public int previous()
    {
        if (text == null) {
            return NULLORDER;
        }
        NormalizerBase.Mode textMode = text.getMode();
        // convert the owner's mode to something the Normalizer understands
        NormalizerBase.Mode ownerMode =
            CollatorUtilities.toNormalizerMode(owner.getDecomposition());
        if (textMode != ownerMode) {
            text.setMode(ownerMode);
        }
        if (buffer != null) {
            if (expIndex > 0) {
                return strengthOrder(buffer[--expIndex]);
            } else {
                buffer = null;
                expIndex = 0;
            }
        } else if (swapOrder != 0) {
            if (Character.isSupplementaryCodePoint(swapOrder)) {
                char[] chars = Character.toChars(swapOrder);
                swapOrder = chars[1];
                return chars[0] << 16;
            }
            int order = swapOrder << 16;
            swapOrder = 0;
            return order;
        }
        int ch = text.previous();
        if (ch == NormalizerBase.DONE) {
            return NULLORDER;
        }

        int value = ordering.getUnicodeOrder(ch);

        if (value == RuleBasedCollator.UNMAPPED) {
            swapOrder = UNMAPPEDCHARVALUE;
            return ch;
        } else if (value >= RuleBasedCollator.CONTRACTCHARINDEX) {
            value = prevContractChar(ch);
        }
        if (value >= RuleBasedCollator.EXPANDCHARINDEX) {
            buffer = ordering.getExpandValueList(value);
            expIndex = buffer.length;
            value = buffer[--expIndex];
        }

        if (ordering.isSEAsianSwapping()) {
            int vowel;
            if (isThaiBaseConsonant(ch)) {
                vowel = text.previous();
                if (isThaiPreVowel(vowel)) {
                    buffer = makeReorderedBuffer(vowel, value, buffer, false);
                    expIndex = buffer.length - 1;
                    value = buffer[expIndex];
                } else {
                    text.next();
                }
            }
            if (isLaoBaseConsonant(ch)) {
                vowel = text.previous();
                if (isLaoPreVowel(vowel)) {
                    buffer = makeReorderedBuffer(vowel, value, buffer, false);
                    expIndex = buffer.length - 1;
                    value = buffer[expIndex];
                } else {
                    text.next();
                }
            }
        }

        return strengthOrder(value);
    }


    public static final int primaryOrder(int order)
    {
        order &= RBCollationTables.PRIMARYORDERMASK;
        return (order >>> RBCollationTables.PRIMARYORDERSHIFT);
    }

    public static final short secondaryOrder(int order)
    {
        order = order & RBCollationTables.SECONDARYORDERMASK;
        return ((short)(order >> RBCollationTables.SECONDARYORDERSHIFT));
    }

    public static final short tertiaryOrder(int order)
    {
        return ((short)(order &= RBCollationTables.TERTIARYORDERMASK));
    }


    final int strengthOrder(int order)
    {
        int s = owner.getStrength();
        if (s == Collator.PRIMARY)
        {
            order &= RBCollationTables.PRIMARYDIFFERENCEONLY;
        } else if (s == Collator.SECONDARY)
        {
            order &= RBCollationTables.SECONDARYDIFFERENCEONLY;
        }
        return order;
    }


    @SuppressWarnings("deprecation") // getBeginIndex, getEndIndex and setIndex are deprecated
    public void setOffset(int newOffset)
    {
        if (text != null) {
            if (newOffset < text.getBeginIndex()
                || newOffset >= text.getEndIndex()) {
                    text.setIndexOnly(newOffset);
            } else {
                int c = text.setIndex(newOffset);

                // if the desired character isn't used in a contracting character
                // sequence, bypass all the backing-up logic-- we're sitting on
                // the right character already
                if (ordering.usedInContractSeq(c)) {
                    // walk backwards through the string until we see a character
                    // that DOESN'T participate in a contracting character sequence
                    while (ordering.usedInContractSeq(c)) {
                        c = text.previous();
                    }
                    // now walk forward using this object's next() method until
                    // we pass the starting point and set our current position
                    // to the beginning of the last "character" before or at
                    // our starting position
                    int last = text.getIndex();
                    while (text.getIndex() <= newOffset) {
                        last = text.getIndex();
                        next();
                    }
                    text.setIndexOnly(last);
                    // we don't need this, since last is the last index
                    // that is the starting of the contraction which encompass
                    // newOffset
                    // text.previous();
                }
            }
        }
        buffer = null;
        expIndex = 0;
        swapOrder = 0;
    }


    public int getOffset()
    {
        return (text != null) ? text.getIndex() : 0;
    }



    public int getMaxExpansion(int order)
    {
        return ordering.getMaxExpansion(order);
    }


    public void setText(String source)
    {
        buffer = null;
        swapOrder = 0;
        expIndex = 0;
        NormalizerBase.Mode mode =
            CollatorUtilities.toNormalizerMode(owner.getDecomposition());
        if (text == null) {
            text = new NormalizerBase(source, mode);
        } else {
            text.setMode(mode);
            text.setText(source);
        }
    }


    public void setText(CharacterIterator source)
    {
        buffer = null;
        swapOrder = 0;
        expIndex = 0;
        NormalizerBase.Mode mode =
            CollatorUtilities.toNormalizerMode(owner.getDecomposition());
        if (text == null) {
            text = new NormalizerBase(source, mode);
        } else {
            text.setMode(mode);
            text.setText(source);
        }
    }

    //============================================================
    // privates
    //============================================================


    private static final boolean isThaiPreVowel(int ch) {
        return (ch >= 0x0e40) && (ch <= 0x0e44);
    }


    private static final boolean isThaiBaseConsonant(int ch) {
        return (ch >= 0x0e01) && (ch <= 0x0e2e);
    }


    private static final boolean isLaoPreVowel(int ch) {
        return (ch >= 0x0ec0) && (ch <= 0x0ec4);
    }


    private static final boolean isLaoBaseConsonant(int ch) {
        return (ch >= 0x0e81) && (ch <= 0x0eae);
    }


    private int[] makeReorderedBuffer(int colFirst,
                                      int lastValue,
                                      int[] lastExpansion,
                                      boolean forward) {

        int[] result;

        int firstValue = ordering.getUnicodeOrder(colFirst);
        if (firstValue >= RuleBasedCollator.CONTRACTCHARINDEX) {
            firstValue = forward? nextContractChar(colFirst) : prevContractChar(colFirst);
        }

        int[] firstExpansion = null;
        if (firstValue >= RuleBasedCollator.EXPANDCHARINDEX) {
            firstExpansion = ordering.getExpandValueList(firstValue);
        }

        if (!forward) {
            int temp1 = firstValue;
            firstValue = lastValue;
            lastValue = temp1;
            int[] temp2 = firstExpansion;
            firstExpansion = lastExpansion;
            lastExpansion = temp2;
        }

        if (firstExpansion == null && lastExpansion == null) {
            result = new int [2];
            result[0] = firstValue;
            result[1] = lastValue;
        }
        else {
            int firstLength = firstExpansion==null? 1 : firstExpansion.length;
            int lastLength = lastExpansion==null? 1 : lastExpansion.length;
            result = new int[firstLength + lastLength];

            if (firstExpansion == null) {
                result[0] = firstValue;
            }
            else {
                System.arraycopy(firstExpansion, 0, result, 0, firstLength);
            }

            if (lastExpansion == null) {
                result[firstLength] = lastValue;
            }
            else {
                System.arraycopy(lastExpansion, 0, result, firstLength, lastLength);
            }
        }

        return result;
    }


    static final boolean isIgnorable(int order)
    {
        return ((primaryOrder(order) == 0) ? true : false);
    }


    private int nextContractChar(int ch)
    {
        // First get the ordering of this single character,
        // which is always the first element in the list
        Vector<EntryPair> list = ordering.getContractValues(ch);
        EntryPair pair = list.firstElement();
        int order = pair.value;

        // find out the length of the longest contracting character sequence in the list.
        // There's logic in the builder code to make sure the longest sequence is always
        // the last.
        pair = list.lastElement();
        int maxLength = pair.entryName.length();

        // (the Normalizer is cloned here so that the seeking we do in the next loop
        // won't affect our real position in the text)
        NormalizerBase tempText = (NormalizerBase)text.clone();

        // extract the next maxLength characters in the string (we have to do this using the
        // Normalizer to ensure that our offsets correspond to those the rest of the
        // iterator is using) and store it in "fragment".
        tempText.previous();
        key.setLength(0);
        int c = tempText.next();
        while (maxLength > 0 && c != NormalizerBase.DONE) {
            if (Character.isSupplementaryCodePoint(c)) {
                key.append(Character.toChars(c));
                maxLength -= 2;
            } else {
                key.append((char)c);
                --maxLength;
            }
            c = tempText.next();
        }
        String fragment = key.toString();
        // now that we have that fragment, iterate through this list looking for the
        // longest sequence that matches the characters in the actual text.  (maxLength
        // is used here to keep track of the length of the longest sequence)
        // Upon exit from this loop, maxLength will contain the length of the matching
        // sequence and order will contain the collation-element value corresponding
        // to this sequence
        maxLength = 1;
        for (int i = list.size() - 1; i > 0; i--) {
            pair = list.elementAt(i);
            if (!pair.fwd)
                continue;

            if (fragment.startsWith(pair.entryName) && pair.entryName.length()
                    > maxLength) {
                maxLength = pair.entryName.length();
                order = pair.value;
            }
        }

        // seek our current iteration position to the end of the matching sequence
        // and return the appropriate collation-element value (if there was no matching
        // sequence, we're already seeked to the right position and order already contains
        // the correct collation-element value for the single character)
        while (maxLength > 1) {
            c = text.next();
            maxLength -= Character.charCount(c);
        }
        return order;
    }


    private int prevContractChar(int ch)
    {
        // This function is identical to nextContractChar(), except that we've
        // switched things so that the next() and previous() calls on the Normalizer
        // are switched and so that we skip entry pairs with the fwd flag turned on
        // rather than off.  Notice that we still use append() and startsWith() when
        // working on the fragment.  This is because the entry pairs that are used
        // in reverse iteration have their names reversed already.
        Vector<EntryPair> list = ordering.getContractValues(ch);
        EntryPair pair = list.firstElement();
        int order = pair.value;

        pair = list.lastElement();
        int maxLength = pair.entryName.length();

        NormalizerBase tempText = (NormalizerBase)text.clone();

        tempText.next();
        key.setLength(0);
        int c = tempText.previous();
        while (maxLength > 0 && c != NormalizerBase.DONE) {
            if (Character.isSupplementaryCodePoint(c)) {
                key.append(Character.toChars(c));
                maxLength -= 2;
            } else {
                key.append((char)c);
                --maxLength;
            }
            c = tempText.previous();
        }
        String fragment = key.toString();

        maxLength = 1;
        for (int i = list.size() - 1; i > 0; i--) {
            pair = list.elementAt(i);
            if (pair.fwd)
                continue;

            if (fragment.startsWith(pair.entryName) && pair.entryName.length()
                    > maxLength) {
                maxLength = pair.entryName.length();
                order = pair.value;
            }
        }

        while (maxLength > 1) {
            c = text.previous();
            maxLength -= Character.charCount(c);
        }
        return order;
    }

    static final int UNMAPPEDCHARVALUE = 0x7FFF0000;

    private NormalizerBase text = null;
    private int[] buffer = null;
    private int expIndex = 0;
    private StringBuffer key = new StringBuffer(5);
    private int swapOrder = 0;
    private RBCollationTables ordering;
    private RuleBasedCollator owner;
}
