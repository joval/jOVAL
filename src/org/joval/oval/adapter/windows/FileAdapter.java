// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.adapter.windows;

import java.io.IOException;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;

import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.MessageType;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.windows.FileObject;
import oval.schemas.systemcharacteristics.core.EntityItemIntType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.EntityItemVersionType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.windows.EntityItemFileTypeType;
import oval.schemas.systemcharacteristics.windows.FileItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFileEx;
import org.joval.intf.io.IRandomAccess;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.system.ISession;
import org.joval.intf.windows.io.IWindowsFileInfo;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.intf.windows.wmi.ISWbemObject;
import org.joval.intf.windows.wmi.ISWbemObjectSet;
import org.joval.intf.windows.wmi.ISWbemProperty;
import org.joval.intf.windows.wmi.ISWbemPropertySet;
import org.joval.intf.windows.wmi.IWmiProvider;
import org.joval.os.windows.Timestamp;
import org.joval.os.windows.pe.Header;
import org.joval.os.windows.pe.ImageDOSHeader;
import org.joval.os.windows.pe.ImageNTHeaders;
import org.joval.os.windows.pe.ImageDataDirectory;
import org.joval.os.windows.pe.LanguageConstants;
import org.joval.os.windows.pe.resource.ImageResourceDirectory;
import org.joval.os.windows.pe.resource.ImageResourceDirectoryEntry;
import org.joval.os.windows.pe.resource.ImageResourceDataEntry;
import org.joval.os.windows.pe.resource.Types;
import org.joval.os.windows.pe.resource.version.Var;
import org.joval.os.windows.pe.resource.version.VarFileInfo;
import org.joval.os.windows.pe.resource.version.VsFixedFileInfo;
import org.joval.os.windows.pe.resource.version.VsVersionInfo;
import org.joval.os.windows.pe.resource.version.StringFileInfo;
import org.joval.os.windows.pe.resource.version.StringTable;
import org.joval.os.windows.pe.resource.version.StringStructure;
import org.joval.oval.CollectException;
import org.joval.oval.Factories;
import org.joval.oval.adapter.independent.BaseFileAdapter;
import org.joval.util.JOVALMsg;
import org.joval.util.StringTools;
import org.joval.util.Version;

