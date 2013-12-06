// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.scap.ocil;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.NoSuchElementException;

import scap.ocil.core.VariableDataType;
import scap.ocil.variables.OcilVariables;
import scap.ocil.variables.VariablesType;
import scap.ocil.variables.VariableType;

import org.joval.intf.xml.ITransformable;

/**
 * Interface to an OcilVariables object.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IVariables extends ITransformable<OcilVariables> {
    void addValue(String id, String value);

    void addValue(String id, VariableDataType type, String value);

    void setComment(String id, String comment) throws NoSuchElementException;

    boolean containsVariable(String id);

    VariableType getVariable(String id) throws NoSuchElementException;

    void writeXML(File f) throws IOException;

    void writeXML(OutputStream out) throws IOException;
}
