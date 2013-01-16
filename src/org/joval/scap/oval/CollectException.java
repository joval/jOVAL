// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval;

import scap.oval.systemcharacteristics.core.FlagEnumeration;

/**
 * An exception class for OVAL item collection.  This is used by implementations of IAdapter to signify that, for some
 * reason, an Object's items could not be collected.  When a CollectException is thrown from a call to IAdapter.getItems,
 * the corresponding object is flagged with the flag specified by this exception.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class CollectException extends Exception {
    private FlagEnumeration flag;

    public CollectException(String message, FlagEnumeration flag) {
	super(message);
	this.flag = flag;
    }

    public CollectException(Exception e, FlagEnumeration flag) {
	super(e);
	this.flag = flag;
    }

    public FlagEnumeration getFlag() {
	return flag;
    }
}
