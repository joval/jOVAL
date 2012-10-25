// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.arf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.NoSuchElementException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.util.JAXBSource;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.transform.Source;

import org.slf4j.cal10n.LocLogger;

import ai.schemas.core.AssetType;
import ai.schemas.core.ComputingDeviceType;
import ai.schemas.core.IpAddressType;
import ai.schemas.core.NetworkInterfaceType;
import arf.schemas.core.AssetReportCollection;
import arf.schemas.core.ReportRequestType;
import arf.schemas.core.ReportType;
import arf.schemas.reporting.RelationshipsContainerType;
import arf.schemas.reporting.RelationshipType;
import oval.schemas.systemcharacteristics.core.InterfaceType;
import oval.schemas.systemcharacteristics.core.SystemInfoType;
import xccdf.schemas.core.BenchmarkType;
import xccdf.schemas.core.TestResultType;

import org.joval.intf.util.ILoggable;
import org.joval.intf.xml.ITransformable;
import org.joval.scap.oval.types.Ip4AddressType;
import org.joval.scap.oval.types.Ip6AddressType;
import org.joval.util.JOVALMsg;
import org.joval.xml.SchemaRegistry;

/**
 * A representation of an ARF report collection.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Report implements ILoggable, ITransformable {
    private LocLogger logger;
    private JAXBContext ctx;
    private AssetReportCollection arc;
    private HashMap<String, BenchmarkType> requests;
    private HashMap<String, AssetType> assets;
    private HashMap<String, TestResultType> reports;

    /**
     * Create an empty report.
     */
    public Report() throws ArfException {
	arc = Factories.core.createAssetReportCollection();
	requests = new HashMap<String, BenchmarkType>();
	assets = new HashMap<String, AssetType>();
	reports = new HashMap<String, TestResultType>();

	try {
	    ctx = JAXBContext.newInstance(SchemaRegistry.lookup(SchemaRegistry.ARF));
	} catch (JAXBException e) {
	    throw new ArfException(e);
	}
	logger = JOVALMsg.getLogger();
    }

    /**
     * Add an XCCDF result to the report.
     *
     * @returns the ID generated for the request
     */
    public synchronized String addRequest(BenchmarkType benchmark) {
	String requestId = new StringBuffer("request_").append(Integer.toString(requests.size())).toString();
	requests.put(requestId, benchmark);

	if (!arc.isSetReportRequests()) {
	    arc.setReportRequests(Factories.core.createAssetReportCollectionReportRequests());
	}
	ReportRequestType request = Factories.core.createReportRequestType();
	request.setId(requestId);
	ReportRequestType.Content content = Factories.core.createReportRequestTypeContent();
	content.setAny(benchmark);
	request.setContent(content);
	arc.getReportRequests().getReportRequest().add(request);
	return requestId;
    }

    /**
     * Add an asset based on a SystemInfoType
     *
     * @returns the ID generated for the asset
     */
    public synchronized String addAsset(SystemInfoType info) {
	ComputingDeviceType cdt = Factories.asset.createComputingDeviceType();
	ComputingDeviceType.Hostname hostname = Factories.asset.createComputingDeviceTypeHostname();
	hostname.setValue(info.getPrimaryHostName());
	cdt.setHostname(hostname);
	if (info.isSetInterfaces() && info.getInterfaces().getInterface() != null) {
	    ComputingDeviceType.Connections connections = Factories.asset.createComputingDeviceTypeConnections();
	    HashMap<String, NetworkInterfaceType> interfaces = new HashMap<String, NetworkInterfaceType>();
	    for (InterfaceType intf : info.getInterfaces().getInterface()) {
		if (intf.isSetMacAddress()) {
		    //
		    // For interfaces specifying a MAC address, keep iterating so that both IP4 and IP6 address
		    // information can be added to the same interface.
		    //
		    String macAddress = intf.getMacAddress();
		    NetworkInterfaceType nit = null;
		    if (interfaces.containsKey(macAddress)) {
			nit = interfaces.get(macAddress);
		    } else {
			Factories.asset.createNetworkInterfaceType();
			NetworkInterfaceType.MacAddress mac = Factories.asset.createNetworkInterfaceTypeMacAddress();
			mac.setValue(macAddress);
			nit.setMacAddress(mac);
		    }
		    if (intf.isSetIpAddress()) {
			try {
			    setIpAddressInfo(nit, intf.getIpAddress());
			    connections.getConnection().add(nit);
			} catch (IllegalArgumentException e) {
			}
		    }
		} else {
		    //
		    // Interfaces where the MAC address is unknown can be added immediately
		    //
		    NetworkInterfaceType nit = Factories.asset.createNetworkInterfaceType();
		    if (intf.isSetIpAddress()) {
			try {
			    setIpAddressInfo(nit, intf.getIpAddress());
			    connections.getConnection().add(nit);
			} catch (IllegalArgumentException e) {
			}
		    }
		}
	    }
	    //
	    // Add all the interfaces with MAC addresses that were stored in the map
	    //
	    for (NetworkInterfaceType nit : interfaces.values()) {
		connections.getConnection().add(nit);
	    }
	    cdt.setConnections(connections);
	}
	String assetId = new StringBuffer("asset").append(Integer.toString(assets.size())).toString();
	assets.put(assetId, cdt);

	if (!arc.isSetAssets()) {
	    arc.setAssets(Factories.core.createAssetReportCollectionAssets());
	}
	AssetReportCollection.Assets.Asset asset = Factories.core.createAssetReportCollectionAssetsAsset();
	asset.setId(assetId);
	asset.setAsset(Factories.asset.createComputingDevice(cdt));
	arc.getAssets().getAsset().add(asset);
	return assetId;
    }

    /**
     * Add an XCCDF result related to the specified request and asset.
     *
     * @returns the ID generated for the report
     */
    public synchronized String addReport(String requestId, String assetId, TestResultType report)
		throws NoSuchElementException {

	if (!requests.containsKey(requestId)) {
	    throw new NoSuchElementException(requestId);
	}
	if (!assets.containsKey(assetId)) {
	    throw new NoSuchElementException(assetId);
	}

	String reportId = new StringBuffer("report_").append(Integer.toString(reports.size())).toString();
	reports.put(reportId, report);

	if (!arc.isSetRelationships()) {
	    arc.setRelationships(Factories.reporting.createRelationshipsContainerTypeRelationships());
	}
	//
	// Relate to the request
	//
	RelationshipType relToRequest = Factories.reporting.createRelationshipType();
	relToRequest.setSubject(reportId);
	relToRequest.setType(Factories.CREATED_FOR);
	relToRequest.getRef().add(requestId);
	arc.getRelationships().getRelationship().add(relToRequest);

	//
	// Relate to the asset
	//
	RelationshipType relToAsset = Factories.reporting.createRelationshipType();
	relToAsset.setSubject(reportId);
	relToAsset.setType(Factories.IS_ABOUT);
	relToAsset.getRef().add(assetId);
	arc.getRelationships().getRelationship().add(relToAsset);

	if (!arc.isSetReports()) {
	    arc.setReports(Factories.core.createAssetReportCollectionReports());
	}
	ReportType rt = Factories.core.createReportType();
	rt.setId(reportId);
	ReportType.Content content = Factories.core.createReportTypeContent();
	content.setAny(report);
	rt.setContent(content);
	arc.getReports().getReport().add(rt);
	return reportId;
    }

    /**
     * Get the underlying JAXB BenchmarkType.
     */
    public AssetReportCollection getAssetReportCollection() {
	return arc;
    }

    public void writeXML(File f) throws IOException {
	OutputStream out = null;
	try {
	    Marshaller marshaller = ctx.createMarshaller();
	    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
	    out = new FileOutputStream(f);
	    marshaller.marshal(arc, out);
	} catch (JAXBException e) {
	    logger.warn(JOVALMsg.ERROR_FILE_GENERATE, f.toString());
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (FactoryConfigurationError e) {
	    logger.warn(JOVALMsg.ERROR_FILE_GENERATE, f.toString());
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (FileNotFoundException e) {
	    logger.warn(JOVALMsg.ERROR_FILE_GENERATE, f.toString());
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} finally {
	    if (out != null) {
		try {
		    out.close();
		} catch (IOException e) {
		    logger.warn(JOVALMsg.ERROR_FILE_CLOSE, f.toString());
		}
	    }
	}
    }

    // Implement ITransformable

    public Source getSource() throws JAXBException {
	return new JAXBSource(ctx, arc);
    }

    // Implement ILoggable

    public void setLogger(LocLogger logger) {
	this.logger = logger;
    }

    public LocLogger getLogger() {
	return logger;
    }

    // Private

    private void setIpAddressInfo(NetworkInterfaceType nit, String ipAddressString) {
	IpAddressType ip = null;
	if (nit.isSetIpAddress()) {
	    ip = nit.getIpAddress();
	} else {
	    ip = Factories.asset.createIpAddressType();
	    nit.setIpAddress(ip);
	}
	IpAddressType subnet = null;
	if (nit.isSetSubnetMask()) {
	    subnet = nit.getSubnetMask();
	} else {
	    subnet = Factories.asset.createIpAddressType();
	    nit.setSubnetMask(subnet);
	}

	try {
	    Ip4AddressType addressType = new Ip4AddressType(ipAddressString);
	    IpAddressType.IpV4 ip4 = Factories.asset.createIpAddressTypeIpV4();
	    ip4.setValue(addressType.getIpAddressString());
	    ip.setIpV4(ip4);
	    IpAddressType.IpV4 ip4subnet = Factories.asset.createIpAddressTypeIpV4();
	    ip4subnet.setValue(addressType.getSubnetString());
	    subnet.setIpV4(ip4subnet);
	} catch (IllegalArgumentException e) {
	    Ip6AddressType addressType = new Ip6AddressType(ipAddressString);
	    IpAddressType.IpV6 ip6 = Factories.asset.createIpAddressTypeIpV6();
	    ip6.setValue(addressType.getIpAddressString());
	    ip.setIpV6(ip6);
	    IpAddressType.IpV6 ip6subnet = Factories.asset.createIpAddressTypeIpV6();
	    ip6subnet.setValue(addressType.getSubnetString());
	    subnet.setIpV6(ip6subnet);
	}
    }
}
