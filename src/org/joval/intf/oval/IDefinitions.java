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
 * Index to an OvalDefinitions object, for fast look-up of definitions, tests, variables, objects and states.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IDefinitions {
    OvalDefinitions getOvalDefinitions();

    <T extends ObjectType> T getObject(String id, Class<T> type) throws OvalException;

    Iterator<ObjectType> iterateObjects(Class type);

    StateType getState(String id) throws OvalException;

    TestType getTest(String id) throws OvalException;

    ObjectType getObject(String id) throws OvalException;

    VariableType getVariable(String id) throws OvalException;

    DefinitionType getDefinition(String id) throws OvalException;

    /**
     * Sort all DefinitionTypes into two lists according to whether the filter allows/disallows them.
     */
    void filterDefinitions(IDefinitionFilter filter, Collection<DefinitionType> allowed, Collection<DefinitionType> disallowed);

    Iterator<ObjectType> iterateObjects();

    Iterator<VariableType> iterateVariables();
}
