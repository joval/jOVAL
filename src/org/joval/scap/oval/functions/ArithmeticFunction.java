// Copyright (C) 2014 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.functions;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Stack;

import scap.oval.definitions.core.ArithmeticEnumeration;
import scap.oval.definitions.core.ArithmeticFunctionType;

import org.joval.intf.scap.oval.IType;
import org.joval.scap.oval.OvalException;
import org.joval.scap.oval.types.TypeFactory;

/**
 * Implementation for the arithmetic function type.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class ArithmeticFunction implements IFunction {
    public ArithmeticFunction() {
    }

    // Implement IFunction<ArithmeticFunctionType>

    public Class<?> getFunctionType() {
	return ArithmeticFunctionType.class;
    }

    public Collection<IType> compute(Object obj, IFunctionContext fc) throws NoSuchElementException,
		UnsupportedOperationException, IllegalArgumentException, ResolveException, OvalException {

	if (getFunctionType().isInstance(obj)) {
	    ArithmeticFunctionType function = (ArithmeticFunctionType)obj;
	    Stack<Collection<IType>> rows = new Stack<Collection<IType>>();
	    ArithmeticEnumeration op = function.getArithmeticOperation();
	    for (Object child : function.getObjectComponentOrVariableComponentOrLiteralComponent()) {
		Collection<IType> row = new ArrayList<IType>();
		for (IType cell : fc.resolveComponent(child)) {
		    row.add(cell);
		}
		rows.add(row);
	    }
	    return computeProduct(op, rows);
	} else {
	    throw new IllegalArgumentException(obj.toString());
	}
    }

    // Private

    /**
     * Perform the Arithmetic operation on permutations of the Stack, and return the resulting permutations.
     */
    private List<IType> computeProduct(ArithmeticEnumeration op, Stack<Collection<IType>> rows)
		throws IllegalArgumentException {

	List<IType> results = new ArrayList<IType>();
	if (rows.empty()) {
	    switch(op) {
		case ADD:
		  results.add(TypeFactory.createType(IType.Type.INT, "0"));
		  break;
		case MULTIPLY:
		  results.add(TypeFactory.createType(IType.Type.INT, "1"));
		  break;
	    }
	} else {
	    for (IType type : rows.pop()) {
		String value = type.getString();
		Stack<Collection<IType>> copy = new Stack<Collection<IType>>();
		copy.addAll(rows);
		for (IType otherType : computeProduct(op, copy)) {
		    String otherValue = otherType.getString();
		    switch(op) {
		      case ADD:
			if (value.indexOf(".") == -1 && otherValue.indexOf(".") == -1) {
			    String sum =  new BigInteger(value).add(new BigInteger(otherValue)).toString();
			    results.add(TypeFactory.createType(IType.Type.INT, sum));
			} else {
			    String sum = new BigDecimal(value).add(new BigDecimal(otherValue)).toString();
			    results.add(TypeFactory.createType(IType.Type.FLOAT, sum));
			}
			break;

		      case MULTIPLY:
			if (value.indexOf(".") == -1 && otherValue.indexOf(".") == -1) {
			    String product = new BigInteger(value).multiply(new BigInteger(otherValue)).toString();
			    results.add(TypeFactory.createType(IType.Type.INT, product));
			} else {
			    String product = new BigDecimal(value).multiply(new BigDecimal(otherValue)).toString();
			    results.add(TypeFactory.createType(IType.Type.FLOAT, product));
			}
			break;
		    }
		}
	    }
	}
	return results;
    }
}
