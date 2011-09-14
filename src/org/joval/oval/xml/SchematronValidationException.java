// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.xml;

import java.util.List;
import java.util.Vector;

/**
 * An exception class for OVAL schematron validation.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SchematronValidationException extends Exception {
    List<String> errors;

    public SchematronValidationException(String message) {
	super(message);
	this.errors = new Vector<String>();
    }

    public SchematronValidationException(String message, List<String> errors) {
	super(message);
	this.errors = errors;
    }

    public SchematronValidationException(Exception e) {
	super(e);
	this.errors = new Vector<String>();
    }

    public List<String> getErrors() {
	return errors;
    }
}
