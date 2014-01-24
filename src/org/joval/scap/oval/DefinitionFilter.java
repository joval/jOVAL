// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Collection;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.JAXBSource;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.cal10n.LocLogger;

import scap.oval.definitions.core.DefinitionType;
import scap.oval.definitions.core.OvalDefinitions;
import scap.oval.evaluation.EvaluationDefinitionIds;

import org.joval.intf.scap.oval.IDefinitionFilter;
import org.joval.intf.scap.oval.IDefinitions;
import org.joval.util.JOVALMsg;
import org.joval.xml.DOMTools;
import org.joval.xml.SchemaRegistry;

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
	return getEvaluationDefinitionIds(new StreamSource(f));
    }

    public static final Collection<String> getEvaluationDefinitionIds(Source src) throws OvalException {
	try {
	    Unmarshaller unmarshaller = SchemaRegistry.OVAL_EVALUATION_ID.getJAXBContext().createUnmarshaller();
	    Object rootObj = unmarshaller.unmarshal(src);
	    if (rootObj instanceof EvaluationDefinitionIds) {
		EvaluationDefinitionIds edi = (EvaluationDefinitionIds)rootObj;
		return edi.getDefinition();
	    } else {
		throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_DEFINITION_FILTER_BAD_SOURCE, src.getSystemId()));
	    }
	} catch (JAXBException e) {
	    throw new OvalException(e);
	}
    }

    private HashSet<String> definitionIDs;
    private LocLogger logger = JOVALMsg.getLogger();

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
	    definitionIDs.addAll(ids);
	}
    }

    /**
     * Create an empty (unfiltered) DefinitionFilter.  An empty filter will accept any ID.
     */
    public DefinitionFilter() {
	definitionIDs = null;
    }

    // Implement ITransformable

    public Source getSource() throws JAXBException {
	return new JAXBSource(SchemaRegistry.OVAL_EVALUATION_ID.getJAXBContext(), getRootObject());
    }

    public EvaluationDefinitionIds getRootObject() {
	EvaluationDefinitionIds ids = Factories.evaluation.createEvaluationDefinitionIds();
	if (definitionIDs == null) {
	    ids.unsetDefinition();
	} else {
	    ids.getDefinition().addAll(definitionIDs);
	}
	return ids;
    }

    public EvaluationDefinitionIds copyRootObject() throws Exception {
	Unmarshaller unmarshaller = getJAXBContext().createUnmarshaller();
	Object rootObj = unmarshaller.unmarshal(new DOMSource(DOMTools.toDocument(this).getDocumentElement()));
	if (rootObj instanceof EvaluationDefinitionIds) {
	    return (EvaluationDefinitionIds)rootObj;
	} else {
	    throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_DEFINITION_FILTER_BAD_SOURCE, toString()));
	}
    }

    public JAXBContext getJAXBContext() throws JAXBException {
	return SchemaRegistry.OVAL_EVALUATION_ID.getJAXBContext();
    }

    // Implement IDefinitionFilter

    public Collection<DefinitionType> filter(IDefinitions definitions, boolean include, LocLogger log) {
	if (log == null) {
	    log = logger;
	}
	if (definitionIDs == null) {
	    OvalDefinitions oval = null;
	    if (include && (oval = definitions.getRootObject()).isSetDefinitions()) {
		return oval.getDefinitions().getDefinition();
	    } else {
		return new ArrayList<DefinitionType>();
	    }
	} else {
	    Collection<String> allIds = new ArrayList<String>();
	    for (DefinitionType def : definitions.getRootObject().getDefinitions().getDefinition()) {
		allIds.add(def.getId());
	    }
	    Collection<DefinitionType> result = new ArrayList<DefinitionType>();
	    HashSet<String> temp = new HashSet<String>();
	    temp.addAll(definitionIDs);
	    for (String id : allIds) {
		if (temp.contains(id)) {
		    temp.remove(id);
		    if (include) {
			result.add(definitions.getDefinition(id));
		    }
		} else if (!include) {
		    result.add(definitions.getDefinition(id));
		}
	    }
	    if (include) {
		//
		// Generate warnings about definitions in the filter that are not defined in the document
		//
		for (String notFound : temp) {
		    log.warn(JOVALMsg.ERROR_REF_DEFINITION, notFound);
		}
	    }
	    return result;
	}
    }

    public void addDefinition(String id) {
	if (definitionIDs == null) {
	    definitionIDs = new HashSet<String>();
	}
	definitionIDs.add(id);
    }

    public void writeXML(File f) {
	OutputStream out = null;
	try {
	    Marshaller marshaller = SchemaRegistry.OVAL_EVALUATION_ID.createMarshaller();
	    out = new FileOutputStream(f);
	    marshaller.marshal(getRootObject(), out);
	} catch (JAXBException e) {
	    logger.warn(JOVALMsg.ERROR_FILE_GENERATE, f.toString());
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (FactoryConfigurationError e) {
	    logger.warn(JOVALMsg.ERROR_FILE_GENERATE, f.toString());
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (FileNotFoundException e) {
	    logger.warn(JOVALMsg.ERROR_FILE_GENERATE, f.toString());
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} finally {
	    if (out != null) {
		try {
		    out.close();
		} catch (IOException e) {
		    logger.warn(JOVALMsg.ERROR_FILE_CLOSE, f.toString());
		}
	    }
	}
    }
}
