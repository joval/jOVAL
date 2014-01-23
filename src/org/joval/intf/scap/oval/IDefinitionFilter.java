// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.scap.oval;

import java.io.File;
import java.util.Collection;

import org.slf4j.cal10n.LocLogger;

import scap.oval.definitions.core.DefinitionType;
import scap.oval.evaluation.EvaluationDefinitionIds;

import org.joval.intf.xml.ITransformable;

/**
 * Interface defining an OVAL Definition Filter.  The filter lets the engine know which tests it should evaluate, and which
 * it should skip.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IDefinitionFilter extends ITransformable<EvaluationDefinitionIds> {
    /**
     * Return a collection of definitions from the IDefinitions that are allowed by this filter.
     *
     * @param definitions The IDefinitions to which you want to apply the filter.
     * @param include Use true if you want a collection of definitions that should be run, or false for a collection
     *                of definitions from the IDefinitions that are not specified in the filter.
     */
    public Collection<DefinitionType> filter(IDefinitions definitions, boolean include, LocLogger logger);

    /**
     * Add a definition to the filter.
     */
    public void addDefinition(String id);

    /**
     * Serialize to a file.
     */
    public void writeXML(File f);
}
