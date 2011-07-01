// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.oval;

import java.util.Iterator;

import oval.schemas.definitions.core.DefinitionType;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.core.OvalDefinitions;
import oval.schemas.definitions.core.StateType;
import oval.schemas.definitions.core.TestType;
import oval.schemas.definitions.core.VariableType;

import org.joval.oval.OvalException;

/**
 * Interface defining an index on an OvalDefinitions object.  This facilitates fast retrieval of important structures, and
 * classes of structures.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IDefinitions {
    /**
     * Get an OvalDefinitions object corresponding to this interface.
     */
    public OvalDefinitions getOvalDefinitions();

    /**
     * Get an ObjectType of the specified class, with the specified ID.
     *
     * @throws OvalException if not found, or if there is a type mismatch.
     */
    public <T extends ObjectType> T getObject(String id, Class<T> type) throws OvalException;

    /**
     * Get a VariableType with the specified ID.
     *
     * @throws OvalException if not found.
     */
    public VariableType getVariable(String id) throws OvalException;

    /**
     * Get a StateType of the specified class, with the specified ID.
     *
     * @throws OvalException if not found, or if there is a type mismatch.
     */
    public <T extends StateType> T getState(String id, Class<T> type) throws OvalException;

    /**
     * Get a TestType of the specified class, with the specified ID.
     *
     * @throws OvalException if not found, or if there is a type mismatch.
     */
    public <T extends TestType> T getTest(String id, Class<T> type) throws OvalException;

    /**
     * Get a DefinitionType with the specified ID.
     *
     * @throws OvalException if not found.
     */
    public DefinitionType getDefinition(String id) throws OvalException;

    /**
     * Returns an Iterator over all the ObjectTypes defined in the IDefinitions.
     *
     * @throws OvalException if not found.
     */
    public Iterator <ObjectType>iterateObjects();

    /**
     * Returns an Iterator over all the ObjectType instances of the class denoted by the class argument.  This is used
     * by the Engine classes to pre-fetch all the objects handled by a given IAdapter all at once.
     */
    public Iterator<ObjectType> iterateObjects(Class type);

    /**
     * Returns an Iterator over all the VariableTypes defined in the IDefinitions.
     *
     * @throws OvalException if not found.
     */
    public Iterator <VariableType>iterateVariables();
}
