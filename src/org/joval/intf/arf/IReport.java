// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.arf;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.NoSuchElementException;
import org.w3c.dom.Element;

import ai.schemas.core.AssetType;
import ai.schemas.core.ComputingDeviceType;
import ai.schemas.core.IpAddressType;
import ai.schemas.core.NetworkInterfaceType;
import arf.schemas.core.AssetReportCollection;
import arf.schemas.core.ReportRequestType;
import arf.schemas.core.ReportType;
import oval.schemas.systemcharacteristics.core.InterfaceType;
import oval.schemas.systemcharacteristics.core.SystemInfoType;

import org.joval.intf.xml.ITransformable;

/**
 * A representation of an ARF report collection.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IReport extends ITransformable {
    /**
     * Get the underlying JAXB type.
     */
    AssetReportCollection getAssetReportCollection();

    /**
     * Add a report request.
     *
     * @returns the ID generated for the request
     */
    String addRequest(Element request);

    /**
     * Add an asset based on a SystemInfoType
     *
     * @returns the ID generated for the asset
     */
    String addAsset(SystemInfoType info);

    /**
     * Add an XCCDF result related to the specified request and asset.
     *
     * @returns the ID generated for the report
     */
    String addReport(String requestId, String assetId, Element report) throws NoSuchElementException;

    /**
     * Serialize the report to a file.
     */
    void writeXML(File f) throws IOException;
}
