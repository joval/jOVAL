// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util;

import java.util.regex.Pattern;

import oval.schemas.definitions.core.EntityStateSimpleBaseType;
import oval.schemas.systemcharacteristics.core.EntityItemSimpleBaseType;

import org.joval.oval.OvalException;

/**
 * Some tools for dealing with OVAL types.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class TypeTools {
    /**
     * Compare a state SimpleBaseType to an item SimpleBaseType.  If the item is null, this method returns false.  That
     * allows callers to simply check if the state is set before invoking the comparison.
     */
    public static boolean compare(EntityStateSimpleBaseType state, EntityItemSimpleBaseType item) throws OvalException {
	if (item == null) {
	    return false;
	}
	switch (state.getOperation()) {
	  case CASE_INSENSITIVE_EQUALS:
	    return ((String)item.getValue()).equalsIgnoreCase((String)state.getValue());
	  case EQUALS:
	    return ((String)item.getValue()).equals((String)state.getValue());
	  case PATTERN_MATCH:
	    return Pattern.compile((String)state.getValue()).matcher((String)item.getValue()).find();
	  default:
	    throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_OPERATION", state.getOperation()));
	}
    }
}
