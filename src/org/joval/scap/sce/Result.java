// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.sce;

import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.Source;

import org.openscap.sce.results.SceResultsType;

import org.joval.intf.scap.sce.IScriptResult;
import org.joval.intf.xml.ITransformable;
import org.joval.scap.ScapException;
import org.joval.xml.SchemaRegistry;

/**
 * Implementation of an SCE IScriptResult.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Result implements IScriptResult {
    private SceResultsType result;

    /**
     * Create a new SCE script result object.
     */
    Result(SceResultsType result) {
	this.result = result;
    }

    // Implement IScriptResult

    public SceResultsType getResult() {
	return result;
    }

    // Implement ITransformable

    public Source getSource() throws JAXBException, ScapException {
	return new JAXBSource(getJAXBContext(), getRootObject());
    }

    public Object getRootObject() {
	return Script.FACTORY.createSceResults(result);
    }

    public JAXBContext getJAXBContext() throws JAXBException {
	return SchemaRegistry.SCE.getJAXBContext();
    }
}
