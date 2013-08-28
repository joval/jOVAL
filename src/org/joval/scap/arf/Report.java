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
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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

import org.gocil.diagnostics.ActionSequenceType;
import org.gocil.diagnostics.ActionType;
import org.gocil.diagnostics.OcilResultDiagnostics;
import org.gocil.diagnostics.QuestionnaireType;
import org.gocil.diagnostics.QuestionRef;
import org.gocil.diagnostics.QuestionnaireRef;
import org.oasis.catalog.Catalog;
import org.oasis.catalog.Uri;
import org.openscap.sce.results.SceResultsType;
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
import scap.ocil.core.BooleanQuestionResultType;
import scap.ocil.core.ChoiceQuestionResultType;
import scap.ocil.core.ChoiceType;
import scap.ocil.core.ExtensionContainerType;
import scap.ocil.core.NumericQuestionResultType;
import scap.ocil.core.OCILType;
import scap.ocil.core.QuestionResultType;
import scap.ocil.core.QuestionnaireResultType;
import scap.ocil.core.QuestionTestActionType;
import scap.ocil.core.StringQuestionResultType;
import scap.oval.definitions.core.CriteriaType;
import scap.oval.definitions.core.CriterionType;
import scap.oval.definitions.core.DefinitionType;
import scap.oval.definitions.core.ExtendDefinitionType;
import scap.oval.definitions.core.ObjectRefType;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.core.StateRefType;
import scap.oval.definitions.core.StateType;
import scap.oval.definitions.core.TestType;
import scap.oval.results.OvalResults;
import scap.oval.results.SystemType;
import scap.oval.systemcharacteristics.core.InterfaceType;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.SystemInfoType;
import scap.xccdf.BenchmarkType;
import scap.xccdf.CheckType;
import scap.xccdf.ComplexCheckType;
import scap.xccdf.RuleResultType;
import scap.xccdf.TestResultType;

