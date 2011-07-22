// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.plugin;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;

import oval.schemas.common.MessageType;
import oval.schemas.definitions.core.VariableType;
import oval.schemas.systemcharacteristics.core.VariableValueType;

import org.joval.oval.OvalException;

/**
 * The IAdapterContext provides services to the IAdapters.
 *
 * The interface for retrieving the values of VariableType objects.  When an ITestEvaluator is registered with the Engine,
 * the Engine will call the setResolver method, passing in its IVariableResolver.  This way, only the Engine needs to worry
 * about chasing around all the references to object values throughout the OVAL definitions.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IAdapterContext {
    /**
     * Log a message.
     */
    public void log(Level level, String message);

    /**
     * Log an exception.
     */
    public void log(Level level, String message, Throwable thrown);

    /**
     * Resolve a variable given its ID String.  A VariableType, in the context of the jOVAL Engine, is either a LocalVariable
     * or an ExternalVariable which has been provided by a file. An external variable is invariably just a "literal" value. A
     * LocalVariable can contain an ObjectComponentType (which is a reference to an ObjectType), a LiteralComponentType
     * (which is a constant), or it can contain any of a number of [XYZ]FunctionType objects, which can themselves contain
     * any of these same types of objects, and which cause some function to be applied to each of them.  It therefore makes
     * sense to return a List of VariableValueType objects, each of which contains the resolution of one of these components
     * that makes up the VariableType.
     *
     * @throws NoSuchElementException if the Variable could not be resolved because it refers to a non-existent resource on
     *                                the target machine.
     * #throws OvalException if the OVAL is somehow invalid, for instance, if a variable ID is referenced but there is no
     *			     variable with that ID defined in the OVAL definitions file.
     */
    public String resolve(String id, List<VariableValueType> list) throws NoSuchElementException, OvalException;

    /**
     * Add a message for an ObjectType record.
     */
    public void addObjectMessage(String objectId, MessageType message);
}
