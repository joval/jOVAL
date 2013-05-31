// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.arf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.JAXBSource;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Element;

import jsaf.intf.util.ILoggable;
import org.slf4j.cal10n.LocLogger;

import org.oasis.catalog.Catalog;
import org.oasis.catalog.Uri;
import scap.ai.AssetType;
import scap.ai.ComputingDeviceType;
import scap.ai.Cpe;
import scap.ai.IpAddressType;
import scap.ai.NetworkInterfaceType;
import scap.arf.core.AssetReportCollection;
import scap.arf.core.ReportRequestType;
import scap.arf.core.ReportType;
import scap.arf.core.AssetReportCollection.ExtendedInfos.ExtendedInfo;
import scap.arf.reporting.RelationshipsContainerType;
import scap.arf.reporting.RelationshipType;
import scap.oval.systemcharacteristics.core.InterfaceType;
import scap.oval.systemcharacteristics.core.SystemInfoType;
import scap.xccdf.TestResultType;

import org.joval.intf.scap.arf.IReport;
import org.joval.intf.scap.xccdf.SystemEnumeration;
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
public class Report implements IReport, ILoggable {
    public static final AssetReportCollection getAssetReportCollection(File f) throws ArfException {
	return getAssetReportCollection(new StreamSource(f));
    }

    public static final AssetReportCollection getAssetReportCollection(Source src) throws ArfException {
	Object rootObj = parse(src);
	if (rootObj instanceof AssetReportCollection) {
	    return (AssetReportCollection)rootObj;
	} else {
	    throw new ArfException(JOVALMsg.getMessage(JOVALMsg.ERROR_ARF_BAD_SOURCE, src.getSystemId()));
	}
    }

    private static final Object parse(InputStream in) throws ArfException {
	return parse(new StreamSource(in));
    }

    private static final Object parse(Source src) throws ArfException {
	try {
	    Unmarshaller unmarshaller = SchemaRegistry.ARF.getJAXBContext().createUnmarshaller();
	    return unmarshaller.unmarshal(src);
	} catch (JAXBException e) {
	    throw new ArfException(e);
	}
    }

    private static final String CATALOG_ID = "urn:joval:reports:catalog";

    private LocLogger logger;
    private AssetReportCollection arc;
    private HashMap<String, Element> requests;
    private HashMap<String, AssetType> assets;
    private HashMap<String, Element> reports;

    /**
     * Create a report based on an existing AssetReportCollection.
     */
    public Report(AssetReportCollection arc) {
	this.arc = arc;
	requests = new HashMap<String, Element>();
	assets = new HashMap<String, AssetType>();
	reports = new HashMap<String, Element>();
	logger = JOVALMsg.getLogger();
    }

    /**
     * Create an empty report.
     */
    public Report() {
	arc = Factories.core.createAssetReportCollection();
	requests = new HashMap<String, Element>();
	assets = new HashMap<String, AssetType>();
	reports = new HashMap<String, Element>();
	logger = JOVALMsg.getLogger();
    }

    // Implement IReport

    public Collection<String> getAssetIds() {
	return assets.keySet();
    }

    public AssetType getAsset(String assetId) throws NoSuchElementException {
	if (assets.containsKey(assetId)) {
	    return assets.get(assetId);
	} else {
	    throw new NoSuchElementException(assetId);
	}
    }

