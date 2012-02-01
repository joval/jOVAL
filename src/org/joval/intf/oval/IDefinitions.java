// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.oval;

import java.util.Collection;
import java.util.Iterator;

import oval.schemas.definitions.core.DefinitionType;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.core.OvalDefinitions;
import oval.schemas.definitions.core.StateType;
import oval.schemas.definitions.core.TestType;
import oval.schemas.definitions.core.VariableType;

import org.joval.oval.OvalException;

/**
 * Interface defining an index to an OvalDefinitions object, facilitating fast look-up of definitions, tests, variables,
 * objects and states defined in the document.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IDefinitions {
    /**
     * Get the raw OVAL definitions object.
     */
    OvalDefinitions getOvalDefinitions();

    /**
     * Retrieve the OVAL state with the specified ID.
     *
     * @throws OvalException if no state with the given ID is found
     */
    StateType getState(String id) throws OvalException;

    /**
     * Retrieve the OVAL test with the specified ID.
     *
     * @throws OvalException if no test with the given ID is found
     */
    TestType getTest(String id) throws OvalException;

    /**
     * Retrieve the Object with the specified ID.
     *
     * @throws OvalException if no object with the given ID is found
     */
    ObjectType getObject(String id) throws OvalException;

    /**
     * Retrieve the OVAL object with the specified ID, corresponding to the specified type.
     *
     * @throws OvalException if the type does not match, or if no object with the given ID is found.
     */
    <T extends ObjectType> T getObject(String id, Class<T> type) throws OvalException;

    /**
     * Retrieve the OVAL variable with the specified ID.
     *
     * @throws OvalException if no variable with the given ID is found
     */
    VariableType getVariable(String id) throws OvalException;

    /**
     * Retrieve the OVAL definition with the specified ID.
     *
     * @throws OvalException if no definition with the given ID is found
     */
    DefinitionType getDefinition(String id) throws OvalException;

    /**
     * Iterate over all the objects.
     */
    Iterator<ObjectType> iterateObjects();

    /**
     * Iterate over all the objects of the specified type.
     */
    Iterator<ObjectType> iterateObjects(Class type);

    /**
     * Iterate over all the variables.
     */
    Iterator<VariableType> iterateVariables();

    /**
     * Sort all DefinitionTypes into two lists according to whether or not the filter allows them.
     */
    void filterDefinitions(IDefinitionFilter filter, Collection<DefinitionType> allowed, Collection<DefinitionType> disallowed);
}
