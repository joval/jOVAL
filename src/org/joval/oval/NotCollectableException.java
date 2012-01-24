// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval;

/**
 * An exception class for OVAL item collection.  This is used by implementations of IAdapter to signify that, for some
 * reason, an Object's items could not be collected.  When a NotCollectableException is thrown from a call to
 * IAdapter.getItems, the corresponding object is flagged as "not collected".
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class NotCollectableException extends Exception {
    public NotCollectableException(String message) {
	super(message);
    }

    public NotCollectableException(Exception e) {
	super(e);
    }
}
