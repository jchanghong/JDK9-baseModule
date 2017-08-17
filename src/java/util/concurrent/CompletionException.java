/*
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
 *
 *
 *
 *
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;


public class CompletionException extends RuntimeException {
    private static final long serialVersionUID = 7830266012832686185L;


    protected CompletionException() { }


    protected CompletionException(String message) {
        super(message);
    }


    public CompletionException(String message, Throwable cause) {
        super(message, cause);
    }


    public CompletionException(Throwable cause) {
        super(cause);
    }
}
