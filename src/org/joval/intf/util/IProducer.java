// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.util;

/**
 * The producer half of the Producer/Observer pattern.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IProducer<T extends Enum> {
    /**
     * Add an observer.  When the producer generates a message in the range between min and max, it notifies the observer
     * using its notify method.
     */
    public void addObserver(IObserver<T> observer);

    /**
     * Remove an observer.
     */
    public void removeObserver(IObserver<T> observer);
}
