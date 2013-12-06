// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.scap.xccdf;

import java.io.File;
import java.io.IOException;
import java.util.NoSuchElementException;
import javax.xml.transform.Transformer;

import scap.xccdf.ProfileType;
import scap.xccdf.ItemType;
import scap.xccdf.XccdfBenchmark;

import org.joval.intf.xml.ITransformable;

/**
 * A representation of a single XCCDF 1.2 benchmark.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IBenchmark extends ITransformable<XccdfBenchmark> {
    /**
     * Get the href of the datastream component source (if any).
     */
    String getHref();

    /**
     * Shortcut for getBenchmark().getBenchmarkId().
     */
    String getId();

    /**
     * Get a profile given its ID.
     */
    ProfileType getProfile(String id) throws NoSuchElementException;

    /**
     * Get an item given its ID. (An item can be a group, rule, or value).
     */
    ItemType getItem(String id) throws NoSuchElementException;

    /**
     * Serialize the benchmark to a file.
     */
    void writeXML(File f) throws IOException;

    /**
     * Transform using the specified template, and serialize to the specified file.
     */
    void writeTransform(Transformer transform, File output);
}
