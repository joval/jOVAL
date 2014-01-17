// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.xccdf;

import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.net.MalformedURLException;
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
public class Bundle implements IDatastream {
    private URL base;
    private Map<String, IBenchmark> benchmarks;
    private IDictionary dictionary;

    public Bundle(File file) throws ScapException {
	benchmarks = new HashMap<String, IBenchmark>();
	if (file.isDirectory()) {
	    try {
		StringBuffer sb = new StringBuffer(file.toURI().toURL().toExternalForm());
		if (sb.toString().endsWith("/")) {
		    base = new URL(sb.toString());
		} else {
		    base = new URL(sb.append("/").toString());
		}
	    } catch (MalformedURLException e) {
		throw new ScapException(e);
	    }
	    for (File f : file.listFiles()) {
		if (f.isFile() && f.getName().endsWith(".xml")) {
		    if (f.getName().toLowerCase().indexOf("xccdf") != -1) {
			IBenchmark benchmark = new Benchmark(f.getName(), Benchmark.getXccdfBenchmark(f));
			benchmarks.put(benchmark.getId(), benchmark);
		    } else if (f.getName().toLowerCase().indexOf("cpe-dictionary") != -1) {
			dictionary = new Dictionary(Dictionary.getCpeList(f));
		    }
		}
	    }
	} else if (file.getName().toLowerCase().endsWith(".zip")) {
	    ZipFile zip = null;
	    try {
		StringBuffer sb = new StringBuffer("zip:").append(file.toURI().toURL().toString()).append("!/");
		base = new URL(null, sb.toString(), new ZipURLStreamHandler());
		zip = new ZipFile(file);
		Enumeration<? extends ZipEntry> entries = zip.entries();
		while(entries.hasMoreElements()) {
		    ZipEntry entry = entries.nextElement();
		    if (!entry.isDirectory() && entry.getName().toLowerCase().endsWith(".xml")) {
			InputStream in = null;
			try {
			    if (entry.getName().toLowerCase().endsWith("-xccdf.xml")) {
				in = zip.getInputStream(entry);
				IBenchmark benchmark = new Benchmark(entry.getName(), Benchmark.getXccdfBenchmark(in));
				benchmarks.put(benchmark.getId(), benchmark);
			    } else if (entry.getName().toLowerCase().endsWith("-cpe-dictionary.xml")) {
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
	    } finally {
		if (zip != null) {
		    try {
			zip.close();
		    } catch (IOException ie) {
		    }
		}
	    }
	} else {
	    String msg = Message.getMessage(Message.ERROR_IO, file.toString(), Message.getMessage(Message.ERROR_IO_NOT_DIR));
	    throw new ScapException(msg);
	}
    }

    // Implement IDatastream

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
	    for (ProfileType profile : benchmarks.get(benchmarkId).getRootObject().getProfile()) {
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
	    return new Context(benchmark, null, profileId);
	} else {
	    throw new NoSuchElementException(benchmarkId);
	}
    }

    public IScapContext getContext(ITailoring tailoring, String profileId) throws NoSuchElementException, ScapException {
	String benchmarkId = tailoring.getBenchmarkId();
	if (benchmarks.containsKey(benchmarkId)) {
	    return new Context(benchmarks.get(benchmarkId), tailoring, profileId);
	} else {
	    throw new NoSuchElementException(benchmarkId);
	}
    }

    public IChecklist getOcil(String href) throws OcilException {
	InputStream in = null;
	try {
	    in = new URL(base, href).openStream();
	    return new Checklist(Checklist.getOCILType(in));
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

    public IDefinitions getOval(String href) throws OvalException {
	InputStream in = null;
	try {
	    in = new URL(base, href).openStream();
	    return new Definitions(Definitions.getOvalDefinitions(in));
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

    public IScript getSce(String href) throws SceException {
	try {
	    return new Script(href, new URL(base, href));
	} catch (MalformedURLException e) {
	    throw new SceException(e);
	}
    }

    // Internal

    class Context extends ScapContext {
	private URL contextBase;

	Context(IBenchmark benchmark, ITailoring tailoring, String profileId) throws XccdfException {
	    super(benchmark, dictionary, tailoring, profileId);
	    try {
		contextBase = new URL(base, benchmark.getHref());
	    } catch (MalformedURLException e) {
		throw new XccdfException(e);
	    }
	}

	// Implement IScapContext

	public IChecklist getOcil(String href) throws OcilException {
	    try {
		return Bundle.this.getOcil(new URL(contextBase, href).toString());
	    } catch (MalformedURLException e) {
		throw new OcilException(e);
	    }
	}

	public IDefinitions getOval(String href) throws OvalException {
	    try {
		return Bundle.this.getOval(new URL(contextBase, href).toString());
	    } catch (MalformedURLException e) {
		throw new OvalException(e);
	    }
	}

	public IScript getSce(String href) throws SceException {
	    try {
		return new Script(href, new URL(contextBase, href));
	    } catch (MalformedURLException e) {
		throw new SceException(e);
	    }
	}
    }
}
