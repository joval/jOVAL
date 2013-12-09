// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.xccdf;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import scap.xccdf.ProfileType;
import scap.xccdf.TailoringType;

import org.joval.intf.scap.xccdf.ITailoring;
import org.joval.scap.ScapFactory;
import org.joval.util.JOVALMsg;
import org.joval.xml.DOMTools;
import org.joval.xml.SchemaRegistry;

/**
 * A representation of an XCCDF 1.2 tailoring document.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Tailoring implements ITailoring {
    public static final TailoringType getTailoringType(File f) throws XccdfException {
	return getTailoringType(new StreamSource(f));
    }

    public static final TailoringType getTailoringType(InputStream in) throws XccdfException {
	return getTailoringType(new StreamSource(in));
    }

    public static final TailoringType getTailoringType(Source source) throws XccdfException {
	try {
	    Unmarshaller unmarshaller = SchemaRegistry.XCCDF.getJAXBContext().createUnmarshaller();
	    Object rootObj = unmarshaller.unmarshal(source);
	    if (rootObj instanceof TailoringType) {
		return (TailoringType)rootObj;
	    } else if (rootObj instanceof JAXBElement) {
		JAXBElement root = (JAXBElement)rootObj;
		if (root.getValue() instanceof TailoringType) {
		    return (TailoringType)root.getValue();
		} else {
		    throw new XccdfException(JOVALMsg.getMessage(JOVALMsg.ERROR_TAILORING_BAD_SOURCE, source.getSystemId()));
		}
	    } else {
		throw new XccdfException(JOVALMsg.getMessage(JOVALMsg.ERROR_TAILORING_BAD_SOURCE, source.getSystemId()));
	    }
	} catch (JAXBException e) {
	    throw new XccdfException(e);
	}
    }

    private TailoringType tt;
    private Map<String, ProfileType> profiles;
    private String href;

    public Tailoring(TailoringType tt) {
	this.tt = tt;
	profiles = new HashMap<String, ProfileType>();
	for (ProfileType profile : tt.getProfile()) {
	    profiles.put(profile.getProfileId(), profile);
	}
    }

    public Tailoring(File f) throws XccdfException {
	this(getTailoringType(f));
	href = f.toURI().toString();
    }

    // Implement ITransformable

    public Source getSource() throws JAXBException {
        return new JAXBSource(SchemaRegistry.XCCDF.getJAXBContext(), getRootObject());
    }

    public JAXBElement<TailoringType> getRootObject() {
        return ScapFactory.XCCDF.createTailoring(tt);
    }

    public JAXBElement<TailoringType> copyRootObject() throws Exception {
        Unmarshaller unmarshaller = getJAXBContext().createUnmarshaller();
        Object rootObj = unmarshaller.unmarshal(new DOMSource(DOMTools.toDocument(this).getDocumentElement()));
        if (rootObj instanceof JAXBElement && ((JAXBElement)rootObj).getValue() instanceof TailoringType) {
	    @SuppressWarnings("unchecked")
	    JAXBElement<TailoringType> result = (JAXBElement<TailoringType>)rootObj;
	    return result;
        } else {
            throw new XccdfException(JOVALMsg.getMessage(JOVALMsg.ERROR_XCCDF_BAD_SOURCE, toString()));
        }
    }

    public JAXBContext getJAXBContext() throws JAXBException {
        return SchemaRegistry.XCCDF.getJAXBContext();
    }

    // Implement ITailoring

    public void setHref(String href) {
	this.href = href;
    }

    public String getHref() {
	return href;
    }

    public String getId() {
	return tt.getTailoringId();
    }

    public String getBenchmarkId() {
	return tt.getBenchmark().getId();
    }

    public Collection<String> getProfileIds() {
	return profiles.keySet();
    }

    public ProfileType getProfile(String id) throws NoSuchElementException {
	if (profiles.containsKey(id)) {
	    return profiles.get(id);
	} else {
	    throw new NoSuchElementException(id);
	}
    }
}
