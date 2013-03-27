// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.scap.oval;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import scap.oval.definitions.core.DefinitionType;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.core.OvalDefinitions;
import scap.oval.definitions.core.StateType;
import scap.oval.definitions.core.TestType;
import scap.oval.definitions.core.VariableType;

import org.joval.intf.xml.ITransformable;

/**
 * Interface defining an index to an OvalDefinitions object, facilitating fast look-up of definitions, tests, variables,
 * objects and states defined in the document.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IDefinitions extends ITransformable {
    /**
     * Get the raw OVAL definitions object.
     */
    OvalDefinitions getOvalDefinitions();

    /**
     * Retrieve the OVAL state with the specified ID.
     *
     * @throws NoSuchElementException if no state with the given ID is found
     */
    StateType getState(String id) throws NoSuchElementException;

    /**
     * Retrieve the OVAL test with the specified ID.
     *
     * @throws NoSuchElementException if no test with the given ID is found
     */
    TestType getTest(String id) throws NoSuchElementException;

    /**
     * Retrieve the Object with the specified ID.
     *
     * @throws NoSuchElementException if no object with the given ID is found
     */
    ObjectType getObject(String id) throws NoSuchElementException;

    /**
     * Retrieve the OVAL object with the specified ID, corresponding to the specified type.
     *
     * @throws NoSuchElementException if the type does not match, or if no object with the given ID is found.
     */
    <T extends ObjectType> T getObject(String id, Class<T> type) throws NoSuchElementException;

    /**
     * Retrieve the OVAL variable with the specified ID.
     *
     * @throws NoSuchElementException if no variable with the given ID is found
     */
    VariableType getVariable(String id) throws NoSuchElementException;

    /**
     * Retrieve the OVAL definition with the specified ID.
     *
     * @throws NoSuchElementException if no definition with the given ID is found
     */
    DefinitionType getDefinition(String id) throws NoSuchElementException;

    /**
     * Get a collection of all the objects.
     */
    Collection<ObjectType> getObjects();

    /**
     * Get a collection of all the variables.
     */
    Collection<VariableType> getVariables();

    /**
     * Sort all DefinitionTypes into two lists according to whether or not the filter allows them.
     */
    void filterDefinitions(IDefinitionFilter filter, Collection<DefinitionType> allowed, Collection<DefinitionType> disallowed);
}
