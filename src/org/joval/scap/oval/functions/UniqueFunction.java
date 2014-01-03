// Copyright (C) 2014 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.functions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.NoSuchElementException;

import scap.oval.definitions.core.UniqueFunctionType;

import org.joval.intf.scap.oval.IType;
import org.joval.scap.oval.OvalException;
import org.joval.scap.oval.types.TypeFactory;

/**
 * Implementation for the unique function type.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class UniqueFunction implements IFunction {
    public UniqueFunction() {
    }

    // Implement IFunction

    public Class<?> getFunctionType() {
	return UniqueFunctionType.class;
    }

    public Collection<IType> compute(Object obj, IFunctionContext fc) throws NoSuchElementException,
		UnsupportedOperationException, IllegalArgumentException, ResolveException, OvalException {

	if (getFunctionType().isInstance(obj)) {
	    UniqueFunctionType function = (UniqueFunctionType)obj;
	    HashSet<IType> values = new HashSet<IType>();
	    for (Object child : function.getObjectComponentOrVariableComponentOrLiteralComponent()) {
		values.addAll(fc.resolveComponent(child));
	    }
	    return values;
	} else {
	    throw new IllegalArgumentException(obj.toString());
	}
    }
}
