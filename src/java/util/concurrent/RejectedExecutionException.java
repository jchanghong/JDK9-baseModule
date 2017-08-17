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


public class RejectedExecutionException extends RuntimeException {
    private static final long serialVersionUID = -375805702767069545L;


    public RejectedExecutionException() { }


    public RejectedExecutionException(String message) {
        super(message);
    }


    public RejectedExecutionException(String message, Throwable cause) {
        super(message, cause);
    }


    public RejectedExecutionException(Throwable cause) {
        super(cause);
    }
}
