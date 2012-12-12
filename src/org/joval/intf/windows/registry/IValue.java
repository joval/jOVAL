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
	REG_NONE      ("REG_NONE",	"None",		0),
	REG_DWORD     ("REG_DWORD",	"DWord",	1),
	REG_BINARY    ("REG_BINARY",	"Binary",	2),
	REG_SZ	      ("REG_SZ",	"String",	3),
	REG_EXPAND_SZ ("REG_EXPAND_SZ",	"ExpandString",	4),
	REG_MULTI_SZ  ("REG_MULTI_SZ",	"MultiString",	5),
	REG_QWORD     ("REG_QWORD",	"QuadWord",	6);

	private String name, kind;
	private int id;

	private Type(String name, String kind, int id) {
	    this.name = name;
	    this.kind = kind;
	    this.id = id;
	}

	public String getName() {
	    return name;
	}

	/**
	 * The "Kind" as returned by C# Microsoft.Win32.RegistryKey::GetValueKind
	 */
	public String getKind() {
	    return kind;
	}

	public int getId() {	
	    return id;
	}

	public static Type fromName(String name) throws IllegalArgumentException {
	    for (Type type : values()) {
		if (type.getName().equals(name)) {
		    return type;
		}
	    }
	    throw new IllegalArgumentException(name);
	}

	public static Type fromKind(String kind) throws IllegalArgumentException {
	    for (Type type : values()) {
		if (type.getKind().equals(kind)) {
		    return type;
		}
	    }
	    throw new IllegalArgumentException(kind);
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
