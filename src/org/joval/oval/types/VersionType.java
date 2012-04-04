// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.types;

import oval.schemas.common.SimpleDatatypeEnumeration;

import org.joval.util.Version;

/**
 * Version type.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class VersionType implements IType<VersionType> {
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

    public String toString() {
	return data.toString();
    }

    // Implement IType

    public SimpleDatatypeEnumeration getType() {
	return SimpleDatatypeEnumeration.VERSION;
    }

    // Implement Comparable

    public int compareTo(VersionType other) {
	return data.compareTo(other.data);
    }
}
