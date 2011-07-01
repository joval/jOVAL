// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.xml;

import java.util.Iterator;
import javax.xml.namespace.NamespaceContext;

public class EmptyNamespaceContext implements NamespaceContext {
    public static final String EMPTY = "";

    public EmptyNamespaceContext() {}

    public String getNamespaceURI(String prefix) {
	return EMPTY;
    }

    public String getPrefix(String namespaceURI) {
	return EMPTY;
    }

    public Iterator getPrefixes(String namespaceURI) {
	return null;
    }
}
