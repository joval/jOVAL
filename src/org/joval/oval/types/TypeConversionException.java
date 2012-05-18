// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.types;

/**
 * An exception class for OVAL type conversion errors.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class TypeConversionException extends Exception {
    public TypeConversionException(String message) {
	super(message);
    }

    public TypeConversionException(Exception e) {
	super(e);
    }
}
