// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.util;

/**
 * Observer half of a Producer/Observer pattern.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IObserver {
    /**
     * An observed IProducer has generated a message.
     */
    public void notify(IProducer source, int msg, Object arg);
}
