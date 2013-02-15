// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.cpe;

import java.io.File;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import scap.cpe.dictionary.CheckType;
import scap.cpe.dictionary.ItemType;
import scap.cpe.dictionary.ListType;

import org.joval.intf.scap.cpe.IDictionary;
import org.joval.util.JOVALMsg;
import org.joval.xml.SchemaRegistry;

/**
 * Representation of a CPE dictionary document.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Dictionary implements IDictionary {
    public static final ListType getCpeList(File f) throws CpeException {
	return getCpeList(new StreamSource(f));
    }

    public static final ListType getCpeList(InputStream in) throws CpeException {
	return getCpeList(new StreamSource(in));
    }

    public static final ListType getCpeList(Source source) throws CpeException {
	try {
	    String packages = SchemaRegistry.lookup(SchemaRegistry.CPE);
	    JAXBContext ctx = JAXBContext.newInstance(packages);
	    Unmarshaller unmarshaller = ctx.createUnmarshaller();
	    Object rootObj = unmarshaller.unmarshal(source);
	    if (rootObj instanceof ListType) {
		return (ListType)rootObj;
	    } else if (rootObj instanceof JAXBElement) {
		JAXBElement root = (JAXBElement)rootObj;
		if (root.getValue() instanceof ListType) {
		    return (ListType)root.getValue();
		} else {
		    throw new CpeException(JOVALMsg.getMessage(JOVALMsg.ERROR_CPE_BAD_SOURCE, source.getSystemId()));
		}
	    } else {
		throw new CpeException(JOVALMsg.getMessage(JOVALMsg.ERROR_CPE_BAD_SOURCE, source.getSystemId()));
	    }
	} catch (JAXBException e) {
	    throw new CpeException(e);
	}
    }

    private ListType list;
    private Hashtable<String, ItemType> ovalMapping;

    /**
     * Create a Directives based on the contents of a directives file.
     */
    public Dictionary(File f) throws CpeException {
	this(getCpeList(f));
    }

    public Dictionary(InputStream in) throws CpeException {
	this(getCpeList(in));
    }

    /**
     * Create a Directives from unmarshalled XML.
     */
    public Dictionary(ListType list) {
	this();
	this.list = list;
	for (ItemType item : list.getCpeItem()) {
	    ovalMapping.put(item.getName(), item);
	}
    }

    /**
     * Create an empty Dictionary.
     */
    public Dictionary() {
	ovalMapping = new Hashtable<String, ItemType>();
    }

    // Implement IDictionary

    public ListType getCpeList() {
	return list;
    }

    public ItemType getItem(String cpeName) throws NoSuchElementException {
	if (ovalMapping.containsKey(cpeName)) {
	    return ovalMapping.get(cpeName);
	} else {
	    throw new NoSuchElementException(cpeName);
	}
    }
}
