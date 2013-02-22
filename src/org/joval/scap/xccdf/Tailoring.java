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
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import scap.xccdf.ProfileType;
import scap.xccdf.TailoringType;

import org.joval.intf.scap.xccdf.ITailoring;
import org.joval.util.JOVALMsg;
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
    private String benchmarkId;
    private Map<String, ProfileType> profiles;

    public Tailoring(TailoringType tt) {
	this.tt = tt;
	benchmarkId = tt.getBenchmark().getId();
	profiles = new HashMap<String, ProfileType>();
	for (ProfileType profile : tt.getProfile()) {
	    profiles.put(profile.getProfileId(), profile);
	}
    }

    // Implement ITailoring

    public String getBenchmarkId() {
	return benchmarkId;
    }

    public TailoringType getTailoring() {
	return tt;
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
