// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.scap.xccdf;

import java.util.Collection;
import java.util.NoSuchElementException;

import org.openscap.sce.xccdf.ScriptDataType;
import scap.datastream.Component;
import scap.datastream.ExtendedComponent;

import org.joval.intf.scap.IScapContext;
import org.joval.intf.scap.cpe.IDictionary;
import org.joval.intf.scap.ocil.IChecklist;
import org.joval.intf.scap.oval.IDefinitions;
import org.joval.intf.scap.xccdf.IBenchmark;
import org.joval.intf.scap.xccdf.ITailoring;
import org.joval.intf.scap.xccdf.SystemEnumeration;
import org.joval.scap.ScapException;
import org.joval.scap.ocil.OcilException;
import org.joval.scap.oval.OvalException;
import org.joval.scap.sce.SceException;
import org.joval.scap.xccdf.XccdfException;

/**
 * Interface defining an SCAP bundle.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IBundle {
    /**
     * Get the CPE dictionary for this bundle.
     */
    IDictionary getDictionary();

    /**
     * Get the IDs of all the benchmarks included in the bundle.
     */
    Collection<String> getBenchmarkIds();

    /**
     * Get the benchmark with the specified ID.
     *
     * @throws NoSuchElementException if there is no benchmark with the specified ID
     */
    IBenchmark getBenchmark(String benchmarkId) throws NoSuchElementException;

    /**
     * Get the IDs of the profiles defined in the specified benchmark.
     *
     * @throws NoSuchElementException if there is no benchmark with the specified ID
     */
    Collection<String> getProfileIds(String benchmarkId) throws NoSuchElementException;

    /**
     * Get a context using the specified benchmark and profile.
     *
     * @throws NoSuchElementException if there is no benchmark or profile with the specified ID
     */
    IScapContext getContext(String benchmarkId, String profileId) throws NoSuchElementException, ScapException;

    /**
     * Get a context using the specified tailoring/profile.
     *
     * @throws NoSuchElementException if there is no benchmark in the bundle that matches the specified tailoring, or if
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
    ScriptDataType getSce(String href) throws NoSuchElementException, SceException;
}
