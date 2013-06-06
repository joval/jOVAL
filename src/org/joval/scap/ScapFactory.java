// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;

import org.openscap.sce.results.SceResultsType;
import scap.ocil.core.OCILType;

import org.joval.intf.plugin.IPlugin;
import org.joval.intf.scap.IScapContext;
import org.joval.intf.scap.arf.IReport;
import org.joval.intf.scap.datastream.IDatastream;
import org.joval.intf.scap.datastream.IDatastreamCollection;
import org.joval.intf.scap.ocil.IChecklist;
import org.joval.intf.scap.sce.IScriptResult;
import org.joval.intf.scap.xccdf.ITailoring;
import org.joval.intf.scap.xccdf.IXccdfEngine;
import org.joval.scap.ScapException;
import org.joval.scap.arf.ArfException;
import org.joval.scap.arf.Report;
import org.joval.scap.datastream.DatastreamCollection;
import org.joval.scap.ocil.Checklist;
import org.joval.scap.ocil.OcilException;
import org.joval.scap.sce.Result;
import org.joval.scap.xccdf.Bundle;
import org.joval.scap.xccdf.Tailoring;
import org.joval.scap.xccdf.XccdfException;
import org.joval.scap.xccdf.engine.Engine;

/**
 * A convenience class for obtaining jOVAL's SCAP management objects.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class ScapFactory {
    /**
     * An empty checklist.
     */
    public static final IChecklist EMPTY_CHECKLIST = Checklist.EMPTY;

    /**
     * Create a datastream collection from a URL.
     */
    public static IDatastreamCollection createDatastreamCollection(URL url) throws ScapException {
	try {
	    return new DatastreamCollection(DatastreamCollection.getDSCollection(url.openStream()));
	} catch (IOException e) {
	    throw new ScapException(e);
	}
    }

    /**
     * Create a datastream collection from a file.
     */
    public static IDatastreamCollection createDatastreamCollection(File f) throws ScapException {
	return new DatastreamCollection(DatastreamCollection.getDSCollection(f));
    }

    /**
     * Create a datastream from an SCAP "bundle" of files (directory or ZIP file).
     */
    public static IDatastream createBundle(File f) throws ScapException {
	return new Bundle(f);
    }

    /**
     * Create an XCCDF tailoring from a file.
     */
    public static ITailoring createTailoring(File f) throws XccdfException {
	return new Tailoring(Tailoring.getTailoringType(f));
    }

    /**
     * Create an OCIL checklist (which can also include the results) from a file.
     */
    public static IChecklist createChecklist(File f) throws OcilException {
	return new Checklist(f);
    }

    /**
     * Create an OCIL checklist (which can also include the results) from a stream.
     */
    public static IChecklist createChecklist(InputStream in) throws OcilException {
	return new Checklist(in);
    }

    /**
     * Create an OCIL checklist (which can also include the results) from a stream.
     */
    public static IChecklist createChecklist(OCILType ocil) throws OcilException {
	return new Checklist(ocil);
    }

    /**
     * Create an SCE script result from a data model object.
     */
    public static IScriptResult createResult(SceResultsType srt) {
	return new Result(srt);
    }

    /**
     * Create an ARF report from a file.
     */
    public static IReport createReport(File f) throws ArfException {
	return new Report(Report.getAssetReportCollection(f));
    }

    /**
     * Discover all the OCIL exports (hrefs, variables and checklists) for the given context.
     */
    public static Collection<IXccdfEngine.OcilMessageArgument> getOcilExports(IScapContext ctx) throws OcilException {
	return Engine.getOcilExports(ctx);
    }

    /**
     * Create an XCCDF processing engine.
     */
    public static IXccdfEngine createEngine(IPlugin plugin) {
	return new OEMEngine(plugin);
    }

    // Private

    private static class OEMEngine extends Engine {
	OEMEngine(IPlugin plugin) {
	    super(plugin);
	}
    }
}
