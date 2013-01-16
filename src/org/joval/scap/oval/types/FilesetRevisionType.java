// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.types;

import org.joval.intf.scap.oval.IType;

/**
 * Fileset revision type implementation.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class FilesetRevisionType extends AbstractType {
    private String data;

    public FilesetRevisionType(String data) {
	this.data = data;
    }

    public String getData() {
	return data;
    }

    // Implement IType

    public Type getType() {
	return Type.FILESET_REVISION;
    }

    public String getString() {
	return data;
    }

    // Implement Comparable

    public int compareTo(IType t) {
	FilesetRevisionType other = null;
	try {
	    other = (FilesetRevisionType)t.cast(getType());
	} catch (TypeConversionException e) {
	    throw new IllegalArgumentException(e);
	}
	return new EvrStringType(data).compareTo(new EvrStringType(other.data));
    }
}
