// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.windows;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import jsaf.intf.io.IFile;
import jsaf.intf.io.IFileEx;
import jsaf.intf.io.IRandomAccess;
import jsaf.intf.system.ISession;
import jsaf.intf.windows.io.IWindowsFileInfo;
import jsaf.intf.windows.system.IWindowsSession;
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
	Date temp = f.getAccessTime();
	if (temp == null) {
	    aTimeType.setStatus(StatusEnumeration.NOT_COLLECTED);
	} else {
	    aTimeType.setValue(Timestamp.toWindowsTimestamp(temp.getTime()));
	    aTimeType.setDatatype(SimpleDatatypeEnumeration.INT.value());
	}
	item.setATime(aTimeType);

	EntityItemIntType cTimeType = Factories.sc.core.createEntityItemIntType();
	temp = f.getCreateTime();
	if (temp == null) {
	    cTimeType.setStatus(StatusEnumeration.NOT_COLLECTED);
	} else {
	    cTimeType.setValue(Timestamp.toWindowsTimestamp(temp.getTime()));
	    cTimeType.setDatatype(SimpleDatatypeEnumeration.INT.value());
	}
	item.setCTime(cTimeType);

	EntityItemIntType mTimeType = Factories.sc.core.createEntityItemIntType();
	temp = f.getLastModified();
	if (temp == null) {
	    mTimeType.setStatus(StatusEnumeration.NOT_COLLECTED);
	} else {
	    mTimeType.setValue(Timestamp.toWindowsTimestamp(temp.getTime()));
	    mTimeType.setDatatype(SimpleDatatypeEnumeration.INT.value());
	}
	item.setMTime(mTimeType);

	EntityItemStringType ownerType = Factories.sc.core.createEntityItemStringType();
	ownerType.setValue(((IWindowsFileInfo)f.getExtended()).getOwner().getNetbiosName());
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

    // Private

    /**
     * Read the Portable Execution format header information and extract data from it.
     */
    private void addHeaderInfo(FileObject fObj, IFile file, FileItem item) throws IOException {
	session.getLogger().trace(JOVALMsg.STATUS_PE_READ, file.toString());
	Map<String, String> peHeaders = ((IWindowsFileInfo)file.getExtended()).getPEHeaders();
	if (peHeaders != null) {
	    if (peHeaders.containsKey(IWindowsFileInfo.PE_MS_CHECKSUM)) {
		EntityItemStringType msChecksumType = Factories.sc.core.createEntityItemStringType();
		msChecksumType.setValue(peHeaders.get(IWindowsFileInfo.PE_MS_CHECKSUM));
		item.setMsChecksum(msChecksumType);
	    }

	    if (peHeaders.containsKey(IWindowsFileInfo.PE_LANGUAGE)) {
		EntityItemStringType languageType = Factories.sc.core.createEntityItemStringType();
		languageType.setValue(peHeaders.get(IWindowsFileInfo.PE_LANGUAGE));
		item.setLanguage(languageType);
	    }

	    if (peHeaders.containsKey(IWindowsFileInfo.PE_VERSION_MAJOR_PART)) {
		int major = Integer.parseInt(peHeaders.get(IWindowsFileInfo.PE_VERSION_MAJOR_PART));
		int minor = 0, build = 0, priv = 0;
		if (peHeaders.containsKey(IWindowsFileInfo.PE_VERSION_MINOR_PART)) {
		    minor = Integer.parseInt(peHeaders.get(IWindowsFileInfo.PE_VERSION_MINOR_PART));
		}
		if (peHeaders.containsKey(IWindowsFileInfo.PE_VERSION_BUILD_PART)) {
		    build = Integer.parseInt(peHeaders.get(IWindowsFileInfo.PE_VERSION_BUILD_PART));
		}
		if (peHeaders.containsKey(IWindowsFileInfo.PE_VERSION_PRIVATE_PART)) {
		    priv = Integer.parseInt(peHeaders.get(IWindowsFileInfo.PE_VERSION_PRIVATE_PART));
		}
		if (major != 0 || minor != 0 || build != 0 || priv != 0) {
		    EntityItemVersionType versionType = Factories.sc.core.createEntityItemVersionType();
		    versionType.setValue(new Version(major, minor, build, priv).toString());
		    versionType.setDatatype(SimpleDatatypeEnumeration.VERSION.value());
		    item.setFileVersion(versionType);
		}
	    }

	    if (peHeaders.containsKey(IWindowsFileInfo.PE_COMPANY_NAME)) {
		EntityItemStringType companyType = Factories.sc.core.createEntityItemStringType();
		companyType.setValue(peHeaders.get(IWindowsFileInfo.PE_COMPANY_NAME));
		item.setCompany(companyType);
	    }

	    if (peHeaders.containsKey(IWindowsFileInfo.PE_INTERNAL_NAME)) {
		EntityItemStringType internalNameType = Factories.sc.core.createEntityItemStringType();
		internalNameType.setValue(peHeaders.get(IWindowsFileInfo.PE_INTERNAL_NAME));
		item.setInternalName(internalNameType);
	    }

	    if (peHeaders.containsKey(IWindowsFileInfo.PE_PRODUCT_NAME)) {
	 	EntityItemStringType productNameType = Factories.sc.core.createEntityItemStringType();
		productNameType.setValue(peHeaders.get(IWindowsFileInfo.PE_PRODUCT_NAME));
		item.setProductName(productNameType);
	    }

	    if (peHeaders.containsKey(IWindowsFileInfo.PE_ORIGINAL_NAME)) {
		EntityItemStringType originalFilenameType = Factories.sc.core.createEntityItemStringType();
		originalFilenameType.setValue(peHeaders.get(IWindowsFileInfo.PE_ORIGINAL_NAME));
		item.setOriginalFilename(originalFilenameType);
	    }

	    if (peHeaders.containsKey(IWindowsFileInfo.PE_PRODUCT_VERSION)) {
		EntityItemVersionType productVersionType = Factories.sc.core.createEntityItemVersionType();
		productVersionType.setValue(peHeaders.get(IWindowsFileInfo.PE_PRODUCT_VERSION));
		productVersionType.setDatatype(SimpleDatatypeEnumeration.VERSION.value());
		item.setProductVersion(productVersionType);
	    }

	    if (peHeaders.containsKey(IWindowsFileInfo.PE_VERSION)) {
		try {
		    EntityItemStringType developmentClassType = Factories.sc.core.createEntityItemStringType();
		    developmentClassType.setValue(getDevelopmentClass(peHeaders.get(IWindowsFileInfo.PE_VERSION)));
		    item.setDevelopmentClass(developmentClassType);
		} catch (IllegalArgumentException e) {
		}
	    }
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
