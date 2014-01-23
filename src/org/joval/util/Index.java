// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.zip.Adler32;

import jsaf.util.Checksum;

/**
 * An index for binary data. Stores up to 1MB of data to avoid computing MD5 hashes.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Index<T> {
    private static final int MAX_MEM = 1048576;

    /**
     * A place-holder to indicate than data with an Adler checksum has been previously encountered.
     */
    private final Entry VISITED = new Entry();

    private Map<String, Entry> adlerIndex;
    private Map<String, T> md5Index;
    private int mem_used = 0;

    /**
     * Create an empty Index.
     */
    public Index() {
	adlerIndex = new HashMap<String, Entry>();
	md5Index = new HashMap<String, T>();
    }

    /**
     * Returns suggestedId if the data has never been indexed before.
     */
    public T getId(byte[] data, T suggestedId) {
	Adler32 adler = new Adler32();
	adler.update(data);
	String cs = Long.toString(adler.getValue());

	if (mem_used > MAX_MEM || adlerIndex.containsKey(cs)) {
	    Entry entry = adlerIndex.get(cs);
	    if (entry != null && !entry.equals(VISITED)) {
		//
		// Time to compute the MD5 of the first entry with this Adler CS.
		//
		md5Index.put(Checksum.getChecksum(entry.data, Checksum.Algorithm.MD5), entry.id);
		int len = entry.data.length;
		entry.data = null;
		adlerIndex.put(cs, VISITED);
		mem_used = mem_used - len;
	    }

	    String md5 = Checksum.getChecksum(data, Checksum.Algorithm.MD5);
	    if (md5Index.containsKey(md5)) {
		return md5Index.get(md5);
	    } else {
		md5Index.put(md5, suggestedId);
		return suggestedId;
	    }
	} else {
	    adlerIndex.put(cs, new Entry(data, suggestedId));
	    mem_used = mem_used + data.length;
	    return suggestedId;
	}
    }

    // Private

    class Entry {
	byte[] data;
	T id;

	Entry() {
	}

	Entry(byte[] data, T id) {
	    this.data = data;
	    this.id = id;
	}
    }
}