import org.joval.intf.scap.arf.IReport;
import org.joval.intf.scap.ocil.IChecklist;
import org.joval.intf.scap.oval.IDefinitions;
import org.joval.intf.scap.oval.IResults;
import org.joval.intf.scap.oval.ISystemCharacteristics;
import org.joval.intf.scap.xccdf.SystemEnumeration;
import org.joval.intf.xml.ITransformable;
import org.joval.scap.ScapException;
import org.joval.scap.ScapFactory;
import org.joval.scap.diagnostics.CheckDiagnostics;
import org.joval.scap.diagnostics.RuleDiagnostics;
import org.joval.scap.oval.OvalFactory;
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
    /**
     * URI for the SCE results schema.
     */
    public static final String SCERES = "http://open-scap.org/page/SCE_result_file";

    /**
     * URI for the OVAL results schema.
     */
    public static final String OVALRES = "http://oval.mitre.org/XMLSchema/oval-results-5";

    public static final AssetReportCollection getAssetReportCollection(File f) throws ArfException {
	return getAssetReportCollection(new StreamSource(f));
    }

    public static final AssetReportCollection getAssetReportCollection(InputStream in) throws ArfException {
	return getAssetReportCollection(new StreamSource(in));
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
    private HashMap<String, String> requestXrefs;
    private HashMap<String, AssetType> assets;
    private HashMap<String, Element> reports;

    /**
     * Create a report based on an existing AssetReportCollection.
     */
    public Report(AssetReportCollection arc) {
	this.arc = arc;
	requests = new HashMap<String, Element>();
	for (ReportRequestType req : arc.getReportRequests().getReportRequest()) {
	    requests.put(req.getId(), (Element)req.getContent().getAny());
	}
	assets = new HashMap<String, AssetType>();
	for (AssetReportCollection.Assets.Asset asset : arc.getAssets().getAsset()) {
	    assets.put(asset.getId(), asset.getAsset().getValue());
	}
	reports = new HashMap<String, Element>();
	for (ReportType report : arc.getReports().getReport()) {
	    reports.put(report.getId(), (Element)report.getContent().getAny());
	}
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

    public Collection<String> getBenchmarkIds() {
	if (requestXrefs == null) {
	    initRequestXrefs();
	}
	return requestXrefs.values();
    }

    public BenchmarkType getBenchmark(String benchmarkId) throws NoSuchElementException {
	if (requestXrefs == null) {
	    initRequestXrefs();
	}
	try {
	    for (Map.Entry<String, String> entry : requestXrefs.entrySet()) {
		if (benchmarkId.equals(entry.getValue())) {
		    Unmarshaller unmarshaller = SchemaRegistry.XCCDF.getJAXBContext().createUnmarshaller();
		    return (BenchmarkType)unmarshaller.unmarshal(requests.get(entry.getKey()));
		}
	    }
	} catch (JAXBException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	throw new NoSuchElementException(benchmarkId);
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
				    profileId == null ? !tr.isSetProfile() : profileId.equals(tr.getProfile().getIdref())) {
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

    public Collection<RuleDiagnostics> getDiagnostics(String assetId, String benchmarkId, String profileId)
		throws NoSuchElementException, ScapException {

	Map<String, Object> reports = resolveCatalog(assetId);
	ArrayList<RuleDiagnostics> results = new ArrayList<RuleDiagnostics>();
	for (RuleResultType result : getTestResult(assetId, benchmarkId, profileId).getRuleResult()) {
	    results.add(getDiagnostics(result, reports));
	}
	return results;
    }

    public RuleDiagnostics getDiagnostics(String assetId, String benchmarkId, String profileId, String ruleId)
		throws NoSuchElementException, ScapException {

	Catalog catalog = getCatalog(assetId);
	RuleResultType rrt = null;
	for (RuleResultType result : getTestResult(assetId, benchmarkId, profileId).getRuleResult()) {
	    if (result.getIdref().equals(ruleId)) {
		return getDiagnostics(result, catalog);
	    }
	}
	throw new NoSuchElementException(ruleId);
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

    public Catalog getCatalog(String assetId) throws ArfException, NoSuchElementException {
	//
	// Find the IDs of all the reports about the specified asset
	//
	HashSet<String> reports = new HashSet<String>();
	for (RelationshipType rel : arc.getRelationships().getRelationship()) {
	    if (rel.getType().equals(Factories.IS_ABOUT)) {
		if (rel.getRef().contains(assetId)) {
		    reports.add(rel.getSubject());
		}
	    }
	}
	//
	// Create a new catalog limited to the report subset.
	//
	Catalog result = Factories.catalog.createCatalog();
	for (Object obj : getCatalog().getPublicOrSystemOrUri()) {
	    obj = ((JAXBElement)obj).getValue();
	    if (obj instanceof Uri) {
		Uri uri = (Uri)obj;
		if (reports.contains(uri.getName())) {
		    result.getPublicOrSystemOrUri().add(uri);
		}
	    }
	}
	return result;
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

    private void initRequestXrefs() {
	requestXrefs = new HashMap<String, String>();
	try {
	    Unmarshaller unmarshaller = SchemaRegistry.XCCDF.getJAXBContext().createUnmarshaller();
	    for (Map.Entry<String, Element> entry : requests.entrySet()) {
		BenchmarkType bt = (BenchmarkType)unmarshaller.unmarshal(entry.getValue());
		requestXrefs.put(entry.getKey(), bt.getBenchmarkId());
	    }
	} catch (Exception e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }

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

    /**
     * Get a map, indexed by href, of unmarshalled JAXB elements.
     */
    private Map<String, Object> resolveCatalog(String assetId) throws ScapException {
	Map<String, Object> resolved = new HashMap<String, Object>();
	for (Object obj : getCatalog(assetId).getPublicOrSystemOrUri()) {
	    obj = ((JAXBElement)obj).getValue();
	    if (obj instanceof Uri) {
		Uri uri = (Uri)obj;
		String report_id = uri.getName();
		String href = uri.getUri().substring(1);
		Element subreport = reports.get(report_id);
		String namespaceURI = subreport.getNamespaceURI();
		try {
		    if (SystemEnumeration.OCIL.namespace().equals(namespaceURI)) {
			Unmarshaller unmarshaller = SchemaRegistry.OCIL.getJAXBContext().createUnmarshaller();
			OCILType ocil = (OCILType)((JAXBElement)unmarshaller.unmarshal(subreport)).getValue();
			resolved.put(href, ocil);
		    } else if (OVALRES.equals(namespaceURI)) {
			Unmarshaller unmarshaller = SchemaRegistry.OVAL_RESULTS.getJAXBContext().createUnmarshaller();
			OvalResults oval = (OvalResults)unmarshaller.unmarshal(subreport);
			resolved.put(href, oval);
		    } else if (SCERES.equals(namespaceURI)) {
			Unmarshaller unmarshaller = SchemaRegistry.SCE.getJAXBContext().createUnmarshaller();
			SceResultsType sceres = (SceResultsType)((JAXBElement)unmarshaller.unmarshal(subreport)).getValue();
			resolved.put(href, sceres);
		    } else if (SystemEnumeration.XCCDF.namespace().equals(namespaceURI)) {
			Unmarshaller unmarshaller = SchemaRegistry.XCCDF.getJAXBContext().createUnmarshaller();
			TestResultType xccdf = (TestResultType)((JAXBElement)unmarshaller.unmarshal(subreport)).getValue();
			resolved.put(href, xccdf);
		    }
		} catch (JAXBException e) {
		    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		    throw new ArfException(e);
		}
	    }
	}
	return resolved;
    }

    /**
     * Get diagnostics for the specified rule, given a pre-resolved map of reports.
     */
    private RuleDiagnostics getDiagnostics(RuleResultType rrt, Map<String, Object> resolved) throws ScapException {
	RuleDiagnostics rd = new RuleDiagnostics();
	rd.setRuleResult(rrt);
	rd.setRuleId(rrt.getIdref());

	//
	// Create a collection of checks that apply to the selected rule
	//
	Collection<CheckType> checks = new ArrayList<CheckType>();
	if (rrt.isSetCheck()) {
	    checks.add(rrt.getCheck().get(0));
	} else if (rrt.isSetComplexCheck()) {
	    checks.addAll(getChecks(rrt.getComplexCheck()));
	}

	for (CheckType check : checks) {
	    CheckDiagnostics cd = new CheckDiagnostics();
	    String system = check.getSystem();
	    String href = check.getCheckContentRef().get(0).getHref();
	    String name = check.getCheckContentRef().get(0).getName();
	    cd.setCheck(check);
	    Object report = resolved.get(href);
	    if (SystemEnumeration.OCIL.namespace().equals(system) && report instanceof OCILType) {
		OCILType ocil = (OCILType)report;
		setDiagnosticInfo(cd, ScapFactory.createChecklist(ocil), name);
	    } else if (SystemEnumeration.OVAL.namespace().equals(system) && report instanceof OvalResults) {
		OvalResults oval = (OvalResults)report;
		if (name == null) {
		    cd.setDefinitions(oval.getOvalDefinitions().getDefinitions());
		    cd.setTests(oval.getOvalDefinitions().getTests());
		    cd.setObjects(oval.getOvalDefinitions().getObjects());
		    cd.setStates(oval.getOvalDefinitions().getStates());
		    SystemType st = oval.getResults().getSystem().get(0);
		    cd.setDefinitionResults(st.getDefinitions());
		    cd.setTestResults(st.getTests());
		    cd.setCollectedObjects(st.getOvalSystemCharacteristics().getCollectedObjects());
		    cd.setItems(st.getOvalSystemCharacteristics().getSystemData());
		} else {
		    cd.setDefinitions(org.joval.scap.oval.Factories.definitions.core.createDefinitionsType());
		    cd.setDefinitionResults(org.joval.scap.oval.Factories.results.createDefinitionsType());
		    cd.setTests(org.joval.scap.oval.Factories.definitions.core.createTestsType());
		    cd.setTestResults(org.joval.scap.oval.Factories.results.createTestsType());
		    cd.setObjects(org.joval.scap.oval.Factories.definitions.core.createObjectsType());
		    cd.setCollectedObjects(org.joval.scap.oval.Factories.sc.core.createCollectedObjectsType());
		    cd.setStates(org.joval.scap.oval.Factories.definitions.core.createStatesType());
		    cd.setItems(org.joval.scap.oval.Factories.sc.core.createSystemDataType());
		    setDiagnosticInfo(cd, OvalFactory.createResults(oval), name);
		}
	    } else if (SystemEnumeration.SCE.namespace().equals(system) && report instanceof SceResultsType) {
		cd.setSceResults((SceResultsType)report);
	    }
	    rd.getCheckDiagnostics().add(cd);
	}
	return rd;
    }

    /**
     * Get diagnostics for the specified rule, given a catalog.
     */
    private RuleDiagnostics getDiagnostics(RuleResultType rrt, Catalog catalog) throws ScapException {
	RuleDiagnostics rd = new RuleDiagnostics();
	rd.setRuleResult(rrt);
	rd.setRuleId(rrt.getIdref());

	//
	// Create a collection of checks that apply to the selected rule
	//
	Collection<CheckType> checks = new ArrayList<CheckType>();
	if (rrt.isSetCheck()) {
	    checks.add(rrt.getCheck().get(0));
	} else if (rrt.isSetComplexCheck()) {
	    checks.addAll(getChecks(rrt.getComplexCheck()));
	}

	for (CheckType check : checks) {
	    CheckDiagnostics cd = new CheckDiagnostics();
	    cd.setCheck(check);

	    String system = check.getSystem();
	    String href = check.getCheckContentRef().get(0).getHref();
	    String name = check.getCheckContentRef().get(0).getName();
	    String report_id = null;
	    for (Object obj : catalog.getPublicOrSystemOrUri()) {
		if (obj instanceof JAXBElement) {
		    obj = ((JAXBElement)obj).getValue();
		}
		if (obj instanceof Uri) {
		    Uri uri = (Uri)obj;
		    if (uri.getUri().substring(1).equals(href)) {
			report_id = uri.getName();
			break;
		    }
		}
	    }
	    if (report_id != null && reports.containsKey(report_id)) {
		Element subreport = reports.get(report_id);
		try {
		    if (SystemEnumeration.OCIL.namespace().equals(system)) {
			Unmarshaller unmarshaller = SchemaRegistry.OCIL.getJAXBContext().createUnmarshaller();
			OCILType ocil = (OCILType)((JAXBElement)unmarshaller.unmarshal(subreport)).getValue();
			setDiagnosticInfo(cd, ScapFactory.createChecklist(ocil), name);
		    } else if (SystemEnumeration.OVAL.namespace().equals(system)) {
			Unmarshaller unmarshaller = SchemaRegistry.OVAL_RESULTS.getJAXBContext().createUnmarshaller();
			OvalResults oval = (OvalResults)unmarshaller.unmarshal(subreport);
			if (name == null) {
			    cd.setDefinitions(oval.getOvalDefinitions().getDefinitions());
			    cd.setTests(oval.getOvalDefinitions().getTests());
			    cd.setObjects(oval.getOvalDefinitions().getObjects());
			    cd.setStates(oval.getOvalDefinitions().getStates());
			    SystemType st = oval.getResults().getSystem().get(0);
			    cd.setDefinitionResults(st.getDefinitions());
			    cd.setTestResults(st.getTests());
			    cd.setCollectedObjects(st.getOvalSystemCharacteristics().getCollectedObjects());
			    cd.setItems(st.getOvalSystemCharacteristics().getSystemData());
			} else {
			    cd.setDefinitions(org.joval.scap.oval.Factories.definitions.core.createDefinitionsType());
			    cd.setDefinitionResults(org.joval.scap.oval.Factories.results.createDefinitionsType());
			    cd.setTests(org.joval.scap.oval.Factories.definitions.core.createTestsType());
			    cd.setTestResults(org.joval.scap.oval.Factories.results.createTestsType());
			    cd.setObjects(org.joval.scap.oval.Factories.definitions.core.createObjectsType());
			    cd.setCollectedObjects(org.joval.scap.oval.Factories.sc.core.createCollectedObjectsType());
			    cd.setStates(org.joval.scap.oval.Factories.definitions.core.createStatesType());
			    cd.setItems(org.joval.scap.oval.Factories.sc.core.createSystemDataType());
			    setDiagnosticInfo(cd, OvalFactory.createResults(oval), name);
			}
		    } else if (SystemEnumeration.SCE.namespace().equals(system)) {
			Unmarshaller unmarshaller = SchemaRegistry.SCE.getJAXBContext().createUnmarshaller();
			JAXBElement elt = (JAXBElement)unmarshaller.unmarshal(subreport);
			cd.setSceResults((SceResultsType)elt.getValue());
		    }
		} catch (NoSuchElementException e) {
		    logger.warn(e.getMessage());
		} catch (JAXBException e) {
		    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		    throw new ArfException(e);
		}
		rd.getCheckDiagnostics().add(cd);
	    }
	}
	return rd;
    }

    /**
     * Find diagnostic information for the specified OCIL questionnaire in the checklist.
     */
    private void setDiagnosticInfo(CheckDiagnostics cd, IChecklist checklist, String questionnaireId) {
	OCILType ocil = checklist.getOCILType();
	if (!ocil.isSetResults()) {
	    return;
	}

	cd.setTargets(ocil.getResults().getTargets());
	OcilResultDiagnostics ord = null;
	if (ocil.getGenerator().isSetAdditionalData()) {
	    for (Object obj : ocil.getGenerator().getAdditionalData().getAny()) {
		if (obj instanceof OcilResultDiagnostics) {
		    ord = (OcilResultDiagnostics)obj;
		    break;
		}
	    }
	}
	if (ord == null) return;

	//
	// Create maps of questionnaire results (indexed by questionnaire ID) and question answers (indexed by question ID)
	// from the checklist results.
	//
	Map<String, String> questionnaireResults = new HashMap<String, String>();
	for (QuestionnaireResultType qr :
		checklist.getOCILType().getResults().getQuestionnaireResults().getQuestionnaireResult()) {

	    questionnaireResults.put(qr.getQuestionnaireRef(), qr.getResult());
	}
	Map<String, String> questionResults = new HashMap<String, String>();
	for (JAXBElement<? extends QuestionResultType> elt : ocil.getResults().getQuestionResults().getQuestionResult()) {
	    QuestionResultType qrt = elt.getValue();
	    String id = qrt.getQuestionRef();
	    if (qrt instanceof BooleanQuestionResultType) {
		questionResults.put(id, ((BooleanQuestionResultType)qrt).getAnswer().toString());
	    } else if (qrt instanceof ChoiceQuestionResultType) {
		ChoiceType choice = checklist.getChoice(((ChoiceQuestionResultType)qrt).getAnswer().getChoiceRef());
		if (choice.isSetValue()) {
		    questionResults.put(id, choice.getValue());
		} else if (choice.isSetVarRef()) {
		    questionResults.put(id, choice.getVarRef()); // TBD: get value from somewhere?
		}
	    } else if (qrt instanceof NumericQuestionResultType) {
		questionResults.put(id, ((NumericQuestionResultType)qrt).getAnswer().toString());
	    } else if (qrt instanceof StringQuestionResultType) {
		questionResults.put(id, ((StringQuestionResultType)qrt).getAnswer());
	    }
	}

	//
	// Starting with the specified questionnaire ID, determine all the questionnaires required to determine
	// its result.  This is the scope of our diagnostic information.
	//
	Map<String, QuestionnaireType> questionnaires = new HashMap<String, QuestionnaireType>();
	for (QuestionnaireType questionnaire : ord.getQuestionnaire()) {
	    questionnaires.put(questionnaire.getId(), questionnaire);
	}
	HashSet<String> scope = new HashSet<String>();
	if (questionnaireId == null) {
	    //
	    // If no questionnaire is specified, then the scope is all questionnaires in the document
	    //
	    scope.addAll(questionnaires.keySet());
	} else {
	    findDependencies(scope, questionnaireId, questionnaires);
	}

	//
	// Create a diagnostic object, and add supplementary information
	//
	OcilResultDiagnostics diagnostics = new OcilResultDiagnostics();
	for (String id : scope) {
	    QuestionnaireType questionnaire = questionnaires.get(id);
	    questionnaire.setResult(questionnaireResults.get(id));
	    questionnaire.setTitle(checklist.getQuestionnaire(id).getTitle());
	    for (ActionSequenceType sequence : questionnaire.getActions().getTestActionSequence()) {
		for (JAXBElement<? extends ActionType> elt : sequence.getAction()) {
		    ActionType action = elt.getValue();
		    if (action instanceof QuestionRef) {
			QuestionRef ref = (QuestionRef)action;
			QuestionTestActionType qta = checklist.getQuestionTestAction(ref.getId());
			String questionId = qta.getQuestionRef();
			ref.setAnswer(questionResults.get(questionId));
			ref.getQuestionText().addAll(checklist.getQuestion(questionId).getQuestionText());
		    }
		}
	    }
	    diagnostics.getQuestionnaire().add(questionnaire);
	}

	cd.setOcilResultDiagnostics(diagnostics);
    }

    /**
     * Find diagnostic information for the specified OVAL definition in the results.
     */
    private void setDiagnosticInfo(CheckDiagnostics cd, IResults results, String definitionId) {
	IDefinitions definitions = results.getDefinitions();

	//
	// Recursively find all the definitions and tests from the OVAL IDefinitions.
	//
	Map<String, DefinitionType> defMap = new HashMap<String, DefinitionType>();
	DefinitionType rootDefinition = definitions.getDefinition(definitionId);
	defMap.put(definitionId, rootDefinition);
	Map<String, JAXBElement<? extends TestType>> testMap = new HashMap<String, JAXBElement<? extends TestType>>();
	collectDefinitionsAndTests(rootDefinition.getCriteria(), definitions, defMap, testMap);

	//
	// Collect all the objects and states that are referenced by the referenced tests
	//
	Map<String, JAXBElement<? extends ObjectType>> objectMap = new HashMap<String, JAXBElement<? extends ObjectType>>();
	Map<String, JAXBElement<? extends StateType>> stateMap = new HashMap<String, JAXBElement<? extends StateType>>();
	collectObjectsAndStates(testMap.keySet(), definitions, objectMap, stateMap);

	//
	// Add all the test, object, state and def definitions and results to the diagnostic XML.
	//
	for (DefinitionType definition : defMap.values()) {
	    cd.getDefinitions().getDefinition().add(definition);
	    cd.getDefinitionResults().getDefinition().add(results.getDefinition(definition.getId()));
	}
	for (JAXBElement<? extends TestType> test : testMap.values()) {
	    TestType tt = test.getValue();
	    cd.getTests().getTest().add(test);
	    cd.getTestResults().getTest().add(results.getTest(tt.getId()));
	}
	//
	// Add objects while collecting items in a map
	//
	Map<BigInteger, JAXBElement<? extends ItemType>> itemMap = new HashMap<BigInteger, JAXBElement<? extends ItemType>>();
	for (Map.Entry<String, JAXBElement<? extends ObjectType>> entry : objectMap.entrySet()) {
	    String objectId = entry.getKey();
	    JAXBElement<? extends ObjectType> object = entry.getValue();
	    cd.getObjects().getObject().add(object);
	    ISystemCharacteristics sc = results.getSystemCharacteristics();
	    if (sc.containsObject(objectId)) {
		cd.getCollectedObjects().getObject().add(sc.getObject(objectId));
		for (JAXBElement<? extends ItemType> item : sc.getItemsByObjectId(objectId)) {
		    itemMap.put(item.getValue().getId(), item);
		}
	    }
	}
	for (JAXBElement<? extends StateType> state : stateMap.values()) {
	    cd.getStates().getState().add(state);
	}
	for (JAXBElement<? extends ItemType> item : itemMap.values()) {
	    cd.getItems().getItem().add(item);
	}
    }

    private void findDependencies(HashSet<String> scope, String id, Map<String, QuestionnaireType> questionnaires) {
	scope.add(id);
	for (ActionSequenceType seq : questionnaires.get(id).getActions().getTestActionSequence()) {
	    for (JAXBElement<? extends ActionType> elt : seq.getAction()) {
		ActionType action = elt.getValue();
		if (action instanceof QuestionnaireRef) {
		    findDependencies(scope, ((QuestionnaireRef)action).getId(), questionnaires);
		}
	    }
	}
    }

    private void collectDefinitionsAndTests(CriteriaType criteria, IDefinitions definitions,
		Map<String, DefinitionType> defMap, Map<String, JAXBElement<? extends TestType>> testMap) {

	for (Object obj : criteria.getCriteriaOrCriterionOrExtendDefinition()) {
	    if (obj instanceof CriterionType) {
		CriterionType criterion = (CriterionType)obj;
		String testId = criterion.getTestRef();
		if (!testMap.containsKey(testId)) {
		    testMap.put(testId, definitions.getTest(testId));
		}
	    } else if (obj instanceof CriteriaType) {
		collectDefinitionsAndTests((CriteriaType)obj, definitions, defMap, testMap);
	    } else if (obj instanceof ExtendDefinitionType) {
		String definitionId = ((ExtendDefinitionType)obj).getDefinitionRef();
		DefinitionType definition = definitions.getDefinition(definitionId);
		if (!defMap.containsKey(definitionId)) {
		    defMap.put(definitionId, definition);
		    collectDefinitionsAndTests(definition.getCriteria(), definitions, defMap, testMap);
		}
	    }
	}
    }

    private void collectObjectsAndStates(Collection<String> testIds, IDefinitions definitions,
		Map<String, JAXBElement<? extends ObjectType>> objMap, Map<String, JAXBElement<? extends StateType>> stateMap) {

	for (String testId : testIds) {
	    TestType test = definitions.getTest(testId).getValue();
	    String objectId = getObjectRef(test);
	    if (objectId != null) {
		objMap.put(objectId, definitions.getObject(objectId));
	    }
	    for (String stateId : getStateRef(test)) {
		stateMap.put(stateId, definitions.getState(stateId));
	    }
	}
    }

    private String getObjectRef(TestType test) {
	String result = null;
	try {
	    Class<?> clazz = test.getClass();
	    Method method = clazz.getMethod("getObject");
	    Object obj = method.invoke(test);
	    if (obj instanceof ObjectRefType) {
		result = ((ObjectRefType)obj).getObjectRef();
	    }
	} catch (Exception e) {
	}
	return result;
    }

    private List<String> getStateRef(TestType test) {
	List<String> result = new ArrayList<String>();
	try {
	    Class<?> clazz = test.getClass();
	    Method method = clazz.getMethod("getState");
	    Object obj = method.invoke(test);
	    if (obj instanceof List) {
		for (Object elt : (List)obj) {
		    if (elt instanceof StateRefType) {
			result.add(((StateRefType)elt).getStateRef());
		    }
		}
	    }
	} catch (Exception e) {
	}
	return result;
    }

    private Collection<CheckType> getChecks(ComplexCheckType complex) {
	Collection<CheckType> checks = new ArrayList<CheckType>();
	for (Object obj : complex.getCheckOrComplexCheck()) {
	    if (obj instanceof CheckType) {
		checks.add((CheckType)obj);
	    } else if (obj instanceof ComplexCheckType) {
		checks.addAll(getChecks((ComplexCheckType)obj));
	    }
	}
	return checks;
    }
}
