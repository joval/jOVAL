// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.oval;

import oval.schemas.common.ComplexDatatypeEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;

import org.joval.scap.oval.types.TypeConversionException;

/**
 * Generic type interface, for both simple and complex OVAL types.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IType extends Comparable<IType> {
    /**
     * An enumeration mixing both simple and complex OVAL types.
     */
    enum Type {
	BINARY,
	BOOLEAN,
	EVR_STRING,
	FILESET_REVISION,
	FLOAT,
	IOS_VERSION,
	INT,
	IPV_4_ADDRESS,
	IPV_6_ADDRESS,
	RECORD,
	STRING,
	VERSION;

	/**
	 * Return the corresponding SimpleDatatypeEnumeration.
	 */
	public SimpleDatatypeEnumeration getSimple() throws UnsupportedOperationException {
	    if (this == RECORD) {
		throw new UnsupportedOperationException("getSimple()");
	    } else {
		switch(this) {
		  case BINARY:
		    return SimpleDatatypeEnumeration.BINARY;
		  case BOOLEAN:
		    return SimpleDatatypeEnumeration.BOOLEAN;
		  case EVR_STRING:
		    return SimpleDatatypeEnumeration.EVR_STRING;
		  case FILESET_REVISION:
		    return SimpleDatatypeEnumeration.FILESET_REVISION;
		  case FLOAT:
		    return SimpleDatatypeEnumeration.FLOAT;
		  case IOS_VERSION:
		    return SimpleDatatypeEnumeration.IOS_VERSION;
		  case INT:
		    return SimpleDatatypeEnumeration.INT;
		  case IPV_4_ADDRESS:
		    return SimpleDatatypeEnumeration.IPV_4_ADDRESS;
		  case IPV_6_ADDRESS:
		    return SimpleDatatypeEnumeration.IPV_6_ADDRESS;
		  case VERSION:
		    return SimpleDatatypeEnumeration.VERSION;
		  case STRING:
		  default:
		    return SimpleDatatypeEnumeration.STRING;
		}
	    }
	}

	/**
	 * Return the corresponding ComplexDatatypeEnumeration.
	 */
	public ComplexDatatypeEnumeration getComplex() throws UnsupportedOperationException {
	    if (this == RECORD) {
		return ComplexDatatypeEnumeration.RECORD;
	    } else {
		throw new UnsupportedOperationException("getComplex()");
	    }
	}
    }

    /**
     * Get the datatype.
     */
    Type getType();

    /**
     * Cast to another Type.
     */
    IType cast(Type type) throws TypeConversionException;

    /**
     * Cast to another Type.
     */
    IType cast(SimpleDatatypeEnumeration type) throws TypeConversionException;

    /**
     * @throws UnsupportedOperationException if the Type is RECORD.
     */
    String getString() throws UnsupportedOperationException;
}
