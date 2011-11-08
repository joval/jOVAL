// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.engine;

import java.io.File;
import java.util.HashSet;
import java.util.Collection;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import oval.schemas.evaluation.id.EvaluationDefinitionIds;

import org.joval.intf.oval.IDefinitionFilter;
import org.joval.oval.OvalException;
import org.joval.util.JOVALSystem;

/**
 * Representation of a Definition Filter, which is constructed using either a list of definition IDs or an XML file that is
 * compliant with the evaluation-id schema (that contains definition IDs).  The filter lets the engine know which tests it
 * should evaluate, and which it should skip.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class DefinitionFilter implements IDefinitionFilter {
    /**
     * Get a list of Definition ID strings from an Evaluation-IDs file.
     */
    public static final Collection<String> getEvaluationDefinitionIds(File f) throws OvalException {
	try {
	    JAXBContext ctx = JAXBContext.newInstance(JOVALSystem.getOvalProperty(JOVALSystem.OVAL_PROP_EVALUATION_ID));
	    Unmarshaller unmarshaller = ctx.createUnmarshaller();
	    Object rootObj = unmarshaller.unmarshal(f);
	    if (rootObj instanceof EvaluationDefinitionIds) {
		EvaluationDefinitionIds edi = (EvaluationDefinitionIds)rootObj;
		return edi.getDefinition();
	    } else {
		throw new OvalException("Not an Evaluation Definitions file: " + f);
	    }
	} catch (JAXBException e) {
	    throw new OvalException(e);
	}
    }

    private HashSet<String> definitionIDs;

    /**
     * Create a DefinitionFilter based on the contents of an evaluation-id schema-compliant file.
     */
    public DefinitionFilter(File f) throws OvalException {
	this(getEvaluationDefinitionIds(f));
    }

    /**
     * Create a DefinitionFilter based on a list.
     */
    public DefinitionFilter(Collection<String> ids) {
	if (ids instanceof HashSet) {
	    definitionIDs = (HashSet<String>)ids;
	} else {
	    definitionIDs = new HashSet<String>();
	    for (String id : ids) {
		definitionIDs.add(id);
	    }
	}
    }

    /**
     * Create an unfiltered DefinitionFilter (accept will always return true).
     */
    public DefinitionFilter() {
	definitionIDs = null;
    }

    // Implement IDefinitionFilter

    public boolean accept(String id) {
	if (definitionIDs == null) {
	    return true;
	} else {
	    return definitionIDs.contains(id);
	}
    }
}
