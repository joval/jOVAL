// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Properties;

import org.joval.intf.util.IProperty;

/**
 * A class for interpreting an ini-style config file.  Each header is treated as a section full of properties.  Comment
 * lines begin with a ';'.  Delimiters can be either ':' or '='. Key and value names are trimmed of leading and trailing
 * white-space.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class IniFile {
    private Hashtable<String, IProperty> sections;

    /**
     * Create an empty IniFile.
     */
    public IniFile() {
	sections = new Hashtable<String, IProperty>();
    }

    /**
     * Create a new IniFile from a File.
     */
    public IniFile(File f) throws IOException {
	this();
	load(f);
    }

    /**
     * Create a new IniFile from an InputStream.
     */
    public IniFile(InputStream in) throws IOException {
	this();
	load(in);
    }

    /**
     * A convenience method for loading files.
     */
    public void load(File f) throws IOException {
	load(new FileInputStream(f));
    }

    /**
     * Add configuratino data from a stream.  If the IniFile already contains configuration information,
     * the information from the stream is added.
     */
    public void load(InputStream in) throws IOException {
	BufferedReader br = null;
	try {
	    br = new BufferedReader(new InputStreamReader(in));
	    String line = null;
	    IProperty section = null;
	    String name = null;
	    int ptr;
	    while ((line = br.readLine()) != null) {
		if (line.startsWith("[") && line.trim().endsWith("]")) {
		    name = line.substring(1, line.length() - 1);
		    section = sections.get(name);
		    if (section == null) {
			section = new PropertyUtil();
			sections.put(name, section);
		    }
		} else if (line.startsWith(SEMICOLON)) {
		    // skip comment
		} else if ((ptr = delimIndex(line)) > 0) {
		    if (section != null) {
			String key = line.substring(0,ptr).trim();
			String val = line.substring(ptr+1).trim();
			section.setProperty(key, val);
		    }
		}
	    }
	} finally {
	    if (br != null) {
		try {
		    br.close();
		} catch (IOException e) {
		}
	    }
	}
    }

    public Collection<String> listSections() {
	return sections.keySet();
    }

    public IProperty getSection(String name) throws NoSuchElementException {
	IProperty section = sections.get(name);
	if (section == null) {
	    throw new NoSuchElementException(name);
	} else {
	    return section;
	}
    }

    public String getProperty(String section, String key) throws NoSuchElementException {
	return getSection(section).getProperty(key);
    }

    // Private

    private static final String SEMICOLON = ";";

    private static final String COLON = ":";
    private static final String EQUAL = "=";

    private int delimIndex(String line) {
	int i1 = line.indexOf(COLON);
	int i2 = line.indexOf(EQUAL);

	if (i1 == -1) {
	    return i2;
	} else if (i2 == -1) {
	    return i1;
	} else {
	    return Math.min(i1, i2);
	}
    }
}
