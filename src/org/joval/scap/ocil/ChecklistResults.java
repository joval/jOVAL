// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.ocil;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.Source;

import ocil.schemas.core.ResultsType;

import org.joval.intf.xml.ITransformable;
import org.joval.xml.SchemaRegistry;

/**
 * Representation of a OCIL checklist result.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class ChecklistResults implements ITransformable {
    private JAXBContext ctx;
    private ResultsType results;

    /**
     * Create results based on a JAXB object.
     */
    public ChecklistResults(ResultsType results) throws OcilException {
	this.results = results;
	try {
	    ctx = JAXBContext.newInstance(SchemaRegistry.lookup(SchemaRegistry.OCIL));
	} catch (JAXBException e) {
	    throw new OcilException(e);
	}
    }

    // Implement ITransformable

    public Source getSource() throws JAXBException {
	return new JAXBSource(ctx, results);
    }
}
