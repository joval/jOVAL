// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.xml.schematron;

import java.util.List;
import java.util.Vector;

/**
 * An exception class for OVAL schematron validation.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class ValidationException extends Exception {
    List<String> errors;

    public ValidationException(String message) {
	super(message);
	this.errors = new Vector<String>();
    }

    public ValidationException(String message, List<String> errors) {
	super(message);
	this.errors = errors;
    }

    public ValidationException(Exception e) {
	super(e);
	this.errors = new Vector<String>();
    }

    public List<String> getErrors() {
	return errors;
    }
}
