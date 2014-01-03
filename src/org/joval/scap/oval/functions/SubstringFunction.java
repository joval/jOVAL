// Copyright (C) 2014 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.functions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;

import scap.oval.definitions.core.SubstringFunctionType;

import org.joval.intf.scap.oval.IType;
import org.joval.scap.oval.OvalException;
import org.joval.scap.oval.types.TypeFactory;
import org.joval.util.JOVALMsg;

/**
 * Implementation for the substring function type.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SubstringFunction implements IFunction {
    public SubstringFunction() {
    }

    // Implement IFunction

    public Class<?> getFunctionType() {
	return SubstringFunctionType.class;
    }

    public Collection<IType> compute(Object obj, IFunctionContext fc) throws NoSuchElementException,
		UnsupportedOperationException, IllegalArgumentException, ResolveException, OvalException {

	if (getFunctionType().isInstance(obj)) {
	    SubstringFunctionType function = (SubstringFunctionType)obj;
	    int start = function.getSubstringStart();
	    start = Math.max(1, start);	// a start index < 1 means start at 1
	    start--;			// OVAL counter begins at 1 instead of 0
	    int len = function.getSubstringLength();
	    Collection<IType> values = new ArrayList<IType>();
	    for (IType value : fc.resolveComponent(fc.getComponent(function))) {
		String str = value.getString();

		//
		// If the substring_start attribute has value greater than the length of the original string
		// an error should be reported.
		//
		if (start > str.length()) {
		    throw new ResolveException(JOVALMsg.getMessage(JOVALMsg.ERROR_SUBSTRING, str, new Integer(start)));

		//
		// A substring_length value greater than the actual length of the string, or a negative value,
		// means to include all of the characters after the starting character.
		//
		} else if (len < 0 || str.length() <= (start+len)) {
		    values.add(TypeFactory.createType(IType.Type.STRING, str.substring(start)));

		} else {
		    values.add(TypeFactory.createType(IType.Type.STRING, str.substring(start, start+len)));
		}
	    }
	    return values;
	} else {
	    throw new IllegalArgumentException(obj.toString());
	}
    }
}
