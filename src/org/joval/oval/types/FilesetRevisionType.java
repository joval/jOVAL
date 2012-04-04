// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.types;

import oval.schemas.common.SimpleDatatypeEnumeration;

/**
 * Fileset revision type implementation.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class FilesetRevisionType implements IType<FilesetRevisionType> {
    private String data;

    public FilesetRevisionType(String data) {
	this.data = data;
    }

    public String toString() {
	return data;
    }

    public String getData() {
	return data;
    }

    // Implement IType

    public SimpleDatatypeEnumeration getType() {
	return SimpleDatatypeEnumeration.FILESET_REVISION;
    }

    // Implement Comparable

    public int compareTo(FilesetRevisionType other) {
	return new EvrStringType(data).compareTo(new EvrStringType(other.data));
    }
}
