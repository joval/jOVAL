// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.scap.arf;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.NoSuchElementException;
import org.w3c.dom.Element;

import scap.ai.AssetType;
import scap.arf.core.AssetReportCollection;
import scap.oval.systemcharacteristics.core.SystemInfoType;
import scap.xccdf.TestResultType;

import org.joval.intf.xml.ITransformable;

/**
 * A representation of an ARF report collection.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IReport extends ITransformable {
    /**
     * Get the asset IDs in the report.
     */
    Collection<String> getAssetIds();

    /**
     * Get a particular asset.
     */
    AssetType getAsset(String assetId) throws NoSuchElementException;

    /**
     * Get all the XCCDF results associated with the asset.
     */
    Collection<TestResultType> getTestResults(String assetId) throws NoSuchElementException;

    /**
     * Get the underlying JAXB type.
     */
    AssetReportCollection getAssetReportCollection();

    /**
     * Add a report request.
     *
     * @return the ID generated for the request
     */
    String addRequest(Element request);

    /**
     * Add an asset based on a SystemInfoType
     *
     * @return the ID generated for the asset
     */
    String addAsset(SystemInfoType info, Collection<String> cpes);

    /**
     * Add an XCCDF result related to the specified request and asset.
     *
     * @return the ID generated for the report
     */
    String addReport(String requestId, String assetId, Element report) throws NoSuchElementException;

    /**
     * Serialize the report to a file.
     */
    void writeXML(File f) throws IOException;
}
