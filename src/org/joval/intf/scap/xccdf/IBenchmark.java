// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.scap.xccdf;

import java.io.File;
import java.io.IOException;

import scap.xccdf.BenchmarkType;

import org.joval.intf.xml.ITransformable;

/**
 * A representation of a single XCCDF 1.2 benchmark.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IBenchmark extends ITransformable {
    /**
     * Get the underlying JAXB BenchmarkType.
     */
    BenchmarkType getBenchmark();

    String getHref();

    /**
     * Serialize the benchmark to a file.
     */
    void writeXML(File f) throws IOException;

    /**
     * Transform using the specified template, and serialize to the specified file.
     */
    void writeTransform(File transform, File output);
}
