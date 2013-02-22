// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.xccdf;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.Source;

import scap.xccdf.TestResultType;
import scap.xccdf.ObjectFactory;

import org.joval.intf.xml.ITransformable;
import org.joval.xml.SchemaRegistry;

/**
 * A representation of a single XCCDF 1.2 benchmark result.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class TestResult implements ITransformable {
    private TestResultType result;
    private ObjectFactory factory;

    public TestResult(TestResultType result) throws XccdfException {
	this.result = result;
	factory = new ObjectFactory();
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

    public Object getRootObject() {
	return factory.createTestResult(result);
    }

    public JAXBContext getJAXBContext() throws JAXBException {
	return SchemaRegistry.XCCDF.getJAXBContext();
    }
}
