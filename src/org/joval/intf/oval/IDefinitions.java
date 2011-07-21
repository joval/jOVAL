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
 * Interface defining an index of an OvalDefinitions object.  This facilitates fast retrieval of objects.
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
     * Returns an Iterator over all the non-Set ObjectType instances of the class denoted by the class argument.
     */
    public Iterator<ObjectType> iterateLeafObjects(Class type);
}
