// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.types;

import org.joval.intf.scap.oval.IType;
import org.joval.util.Version;

/**
 * Version type.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class VersionType extends AbstractType {
    private Version data;

    public VersionType(String data) throws IllegalArgumentException {
	this(new Version(data));
    }

    public VersionType(Version data) {
	this.data = data;
    }

    public Version getData() {
	return data;
    }

    // Implement IType

    public Type getType() {
	return Type.VERSION;
    }

    public String getString() {
	return data.toString();
    }

    // Implement Comparable

    public int compareTo(IType t) {
	VersionType other = null;
	try {
	    other = (VersionType)t.cast(getType());
	} catch (TypeConversionException e) {
	    throw new IllegalArgumentException(e);
	}
	return data.compareTo(other.data);
    }
}
