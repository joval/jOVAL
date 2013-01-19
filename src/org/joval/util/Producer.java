// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util;

import java.util.Collection;
import java.util.ArrayList;

import org.joval.intf.util.IObserver;
import org.joval.intf.util.IProducer;

/**
 * Utility class for an IProducer.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Producer<T extends Enum> implements IProducer<T> {
    private Collection<IObserver<T>> observers;

    public Producer() {
	observers = new ArrayList<IObserver<T>>();
    }

    public void sendNotify(T msg, Object arg) {
	//
	// Create a method-local copy of the observers so that an IObserver can remove itself in a notification
        // response if desired. 
	//
	Collection<IObserver<T>> local = new ArrayList<IObserver<T>>();
	local.addAll(observers);
	for (IObserver<T> observer : local) {
	     observer.notify(this, msg, arg);
	}
    }

    // Implement IProducer

    public void addObserver(IObserver<T> observer) {
	if (!observers.contains(observer)) {
	    observers.add(observer);
	}
    }

    public void removeObserver(IObserver<T> observer) {
	observers.remove(observer);
    }
}
