// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.scap;

import java.util.Collection;
import java.util.NoSuchElementException;

import org.openscap.sce.xccdf.ScriptDataType;
import scap.datastream.Component;
import scap.datastream.ExtendedComponent;

import org.joval.intf.cpe.IDictionary;
import org.joval.intf.ocil.IChecklist;
import org.joval.intf.oval.IDefinitions;
import org.joval.intf.scap.IDatastream;
import org.joval.intf.xccdf.IBenchmark;
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
    IView view(String benchmarkId, String profileId) throws NoSuchElementException, ScapException;

    /**
     * Get a component (or extended component) from the stream given its href.
     *
     * @throws NoSuchElementException if there is no component with the specified href in the stream
     */
    Object resolve(String href) throws NoSuchElementException;

    /**
     * Returns the SystemEnumeration corresponding to the component href.
     *
     * @throws NoSuchElementException if there is no component with the specified href in the stream
     */
    SystemEnumeration getSystem(String href) throws NoSuchElementException;

    IChecklist getOcil(String href) throws NoSuchElementException, OcilException;
    IDefinitions getOval(String href) throws NoSuchElementException, OvalException;
    ScriptDataType getSce(String href) throws NoSuchElementException, SceException;
}
