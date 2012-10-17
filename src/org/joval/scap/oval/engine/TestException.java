// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.engine;

/**
 * An exception class for Test evaluations, used to propagate messages upstream to the object.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class TestException extends Exception {
    TestException(String message) {
	super(message);
    }

    TestException(Exception e) {
	super(e);
    }
}
