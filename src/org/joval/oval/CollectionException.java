// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval;

/**
 * An exception class for OVAL item collection.  This is used by implementations of IAdapter to signify that, for some
 * reason, an Object's items could not be collected.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class CollectionException extends Exception {
    public CollectionException(String message) {
	super(message);
    }

    public CollectionException(Exception e) {
	super(e);
    }
}
