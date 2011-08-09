// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval;

/**
 * An exception class for Test evaluations, used to propagate messages upstream to the object.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class TestException extends Exception {
    public TestException(String message) {
	super(message);
    }

    public TestException(Exception e) {
	super(e);
    }
}
