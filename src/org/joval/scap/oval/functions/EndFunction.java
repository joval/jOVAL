// Copyright (C) 2014 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.functions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;

import scap.oval.definitions.core.EndFunctionType;

import org.joval.intf.scap.oval.IType;
import org.joval.scap.oval.OvalException;
import org.joval.scap.oval.types.TypeFactory;

/**
 * Implementation for the end function type.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class EndFunction implements IFunction {
    public EndFunction() {
    }

    // Implement IFunction

    public Class<?> getFunctionType() {
	return EndFunctionType.class;
    }

    public Collection<IType> compute(Object obj, IFunctionContext fc) throws NoSuchElementException,
		UnsupportedOperationException, IllegalArgumentException, ResolveException, OvalException {

	if (getFunctionType().isInstance(obj)) {
	    EndFunctionType function = (EndFunctionType)obj;
	    String s = function.getCharacter();
	    Collection<IType> values = new ArrayList<IType>();
	    for (IType value : fc.resolveComponent(fc.getComponent(function))) {
		String str = value.getString();
		if (str.endsWith(s)) {
		    values.add(value);
		} else {
		    values.add(TypeFactory.createType(IType.Type.STRING, str + s));
		}
	    }
	    return values;
	} else {
	    throw new IllegalArgumentException(obj.toString());
	}
    }
}
