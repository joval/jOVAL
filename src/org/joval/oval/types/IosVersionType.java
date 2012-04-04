// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.types;

import oval.schemas.common.SimpleDatatypeEnumeration;

/**
 * IOS version type.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class IosVersionType implements IType<IosVersionType> {
    String data;

    public IosVersionType(String data) {
	this.data = data;
    }

    public String getData() {
	return data;
    }

    public String toString() {
	return data;
    }

    // Implement IType

    public SimpleDatatypeEnumeration getType() {
	return SimpleDatatypeEnumeration.IOS_VERSION;
    }

    // Implement Comparable

    public int compareTo(IosVersionType other) {
	return new EvrStringType(data).compareTo(new EvrStringType(other.data));
    }
}
