// Copyright (C) 2014 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.functions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;

import scap.oval.definitions.core.BeginFunctionType;

import org.joval.intf.scap.oval.IType;
import org.joval.scap.oval.OvalException;
import org.joval.scap.oval.types.TypeFactory;

/**
 * Implementation for the begin function type.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class BeginFunction implements IFunction {
    public BeginFunction() {
    }

    // Implement IFunction

    public Class<?> getFunctionType() {
	return BeginFunctionType.class;
    }

    public Collection<IType> compute(Object obj, IFunctionContext fc) throws NoSuchElementException,
		UnsupportedOperationException, IllegalArgumentException, ResolveException, OvalException {

	if (getFunctionType().isInstance(obj)) {
	    BeginFunctionType function = (BeginFunctionType)obj;
	    String s = function.getCharacter();
	    Collection<IType> values = new ArrayList<IType>();
	    for (IType value : fc.resolveComponent(fc.getComponent(function))) {
		String str = value.getString();
		if (str.startsWith(s)) {
		    values.add(value);
		} else {
		    values.add(TypeFactory.createType(IType.Type.STRING, s + str));
		}
	    }
	    return values;
	} else {
	    throw new IllegalArgumentException(obj.toString());
	}
    }
}
