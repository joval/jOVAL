// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.windows;

import java.math.BigInteger;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.MessageType;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.EntityStateVersionType;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.core.StateType;
import oval.schemas.definitions.windows.FileObject;
import oval.schemas.definitions.windows.FileState;
import oval.schemas.definitions.windows.FileTest;
import oval.schemas.systemcharacteristics.core.EntityItemIntType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.EntityItemVersionType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.core.VariableValueType;
import oval.schemas.systemcharacteristics.windows.EntityItemFileTypeType;
import oval.schemas.systemcharacteristics.windows.FileItem;
import oval.schemas.results.core.ResultEnumeration;
import oval.schemas.results.core.TestedItemType;
import oval.schemas.results.core.TestedVariableType;
import oval.schemas.results.core.TestType;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.io.IRandomAccess;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IAdapterContext;
import org.joval.intf.windows.wmi.ISWbemObject;
import org.joval.intf.windows.wmi.ISWbemObjectSet;
import org.joval.intf.windows.wmi.ISWbemProperty;
import org.joval.intf.windows.wmi.ISWbemPropertySet;
import org.joval.intf.windows.wmi.IWmiProvider;
import org.joval.oval.OvalException;
import org.joval.oval.TestException;
import org.joval.util.BaseFileAdapter;
import org.joval.util.JOVALSystem;
import org.joval.util.StringTools;
import org.joval.util.Version;
import org.joval.windows.Timestamp;
import org.joval.windows.pe.ImageDOSHeader;
import org.joval.windows.pe.ImageNTHeaders;
import org.joval.windows.pe.ImageDataDirectory;
import org.joval.windows.pe.LanguageConstants;
import org.joval.windows.pe.resource.ImageResourceDirectory;
import org.joval.windows.pe.resource.ImageResourceDirectoryEntry;
import org.joval.windows.pe.resource.ImageResourceDataEntry;
import org.joval.windows.pe.resource.Types;
import org.joval.windows.pe.resource.version.VsFixedFileInfo;
import org.joval.windows.pe.resource.version.VsVersionInfo;
import org.joval.windows.pe.resource.version.StringFileInfo;
import org.joval.windows.pe.resource.version.StringTable;
import org.joval.windows.pe.resource.version.StringStructure;

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

    public FileAdapter(IFilesystem fs, IWmiProvider wmi) {
	super(fs);
	this.wmi = wmi;
    }

    // Implement IAdapter

    public Class getObjectClass() {
	return FileObject.class;
    }

    public Class getStateClass() {
	return FileState.class;
    }

    public Class getItemClass() {
	return FileItem.class;
    }

    public boolean connect() {
	if (wmi != null) {
	    return wmi.connect();
	}
	return false;
    }

    public void disconnect() {
	if (wmi != null) {
	    wmi.disconnect();
	}
    }

    public ResultEnumeration compare(StateType st, ItemType it) throws TestException, OvalException {
	FileState state = (FileState)st;
	FileItem item = (FileItem)it;

	if (state == null) {
	    return ResultEnumeration.TRUE; // existence check
	}

	if (state.isSetATime()) {
	    ResultEnumeration result = ctx.test(state.getATime(), item.getATime());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.isSetCompany()) {
	    ResultEnumeration result = ctx.test(state.getCompany(), item.getCompany());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.isSetCTime()) {
	    ResultEnumeration result = ctx.test(state.getCTime(), item.getCTime());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.isSetDevelopmentClass()) {
	    ResultEnumeration result = ctx.test(state.getDevelopmentClass(), item.getDevelopmentClass());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.isSetFilename()) {
	    if (item.isSetFilename()) {
		JAXBElement<EntityItemStringType> itemJE = item.getFilename();
		ResultEnumeration result = ctx.test(state.getFilename(), itemJE.getValue());
		if (result != ResultEnumeration.TRUE) {
		    return result;
		}
	    } else {
		return ResultEnumeration.FALSE;
	    }
	}
	if (state.isSetFilepath()) {
	    ResultEnumeration result = ctx.test(state.getFilepath(), item.getFilepath());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.isSetFileVersion()) {
	    ResultEnumeration result = ctx.test(state.getFileVersion(), item.getVersion());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.isSetInternalName()) {
	    ResultEnumeration result = ctx.test(state.getInternalName(), item.getInternalName());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.isSetLanguage()) {
	    ResultEnumeration result = ctx.test(state.getLanguage(), item.getLanguage());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.isSetMsChecksum()) {
	    ResultEnumeration result = ctx.test(state.getMsChecksum(), item.getMsChecksum());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.isSetMTime()) {
	    ResultEnumeration result = ctx.test(state.getMTime(), item.getMTime());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.isSetOriginalFilename()) {
	    ResultEnumeration result = ctx.test(state.getOriginalFilename(), item.getOriginalFilename());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.isSetOwner()) {
	    ResultEnumeration result = ctx.test(state.getOwner(), item.getOwner());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.isSetPath()) {
	    ResultEnumeration result = ctx.test(state.getPath(), item.getPath());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.isSetProductName()) {
	    ResultEnumeration result = ctx.test(state.getProductName(), item.getProductName());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.isSetProductVersion()) {
	    ResultEnumeration result = ctx.test(state.getProductVersion(), item.getProductVersion());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.isSetSize()) {
	    ResultEnumeration result = ctx.test(state.getSize(), item.getSize());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.isSetType()) {
	    ResultEnumeration result = ctx.test(state.getType(), item.getType());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}

	return ResultEnumeration.TRUE;
    }

    // Protected

    protected Object convertFilename(EntityItemStringType filename) {
	return JOVALSystem.factories.sc.windows.createFileItemFilename(filename);
    }

    protected ItemType createFileItem() {
	return JOVALSystem.factories.sc.windows.createFileItem();
    }

    protected List<JAXBElement<? extends ItemType>>
	getItems(ItemType base, ObjectType obj, IFile f, List<VariableValueType> vars) throws IOException, OvalException {

	List<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();
	if (base instanceof FileItem) {
	    setItem((FileItem)base, f);
	    items.add(JOVALSystem.factories.sc.windows.createFileItem((FileItem)base));
	}
	return items;
    }

    // Private

    /**
     * Populate the FileItem with everything except the path, filename and filepath. 
     */
    private void setItem(FileItem fItem, IFile file) throws IOException {
	//
	// Get some information from the IFile
	//
	fItem.setStatus(StatusEnumeration.EXISTS);
	EntityItemIntType sizeType = JOVALSystem.factories.sc.core.createEntityItemIntType();
	sizeType.setValue(new Long(file.length()).toString());
	sizeType.setDatatype(SimpleDatatypeEnumeration.INT.value());
	fItem.setSize(sizeType);
	EntityItemFileTypeType typeType = JOVALSystem.factories.sc.windows.createEntityItemFileTypeType();
	switch(file.getType()) {
	  case IFile.FILE_TYPE_DISK:
	    typeType.setValue("FILE_TYPE_DISK");
	    break;
	  case IFile.FILE_TYPE_REMOTE:
	    typeType.setValue("FILE_TYPE_REMOTE");
	    break;
	  case IFile.FILE_TYPE_PIPE:
	    typeType.setValue("FILE_TYPE_PIPE");
	    break;
	  case IFile.FILE_TYPE_CHAR:
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
	    cTimeType.setValue(Timestamp.toWindowsTimestamp(file.createTime()));
	    mTimeType.setValue(Timestamp.toWindowsTimestamp(file.lastModified()));
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
		    MessageType msg = new MessageType();
		    msg.setLevel(MessageLevelEnumeration.INFO);
		    msg.setValue(JOVALSystem.getMessage("ERROR_WINFILE_OWNER", objSet.getSize()));
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
		    ISWbemProperty cTimeProp = filePropSet.getItem("InstallDate");
		    cTimeType.setValue(Timestamp.toWindowsTimestamp(cTimeProp.getValueAsString()));
		    ISWbemProperty mTimeProp = filePropSet.getItem("LastModified");
		    mTimeType.setValue(Timestamp.toWindowsTimestamp(mTimeProp.getValueAsString()));
		}
	    } catch (Exception e) {
		MessageType msg = new MessageType();
		msg.setLevel(MessageLevelEnumeration.INFO);
		msg.setValue(e.getMessage());
		fItem.getMessage().add(msg);
		ownerType.setStatus(StatusEnumeration.ERROR);
	    }
	}
	fItem.setOwner(ownerType);
	fItem.setATime(aTimeType);
	fItem.setCTime(cTimeType);
	fItem.setMTime(mTimeType);

	//
	// If possible, read the PE header information
	//
	if (file.isFile()) {
	    if (file.length() > 0) {
		readPEHeaders(file, fItem);
	    } else {
		ctx.log(Level.INFO, JOVALSystem.getMessage("STATUS_EMPTY_FILE", file.toString()));
		EntityItemVersionType versionType = JOVALSystem.factories.sc.core.createEntityItemVersionType();
		versionType.setDatatype(SimpleDatatypeEnumeration.VERSION.value());
//DAS: this is what Ovaldi does now, but JB says it'll change and do the right thing soon
		versionType.setStatus(StatusEnumeration.ERROR);
		fItem.setVersion(versionType);
		MessageType msg = new MessageType();
		msg.setLevel(MessageLevelEnumeration.INFO);
		msg.setValue(JOVALSystem.getMessage("STATUS_PE_EMPTY"));
		fItem.getMessage().add(msg);
	    }
	}
    }

    /**
     * Read the Portable Execution format header information, including String Tables.
     */
    private void readPEHeaders(IFile file, FileItem fItem) throws IOException {
	IRandomAccess ra = file.getRandomAccess("r");
	ctx.log(Level.FINE, JOVALSystem.getMessage("STATUS_PE_READ", file.toString()));
	try {
	    ImageDOSHeader dh = new ImageDOSHeader(ra);
	    ra.seek((long)dh.getELFHeaderRVA());
	    ImageNTHeaders nh = new ImageNTHeaders(ra);

	    // Get the MS Checksum
	    EntityItemStringType msChecksumType = JOVALSystem.factories.sc.core.createEntityItemStringType();
	    msChecksumType.setValue(new Integer(nh.getImageOptionalHeader().getChecksum()).toString());
	    fItem.setMsChecksum(msChecksumType);

	    //
	    // Get the version directory entry from the image resource directory
	    //
	    long rba = nh.getResourceBaseAddress(ImageDataDirectory.RESOURCE_TABLE);
	    if (rba == 0) {
		throw new IOException("Missing resource section!");
	    }
	    ra.seek(rba);
	    ImageResourceDirectory root = new ImageResourceDirectory(ra);
	    ImageResourceDataEntry vde = getDirectoryEntry(Types.RT_VERSION, root, 1, ra, rba).getDataEntry(ra, rba);
	    ra.seek(vde.getDataAddress(rba, nh.getImageDirEntryRVA(ImageDataDirectory.RESOURCE_TABLE)));
	    VsVersionInfo vi = new VsVersionInfo(ra);
	    VsFixedFileInfo vffi = vi.getValue();

	    //
	    // Get the file version from the VsFixedFileInfo structure
	    //
	    EntityItemVersionType versionType = JOVALSystem.factories.sc.core.createEntityItemVersionType();
	    versionType.setValue(vffi.getFileVersion().toString());
	    versionType.setDatatype(SimpleDatatypeEnumeration.VERSION.value());
	    fItem.setVersion(versionType);

	    //
	    // Look at the StringTables for any interesting information
	    //
	    String companyName=null, internalName=null, productName=null, originalFilename=null, productVersion=null,
		   fileVersion=null;
	    for (Object obj : vi.getChildren()) {
		if (obj instanceof StringFileInfo) {
		    StringFileInfo sfi = (StringFileInfo)obj;
		    List <StringTable>stringTables = sfi.getChildren();
		    if (stringTables.size() > 0) {
			StringTable st = stringTables.get(0); //DAS: just going with the first String Table
			String key = st.getKey();
			EntityItemStringType languageType = JOVALSystem.factories.sc.core.createEntityItemStringType();
			String locale = LanguageConstants.getLocaleString(key);
			if (locale == null) {
			    languageType.setStatus(StatusEnumeration.ERROR);
			    MessageType msg = new MessageType();
			    msg.setLevel(MessageLevelEnumeration.INFO);
			    msg.setValue(JOVALSystem.getMessage("ERROR_WINFILE_LANGUAGE", key));
			    fItem.getMessage().add(msg);
			} else {
			    languageType.setValue(LanguageConstants.getLocaleString(key));
			}
			fItem.setLanguage(languageType);
			for (StringStructure string : st.getChildren()) {
			    if (string.getKey().trim().equals("CompanyName")) {
				companyName = string.getValue().trim();
			    } else if (string.getKey().trim().equals("InternalName")) {
				internalName = string.getValue().trim();
			    } else if (string.getKey().trim().equals("ProductName")) {
				productName = string.getValue().trim();
			    } else if (string.getKey().trim().equals("OriginalFilename")) {
				originalFilename = string.getValue().trim();
			    } else if (string.getKey().trim().equals("ProductVersion")) {
				productVersion = string.getValue().trim();
			    } else if (string.getKey().trim().equals("FileVersion")) {
				fileVersion = string.getValue().trim();
			    }
			}
		    }
		}
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
		companyType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    } else {
		internalNameType.setValue(internalName);
	    }
	    fItem.setInternalName(internalNameType);
	    EntityItemStringType productNameType = JOVALSystem.factories.sc.core.createEntityItemStringType();
	    if (productName == null) {
		companyType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    } else {
		productNameType.setValue(productName);
	    }
	    fItem.setProductName(productNameType);
	    EntityItemStringType originalFilenameType = JOVALSystem.factories.sc.core.createEntityItemStringType();
	    if (originalFilename == null) {
		companyType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    } else {
		originalFilenameType.setValue(originalFilename);
	    }
	    fItem.setOriginalFilename(originalFilenameType);
	    EntityItemVersionType productVersionType = JOVALSystem.factories.sc.core.createEntityItemVersionType();
	    if (productVersion == null) {
		companyType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    } else {
		productVersionType.setValue(productVersion);
	    }
	    productVersionType.setDatatype(SimpleDatatypeEnumeration.VERSION.value());
	    fItem.setProductVersion(productVersionType);
	    EntityItemStringType developmentClassType = JOVALSystem.factories.sc.core.createEntityItemStringType();
	    if (fileVersion == null) {
		developmentClassType.setStatus(StatusEnumeration.NOT_COLLECTED);
	    } else {
		try {
		    developmentClassType.setValue(getDevelopmentClass(fileVersion));
		} catch (IllegalArgumentException e) {
		    MessageType msg = new MessageType();
		    msg.setLevel(MessageLevelEnumeration.INFO);
		    msg.setValue(JOVALSystem.getMessage("ERROR_WINFILE_DEVCLASS", e.getMessage()));
		    fItem.getMessage().add(msg);
		    developmentClassType.setStatus(StatusEnumeration.ERROR);
		}
	    }
	    fItem.setDevelopmentClass(developmentClassType);
	} catch (Exception e) {
	    ctx.log(Level.INFO, JOVALSystem.getMessage("ERROR_PE", file.getLocalName(), e));
	    boolean reported = false;
	    for (MessageType msg : fItem.getMessage()) {
		if (((String)msg.getValue()).equals(e.getMessage())) {
		    reported = true;
		    break;
		}
	    }
	    if (!reported) {
		MessageType msg = new MessageType();
		msg.setLevel(MessageLevelEnumeration.INFO);
		msg.setValue(e.getMessage());
		fItem.getMessage().add(msg);
	    }
	} finally {
	    if (ra != null) {
		try {
		    ra.close();
		} catch (IOException e) {
		    ctx.log(Level.WARNING, JOVALSystem.getMessage("ERROR_FILE_STREAM_CLOSE", file.toString()), e);
		}
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
