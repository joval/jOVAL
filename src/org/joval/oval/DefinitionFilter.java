// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Collection;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.cal10n.LocLogger;

import oval.schemas.evaluation.id.EvaluationDefinitionIds;

import org.joval.intf.oval.IDefinitionFilter;
import org.joval.intf.util.ILoggable;
import org.joval.oval.OvalException;
import org.joval.util.JOVALSystem;
import org.joval.util.JOVALMsg;

/**
 * Representation of a Definition Filter, which is constructed using either a list of definition IDs or an XML file that is
 * compliant with the evaluation-id schema (that contains definition IDs).  The filter lets the engine know which tests it
 * should evaluate, and which it should skip.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class DefinitionFilter implements IDefinitionFilter, ILoggable {
    /**
     * Get a list of Definition ID strings from an Evaluation-IDs file.
     */
    public static final Collection<String> getEvaluationDefinitionIds(File f) throws OvalException {
	return getEvaluationDefinitionIds(new StreamSource(f));
    }

    public static final Collection<String> getEvaluationDefinitionIds(Source src) throws OvalException {
	try {
	    String packages = JOVALSystem.getSchemaProperty(JOVALSystem.OVAL_PROP_EVALUATION_ID);
	    JAXBContext ctx = JAXBContext.newInstance(packages);
	    Unmarshaller unmarshaller = ctx.createUnmarshaller();
	    Object rootObj = unmarshaller.unmarshal(src);
	    if (rootObj instanceof EvaluationDefinitionIds) {
		EvaluationDefinitionIds edi = (EvaluationDefinitionIds)rootObj;
		return edi.getDefinition();
	    } else {
		throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_DEFINITION_FILTER_BAD_SOURCE, src.getSystemId()));
	    }
	} catch (JAXBException e) {
	    throw new OvalException(e);
	}
    }

    private HashSet<String> definitionIDs;
    private LocLogger logger = JOVALSystem.getLogger();

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
     * Create an empty (unfiltered) DefinitionFilter.  An empty filter will accept any ID.
     */
    public DefinitionFilter() {
	definitionIDs = null;
    }

    public EvaluationDefinitionIds getEvaluationDefinitionIds() {
	EvaluationDefinitionIds ids = JOVALSystem.factories.evaluation.createEvaluationDefinitionIds();
	for (String id : definitionIDs) {
	    ids.getDefinition().add(id);
	}
	return ids;
    }

    /**
     * Add a definition to the filter.
     */
    public void addDefinition(String id) {
	if (definitionIDs == null) {
	    definitionIDs = new HashSet<String>();
	}
	definitionIDs.add(id);
    }

    public void writeXML(File f) {
        OutputStream out = null;
        try {
	    String packages = JOVALSystem.getSchemaProperty(JOVALSystem.OVAL_PROP_EVALUATION_ID);
	    JAXBContext ctx = JAXBContext.newInstance(packages);
            Marshaller marshaller = ctx.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            out = new FileOutputStream(f);
            marshaller.marshal(getEvaluationDefinitionIds(), out);
        } catch (JAXBException e) {
            logger.warn(JOVALMsg.ERROR_FILE_GENERATE, f.toString());
            logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
        } catch (FactoryConfigurationError e) {
            logger.warn(JOVALMsg.ERROR_FILE_GENERATE, f.toString());
            logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
        } catch (FileNotFoundException e) {
            logger.warn(JOVALMsg.ERROR_FILE_GENERATE, f.toString());
            logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
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

    // Implement IDefinitionFilter

    public boolean accept(String id) {
	if (definitionIDs == null) {
	    return true;
	} else {
	    return definitionIDs.contains(id);
	}
    }

    // Implement ILoggable

    public void setLogger(LocLogger logger) {
	this.logger = logger;
    }

    public LocLogger getLogger() {
	return logger;
    }
}
