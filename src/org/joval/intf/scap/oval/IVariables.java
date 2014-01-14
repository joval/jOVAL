// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.scap.oval;

import java.io.File;
import java.util.List;
import java.util.NoSuchElementException;

import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.VariableType;
import scap.oval.variables.OvalVariables;

import org.joval.intf.xml.ITransformable;

/**
 * Interface defining a set of variables.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IVariables extends ITransformable<OvalVariables> {
    /**
     * Get the values of the variable, specified by its ID.
     */
    List<IType> getValue(String id) throws NoSuchElementException;

    /**
     * Set an untyped multi-valued variable, over-writing any previously set/added values.
     */
    void setValue(String id, List<String> values);

    /**
     * Set a typed multi-valued variable, over-writing any previously set/added values.
     */
    void setValue(String id, SimpleDatatypeEnumeration type, List<String> values);

    /**
     * Add an untyped variable value.
     */
    void addValue(String id, String value);

    /**
     * Add a typed variable value.
     */
    void addValue(String id, SimpleDatatypeEnumeration type, String value);

    /**
     * Set a comment for a variable.
     */
    void setComment(String id, String comment);

    /**
     * Serialize the variables to an XML file.
     */
    void writeXML(File f);
}
