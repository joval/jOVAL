// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.windows.registry;

/**
 * Interface to an abstract Windows registry value.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IValue {
    enum Type {
	REG_NONE("REG_NONE", 0),
	REG_DWORD("REG_DWORD", 1),
	REG_BINARY("REG_BINARY", 2),
	REG_SZ("REG_SZ", 3),
	REG_EXPAND_SZ("REG_EXPAND_SZ", 4),
	REG_MULTI_SZ("REG_MULTI_SZ", 5),
	REG_QWORD("REG_QWORD", 6);

	private String name;
	private int id;

	private Type(String name, int id) {
	    this.name = name;
	    this.id = id;
	}

	public String getName() {
	    return name;
	}

	public int getId() {	
	    return id;
	}

	public static Type typeOf(String name) throws IllegalArgumentException {
	    for (Type type : values()) {
		if (type.getName().equals(name)) {
		    return type;
		}
	    }
	    throw new IllegalArgumentException(name);
	}
    }

    /**
     * Returns the corresponding REG_ constant.
     */
    public Type getType();

    /**
     * Return the Key under which this Value lies.
     */
    public IKey getKey();

    /**
     * Return the Value's name.
     */
    public String getName();

    /**
     * Returns a String suitable for logging about the Value.
     */
    public String toString();
}
