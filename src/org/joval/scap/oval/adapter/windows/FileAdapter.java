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
import java.util.StringTokenizer;
import java.util.regex.Matcher;

import jsaf.Message;
import jsaf.intf.io.IFile;
import jsaf.intf.io.IFileEx;
import jsaf.intf.io.IRandomAccess;
import jsaf.intf.system.ISession;
import jsaf.intf.unix.io.IUnixFileInfo;
import jsaf.intf.windows.io.IWindowsFileInfo;
import jsaf.intf.windows.system.IWindowsSession;
import jsaf.provider.windows.Timestamp;
import jsaf.util.StringTools;

import jpe.header.Header;
import jpe.resource.version.VsFixedFileInfo;
import jpe.resource.version.VsVersionInfo;
import jpe.util.LanguageConstants;

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
    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	try {
	    baseInit(session);
	    classes.add(FileObject.class);
	} catch (UnsupportedOperationException e) {
	    notapplicable.add(FileObject.class);
	}
	return classes;
    }

    // Protected

    protected Class getItemClass() {
	return FileItem.class;
    }

    protected Collection<FileItem> getItems(ObjectType obj, Collection<IFile> files, IRequestContext rc)
		throws CollectException {

	FileObject fObj = (FileObject)obj;
	Collection<FileItem> items = new ArrayList<FileItem>();
	for (IFile f : files) {
	    try {
		FileItem item = (FileItem)getBaseItem(obj, f);
		if (item != null) {
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

		    EntityItemIntType sizeType = Factories.sc.core.createEntityItemIntType();
		    if (f.isFile()) {
			sizeType.setValue(new Long(f.length()).toString());
			sizeType.setDatatype(SimpleDatatypeEnumeration.INT.value());
		    } else {
			sizeType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
		    }
		    item.setSize(sizeType);

		    addExtendedInfo(fObj, f.getExtended(), item);
		    addHeaderInfo(fObj, f, item);
		    items.add(item);
		}
	    } catch (IOException e) {
		session.getLogger().warn(Message.ERROR_IO, f.getPath(), e.getMessage());
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(e.getMessage());
		rc.addMessage(msg);
	    }
	}
	return items;
    }

    // Private

    /**
     * Interrogate the IFileEx.
     */
    private void addExtendedInfo(FileObject fObj, IFileEx extended, FileItem item) throws IOException {
	if (extended instanceof IWindowsFileInfo) {
	    EntityItemFileTypeType typeType = Factories.sc.windows.createEntityItemFileTypeType();
	    switch(((IWindowsFileInfo)extended).getWindowsFileType()) {
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

	    EntityItemStringType ownerType = Factories.sc.core.createEntityItemStringType();
	    ownerType.setValue(((IWindowsFileInfo)extended).getOwner().getNetbiosName());
	    item.setOwner(ownerType);
	} else if (extended instanceof IUnixFileInfo) {
	    EntityItemFileTypeType typeType = Factories.sc.windows.createEntityItemFileTypeType();
	    String type = ((IUnixFileInfo)extended).getUnixFileType();
	    if (IUnixFileInfo.FILE_TYPE_DIR.equals(type)) {
		typeType.setValue("FILE_ATTRIBUTE_DIRECTORY");
	    } else if (IUnixFileInfo.FILE_TYPE_REGULAR.equals(type)) {
		typeType.setValue("FILE_TYPE_DISK");
	    } else if (IUnixFileInfo.FILE_TYPE_FIFO.equals(type)) {
		typeType.setValue("FILE_TYPE_PIPE");
	    } else if (IUnixFileInfo.FILE_TYPE_CHAR.equals(type)) {
		typeType.setValue("FILE_TYPE_CHAR");
	    } else {
		typeType.setValue("FILE_TYPE_UNKNOWN");
	    }
	    item.setType(typeType);
	}
    }

    /**
     * Read the Portable Execution format header information from the IFileEx, or directly from the file itself, and
     * convey it to the item.
     */
    private void addHeaderInfo(FileObject fObj, IFile file, FileItem item) throws IOException {
	session.getLogger().trace(JOVALMsg.STATUS_PE_READ, file.toString());
	IFileEx extended = file.getExtended();
	Map<String, String> peHeaders = null;
	if (file.isFile() && file.length() == 0) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.INFO);
	    msg.setValue(JOVALMsg.getMessage(JOVALMsg.STATUS_PE_EMPTY));
	    item.getMessage().add(msg);
	} else if (extended instanceof IWindowsFileInfo) {
	    peHeaders = ((IWindowsFileInfo)extended).getPEHeaders();
	} else if (file.isFile()) {
	    try {
		Header header = new Header(file);
		peHeaders = new HashMap<String, String>();
		String cs = Integer.toString(header.getNTHeader().getImageOptionalHeader().getChecksum());
		peHeaders.put(IWindowsFileInfo.PE_MS_CHECKSUM, cs);
		String key = VsVersionInfo.LANGID_KEY;
		VsVersionInfo versionInfo = header.getVersionInfo();
		VsFixedFileInfo value = null;
		if (versionInfo != null) {
		    value = versionInfo.getValue();
		    key = versionInfo.getDefaultTranslation();
		}
		if (value != null) {
		    String version = versionInfo.getValue().getFileVersion().toString();
		    peHeaders.put(IWindowsFileInfo.PE_VERSION, version);
		    StringTokenizer tok = new StringTokenizer(version, ".");
		    if (tok.countTokens() == 4) {
			peHeaders.put(IWindowsFileInfo.PE_VERSION_MAJOR_PART, tok.nextToken());
			peHeaders.put(IWindowsFileInfo.PE_VERSION_MINOR_PART, tok.nextToken());
			peHeaders.put(IWindowsFileInfo.PE_VERSION_BUILD_PART, tok.nextToken());
			peHeaders.put(IWindowsFileInfo.PE_VERSION_PRIVATE_PART, tok.nextToken());
		    }
		    String locale = LanguageConstants.getLocaleString(key);
		    if (locale != null) {
			peHeaders.put(IWindowsFileInfo.PE_LANGUAGE, LanguageConstants.getLocaleString(key));
		    }
		    Map<String, String> stringTable = versionInfo.getStringTable(key);
		    if (stringTable.containsKey("CompanyName")) {
			peHeaders.put(IWindowsFileInfo.PE_COMPANY_NAME, stringTable.get("CompanyName"));
		    }
		    if (stringTable.containsKey("InternalName")) {
			peHeaders.put(IWindowsFileInfo.PE_INTERNAL_NAME, stringTable.get("InternalName"));
		    }
		    if (stringTable.containsKey("ProductName")) {
			peHeaders.put(IWindowsFileInfo.PE_PRODUCT_NAME, stringTable.get("ProductName"));
		    }
		    if (stringTable.containsKey("OriginalFilename")) {
			peHeaders.put(IWindowsFileInfo.PE_ORIGINAL_NAME, stringTable.get("OriginalFilename"));
		    }
		    if (stringTable.containsKey("ProductVersion")) {
			peHeaders.put(IWindowsFileInfo.PE_PRODUCT_VERSION, stringTable.get("ProductVersion"));
		    }
		}
	    } catch (IllegalArgumentException e) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(e.getMessage());
		item.getMessage().add(msg);
	    }
	}

	EntityItemStringType msChecksumType = Factories.sc.core.createEntityItemStringType();
	if (peHeaders != null && peHeaders.containsKey(IWindowsFileInfo.PE_MS_CHECKSUM)) {
	    msChecksumType.setValue(peHeaders.get(IWindowsFileInfo.PE_MS_CHECKSUM));
	} else {
	    msChecksumType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	}
	item.setMsChecksum(msChecksumType);

	EntityItemStringType languageType = Factories.sc.core.createEntityItemStringType();
	if (peHeaders != null && peHeaders.containsKey(IWindowsFileInfo.PE_LANGUAGE)) {
	    languageType.setValue(peHeaders.get(IWindowsFileInfo.PE_LANGUAGE));
	} else {
	    languageType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	}
	item.setLanguage(languageType);

	EntityItemVersionType versionType = Factories.sc.core.createEntityItemVersionType();
	if (peHeaders != null && peHeaders.containsKey(IWindowsFileInfo.PE_VERSION_MAJOR_PART)) {
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
	    if (major == 0 && minor == 0 && build == 0 && priv == 0) {
		versionType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    } else {
		versionType.setValue(new Version(major, minor, build, priv).toString());
		versionType.setDatatype(SimpleDatatypeEnumeration.VERSION.value());
	    }
	} else {
	    versionType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	}
	item.setFileVersion(versionType);

	EntityItemStringType companyType = Factories.sc.core.createEntityItemStringType();
	if (peHeaders != null && peHeaders.containsKey(IWindowsFileInfo.PE_COMPANY_NAME)) {
	    companyType.setValue(peHeaders.get(IWindowsFileInfo.PE_COMPANY_NAME));
	} else {
	    companyType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	}
	item.setCompany(companyType);

	EntityItemStringType internalNameType = Factories.sc.core.createEntityItemStringType();
	if (peHeaders != null && peHeaders.containsKey(IWindowsFileInfo.PE_INTERNAL_NAME)) {
	    internalNameType.setValue(peHeaders.get(IWindowsFileInfo.PE_INTERNAL_NAME));
	} else {
	    internalNameType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	}
	item.setInternalName(internalNameType);

	EntityItemStringType productNameType = Factories.sc.core.createEntityItemStringType();
	if (peHeaders != null && peHeaders.containsKey(IWindowsFileInfo.PE_PRODUCT_NAME)) {
	    productNameType.setValue(peHeaders.get(IWindowsFileInfo.PE_PRODUCT_NAME));
	} else {
	    productNameType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	}
	item.setProductName(productNameType);

	EntityItemStringType originalFilenameType = Factories.sc.core.createEntityItemStringType();
	if (peHeaders != null && peHeaders.containsKey(IWindowsFileInfo.PE_ORIGINAL_NAME)) {
	    originalFilenameType.setValue(peHeaders.get(IWindowsFileInfo.PE_ORIGINAL_NAME));
	} else {
	    originalFilenameType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	}
	item.setOriginalFilename(originalFilenameType);

	EntityItemVersionType productVersionType = Factories.sc.core.createEntityItemVersionType();
	if (peHeaders != null && peHeaders.containsKey(IWindowsFileInfo.PE_PRODUCT_VERSION)) {
	    productVersionType.setValue(peHeaders.get(IWindowsFileInfo.PE_PRODUCT_VERSION));
	    productVersionType.setDatatype(SimpleDatatypeEnumeration.VERSION.value());
	} else {
	    productVersionType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	}
	item.setProductVersion(productVersionType);

	EntityItemStringType developmentClassType = Factories.sc.core.createEntityItemStringType();
	if (peHeaders != null && peHeaders.containsKey(IWindowsFileInfo.PE_VERSION)) {
	    try {
		developmentClassType.setValue(getDevelopmentClass(peHeaders.get(IWindowsFileInfo.PE_VERSION)));
	    } catch (IllegalArgumentException e) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.INFO);
		msg.setValue(JOVALMsg.getMessage(JOVALMsg.STATUS_PE_DEVCLASS, e.getMessage()));
		item.getMessage().add(msg);
		developmentClassType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    }
	} else {
	    developmentClassType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	}
	item.setDevelopmentClass(developmentClassType);
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
