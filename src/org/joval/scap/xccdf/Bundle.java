// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.xccdf;

import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import scap.xccdf.ProfileType;

import jsaf.Message;
import jsaf.protocol.zip.ZipURLStreamHandler;

import org.joval.intf.scap.IScapContext;
import org.joval.intf.scap.cpe.IDictionary;
import org.joval.intf.scap.datastream.IDatastream;
import org.joval.intf.scap.ocil.IChecklist;
import org.joval.intf.scap.oval.IDefinitions;
import org.joval.intf.scap.sce.IScript;
import org.joval.intf.scap.xccdf.IBundle;
import org.joval.intf.scap.xccdf.IBenchmark;
import org.joval.intf.scap.xccdf.ITailoring;
import org.joval.scap.ScapContext;
import org.joval.scap.ScapException;
import org.joval.scap.cpe.Dictionary;
import org.joval.scap.ocil.Checklist;
import org.joval.scap.ocil.OcilException;
import org.joval.scap.oval.Definitions;
import org.joval.scap.oval.OvalException;
import org.joval.scap.sce.SceException;
import org.joval.scap.sce.Script;
import org.joval.scap.xccdf.XccdfException;

/**
 * A bundle of SCAP documents (similar to an IDatastream, but without any specification).
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Bundle implements IBundle {
    private File base;
    private ZipFile zip;
    private Map<String, IBenchmark> benchmarks;
    private IDictionary dictionary;

    public Bundle(File file) throws ScapException {
	benchmarks = new HashMap<String, IBenchmark>();
	if (file.isDirectory()) {
	    base = file;
	    for (File f : base.listFiles()) {
		if (f.isFile() && f.getName().endsWith(".xml")) {
		    if (f.getName().toLowerCase().indexOf("xccdf") != -1) {
			IBenchmark benchmark = new Benchmark(Benchmark.getBenchmarkType(f));
			benchmarks.put(benchmark.getId(), benchmark);
		    } else if (f.getName().toLowerCase().indexOf("cpe-dictionary") != -1) {
			dictionary = new Dictionary(Dictionary.getCpeList(f));
		    }
		}
	    }
	} else if (file.getName().toLowerCase().endsWith(".zip")) {
	    try {
		zip = new ZipFile(file);
		Enumeration<? extends ZipEntry> entries = zip.entries();
		while(entries.hasMoreElements()) {
		    ZipEntry entry = entries.nextElement();
		    if (!entry.isDirectory() && entry.getName().toLowerCase().endsWith(".xml")) {
			InputStream in = null;
			try {
			    if (entry.getName().toLowerCase().indexOf("xccdf") != -1) {
				in = zip.getInputStream(entry);
				IBenchmark benchmark = new Benchmark(Benchmark.getBenchmarkType(in));
				benchmarks.put(benchmark.getId(), benchmark);
			    } else if (entry.getName().toLowerCase().indexOf("cpe-dictionary") != -1) {
				in = zip.getInputStream(entry);
				dictionary = new Dictionary(Dictionary.getCpeList(in));
			    }
			} finally {
			    if (in != null) {
				try {
				    in.close();
				} catch (IOException e) {
				}
			    }
			}
		    }
		}
	    } catch (IOException e) {
		throw new ScapException(e);
	    }
	} else {
	    String msg = Message.getMessage(Message.ERROR_IO, file.toString(), Message.getMessage(Message.ERROR_IO_NOT_DIR));
	    throw new ScapException(msg);
	}
    }

    // Implement IBundle

    public IDictionary getDictionary() {
	return dictionary;
    }

    public Collection<String> getBenchmarkIds() {
	return benchmarks.keySet();
    }

    public IBenchmark getBenchmark(String benchmarkId) throws NoSuchElementException {
	if (benchmarks.containsKey(benchmarkId)) {
	    return benchmarks.get(benchmarkId);
	} else {
	    throw new NoSuchElementException(benchmarkId);
	}
    }

    public Collection<String> getProfileIds(String benchmarkId) throws NoSuchElementException {
	if (benchmarks.containsKey(benchmarkId)) {
	    Collection<String> result = new ArrayList<String>();
	    for (ProfileType profile : benchmarks.get(benchmarkId).getBenchmark().getProfile()) {
		result.add(profile.getProfileId());
	    }
	    return result;
	} else {
	    throw new NoSuchElementException(benchmarkId);
	}
    }

    public IScapContext getContext(String benchmarkId, String profileId) throws NoSuchElementException, ScapException {
	if (benchmarks.containsKey(benchmarkId)) {
	    IBenchmark benchmark = benchmarks.get(benchmarkId);
	    return new Context(benchmark, profileId == null ? null : benchmark.getProfile(profileId));
	} else {
	    throw new NoSuchElementException(benchmarkId);
	}
    }

    public IScapContext getContext(ITailoring tailoring, String profileId) throws NoSuchElementException, ScapException {
	String benchmarkId = tailoring.getBenchmarkId();
	if (benchmarks.containsKey(benchmarkId)) {
	    return new Context(benchmarks.get(benchmarkId), tailoring.getProfile(profileId));
	} else {
	    throw new NoSuchElementException(benchmarkId);
	}
    }

    public IChecklist getOcil(String href) throws NoSuchElementException, OcilException {
	if (href.startsWith("http://") || href.startsWith("https://")) {
	    throw new NoSuchElementException(href);
	} else if (base != null) {
	    File f = new File(base, href);
	    if (f.exists()) {
		return new Checklist(Checklist.getOCILType(f));
	    } else {
		throw new NoSuchElementException(href);
	    }
	} else {
	    InputStream in = null;
	    try {
		ZipEntry entry = zip.getEntry(href);
		if (entry == null) {
		    throw new NoSuchElementException(href);
		} else {
		    return new Checklist(Checklist.getOCILType(zip.getInputStream(entry)));
		}
	    } catch (IOException e) {
		throw new OcilException(e);
	    } finally {
		if (in != null) {
		    try {
			in.close();
		    } catch (IOException e) {
		    }
		}
	    }
	}
    }

    public IDefinitions getOval(String href) throws NoSuchElementException, OvalException {
	if (href.startsWith("http://")) {
	    throw new NoSuchElementException(href);
	} else if (base != null) {
	    File f = new File(base, href);
	    if (f.exists()) {
		return new Definitions(Definitions.getOvalDefinitions(f));
	    } else {
		throw new NoSuchElementException(href);
	    }
	} else {
	    InputStream in = null;
	    try {
		ZipEntry entry = zip.getEntry(href);
		if (entry == null) {
		    throw new NoSuchElementException(href);
		} else {
		    return new Definitions(Definitions.getOvalDefinitions(zip.getInputStream(entry)));
		}
	    } catch (IOException e) {
		throw new OvalException(e);
	    } finally {
		if (in != null) {
		    try {
			in.close();
		    } catch (IOException e) {
		    }
		}
	    }
	}
    }

    public IScript getSce(String href) throws NoSuchElementException, SceException {
	if (href.startsWith("http://")) {
	    throw new NoSuchElementException(href);
	} else if (base != null) {
	    File f = new File(base, href);
	    if (f.exists()) {
		try {
		    return new Script(href, f.toURI().toURL());
		} catch (IOException e) {
		    throw new SceException(e);
		}
	    } else {
		throw new NoSuchElementException(href);
	    }
	} else {
	    try {
		StringBuffer sb = new StringBuffer("zip:");
		sb.append(new File(zip.getName()).toURI().toURL().toString());
		sb.append("!");
		if (!href.startsWith("/")) {
		    sb.append("/");
		}
		sb.append(href);
		URL url = new URL(null, sb.toString(), new ZipURLStreamHandler());
		return new Script(href, url);
	    } catch (IOException e) {
		throw new SceException(e);
	    }
	}
    }

    // Internal

    @Override
    protected void finalize() {
	if (zip != null) {
	    try {
		zip.close();
	    } catch (IOException e) {
	    }
	}
    }

    class Context extends ScapContext {
	Context(IBenchmark benchmark, ProfileType profile) throws XccdfException {
	    super(benchmark, dictionary, profile);
	}

	// Implement IScapContext

	public IChecklist getOcil(String href) throws NoSuchElementException, OcilException {
	    return Bundle.this.getOcil(href);
	}

	public IDefinitions getOval(String href) throws NoSuchElementException, OvalException {
	    return Bundle.this.getOval(href);
	}

	public IScript getSce(String href) throws NoSuchElementException, SceException {
	    return Bundle.this.getSce(href);
	}
    }
}
