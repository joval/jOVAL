// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.scap.datastream;

import java.util.Collection;
import java.util.NoSuchElementException;

import scap.datastream.Component;
import scap.datastream.ExtendedComponent;

import org.joval.intf.scap.IScapContext;
import org.joval.intf.scap.cpe.IDictionary;
import org.joval.intf.scap.ocil.IChecklist;
import org.joval.intf.scap.oval.IDefinitions;
import org.joval.intf.scap.sce.IScript;
import org.joval.intf.scap.xccdf.IBenchmark;
import org.joval.intf.scap.xccdf.ITailoring;
import org.joval.intf.scap.xccdf.SystemEnumeration;
import org.joval.scap.ScapException;
import org.joval.scap.ocil.OcilException;
import org.joval.scap.oval.OvalException;
import org.joval.scap.sce.SceException;
import org.joval.scap.xccdf.XccdfException;

/**
 * Interface defining an SCAP datastream.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IDatastream {
    /**
     * Get the CPE dictionary for this stream.
     */
    IDictionary getDictionary();

    /**
     * Get the IDs of the benchmarks defined in the stream.
     */
    Collection<String> getBenchmarkIds();

    /**
     * Get a benchmark defined in the stream.
     */
    IBenchmark getBenchmark(String benchmarkId) throws NoSuchElementException, XccdfException;

    /**
     * Get the IDs of the profiles defined in the specified benchmark.
     *
     * @throws NoSuchElementException if there is no benchmark with the specified ID
     */
    Collection<String> getProfileIds(String benchmarkId) throws NoSuchElementException;

    /**
     * Get the view of the benchmark given the specified profile.
     *
     * @throws NoSuchElementException if there is no benchmark or profile with the specified ID
     */
    IScapContext getContext(String benchmarkId, String profileId) throws NoSuchElementException, ScapException;

    /**
     * Get the view of the stream given the specified tailoring and profile ID.
     *
     * @throws NoSuchElementException if there is no benchmark in the stream that matches the specified tailoring, or if
     *                                there is no profile in the tailoring that matches the specified profileId.
     */
    IScapContext getContext(ITailoring tailoring, String profileId) throws NoSuchElementException, ScapException;

    /**
     * Get an OCIL checklist document with the specified component href.
     */
    IChecklist getOcil(String href) throws NoSuchElementException, OcilException;

    /**
     * Get an OVAL definitions document with the specified component href.
     */
    IDefinitions getOval(String href) throws NoSuchElementException, OvalException;

    /**
     * Get SCE script data with the specified component href.
     */
    IScript getSce(String href) throws NoSuchElementException, SceException;
}
