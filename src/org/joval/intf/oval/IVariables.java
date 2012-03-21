// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.oval;

import java.io.File;
import java.util.List;

import oval.schemas.variables.core.OvalVariables;

import oval.schemas.definitions.core.DefinitionType;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.core.OvalDefinitions;
import oval.schemas.definitions.core.StateType;
import oval.schemas.definitions.core.TestType;
import oval.schemas.definitions.core.VariableType;

import org.joval.intf.xml.ITransformable;
import org.joval.oval.OvalException;

/**
 * Interface defining a set of variables.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IVariables extends ITransformable {
    /**
     * Get the raw OVAL variables object.
     */
    OvalVariables getOvalVariables();

    /**
     * Get the values of the variable, specified by its ID.
     */
    List<String> getValue(String id);

    void setValue(String id, List<String> values);

    void addValue(String id, String value);

    void setComment(String id, String comment);

    void writeXML(File f);
}
