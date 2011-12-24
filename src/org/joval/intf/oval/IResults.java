// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.oval;

import java.io.File;
import java.util.Collection;

import oval.schemas.results.core.DefinitionType;
import oval.schemas.results.core.OvalResults;

import org.joval.intf.util.ILoggable;
import org.joval.oval.OvalException;

/**
 * Interface to an OVAL results structure.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IResults extends ILoggable {
    /**
     * Set a file containing OVAL directives, to govern the behavior of this IResults.
     */
    void setDirectives(File f) throws OvalException;

    /**
     * Get the OVAL results, with the system definitions in full or thin format according to the directives.
     */
    OvalResults getOvalResults();

    /**
     * Serialize the contents of this IResults to a file.
     */
    void writeXML(File f);

    /**
     * Serialize the contents of this IResults to the output file, after applying the given XSL transform.
     */
    void writeTransform(File transform, File output);
}
