// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.engine;

import java.io.File;
import java.util.Iterator;
import java.util.logging.Level;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import oval.schemas.common.ClassEnumeration;
import oval.schemas.directives.core.ObjectFactory;
import oval.schemas.directives.core.OvalDirectives;
import oval.schemas.results.core.ClassDirectivesType;
import oval.schemas.results.core.ContentEnumeration;
import oval.schemas.results.core.DefaultDirectivesType;
import oval.schemas.results.core.DefinitionType;
import oval.schemas.results.core.DirectivesType;
import oval.schemas.results.core.DirectiveType;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.oval.OvalException;
import org.joval.util.JOVALSystem;

/**
 * Representation of OvalDirectives, which specify to the engine how much detail should be put in the results XML.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class Directives {
    /**
     * Get a list of Definition ID strings from an Evaluation-IDs file.
     */
    static final OvalDirectives getOvalDirectives(File f) throws OvalException {
	try {
	    JAXBContext ctx = JAXBContext.newInstance(JOVALSystem.getOvalProperty(JOVALSystem.OVAL_PROP_DIRECTIVES));
	    Unmarshaller unmarshaller = ctx.createUnmarshaller();
	    Object rootObj = unmarshaller.unmarshal(f);
	    if (rootObj instanceof OvalDirectives) {
		return (OvalDirectives)rootObj;
	    } else {
		throw new OvalException(JOVALSystem.getMessage("ERROR_DIRECTIVES_BAD_FILE", f.toString()));
	    }
	} catch (JAXBException e) {
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_DIRECTIVES_PARSE", f.toString()), e);
	    throw new OvalException(e);
	}
    }

    private OvalDirectives directives;

    /**
     * Create a Directives based on the contents of a directives file.
     */
    Directives(File f) throws OvalException {
	this(getOvalDirectives(f));
    }

    /**
     * Create a Directives from unmarshalled XML.
     */
    Directives(OvalDirectives directives) {
	this.directives = directives;
    }

    /**
     * Create a Directives with default behavior (full reporting for everything).
     */
    Directives() {
	directives = new ObjectFactory().createOvalDirectives();
	DefaultDirectivesType ddt = JOVALSystem.resultsFactory.createDefaultDirectivesType();
	ddt.setIncludeSourceDefinitions(true);
	DirectiveType dt = JOVALSystem.resultsFactory.createDirectiveType();
	dt.setReported(true);
	dt.setContent(ContentEnumeration.FULL);
	ddt.setDefinitionError(dt);
	ddt.setDefinitionFalse(dt);
	ddt.setDefinitionNotApplicable(dt);
	ddt.setDefinitionNotEvaluated(dt);
	ddt.setDefinitionTrue(dt);
	ddt.setDefinitionUnknown(dt);
	directives.setDirectives(ddt);
    }

    OvalDirectives getOvalDirectives() {
	return directives;
    }

    /**
     * Specifies whether or not the OvalDefinitions source should be included in the results.
     */
    boolean includeSource() {
	return directives.getDirectives().isIncludeSourceDefinitions();
    }

    /**
     * Returns the DirectiveType for the given results.DefinitionType.
     */
    DirectiveType getDirective(DefinitionType definition) {
	// Start with defaults
	DirectivesType dt = directives.getDirectives();

	// See if there's a directive matching the DefinitionType's class
	ClassEnumeration clazz = definition.getClazz();
	for (ClassDirectivesType cdt : directives.getClassDirectives()) {
	    if (cdt.getClazz() == clazz) {
		dt = cdt;
		break;
	    }
	}

	switch(definition.getResult()) {
	  case ERROR:
	    return dt.getDefinitionError();
	  case FALSE:
	    return dt.getDefinitionFalse();
	  case NOT_APPLICABLE:
	    return dt.getDefinitionNotApplicable();
	  case NOT_EVALUATED:
	    return dt.getDefinitionNotEvaluated();
	  case TRUE:
	    return dt.getDefinitionTrue();
	  case UNKNOWN:
	  default:
	    return dt.getDefinitionUnknown();
	}
    }
}
