// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.sce;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.Source;

import org.openscap.sce.results.SceResultsType;
import scap.xccdf.ResultEnumType;

import org.joval.intf.scap.sce.IScriptResult;
import org.joval.intf.xml.ITransformable;
import org.joval.scap.ScapException;
import org.joval.scap.ScapFactory;
import org.joval.util.JOVALMsg;
import org.joval.xml.SchemaRegistry;

/**
 * Implementation of an SCE IScriptResult.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Result implements IScriptResult {
    /**
     * Obtain an SceResultsType from a JAXBSource.
     */
    public static final SceResultsType getSceResults(Source src) throws SceException {
	Object rootObj = parse(src);
	if (rootObj instanceof JAXBElement) {
	    rootObj = ((JAXBElement)rootObj).getValue();
	}
	if (rootObj instanceof SceResultsType) {
	    return (SceResultsType)rootObj;
	} else {
	    throw new SceException(JOVALMsg.getMessage(JOVALMsg.ERROR_SCE_RESULT_BAD_SOURCE, src.getSystemId()));
	}
    }

    private static final Object parse(Source src) throws SceException {
	try {
	    Unmarshaller unmarshaller = SchemaRegistry.SCE.getJAXBContext().createUnmarshaller();
	    return unmarshaller.unmarshal(src);
	} catch (JAXBException e) {
	    throw new SceException(e);
	}
    }

    private SceResultsType result;
    private Throwable error;

    /**
     * Create a new SCE script result object.
     */
    public Result(SceResultsType result) {
	this.result = result;
    }

    /**
     * Create a new SCE script result object representing a failure to run the script.
     */
    public Result(String scriptPath, Throwable error) {
	this.error = error;
	result = ScapFactory.SCE_RESULTS.createSceResultsType();
	result.setResult(ResultEnumType.ERROR);
	result.setScriptPath(scriptPath);
    }

    // Implement IScriptResult

    public SceResultsType getResult() {
	return result;
    }

    public boolean hasError() {
	return error != null;
    }

    public Throwable getError() {
	return error;
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
