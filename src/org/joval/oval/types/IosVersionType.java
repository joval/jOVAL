// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.types;

import org.joval.intf.oval.IType;

/**
 * IOS version type.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class IosVersionType extends AbstractType {
    String data;

    public IosVersionType(String data) {
	this.data = data;
    }

    public String getData() {
	return data;
    }

    // Implement IType

    public Type getType() {
	return Type.IOS_VERSION;
    }

    public String getString() {
	return data;
    }

    // Implement Comparable

    public int compareTo(IType t) {
	IosVersionType other = null;
	try {
	    other = (IosVersionType)t.cast(getType());
	} catch (TypeConversionException e) {
	    throw new IllegalArgumentException(e);
	}
	return new EvrStringType(data).compareTo(new EvrStringType(other.data));
    }
}
