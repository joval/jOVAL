// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.scap.oval;

import java.io.File;
import java.util.Collection;
import java.util.NoSuchElementException;

import scap.oval.systemcharacteristics.core.SystemInfoType;
import scap.oval.results.DefinitionType;
import scap.oval.results.OvalResults;
import scap.oval.results.ResultEnumeration;
import scap.oval.results.TestType;

import org.joval.intf.xml.ITransformable;
import org.joval.scap.oval.OvalException;

/**
 * Interface to an OVAL results structure, representing (by convention) the results from a single system.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IResults extends ITransformable {
    /**
     * Set a file containing OVAL directives, to govern the behavior of this IResults.
     */
    void setDirectives(File f) throws OvalException;

    /**
     * Get the OVAL results, with the system definitions in full or thin format according to the directives.
     */
    OvalResults getOvalResults() throws OvalException;

    /**
     * Shortcut to getOvalResults().getResults().getSystem().get(0).getDefinitions().getDefinition()
     */
    Collection<DefinitionType> getDefinitionResults() throws OvalException;

    /**
     * Get a definition given its ID.
     */
    DefinitionType getDefinition(String definitionId) throws NoSuchElementException;

    /**
     * Get a test given its ID.
     */
    TestType getTest(String id);

    /**
     * Get the result of a specific definition, given its ID.
     */
    ResultEnumeration getDefinitionResult(String definitionId) throws NoSuchElementException;

    /**
     * Get the IDefinitions used to construct the result.
     */
    IDefinitions getDefinitions();

    /**
     * Get the ISystemCharacteristics used to construct the result.
     */
    ISystemCharacteristics getSystemCharacteristics();

    /**
     * Serialize the contents of this IResults to a file.
     */
    void writeXML(File f);

    /**
     * Serialize the contents of this IResults to the output file, after applying the given XSL transform.
     */
    void writeTransform(File transform, File output);
}
