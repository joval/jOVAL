// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.util;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * An interface for searching something.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface ISearchable<T> {
    int TYPE_EQUALITY	= 0;
    int TYPE_INEQUALITY	= 1;
    int TYPE_PATTERN	= 2;

    /**
     * Depth condition field ID for recursive searches.
     */
    int FIELD_DEPTH	= 0;

    /**
     * Starting point condition field ID for recursive searches.
     */
    int FIELD_FROM	= 1;

    /**
     * Unlimited depth condition value for recursive searches.
     */
    int DEPTH_UNLIMITED = -1;
    Object DEPTH_UNLIMITED_VALUE = new Integer(DEPTH_UNLIMITED);

    /**
     * Hazard a guess for the parent path of the specified pattern. Returns null if indeterminate. This method is
     * useful when building "from" conditions.
     */
    String[] guessParent(Pattern p, Object... args);

    /**
     * Recursively search for elements matching the given pattern.
     *
     * @param conditions a list of search conditions.
     */
    Collection<T> search(List<ICondition> conditions) throws Exception;

    /**
     * A search condition interface.
     */
    public interface ICondition {
	/**
	 * The type of assertion made by the condition.
	 */
	int getType();

	/**
	 * The scope of assertion made by the condition.
	 */
	int getField();

	/**
	 * The value of assertion made by the condition.
	 */
	Object getValue();
    }

    /**
     * A condition for unlimited recursion.
     */
    ICondition RECURSE = new GenericCondition(FIELD_DEPTH, TYPE_EQUALITY, DEPTH_UNLIMITED_VALUE);

    /**
     * Implement as: return new GenericCondition(field, type, value)
     */
    ISearchable.ICondition condition(int field, int type, Object value);

    // Class definitions for implementors

    static class GenericCondition implements ISearchable.ICondition {
	private int field, type;
	private Object value;

	public GenericCondition(int field, int type, Object value) {
	    this.field = field;
	    this.type = type;
	    this.value = value;
	}

	public int getType() {
	    return type;
	}

	public int getField() {
	    return field;
	}

	public Object getValue() {
	    return value;
	}
    }
}
