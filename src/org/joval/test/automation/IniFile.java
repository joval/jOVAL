// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.test.automation;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Properties;

/**
 * A class for interpreting an ini-style config file.  Each header is treated as a section full of properties.
 *
 * @author David A. Solin
 */
class IniFile {
    Hashtable<String, Properties> sections;

    IniFile(File f) throws IOException {
	sections = new Hashtable<String, Properties>();
	BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
	String line = null;
	Properties section = null;
	String name = null;
	int ptr;
	while ((line = br.readLine()) != null) {
	    if (line.startsWith("[") && line.trim().endsWith("]")) {
		if (section != null) {
		    sections.put(name, section);
		}
		section = new Properties();
		name = line.substring(1, line.length() - 1);
	    } else if (line.startsWith("#")) {
		// skip comment
	    } else if ((ptr = line.indexOf("=")) > 0) {
		if (section != null) {
		    String key = line.substring(0,ptr);
		    String val = line.substring(ptr+1);
		    section.setProperty(key, val);
		}
	    }
	}
	if (section != null) {
	    sections.put(name, section);
	}
    }

    Collection<String> listSections() {
	return sections.keySet();
    }

    Properties getSection(String name) {
	return sections.get(name);
    }

    String getProperty(String section, String key) {
	Properties p = getSection(section);
	String val = null;
	if (p != null) {
	    val = p.getProperty(key);
	}
	return val;
    }
}
