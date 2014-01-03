// Copyright (C) 2014 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.functions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.NoSuchElementException;

import scap.oval.definitions.core.ConcatFunctionType;

import org.joval.intf.scap.oval.IType;
import org.joval.scap.oval.OvalException;
import org.joval.scap.oval.types.TypeFactory;

/**
 * Implementation for the concat function type.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class ConcatFunction implements IFunction {
    public ConcatFunction() {
    }

    // Implement IFunction<ConcatFunctionType>

    public Class<?> getFunctionType() {
	return ConcatFunctionType.class;
    }

    public Collection<IType> compute(Object obj, IFunctionContext fc) throws NoSuchElementException,
		UnsupportedOperationException, IllegalArgumentException, ResolveException, OvalException {

	if (getFunctionType().isInstance(obj)) {
	    ConcatFunctionType function = (ConcatFunctionType)obj;
	    Collection<IType> values = new ArrayList<IType>();
	    for (Object child : function.getObjectComponentOrVariableComponentOrLiteralComponent()) {
		Collection<IType> next = fc.resolveComponent(child);
		if (next.size() == 0) {
		    @SuppressWarnings("unchecked")
		    Collection<IType> empty = (Collection<IType>)Collections.EMPTY_LIST;
		    return empty;
		} else if (values.size() == 0) {
		    values.addAll(next);
		} else {
		    //
		    // Cartesian product
		    //
		    Collection<IType> newValues = new ArrayList<IType>();
		    for (IType base : values) {
			for (IType val : next) {
			    newValues.add(TypeFactory.createType(IType.Type.STRING, base.getString() + val.getString()));
			}
		    }
		    values = newValues;
		}
	    }
	    return values;
	} else {
	    throw new IllegalArgumentException(obj.toString());
	}
    }
}
