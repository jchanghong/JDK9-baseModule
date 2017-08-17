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

package java.util.concurrent.locks;


public abstract class AbstractOwnableSynchronizer
    implements java.io.Serializable {


    private static final long serialVersionUID = 3737899427754241961L;


    protected AbstractOwnableSynchronizer() { }


    private transient Thread exclusiveOwnerThread;


    protected final void setExclusiveOwnerThread(Thread thread) {
        exclusiveOwnerThread = thread;
    }


    protected final Thread getExclusiveOwnerThread() {
        return exclusiveOwnerThread;
    }
}
