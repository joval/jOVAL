// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.xccdf;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import scap.xccdf.TestResultType;

import org.joval.intf.xml.ITransformable;
import org.joval.scap.ScapFactory;
import org.joval.util.JOVALMsg;
import org.joval.xml.DOMTools;
import org.joval.xml.SchemaRegistry;

/**
 * A representation of a single XCCDF 1.2 benchmark result.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class TestResult implements ITransformable<JAXBElement<TestResultType>> {
    private TestResultType result;

    public TestResult(TestResultType result) throws XccdfException {
	this.result = result;
    }

    /**
     * Get the underlying JAXB TestResultType.
     */
    public TestResultType getTestResult() {
	return result;
    }

    // Implement ITransformable

    public Source getSource() throws JAXBException {
	return new JAXBSource(SchemaRegistry.XCCDF.getJAXBContext(), getRootObject());
    }

    public JAXBElement<TestResultType> getRootObject() {
	return ScapFactory.XCCDF.createTestResult(result);
    }

    public JAXBElement<TestResultType> copyRootObject() throws Exception {
	Unmarshaller unmarshaller = getJAXBContext().createUnmarshaller();
	Object rootObj = unmarshaller.unmarshal(new DOMSource(DOMTools.toDocument(this).getDocumentElement()));
	if (rootObj instanceof JAXBElement && ((JAXBElement)rootObj).getValue() instanceof TestResultType) {
	    @SuppressWarnings("unchecked")
	    JAXBElement<TestResultType> result = (JAXBElement<TestResultType>)rootObj;
	    return result;
	} else {
	    throw new XccdfException(JOVALMsg.getMessage(JOVALMsg.ERROR_XCCDF_BAD_SOURCE, toString()));
	}
    }

    public JAXBContext getJAXBContext() throws JAXBException {
	return SchemaRegistry.XCCDF.getJAXBContext();
    }
}
