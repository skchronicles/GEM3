package edu.mit.csail.cgs.projects.readdb;

import java.util.*;

/**
 * Static class to maintain locks on objects within the JVM.  We need this because...
 * - multiple threads can have a read lock on an object at once; synchronized doesn't
 *   do this
 * - the java.io locking stuff only locks between processes, not within the JVM
 */
public class Lock {

    private static Map<String,Set<ServerTask>> readLocks = Collections.synchronizedMap(new HashMap<String,Set<ServerTask>>());
    private static Map<String,ServerTask> writeLocks = Collections.synchronizedMap(new HashMap<String,ServerTask>());

    /**
     * blocks to acquire a shared lock to the specified file.
     * The locking is implemented in java, so it's only
     * good for keeping out other threads.  It's keyed off the file name
     * you provide, so make sure you always generate the filename in the same way.
     */
    protected static void readLock(ServerTask locker, String fname) {
        boolean locked = false;
        /* return now if we already have a read lock.
           this is ok outside the synchronized since
           - a thread only acquires locks for itself
           - a thread therefore won't release its lock at the same
             time that it's acquiring it
        */
        synchronized (readLocks) {
            if (readLocks.containsKey(fname) &&
                readLocks.get(fname).contains(locker)) {
                return;
            }
        }
        while (!locked) {
            if (writeLocks.containsKey(fname)) {
                Thread.yield();
                continue;
            }
            synchronized(readLocks) {
                synchronized(writeLocks) {
                    if (writeLocks.containsKey(fname)) {
                        Thread.yield();
                        continue;
                    }
                    if (!readLocks.containsKey(fname)) {
                        readLocks.put(fname, new HashSet<ServerTask>());
                    }
                    readLocks.get(fname).add(locker);
                    locked = true;
                }
            }
        }
    }
    protected static void readUnLock(ServerTask locker, String fname) {
        synchronized(readLocks) {
            if (readLocks.containsKey(fname)) {
                Set<ServerTask> set = readLocks.get(fname);
                set.remove(locker);
                if (set.size() == 0) {
                    readLocks.remove(fname);
                }
            }
        }
    }
    protected static void writeLock(ServerTask locker, String fname) {
        boolean locked = false;
        synchronized(writeLocks) {
            if (writeLocks.containsKey(fname) &&
                writeLocks.get(fname) == locker) {
                return;
            }
        }
        while (!locked) {
            if (writeLocks.containsKey(fname)) {
                Thread.yield();
                continue;
            }
            synchronized(readLocks) {
                synchronized(writeLocks) {                
                    boolean justus = !readLocks.containsKey(fname) ||
                        (readLocks.containsKey(fname) &&
                         readLocks.get(fname).size() == 1 &&
                         readLocks.get(fname).contains(locker));
                    // can't acquire write lock if other people haveit read locked
                    if (writeLocks.containsKey(fname) ||
                        (readLocks.containsKey(fname) && !justus)) {
                        Thread.yield();
                        continue;
                    }
                    
                    writeLocks.put(fname, locker);
                    locked = true;
                }
            }
        }
    }
    protected static void writeUnLock(ServerTask locker, String fname) {
        synchronized(writeLocks) {
            if (writeLocks.containsKey(fname) && writeLocks.get(fname) == locker) {
                writeLocks.remove(fname);
            }
        }
    }
    /* call to ensure that all a thread's locks have been released */
    protected static void releaseLocks(ServerTask t) {
        synchronized(writeLocks) {
            for (String k : writeLocks.keySet()) {
                if (writeLocks.get(k) == t) {
                    writeLocks.remove(k);
                }
            }            
        }
        synchronized(readLocks) {
            for (String k : readLocks.keySet()) {
                Set<ServerTask> l = readLocks.get(k);
                l.remove(t);
                if (l.size() == 0) {
                    readLocks.remove(l);
                }
            }
        }
    }



}