    public TestResultType getTestResult(String assetId, String benchmarkId, String profileId) throws NoSuchElementException {
	try {
	    Unmarshaller unmarshaller = SchemaRegistry.XCCDF.getJAXBContext().createUnmarshaller();
	    for (RelationshipType rel : arc.getRelationships().getRelationship()) {
		if (rel.getType().equals(Factories.IS_ABOUT)) {
		    for (String ref : rel.getRef()) {
			if (assetId.equals(ref) && reports.containsKey(rel.getSubject())) {
			    Element elt = reports.get(rel.getSubject());
			    if ("TestResult".equals(elt.getLocalName()) &&
				SystemEnumeration.XCCDF.namespace().equals(elt.getNamespaceURI())) {
				TestResultType tr = (TestResultType)(((JAXBElement)unmarshaller.unmarshal(elt)).getValue());
				if (benchmarkId.equals(tr.getBenchmark().getId()) &&
				    profileId.equals(tr.getProfile().getIdref())) {
				    return tr;
				}
			    }
			}
		    }
		}
	    }
	} catch (JAXBException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	throw new NoSuchElementException(assetId + ":" + benchmarkId + ":" + profileId);
    }

    public Catalog getCatalog() throws ArfException, NoSuchElementException {
	if (arc.isSetExtendedInfos()) {
	    for (ExtendedInfo ext : arc.getExtendedInfos().getExtendedInfo()) {
		if (CATALOG_ID.equals(ext.getId())) {
		    Object obj = ext.getAny();
		    if (obj instanceof JAXBElement) {
			obj = ((JAXBElement)obj).getValue();
		    }
		    if (obj instanceof Catalog) {
			return (Catalog)obj;
		    } else {
			throw new ArfException(JOVALMsg.getMessage(JOVALMsg.ERROR_ARF_CATALOG, obj.getClass().getName()));
		    }
		}
	    }
	}
	throw new NoSuchElementException();
    }

    public AssetReportCollection getAssetReportCollection() {
	return arc;
    }

    public synchronized String addRequest(Element request) {
	String requestId = new StringBuffer("request_").append(Integer.toString(requests.size())).toString();
	requests.put(requestId, request);

	if (!arc.isSetReportRequests()) {
	    arc.setReportRequests(Factories.core.createAssetReportCollectionReportRequests());
	}
	ReportRequestType requestType = Factories.core.createReportRequestType();
	requestType.setId(requestId);
	ReportRequestType.Content content = Factories.core.createReportRequestTypeContent();
	content.setAny(request);
	requestType.setContent(content);
	arc.getReportRequests().getReportRequest().add(requestType);
	return requestId;
    }

    public synchronized String addAsset(SystemInfoType info, Collection<String> cpes) {
	ComputingDeviceType cdt = Factories.asset.createComputingDeviceType();
	if (cpes != null) {
	    for (String cpe : cpes) {
		Cpe cpeType = Factories.asset.createCpe();
		cpeType.setValue(cpe);
		cdt.getCpe().add(cpeType);
	    }
	}
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
			nit = Factories.asset.createNetworkInterfaceType();
			NetworkInterfaceType.MacAddress mac = Factories.asset.createNetworkInterfaceTypeMacAddress();
			mac.setValue(macAddress);
			nit.setMacAddress(mac);
			interfaces.put(macAddress, nit);
		    }
		    if (intf.isSetIpAddress()) {
			try {
			    setIpAddressInfo(nit, intf.getIpAddress());
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
	String assetId = new StringBuffer("asset_").append(Integer.toString(assets.size())).toString();
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

    public synchronized String addReport(String requestId, String assetId, String ref, Element report)
		throws NoSuchElementException, ArfException {

	if (!requests.containsKey(requestId)) {
	    throw new NoSuchElementException(requestId);
	}
	if (!assets.containsKey(assetId)) {
	    throw new NoSuchElementException(assetId);
	}

	String reportId = new StringBuffer("report_").append(Integer.toString(reports.size())).toString();
	reports.put(reportId, report);
	if (ref != null && ref.length() > 0) {
	    Uri uri = Factories.catalog.createUri();
	    uri.setName(reportId);
	    if (ref.startsWith("#")) {
		uri.setUri(ref);
	    } else {
		uri.setUri(new StringBuffer("#").append(ref).toString());
	    }
	    getCreateCatalog().getPublicOrSystemOrUri().add(Factories.catalog.createUri(uri));
	}
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

    public void writeXML(File f) throws IOException {
	OutputStream out = null;
	try {
	    Marshaller marshaller = SchemaRegistry.ARF.getJAXBContext().createMarshaller();
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
	return new JAXBSource(SchemaRegistry.ARF.getJAXBContext(), getRootObject());
    }

    public Object getRootObject() {
	return arc;
    }

    public JAXBContext getJAXBContext() throws JAXBException {
	return SchemaRegistry.ARF.getJAXBContext();
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

    private Catalog getCreateCatalog() throws ArfException {
	try {
	    return getCatalog();
	} catch (NoSuchElementException e) {
	    //
	    // Catalog was not found, so generate it
	    //
	    if (!arc.isSetExtendedInfos()) {
		arc.setExtendedInfos(Factories.core.createAssetReportCollectionExtendedInfos());
	    }
	    ExtendedInfo info = Factories.core.createAssetReportCollectionExtendedInfosExtendedInfo();
	    arc.getExtendedInfos().getExtendedInfo().add(info);
	    Catalog catalog = Factories.catalog.createCatalog();
	    info.setId(CATALOG_ID);
	    info.setAny(Factories.catalog.createCatalog(catalog));
	    return catalog;
	}
    }
}
