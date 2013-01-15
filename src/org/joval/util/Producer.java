// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joval.intf.util.IObserver;
import org.joval.intf.util.IProducer;

/**
 * Utility class for an IProducer.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Producer implements IProducer {
    Map<IObserver, ObserverContext> observers;

    public Producer() {
	observers = new HashMap<IObserver, ObserverContext>();
    }

    public void sendNotify(int msg, Object arg) {
	//
	// Create a method-local copy of the observers so that an IObserver can remove itself if desired. 
	//
	List<ObserverContext> local = new ArrayList<ObserverContext>();
	local.addAll(observers.values());
	for (ObserverContext ctx : local) {
	     ctx.sendNotify(this, msg, arg);
	}
    }

    // Implement IProducer

    public void addObserver(IObserver observer, int min, int max) {
	if (!observers.containsKey(observer)) {
	    observers.put(observer, new ObserverContext(observer, min, max));
	}
    }

    public void removeObserver(IObserver observer) {
	if (observers.containsKey(observer)) {
	    observers.remove(observer);
	}
    }

    // Private

    private class ObserverContext {
	private int min, max;
	private IObserver observer;

	private ObserverContext(IObserver observer, int min, int max) {
	    this.observer = observer;
	    this.min = min;
	    this.max = max;
	}

	private boolean sendNotify(IProducer producer, int msg, Object arg) {
	    if (msg >= min && msg <= max) {
		observer.notify(producer, msg, arg);
		return true;
	    } else {
		return false;
	    }
	}
    }
}
