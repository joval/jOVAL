// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.windows;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import jsaf.intf.io.IFile;
import jsaf.intf.io.IFileEx;
import jsaf.intf.io.IRandomAccess;
import jsaf.intf.system.ISession;
import jsaf.intf.windows.io.IWindowsFileInfo;
import jsaf.intf.windows.powershell.IRunspace;
import jsaf.intf.windows.system.IWindowsSession;
import jsaf.intf.windows.wmi.ISWbemObject;
import jsaf.intf.windows.wmi.ISWbemObjectSet;
import jsaf.intf.windows.wmi.ISWbemProperty;
import jsaf.intf.windows.wmi.ISWbemPropertySet;
import jsaf.intf.windows.wmi.IWmiProvider;
import jsaf.provider.windows.Timestamp;
import jsaf.util.StringTools;

import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.MessageType;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.windows.FileObject;
import scap.oval.systemcharacteristics.core.EntityItemIntType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.EntityItemVersionType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.windows.EntityItemFileTypeType;
import scap.oval.systemcharacteristics.windows.FileItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.scap.oval.adapter.independent.BaseFileAdapter;
import org.joval.util.JOVALMsg;
import org.joval.util.Version;

/**
 * Collects Windows FileItems.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class FileAdapter extends BaseFileAdapter<FileItem> {
    private static final String OWNER_WQL = "ASSOCIATORS OF {Win32_LogicalFileSecuritySetting='$path'} " +
					    "WHERE AssocClass=Win32_LogicalFileOwner ResultRole=Owner";

    private IWindowsSession ws;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IWindowsSession) {
	    baseInit(session);
	    ws = (IWindowsSession)session;
	    classes.add(FileObject.class);
	} else {
	    notapplicable.add(FileObject.class);
	}
	return classes;
    }

    // Protected

    protected Class getItemClass() {
	return FileItem.class;
    }

    protected Collection<FileItem> getItems(ObjectType obj, ItemType base, IFile f, IRequestContext rc)
		throws IOException, CollectException {

	FileObject fObj = (FileObject)obj;
	FileItem baseItem = (FileItem)base;
	FileItem item = Factories.sc.windows.createFileItem();
	item.setPath(baseItem.getPath());
	item.setFilename(baseItem.getFilename());
	item.setFilepath(baseItem.getFilepath());
	item.setWindowsView(baseItem.getWindowsView());

	//
	// Get some information from the IFile
	//
	item.setStatus(StatusEnumeration.EXISTS);
	EntityItemFileTypeType typeType = Factories.sc.windows.createEntityItemFileTypeType();
	switch(((IWindowsFileInfo)f.getExtended()).getWindowsFileType()) {
	  case IWindowsFileInfo.FILE_ATTRIBUTE_DIRECTORY:
	    typeType.setValue("FILE_ATTRIBUTE_DIRECTORY");
	    break;
	  case IWindowsFileInfo.FILE_TYPE_DISK:
	    typeType.setValue("FILE_TYPE_DISK");
	    break;
	  case IWindowsFileInfo.FILE_TYPE_REMOTE:
	    typeType.setValue("FILE_TYPE_REMOTE");
	    break;
	  case IWindowsFileInfo.FILE_TYPE_PIPE:
	    typeType.setValue("FILE_TYPE_PIPE");
	    break;
	  case IWindowsFileInfo.FILE_TYPE_CHAR:
	    typeType.setValue("FILE_TYPE_CHAR");
	    break;
	  default:
	    typeType.setValue("FILE_TYPE_UNKNOWN");
	    break;
	}
	item.setType(typeType);

	EntityItemIntType aTimeType = Factories.sc.core.createEntityItemIntType();
	aTimeType.setDatatype(SimpleDatatypeEnumeration.INT.value());
	aTimeType.setValue(Timestamp.toWindowsTimestamp(f.accessTime()));
	item.setATime(aTimeType);

	EntityItemIntType cTimeType = Factories.sc.core.createEntityItemIntType();
	cTimeType.setDatatype(SimpleDatatypeEnumeration.INT.value());
	cTimeType.setValue(Timestamp.toWindowsTimestamp(f.createTime()));
	item.setCTime(cTimeType);

	EntityItemIntType mTimeType = Factories.sc.core.createEntityItemIntType();
	mTimeType.setDatatype(SimpleDatatypeEnumeration.INT.value());
	mTimeType.setValue(Timestamp.toWindowsTimestamp(f.lastModified()));
	item.setMTime(mTimeType);

	//
	// Use WMI to retrieve owner information for the file
	//
	EntityItemStringType ownerType = Factories.sc.core.createEntityItemStringType();
	IWmiProvider wmi = ws.getWmiProvider();
	try {
	    String wql = OWNER_WQL.replaceAll("(?i)\\$path", Matcher.quoteReplacement(f.getPath()));
	    ISWbemObjectSet objSet = wmi.execQuery(IWmiProvider.CIMv2, wql);
	    if (objSet.getSize() == 1) {
		ISWbemObject ownerObj = objSet.iterator().next();
		ISWbemPropertySet ownerPropSet = ownerObj.getProperties();
		ISWbemProperty usernameProp = ownerPropSet.getItem("AccountName");
		String username = usernameProp.getValueAsString();
		ISWbemProperty domainProp = ownerPropSet.getItem("ReferencedDomainName"); 
		String domain = domainProp.getValueAsString();
		String ownerAccount = new StringBuffer(domain).append("\\").append(username).toString();
		ownerType.setValue(ownerAccount);
	    } else {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.INFO);
		msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_WINFILE_OWNER, objSet.getSize()));
		item.getMessage().add(msg);
		ownerType.setStatus(StatusEnumeration.ERROR);
	    }
	} catch (Exception e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.INFO);
	    msg.setValue(e.getMessage());
	    item.getMessage().add(msg);
	    ownerType.setStatus(StatusEnumeration.ERROR);
	}
	item.setOwner(ownerType);

	//
	// If possible, read the PE header information
	//
	if (f.isFile()) {
	    EntityItemIntType sizeType = Factories.sc.core.createEntityItemIntType();
	    sizeType.setValue(new Long(f.length()).toString());
	    sizeType.setDatatype(SimpleDatatypeEnumeration.INT.value());
	    item.setSize(sizeType);
	    if (f.length() > 0) {
		addHeaderInfo(fObj, f, item);
	    } else {
		session.getLogger().info(JOVALMsg.STATUS_EMPTY_FILE, f.toString());

		EntityItemVersionType versionType = Factories.sc.core.createEntityItemVersionType();
		versionType.setDatatype(SimpleDatatypeEnumeration.VERSION.value());
		versionType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
		item.setFileVersion(versionType);

		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.INFO);
		msg.setValue(JOVALMsg.getMessage(JOVALMsg.STATUS_PE_EMPTY));
		item.getMessage().add(msg);
	    }
	}

	return Arrays.asList(item);
    }

    @Override
    protected List<InputStream> getPowershellModules() {
	return Arrays.asList(getClass().getResourceAsStream("File.psm1"));
    }

    // Private

    /**
     * Read the Portable Execution format header information and extract data from it.
     */
    private void addHeaderInfo(FileObject fObj, IFile file, FileItem item) throws IOException {
	session.getLogger().trace(JOVALMsg.STATUS_PE_READ, file.toString());
	String error = null;
	try {
	    Map<String, String> props = new HashMap<String, String>();
	    IRunspace runspace = getRunspace(getView(fObj.getBehaviors()));
	    String data = runspace.invoke("Print-FileInfoEx -Path \"" + file.getPath() + "\"");
	    for (String line : data.split("\n")) {
		line = line.trim();
		int ptr = line.indexOf(":");
		if (ptr > 1) {
		    String key = line.substring(0,ptr);
		    String val = line.substring(ptr+1).trim();
		    if (val.length() > 0) {
			props.put(key, val);
		    }
		}
	    }

	    if (props.containsKey("MSChecksum")) {
		EntityItemStringType msChecksumType = Factories.sc.core.createEntityItemStringType();
		msChecksumType.setValue(props.get("MSChecksum"));
		item.setMsChecksum(msChecksumType);
	    }

	    if (props.containsKey("Language")) {
		EntityItemStringType languageType = Factories.sc.core.createEntityItemStringType();
		languageType.setValue(props.get("Language"));
		item.setLanguage(languageType);
	    }

	    if (props.containsKey("FileMajorPart")) {
		int major = Integer.parseInt(props.get("FileMajorPart"));
		int minor = 0, build = 0, priv = 0;
		if (props.containsKey("FileMinorPart")) {
		    minor = Integer.parseInt(props.get("FileMinorPart"));
		}
		if (props.containsKey("FileBuildPart")) {
		    build = Integer.parseInt(props.get("FileBuildPart"));
		}
		if (props.containsKey("FilePrivatePart")) {
		    priv = Integer.parseInt(props.get("FilePrivatePart"));
		}
		if (major != 0 || minor != 0 || build != 0 || priv != 0) {
		    EntityItemVersionType versionType = Factories.sc.core.createEntityItemVersionType();
		    versionType.setValue(new Version(major, minor, build, priv).toString());
		    versionType.setDatatype(SimpleDatatypeEnumeration.VERSION.value());
		    item.setFileVersion(versionType);
		}
	    }

	    if (props.containsKey("Company Name")) {
		EntityItemStringType companyType = Factories.sc.core.createEntityItemStringType();
		companyType.setValue(props.get("Company Name"));
		item.setCompany(companyType);
	    }

	    if (props.containsKey("Internal Name")) {
		EntityItemStringType internalNameType = Factories.sc.core.createEntityItemStringType();
		internalNameType.setValue(props.get("Internal Name"));
		item.setInternalName(internalNameType);
	    }

	    if (props.containsKey("Product Name")) {
	 	EntityItemStringType productNameType = Factories.sc.core.createEntityItemStringType();
		productNameType.setValue(props.get("Product Name"));
		item.setProductName(productNameType);
	    }

	    if (props.containsKey("Original Filename")) {
		EntityItemStringType originalFilenameType = Factories.sc.core.createEntityItemStringType();
		originalFilenameType.setValue(props.get("Original Filename"));
		item.setOriginalFilename(originalFilenameType);
	    }

	    if (props.containsKey("Product Version")) {
		EntityItemVersionType productVersionType = Factories.sc.core.createEntityItemVersionType();
		productVersionType.setValue(props.get("Product Version"));
		productVersionType.setDatatype(SimpleDatatypeEnumeration.VERSION.value());
		item.setProductVersion(productVersionType);
	    }

	    if (props.containsKey("File Version")) {
		try {
		    EntityItemStringType developmentClassType = Factories.sc.core.createEntityItemStringType();
		    developmentClassType.setValue(getDevelopmentClass(props.get("File Version")));
		    item.setDevelopmentClass(developmentClassType);
		} catch (IllegalArgumentException e) {
		}
	    }
	} catch (Exception e) {
	    error = e.getMessage();
	    session.getLogger().info(JOVALMsg.ERROR_PE, file.getPath(), error);
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);

	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.WARNING);
	    msg.setValue(e.getMessage());
	    item.getMessage().add(msg);
	}
    }

    /**
     * Retrieve the development class from the file_version string, if possible.
     */
    private String getDevelopmentClass(String fileVersion) throws IllegalArgumentException {
	int begin=0, end=0, trunc=0;
	begin = fileVersion.indexOf("(");
	if (begin != -1) {
	    end = fileVersion.indexOf(")");
	    if (end > begin) {
		String vs = fileVersion.substring(begin+1, end);
		trunc = vs.length() - 12; // strip off .XXXXXX-YYY
		if (trunc > 0) {
		    return vs.substring(0, trunc);
		}
	    }
	}
	throw new IllegalArgumentException(fileVersion);
    }
}
