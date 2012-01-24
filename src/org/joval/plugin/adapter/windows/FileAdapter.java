// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.windows;

import java.io.IOException;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.MessageType;
import oval.schemas.common.SimpleDatatypeEnumeration;
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
import org.joval.intf.io.IRandomAccess;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.windows.io.IWindowsFile;
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
import org.joval.oval.NotCollectableException;
import org.joval.oval.OvalException;
import org.joval.plugin.adapter.independent.BaseFileAdapter;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.StringTools;
import org.joval.util.Version;

/**
 * Evaluates FileTest OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class FileAdapter extends BaseFileAdapter {
    private static final String CIMV2		= "root\\cimv2";
    private static final String OWNER_WQL	= "ASSOCIATORS OF {Win32_LogicalFileSecuritySetting='$path'} " +
						  "WHERE AssocClass=Win32_LogicalFileOwner ResultRole=Owner";
    private static final String TIME_WQL	= "SELECT * FROM CIM_DataFile WHERE Name='$path'";

    private IWmiProvider wmi;

    public FileAdapter(IWindowsSession session) {
	super(session);
    }

    // Implement IAdapter

    private static Class[] objectClasses = {FileObject.class};

    public Class[] getObjectClasses() {
	return objectClasses;
    }

    public boolean connect() {
	if (wmi == null) {
	    wmi = ((IWindowsSession)session).getWmiProvider();
	}
	return wmi.connect();
    }

    public void disconnect() {
	if (wmi != null) {
	    wmi.disconnect();
	}
    }

    // Protected

    protected Object convertFilename(EntityItemStringType filename) {
	return JOVALSystem.factories.sc.windows.createFileItemFilename(filename);
    }

    protected ItemType createFileItem() {
	return JOVALSystem.factories.sc.windows.createFileItem();
    }

    protected Collection<JAXBElement<? extends ItemType>> getItems(ItemType base, IFile f, IRequestContext rc)
		throws IOException, NotCollectableException, OvalException {

	Collection<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();
	if (base instanceof FileItem) {
	    IWindowsFile wf = null;
	    if (f instanceof IWindowsFile) {
		wf = (IWindowsFile)f;
	    } else {
		throw new NotCollectableException(JOVALSystem.getMessage(JOVALMsg.ERROR_WINFILE_TYPE, f.getClass().getName()));
	    }
	    setItem((FileItem)base, wf);
	    items.add(JOVALSystem.factories.sc.windows.createFileItem((FileItem)base));
	}
	return items;
    }

    // Private

    /**
     * Populate the FileItem with everything except the path, filename and filepath. 
     */
    private void setItem(FileItem fItem, IWindowsFile file) throws IOException {
	//
	// Get some information from the IFile
	//
	fItem.setStatus(StatusEnumeration.EXISTS);
	EntityItemFileTypeType typeType = JOVALSystem.factories.sc.windows.createEntityItemFileTypeType();
	switch(file.getWindowsFileType()) {
	  case IWindowsFile.FILE_TYPE_DISK:
	    typeType.setValue("FILE_TYPE_DISK");
	    break;
	  case IWindowsFile.FILE_TYPE_REMOTE:
	    typeType.setValue("FILE_TYPE_REMOTE");
	    break;
	  case IWindowsFile.FILE_TYPE_PIPE:
	    typeType.setValue("FILE_TYPE_PIPE");
	    break;
	  case IWindowsFile.FILE_TYPE_CHAR:
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
	EntityItemStringType ownerType = JOVALSystem.factories.sc.core.createEntityItemStringType();
	EntityItemIntType aTimeType = JOVALSystem.factories.sc.core.createEntityItemIntType();
	aTimeType.setDatatype(SimpleDatatypeEnumeration.INT.value());
	EntityItemIntType cTimeType = JOVALSystem.factories.sc.core.createEntityItemIntType();
	cTimeType.setDatatype(SimpleDatatypeEnumeration.INT.value());
	EntityItemIntType mTimeType = JOVALSystem.factories.sc.core.createEntityItemIntType();
	mTimeType.setDatatype(SimpleDatatypeEnumeration.INT.value());
	if (wmi == null) {
	    ownerType.setStatus(StatusEnumeration.NOT_COLLECTED);
	    aTimeType.setStatus(StatusEnumeration.NOT_COLLECTED);
	    fItem.setATime(aTimeType);
	    cTimeType.setValue(Timestamp.toWindowsTimestamp(file.createTime()));
	    fItem.setCTime(cTimeType);
	    mTimeType.setValue(Timestamp.toWindowsTimestamp(file.lastModified()));
	    fItem.setMTime(mTimeType);
	} else {
	    try {
		String wql = OWNER_WQL.replaceAll("(?i)\\$path", Matcher.quoteReplacement(file.getLocalName()));
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
		    MessageType msg = JOVALSystem.factories.common.createMessageType();
		    msg.setLevel(MessageLevelEnumeration.INFO);
		    msg.setValue(JOVALSystem.getMessage(JOVALMsg.ERROR_WINFILE_OWNER, objSet.getSize()));
		    fItem.getMessage().add(msg);
		    ownerType.setStatus(StatusEnumeration.ERROR);
		}

		wql = TIME_WQL.replaceAll("(?i)\\$path", escapePath(Matcher.quoteReplacement(file.getLocalName())));
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
		MessageType msg = JOVALSystem.factories.common.createMessageType();
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
	    EntityItemIntType sizeType = JOVALSystem.factories.sc.core.createEntityItemIntType();
	    sizeType.setValue(new Long(file.length()).toString());
	    sizeType.setDatatype(SimpleDatatypeEnumeration.INT.value());
	    fItem.setSize(sizeType);
	    if (file.length() > 0) {
		readPEHeader(file, fItem);
	    } else {
		session.getLogger().info(JOVALMsg.STATUS_EMPTY_FILE, file.toString());

		EntityItemVersionType versionType = JOVALSystem.factories.sc.core.createEntityItemVersionType();
		versionType.setDatatype(SimpleDatatypeEnumeration.VERSION.value());
		versionType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
		fItem.setFileVersion(versionType);

		MessageType msg = JOVALSystem.factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.INFO);
		msg.setValue(JOVALSystem.getMessage(JOVALMsg.STATUS_PE_EMPTY));
		fItem.getMessage().add(msg);
	    }
	}
    }

    /**
     * Read the Portable Execution format header information and extract data from it.
     */
    private void readPEHeader(IFile file, FileItem fItem) throws IOException {
	session.getLogger().trace(JOVALMsg.STATUS_PE_READ, file.toString());
	try {
	    Header header = new Header(file, session.getLogger());

	    //
	    // Get the MS Checksum from the NT headers
	    //
	    EntityItemStringType msChecksumType = JOVALSystem.factories.sc.core.createEntityItemStringType();
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
	    EntityItemStringType languageType = JOVALSystem.factories.sc.core.createEntityItemStringType();
	    String locale = LanguageConstants.getLocaleString(key);
	    if (locale == null) {
		languageType.setStatus(StatusEnumeration.ERROR);
		MessageType msg = JOVALSystem.factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.INFO);
		msg.setValue(JOVALSystem.getMessage(JOVALMsg.ERROR_WINFILE_LANGUAGE, key));
		fItem.getMessage().add(msg);
	    } else {
		languageType.setValue(LanguageConstants.getLocaleString(key));
	    }
	    fItem.setLanguage(languageType);

	    //
	    // Get the file version from the VsFixedFileInfo structure
	    //
	    EntityItemVersionType versionType = JOVALSystem.factories.sc.core.createEntityItemVersionType();
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

	    EntityItemStringType companyType = JOVALSystem.factories.sc.core.createEntityItemStringType();
	    if (companyName == null) {
		companyType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    } else {
		companyType.setValue(companyName);
	    }
	    fItem.setCompany(companyType);

	    EntityItemStringType internalNameType = JOVALSystem.factories.sc.core.createEntityItemStringType();
	    if (internalName == null) {
		internalNameType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    } else {
		internalNameType.setValue(internalName);
	    }
	    fItem.setInternalName(internalNameType);

	    EntityItemStringType productNameType = JOVALSystem.factories.sc.core.createEntityItemStringType();
	    if (productName == null) {
		productNameType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    } else {
		productNameType.setValue(productName);
	    }
	    fItem.setProductName(productNameType);

	    EntityItemStringType originalFilenameType = JOVALSystem.factories.sc.core.createEntityItemStringType();
	    if (originalFilename == null) {
		originalFilenameType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    } else {
		originalFilenameType.setValue(originalFilename);
	    }
	    fItem.setOriginalFilename(originalFilenameType);

	    EntityItemVersionType productVersionType = JOVALSystem.factories.sc.core.createEntityItemVersionType();
	    if (productVersion == null) {
		productVersionType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    } else {
		productVersionType.setValue(productVersion);
	    }
	    productVersionType.setDatatype(SimpleDatatypeEnumeration.VERSION.value());
	    fItem.setProductVersion(productVersionType);

	    EntityItemStringType developmentClassType = JOVALSystem.factories.sc.core.createEntityItemStringType();
	    if (fileVersion == null) {
		developmentClassType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    } else {
		try {
		    developmentClassType.setValue(getDevelopmentClass(fileVersion));
		} catch (IllegalArgumentException e) {
		    MessageType msg = JOVALSystem.factories.common.createMessageType();
		    msg.setLevel(MessageLevelEnumeration.INFO);
		    msg.setValue(JOVALSystem.getMessage(JOVALMsg.ERROR_WINFILE_DEVCLASS, e.getMessage()));
		    fItem.getMessage().add(msg);
		    developmentClassType.setStatus(StatusEnumeration.ERROR);
		}
	    }
	    fItem.setDevelopmentClass(developmentClassType);
	} catch (Exception e) {
	    session.getLogger().info(JOVALMsg.ERROR_PE, file.getLocalName());
	    session.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    boolean reported = false;
	    for (MessageType msg : fItem.getMessage()) {
		if (((String)msg.getValue()).equals(e.getMessage())) {
		    reported = true;
		    break;
		}
	    }
	    if (!reported) {
		MessageType msg = JOVALSystem.factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.INFO);
		msg.setValue(e.getMessage());
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