/**
 * Collects Windows FileItems.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class FileAdapter extends BaseFileAdapter<FileItem> {
    private static final String CIMV2		= "root\\cimv2";
    private static final String OWNER_WQL	= "ASSOCIATORS OF {Win32_LogicalFileSecuritySetting='$path'} " +
						  "WHERE AssocClass=Win32_LogicalFileOwner ResultRole=Owner";
    private static final String TIME_WQL	= "SELECT * FROM CIM_DataFile WHERE Name='$path'";

    private IWindowsSession ws;
    private IWmiProvider wmi;

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IWindowsSession) {
	    super.init((ISession)session);
	    ws = (IWindowsSession)session;
	    classes.add(FileObject.class);
	}
	return classes;
    }

    // Protected

    protected Class getItemClass() {
	return FileItem.class;
    }

    protected Collection<FileItem> getItems(ObjectType obj, ItemType base, IFile f, IRequestContext rc)
		throws IOException, CollectException {
	//
	// Always grab a fresh WMI provider in case there's been a reconnection since initialization.
	//
	wmi = ws.getWmiProvider();

	if (base instanceof FileItem) {
	    Collection<FileItem> items = new Vector<FileItem>();
	    IFileEx info = f.getExtended();
	    IWindowsFileInfo wfi;
	    if (info instanceof IWindowsFileInfo) {
		wfi = (IWindowsFileInfo)info;
	    } else {
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_WINFILE_TYPE, f.getClass().getName());
		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	    }
	    items.add(setItem((FileItem)base, f, wfi));
	    return items;
	} else {
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_ITEM, base.getClass().getName());
	    throw new CollectException(msg, FlagEnumeration.ERROR);
	}
    }

    // Private

    /**
     * Populate the FileItem with everything except the path, filename and filepath. 
     */
    private FileItem setItem(FileItem fItem, IFile file, IWindowsFileInfo info) throws IOException {
	//
	// Get some information from the IFile
	//
	fItem.setStatus(StatusEnumeration.EXISTS);
	EntityItemFileTypeType typeType = Factories.sc.windows.createEntityItemFileTypeType();
	switch(info.getWindowsFileType()) {
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
	fItem.setType(typeType);

	//
	// If possible, use WMI to retrieve owner, aTime, cTime and mTime values
	//
	EntityItemStringType ownerType = Factories.sc.core.createEntityItemStringType();
	EntityItemIntType aTimeType = Factories.sc.core.createEntityItemIntType();
	aTimeType.setDatatype(SimpleDatatypeEnumeration.INT.value());
	EntityItemIntType cTimeType = Factories.sc.core.createEntityItemIntType();
	cTimeType.setDatatype(SimpleDatatypeEnumeration.INT.value());
	EntityItemIntType mTimeType = Factories.sc.core.createEntityItemIntType();
	mTimeType.setDatatype(SimpleDatatypeEnumeration.INT.value());
	if (wmi == null) {
	    ownerType.setStatus(StatusEnumeration.NOT_COLLECTED);

	    long at = file.accessTime();
	    if (at == IFile.UNKNOWN_TIME) {
		aTimeType.setStatus(StatusEnumeration.NOT_COLLECTED);
	    } else {
		aTimeType.setValue(Timestamp.toWindowsTimestamp(at));
	    }
	    fItem.setATime(aTimeType);

	    long ct = file.createTime();
	    if (ct == IFile.UNKNOWN_TIME) {
		cTimeType.setStatus(StatusEnumeration.NOT_COLLECTED);
	    } else {
		cTimeType.setValue(Timestamp.toWindowsTimestamp(ct));
	    }
	    fItem.setCTime(cTimeType);

	    long lm = file.lastModified();
	    if (lm == IFile.UNKNOWN_TIME) {
		mTimeType.setStatus(StatusEnumeration.NOT_COLLECTED);
	    } else {
		mTimeType.setValue(Timestamp.toWindowsTimestamp(lm));
	    }
	    fItem.setMTime(mTimeType);
	} else {
	    try {
		String wql = OWNER_WQL.replaceAll("(?i)\\$path", Matcher.quoteReplacement(file.getPath()));
		ISWbemObjectSet objSet = wmi.execQuery(CIMV2, wql);
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
		    fItem.getMessage().add(msg);
		    ownerType.setStatus(StatusEnumeration.ERROR);
		}

		wql = TIME_WQL.replaceAll("(?i)\\$path", escapePath(Matcher.quoteReplacement(file.getPath())));
		objSet = wmi.execQuery(CIMV2, wql);
		if (objSet.getSize() == 1) {
		    ISWbemObject fileObj = objSet.iterator().next();
		    ISWbemPropertySet filePropSet = fileObj.getProperties();
		    ISWbemProperty aTimeProp = filePropSet.getItem("LastAccessed");
		    aTimeType.setValue(Timestamp.toWindowsTimestamp(aTimeProp.getValueAsString()));
		    fItem.setATime(aTimeType);
		    ISWbemProperty cTimeProp = filePropSet.getItem("InstallDate");
		    cTimeType.setValue(Timestamp.toWindowsTimestamp(cTimeProp.getValueAsString()));
		    fItem.setCTime(cTimeType);
		    ISWbemProperty mTimeProp = filePropSet.getItem("LastModified");
		    mTimeType.setValue(Timestamp.toWindowsTimestamp(mTimeProp.getValueAsString()));
		    fItem.setMTime(mTimeType);
		}
	    } catch (Exception e) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.INFO);
		msg.setValue(e.getMessage());
		fItem.getMessage().add(msg);
		ownerType.setStatus(StatusEnumeration.ERROR);
	    }
	}
	fItem.setOwner(ownerType);

	//
	// If possible, read the PE header information
	//
	if (file.isFile()) {
	    EntityItemIntType sizeType = Factories.sc.core.createEntityItemIntType();
	    sizeType.setValue(new Long(file.length()).toString());
	    sizeType.setDatatype(SimpleDatatypeEnumeration.INT.value());
	    fItem.setSize(sizeType);
	    if (file.length() > 0) {
		readPEHeader(file, fItem);
	    } else {
		session.getLogger().info(JOVALMsg.STATUS_EMPTY_FILE, file.toString());

		EntityItemVersionType versionType = Factories.sc.core.createEntityItemVersionType();
		versionType.setDatatype(SimpleDatatypeEnumeration.VERSION.value());
		versionType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
		fItem.setFileVersion(versionType);

		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.INFO);
		msg.setValue(JOVALMsg.getMessage(JOVALMsg.STATUS_PE_EMPTY));
		fItem.getMessage().add(msg);
	    }
	}

	return fItem;
    }

    /**
     * Read the Portable Execution format header information and extract data from it.
     */
    private void readPEHeader(IFile file, FileItem fItem) throws IOException {
	session.getLogger().trace(JOVALMsg.STATUS_PE_READ, file.toString());
	String error = null;
	try {
	    Header header = new Header(file, session.getLogger());

	    //
	    // Get the MS Checksum from the NT headers
	    //
	    EntityItemStringType msChecksumType = Factories.sc.core.createEntityItemStringType();
	    msChecksumType.setValue(new Integer(header.getNTHeader().getImageOptionalHeader().getChecksum()).toString());
	    fItem.setMsChecksum(msChecksumType);

	    String key = VsVersionInfo.LANGID_KEY;
	    VsVersionInfo versionInfo = header.getVersionInfo();
	    VsFixedFileInfo value = null;
	    Hashtable<String, String> stringTable = null;
	    if (versionInfo != null) {
		value = versionInfo.getValue();
		key = versionInfo.getDefaultTranslation();
	    }
	    stringTable = versionInfo.getStringTable(key);

	    //
	    // Get the language from the key
	    //
	    EntityItemStringType languageType = Factories.sc.core.createEntityItemStringType();
	    String locale = LanguageConstants.getLocaleString(key);
	    if (locale == null) {
		languageType.setStatus(StatusEnumeration.ERROR);
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.INFO);
		msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_WINFILE_LANGUAGE, key));
		fItem.getMessage().add(msg);
	    } else {
		languageType.setValue(LanguageConstants.getLocaleString(key));
	    }
	    fItem.setLanguage(languageType);

	    //
	    // Get the file version from the VsFixedFileInfo structure
	    //
	    EntityItemVersionType versionType = Factories.sc.core.createEntityItemVersionType();
	    if (value == null) {
		versionType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    } else {
		versionType.setValue(versionInfo.getValue().getFileVersion().toString());
		versionType.setDatatype(SimpleDatatypeEnumeration.VERSION.value());
	    }
	    fItem.setFileVersion(versionType);

	    //
	    // Get remaining file information from the StringTable
	    //
	    String companyName = null;
	    String internalName = null;
	    String productName = null;
	    String originalFilename = null;
	    String productVersion = null;
	    String fileVersion = null;
	    if (stringTable != null) {
		companyName	= stringTable.get("CompanyName");
		internalName	= stringTable.get("InternalName");
		productName	= stringTable.get("ProductName");
		originalFilename= stringTable.get("OriginalFilename");
		productVersion	= stringTable.get("ProductVersion");
		fileVersion	= stringTable.get("FileVersion");
	    }

	    EntityItemStringType companyType = Factories.sc.core.createEntityItemStringType();
	    if (companyName == null) {
		companyType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    } else {
		companyType.setValue(companyName);
	    }
	    fItem.setCompany(companyType);

	    EntityItemStringType internalNameType = Factories.sc.core.createEntityItemStringType();
	    if (internalName == null) {
		internalNameType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    } else {
		internalNameType.setValue(internalName);
	    }
	    fItem.setInternalName(internalNameType);

	    EntityItemStringType productNameType = Factories.sc.core.createEntityItemStringType();
	    if (productName == null) {
		productNameType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    } else {
		productNameType.setValue(productName);
	    }
	    fItem.setProductName(productNameType);

	    EntityItemStringType originalFilenameType = Factories.sc.core.createEntityItemStringType();
	    if (originalFilename == null) {
		originalFilenameType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    } else {
		originalFilenameType.setValue(originalFilename);
	    }
	    fItem.setOriginalFilename(originalFilenameType);

	    EntityItemVersionType productVersionType = Factories.sc.core.createEntityItemVersionType();
	    if (productVersion == null) {
		productVersionType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    } else {
		productVersionType.setValue(productVersion);
	    }
	    productVersionType.setDatatype(SimpleDatatypeEnumeration.VERSION.value());
	    fItem.setProductVersion(productVersionType);

	    EntityItemStringType developmentClassType = Factories.sc.core.createEntityItemStringType();
	    if (fileVersion == null) {
		developmentClassType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    } else {
		try {
		    developmentClassType.setValue(getDevelopmentClass(fileVersion));
		} catch (IllegalArgumentException e) {
		    MessageType msg = Factories.common.createMessageType();
		    msg.setLevel(MessageLevelEnumeration.INFO);
		    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_WINFILE_DEVCLASS, e.getMessage()));
		    fItem.getMessage().add(msg);
		    developmentClassType.setStatus(StatusEnumeration.ERROR);
		}
	    }
	    fItem.setDevelopmentClass(developmentClassType);
	} catch (IllegalArgumentException e) {
	    error = e.getMessage();
	    session.getLogger().info(JOVALMsg.ERROR_PE, file.getPath(), error);
	} catch (Exception e) {
	    error = e.getMessage();
	    session.getLogger().info(JOVALMsg.ERROR_PE, file.getPath(), error);
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	if (error != null) {
	    boolean reported = false;
	    for (MessageType msg : fItem.getMessage()) {
		if (((String)msg.getValue()).equals(error)) {
		    reported = true;
		    break;
		}
	    }
	    if (!reported) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.INFO);
		msg.setValue(error);
		fItem.getMessage().add(msg);
	    }
	}
    }

    /**
     * Retrieves the first leaf node associated with the specified type in a PE-format file.
     *
     * @param type the constant from the Types class indicating the resource to retrieve.
     * @param ra the PE file
     * @param rba the position in the PE file to the start of the RESOURCE image directory.
     */
    private ImageResourceDirectoryEntry getDirectoryEntry(int type, ImageResourceDirectory dir,
							  int level, IRandomAccess ra, long rba) throws IOException {
	ImageResourceDirectoryEntry[] entries = dir.getChildEntries();
	for (int i=0; i < entries.length; i++) {
	    ImageResourceDirectoryEntry entry = entries[i];
	    if (entry.isDir()) {
		if (level == 1 && type != entries[i].getType()) {
		    continue;
		} else {
		    ra.seek(rba + entry.getOffset());
		    ImageResourceDirectory subdir = new ImageResourceDirectory(ra);
		    return getDirectoryEntry(type, subdir, level+1, ra, rba);
		}
	    } else {
		return entry;
	    }
	}
	return null;
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

    /**
     * Escape '\' characters for use as a path in a WQL query.
     */
    private String escapePath(String path) {
	Iterator iter = StringTools.tokenize(path, "\\");
	StringBuffer sb = new StringBuffer();
	while (iter.hasNext()) {
	    sb.append(iter.next());
	    if (iter.hasNext()) {
		sb.append("\\\\");
	    }
	}
	return sb.toString();
    }
}
