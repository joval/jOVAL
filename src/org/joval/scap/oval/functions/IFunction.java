// Copyright (C) 2014 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.functions;

import java.util.Collection;
import java.util.NoSuchElementException;

import jsaf.intf.system.ISession;

import scap.oval.definitions.core.LiteralComponentType;
import scap.oval.definitions.core.ObjectComponentType;
import scap.oval.definitions.core.VariableComponentType;
import scap.oval.definitions.core.VariableType;

import org.joval.scap.oval.OvalException;
import org.joval.intf.scap.oval.IProvider;
import org.joval.intf.scap.oval.IType;

/**
 * Interface for all OVAL function implementations.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IFunction {
    /**
     * Returns the scap.oval.definitions.core.[*]FunctionType handled by this IFunction.
     */
    Class<?> getFunctionType();

    /**
     * Compute the value of the function.
     */
    Collection<IType> compute(Object function, IFunctionContext fc) throws NoSuchElementException,
	UnsupportedOperationException, IllegalArgumentException, ResolveException, OvalException;

    /**
     * The interface for the argument supplied to a function compute call, which provides a facility for adding messages
     * related to the request, and resolving sub-components.
     */
    interface IFunctionContext extends IProvider.IRequestContext {
	/**
	 * Get the session associated with the request.
	 */
	ISession getSession();

	/**
	 * Functions contain one or more components upon which they operate. If there is more than one, the object will
	 * have a method called getObjectComponentOrVariableComponentOrLiteralComponent() that returns a list whose
	 * elements can be fed to this method.
	 *
	 * Otherwise, the function will have only one child component. The spectrum of getter methods for supported child
	 * types depends on the scope of supported functions. This method can be used to return the child component in a
	 * neutral fashion.
	 *
	 * @throw OvalException if the object is not a supported component or function type
	 */
	Object getComponent(Object obj) throws OvalException;

	/**
	 * Given a component, return its resolved values.
	 */
	Collection<IType> resolveComponent(Object component) throws NoSuchElementException,
	    UnsupportedOperationException, IllegalArgumentException, ResolveException, OvalException;
    }
}
