// Copyright (C) 2014 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.functions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;

import scap.oval.definitions.core.CountFunctionType;

import org.joval.intf.scap.oval.IType;
import org.joval.scap.oval.OvalException;
import org.joval.scap.oval.types.TypeFactory;

/**
 * Implementation for the count function type.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class CountFunction implements IFunction {
    public CountFunction() {
    }

    // Implement IFunction

    public Class<?> getFunctionType() {
	return CountFunctionType.class;
    }

    public Collection<IType> compute(Object obj, IFunctionContext fc) throws NoSuchElementException,
		UnsupportedOperationException, IllegalArgumentException, ResolveException, OvalException {

	if (getFunctionType().isInstance(obj)) {
	    CountFunctionType function = (CountFunctionType)obj;
	    Collection<IType> children = new ArrayList<IType>();
	    for (Object child : function.getObjectComponentOrVariableComponentOrLiteralComponent()) {
		children.addAll(fc.resolveComponent(child));
	    }
	    Collection<IType> values = new ArrayList<IType>();
	    values.add(TypeFactory.createType(IType.Type.INT, Integer.toString(children.size())));
	    return values;
	} else {
	    throw new IllegalArgumentException(obj.toString());
	}
    }
}
