// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.types;

import oval.schemas.common.SimpleDatatypeEnumeration;

/**
 * String type.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class StringType implements IType<StringType> {
    private String data;

    public StringType(String data) {
	this.data = data;
    }

    public String getData() {
	return data;
    }

    public String toString() {
	return data;
    }

    // Implement ITyped

    public SimpleDatatypeEnumeration getType() {
	return SimpleDatatypeEnumeration.STRING;
    }

    // Implement Comparable

    public int compareTo(StringType other) {
	return data.compareTo(other.data);
    }
}
