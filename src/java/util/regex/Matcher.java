/*
 * Copyright (c) 1999, 2015, Oracle and/or its affiliates. All rights reserved.
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

package java.util.regex;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;



public final class Matcher implements MatchResult {


    Pattern parentPattern;


    int[] groups;


    int from, to;


    int lookbehindTo;


    CharSequence text;


    static final int ENDANCHOR = 1;
    static final int NOANCHOR = 0;
    int acceptMode = NOANCHOR;


    int first = -1, last = 0;


    int oldLast = -1;


    int lastAppendPosition = 0;


    int[] locals;


    IntHashSet[] localsPos;


    boolean hitEnd;


    boolean requireEnd;


    boolean transparentBounds = false;


    boolean anchoringBounds = true;


    int modCount;


    Matcher() {
    }


    Matcher(Pattern parent, CharSequence text) {
        this.parentPattern = parent;
        this.text = text;

        // Allocate state storage
        int parentGroupCount = Math.max(parent.capturingGroupCount, 10);
        groups = new int[parentGroupCount * 2];
        locals = new int[parent.localCount];
        localsPos = new IntHashSet[parent.localTCNCount];

        // Put fields into initial states
        reset();
    }


    public Pattern pattern() {
        return parentPattern;
    }


    public MatchResult toMatchResult() {
        return toMatchResult(text.toString());
    }

    private MatchResult toMatchResult(String text) {
        return new ImmutableMatchResult(this.first,
                                        this.last,
                                        groupCount(),
                                        this.groups.clone(),
                                        text);
    }

    private static class ImmutableMatchResult implements MatchResult {
        private final int first;
        private final int last;
        private final int[] groups;
        private final int groupCount;
        private final String text;

        ImmutableMatchResult(int first, int last, int groupCount,
                             int groups[], String text)
        {
            this.first = first;
            this.last = last;
            this.groupCount = groupCount;
            this.groups = groups;
            this.text = text;
        }

        @Override
        public int start() {
            checkMatch();
            return first;
        }

        @Override
        public int start(int group) {
            checkMatch();
            if (group < 0 || group > groupCount)
                throw new IndexOutOfBoundsException("No group " + group);
            return groups[group * 2];
        }

        @Override
        public int end() {
            checkMatch();
            return last;
        }

        @Override
        public int end(int group) {
            checkMatch();
            if (group < 0 || group > groupCount)
                throw new IndexOutOfBoundsException("No group " + group);
            return groups[group * 2 + 1];
        }

        @Override
        public int groupCount() {
            return groupCount;
        }

        @Override
        public String group() {
            checkMatch();
            return group(0);
        }

        @Override
        public String group(int group) {
            checkMatch();
            if (group < 0 || group > groupCount)
                throw new IndexOutOfBoundsException("No group " + group);
            if ((groups[group*2] == -1) || (groups[group*2+1] == -1))
                return null;
            return text.subSequence(groups[group * 2], groups[group * 2 + 1]).toString();
        }

        private void checkMatch() {
            if (first < 0)
                throw new IllegalStateException("No match found");

        }
    }


    public Matcher usePattern(Pattern newPattern) {
        if (newPattern == null)
            throw new IllegalArgumentException("Pattern cannot be null");
        parentPattern = newPattern;

        // Reallocate state storage
        int parentGroupCount = Math.max(newPattern.capturingGroupCount, 10);
        groups = new int[parentGroupCount * 2];
        locals = new int[newPattern.localCount];
        for (int i = 0; i < groups.length; i++)
            groups[i] = -1;
        for (int i = 0; i < locals.length; i++)
            locals[i] = -1;
        localsPos = new IntHashSet[parentPattern.localTCNCount];
        modCount++;
        return this;
    }


    public Matcher reset() {
        first = -1;
        last = 0;
        oldLast = -1;
        for(int i=0; i<groups.length; i++)
            groups[i] = -1;
        for(int i=0; i<locals.length; i++)
            locals[i] = -1;
        for (int i = 0; i < localsPos.length; i++) {
            if (localsPos[i] != null)
                localsPos[i].clear();
        }
        lastAppendPosition = 0;
        from = 0;
        to = getTextLength();
        modCount++;
        return this;
    }


    public Matcher reset(CharSequence input) {
        text = input;
        return reset();
    }


    public int start() {
        if (first < 0)
            throw new IllegalStateException("No match available");
        return first;
    }


    public int start(int group) {
        if (first < 0)
            throw new IllegalStateException("No match available");
        if (group < 0 || group > groupCount())
            throw new IndexOutOfBoundsException("No group " + group);
        return groups[group * 2];
    }


    public int start(String name) {
        return groups[getMatchedGroupIndex(name) * 2];
    }


    public int end() {
        if (first < 0)
            throw new IllegalStateException("No match available");
        return last;
    }


    public int end(int group) {
        if (first < 0)
            throw new IllegalStateException("No match available");
        if (group < 0 || group > groupCount())
            throw new IndexOutOfBoundsException("No group " + group);
        return groups[group * 2 + 1];
    }


    public int end(String name) {
        return groups[getMatchedGroupIndex(name) * 2 + 1];
    }


    public String group() {
        return group(0);
    }


    public String group(int group) {
        if (first < 0)
            throw new IllegalStateException("No match found");
        if (group < 0 || group > groupCount())
            throw new IndexOutOfBoundsException("No group " + group);
        if ((groups[group*2] == -1) || (groups[group*2+1] == -1))
            return null;
        return getSubSequence(groups[group * 2], groups[group * 2 + 1]).toString();
    }


    public String group(String name) {
        int group = getMatchedGroupIndex(name);
        if ((groups[group*2] == -1) || (groups[group*2+1] == -1))
            return null;
        return getSubSequence(groups[group * 2], groups[group * 2 + 1]).toString();
    }


    public int groupCount() {
        return parentPattern.capturingGroupCount - 1;
    }


    public boolean matches() {
        return match(from, ENDANCHOR);
    }


    public boolean find() {
        int nextSearchIndex = last;
        if (nextSearchIndex == first)
            nextSearchIndex++;

        // If next search starts before region, start it at region
        if (nextSearchIndex < from)
            nextSearchIndex = from;

        // If next search starts beyond region then it fails
        if (nextSearchIndex > to) {
            for (int i = 0; i < groups.length; i++)
                groups[i] = -1;
            return false;
        }
        return search(nextSearchIndex);
    }


    public boolean find(int start) {
        int limit = getTextLength();
        if ((start < 0) || (start > limit))
            throw new IndexOutOfBoundsException("Illegal start index");
        reset();
        return search(start);
    }


    public boolean lookingAt() {
        return match(from, NOANCHOR);
    }


    public static String quoteReplacement(String s) {
        if ((s.indexOf('\\') == -1) && (s.indexOf('$') == -1))
            return s;
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' || c == '$') {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }


    public Matcher appendReplacement(StringBuffer sb, String replacement) {
        // If no match, return error
        if (first < 0)
            throw new IllegalStateException("No match available");
        StringBuilder result = new StringBuilder();
        appendExpandedReplacement(replacement, result);
        // Append the intervening text
        sb.append(text, lastAppendPosition, first);
        // Append the match substitution
        sb.append(result);
        lastAppendPosition = last;
        modCount++;
        return this;
    }


    public Matcher appendReplacement(StringBuilder sb, String replacement) {
        // If no match, return error
        if (first < 0)
            throw new IllegalStateException("No match available");
        StringBuilder result = new StringBuilder();
        appendExpandedReplacement(replacement, result);
        // Append the intervening text
        sb.append(text, lastAppendPosition, first);
        // Append the match substitution
        sb.append(result);
        lastAppendPosition = last;
        modCount++;
        return this;
    }


    private StringBuilder appendExpandedReplacement(
        String replacement, StringBuilder result) {
        int cursor = 0;
        while (cursor < replacement.length()) {
            char nextChar = replacement.charAt(cursor);
            if (nextChar == '\\') {
                cursor++;
                if (cursor == replacement.length())
                    throw new IllegalArgumentException(
                        "character to be escaped is missing");
                nextChar = replacement.charAt(cursor);
                result.append(nextChar);
                cursor++;
            } else if (nextChar == '$') {
                // Skip past $
                cursor++;
                // Throw IAE if this "$" is the last character in replacement
                if (cursor == replacement.length())
                   throw new IllegalArgumentException(
                        "Illegal group reference: group index is missing");
                nextChar = replacement.charAt(cursor);
                int refNum = -1;
                if (nextChar == '{') {
                    cursor++;
                    StringBuilder gsb = new StringBuilder();
                    while (cursor < replacement.length()) {
                        nextChar = replacement.charAt(cursor);
                        if (ASCII.isLower(nextChar) ||
                            ASCII.isUpper(nextChar) ||
                            ASCII.isDigit(nextChar)) {
                            gsb.append(nextChar);
                            cursor++;
                        } else {
                            break;
                        }
                    }
                    if (gsb.length() == 0)
                        throw new IllegalArgumentException(
                            "named capturing group has 0 length name");
                    if (nextChar != '}')
                        throw new IllegalArgumentException(
                            "named capturing group is missing trailing '}'");
                    String gname = gsb.toString();
                    if (ASCII.isDigit(gname.charAt(0)))
                        throw new IllegalArgumentException(
                            "capturing group name {" + gname +
                            "} starts with digit character");
                    if (!parentPattern.namedGroups().containsKey(gname))
                        throw new IllegalArgumentException(
                            "No group with name {" + gname + "}");
                    refNum = parentPattern.namedGroups().get(gname);
                    cursor++;
                } else {
                    // The first number is always a group
                    refNum = nextChar - '0';
                    if ((refNum < 0) || (refNum > 9))
                        throw new IllegalArgumentException(
                            "Illegal group reference");
                    cursor++;
                    // Capture the largest legal group string
                    boolean done = false;
                    while (!done) {
                        if (cursor >= replacement.length()) {
                            break;
                        }
                        int nextDigit = replacement.charAt(cursor) - '0';
                        if ((nextDigit < 0) || (nextDigit > 9)) { // not a number
                            break;
                        }
                        int newRefNum = (refNum * 10) + nextDigit;
                        if (groupCount() < newRefNum) {
                            done = true;
                        } else {
                            refNum = newRefNum;
                            cursor++;
                        }
                    }
                }
                // Append group
                if (start(refNum) != -1 && end(refNum) != -1)
                    result.append(text, start(refNum), end(refNum));
            } else {
                result.append(nextChar);
                cursor++;
            }
        }
        return result;
    }


    public StringBuffer appendTail(StringBuffer sb) {
        sb.append(text, lastAppendPosition, getTextLength());
        return sb;
    }


    public StringBuilder appendTail(StringBuilder sb) {
        sb.append(text, lastAppendPosition, getTextLength());
        return sb;
    }


    public String replaceAll(String replacement) {
        reset();
        boolean result = find();
        if (result) {
            StringBuilder sb = new StringBuilder();
            do {
                appendReplacement(sb, replacement);
                result = find();
            } while (result);
            appendTail(sb);
            return sb.toString();
        }
        return text.toString();
    }


    public String replaceAll(Function<MatchResult, String> replacer) {
        Objects.requireNonNull(replacer);
        reset();
        boolean result = find();
        if (result) {
            StringBuilder sb = new StringBuilder();
            do {
                int ec = modCount;
                String replacement =  replacer.apply(this);
                if (ec != modCount)
                    throw new ConcurrentModificationException();
                appendReplacement(sb, replacement);
                result = find();
            } while (result);
            appendTail(sb);
            return sb.toString();
        }
        return text.toString();
    }


    public Stream<MatchResult> results() {
        class MatchResultIterator implements Iterator<MatchResult> {
            // -ve for call to find, 0 for not found, 1 for found
            int state = -1;
            // State for concurrent modification checking
            // -1 for uninitialized
            int expectedCount = -1;
            // The input sequence as a string, set once only after first find
            // Avoids repeated conversion from CharSequence for each match
            String textAsString;

            @Override
            public MatchResult next() {
                if (expectedCount >= 0 && expectedCount != modCount)
                    throw new ConcurrentModificationException();

                if (!hasNext())
                    throw new NoSuchElementException();

                state = -1;
                return toMatchResult(textAsString);
            }

            @Override
            public boolean hasNext() {
                if (state >= 0)
                    return state == 1;

                // Defer throwing ConcurrentModificationException to when next
                // or forEachRemaining is called.  The is consistent with other
                // fail-fast implementations.
                if (expectedCount >= 0 && expectedCount != modCount)
                    return true;

                boolean found = find();
                // Capture the input sequence as a string on first find
                if (found && state < 0)
                    textAsString = text.toString();
                state = found ? 1 : 0;
                expectedCount = modCount;
                return found;
            }

            @Override
            public void forEachRemaining(Consumer<? super MatchResult> action) {
                if (expectedCount >= 0 && expectedCount != modCount)
                    throw new ConcurrentModificationException();

                int s = state;
                if (s == 0)
                    return;

                // Set state to report no more elements on further operations
                state = 0;
                expectedCount = -1;

                // Perform a first find if required
                if (s < 0 && !find())
                    return;

                // Capture the input sequence as a string on first find
                textAsString = text.toString();

                do {
                    int ec = modCount;
                    action.accept(toMatchResult(textAsString));
                    if (ec != modCount)
                        throw new ConcurrentModificationException();
                } while (find());
            }
        }
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                new MatchResultIterator(), Spliterator.ORDERED | Spliterator.NONNULL), false);
    }


    public String replaceFirst(String replacement) {
        if (replacement == null)
            throw new NullPointerException("replacement");
        reset();
        if (!find())
            return text.toString();
        StringBuilder sb = new StringBuilder();
        appendReplacement(sb, replacement);
        appendTail(sb);
        return sb.toString();
    }


    public String replaceFirst(Function<MatchResult, String> replacer) {
        Objects.requireNonNull(replacer);
        reset();
        if (!find())
            return text.toString();
        StringBuilder sb = new StringBuilder();
        int ec = modCount;
        String replacement = replacer.apply(this);
        if (ec != modCount)
            throw new ConcurrentModificationException();
        appendReplacement(sb, replacement);
        appendTail(sb);
        return sb.toString();
    }


    public Matcher region(int start, int end) {
        if ((start < 0) || (start > getTextLength()))
            throw new IndexOutOfBoundsException("start");
        if ((end < 0) || (end > getTextLength()))
            throw new IndexOutOfBoundsException("end");
        if (start > end)
            throw new IndexOutOfBoundsException("start > end");
        reset();
        from = start;
        to = end;
        return this;
    }


    public int regionStart() {
        return from;
    }


    public int regionEnd() {
        return to;
    }


    public boolean hasTransparentBounds() {
        return transparentBounds;
    }


    public Matcher useTransparentBounds(boolean b) {
        transparentBounds = b;
        return this;
    }


    public boolean hasAnchoringBounds() {
        return anchoringBounds;
    }


    public Matcher useAnchoringBounds(boolean b) {
        anchoringBounds = b;
        return this;
    }


    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("java.util.regex.Matcher")
                .append("[pattern=").append(pattern())
                .append(" region=")
                .append(regionStart()).append(',').append(regionEnd())
                .append(" lastmatch=");
        if ((first >= 0) && (group() != null)) {
            sb.append(group());
        }
        sb.append(']');
        return sb.toString();
    }


    public boolean hitEnd() {
        return hitEnd;
    }


    public boolean requireEnd() {
        return requireEnd;
    }


    boolean search(int from) {
        this.hitEnd = false;
        this.requireEnd = false;
        from        = from < 0 ? 0 : from;
        this.first  = from;
        this.oldLast = oldLast < 0 ? from : oldLast;
        for (int i = 0; i < groups.length; i++)
            groups[i] = -1;
        for (int i = 0; i < localsPos.length; i++) {
            if (localsPos[i] != null)
                localsPos[i].clear();
        }
        acceptMode = NOANCHOR;
        boolean result = parentPattern.root.match(this, from, text);
        if (!result)
            this.first = -1;
        this.oldLast = this.last;
        this.modCount++;
        return result;
    }


    boolean match(int from, int anchor) {
        this.hitEnd = false;
        this.requireEnd = false;
        from        = from < 0 ? 0 : from;
        this.first  = from;
        this.oldLast = oldLast < 0 ? from : oldLast;
        for (int i = 0; i < groups.length; i++)
            groups[i] = -1;
        for (int i = 0; i < localsPos.length; i++) {
            if (localsPos[i] != null)
                localsPos[i].clear();
        }
        acceptMode = anchor;
        boolean result = parentPattern.matchRoot.match(this, from, text);
        if (!result)
            this.first = -1;
        this.oldLast = this.last;
        this.modCount++;
        return result;
    }


    int getTextLength() {
        return text.length();
    }


    CharSequence getSubSequence(int beginIndex, int endIndex) {
        return text.subSequence(beginIndex, endIndex);
    }


    char charAt(int i) {
        return text.charAt(i);
    }


    int getMatchedGroupIndex(String name) {
        Objects.requireNonNull(name, "Group name");
        if (first < 0)
            throw new IllegalStateException("No match found");
        if (!parentPattern.namedGroups().containsKey(name))
            throw new IllegalArgumentException("No group with name <" + name + ">");
        return parentPattern.namedGroups().get(name);
    }
}
