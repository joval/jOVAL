// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.windows;

import java.math.BigInteger;
import java.io.IOException;
import java.net.MalformedURLException;
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
import oval.schemas.systemcharacteristics.windows.ObjectFactory;
import oval.schemas.results.core.ResultEnumeration;
import oval.schemas.results.core.TestedItemType;
import oval.schemas.results.core.TestedVariableType;
import oval.schemas.results.core.TestType;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.io.IRandomAccess;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IAdapterContext;
import org.joval.intf.oval.IDefinitions;
import org.joval.intf.oval.ISystemCharacteristics;
import org.joval.intf.windows.wmi.ISWbemObject;
import org.joval.intf.windows.wmi.ISWbemObjectSet;
import org.joval.intf.windows.wmi.ISWbemProperty;
import org.joval.intf.windows.wmi.ISWbemPropertySet;
import org.joval.intf.windows.wmi.IWmiProvider;
import org.joval.oval.OvalException;
import org.joval.util.BaseFileAdapter;
import org.joval.util.JOVALSystem;
import org.joval.util.Version;
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
    private static final String OWNER_WQL	= "ASSOCIATORS OF {Win32_LogicalFileSecuritySetting='$path'} WHERE AssocClass=Win32_LogicalFileOwner ResultRole=Owner";

    private ObjectFactory windowsFactory;
    private IWmiProvider wmi;

    public FileAdapter(IFilesystem fs, IWmiProvider wmi) {
	super(fs);
	this.wmi = wmi;
	windowsFactory = new ObjectFactory();
    }

    // Implement IAdapter

    public Class getObjectClass() {
	return FileObject.class;
    }

    public Class getTestClass() {
	return FileTest.class;
    }

    public Class getStateClass() {
	return FileState.class;
    }

    public Class getItemClass() {
	return FileItem.class;
    }

    public void scan(ISystemCharacteristics sc) throws OvalException {
	if (wmi != null) {
	    wmi.connect();
	}
	try {
	    super.scan(sc);
	} finally {
	    if (wmi != null) {
		wmi.disconnect();
	    }
	}
    }

    public ResultEnumeration compare(StateType st, ItemType it) throws OvalException {
	FileState state = (FileState)st;
	FileItem item = (FileItem)it;

	ResultEnumeration result = ResultEnumeration.UNKNOWN;
	if (state == null) {
	    result = ResultEnumeration.TRUE; // existence check
	} else if (state.isSetFileVersion()) {
	    if (item.isSetVersion()) {
		result = match(state.getFileVersion(), item.getVersion());
	    } else {
		result = ResultEnumeration.NOT_APPLICABLE;
	    }
	} else if (state.isSetProductVersion()) {
	    if (item.isSetProductVersion()) {
		result = match(state.getProductVersion(), item.getProductVersion());
	    } else {
		result = ResultEnumeration.NOT_APPLICABLE;
	    }
	} else {
	    throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_STATE", state.getId()));
	}
	return result;
    }


    // Protected

    protected JAXBElement<? extends ItemType> createStorageItem(ItemType item) {
	return windowsFactory.createFileItem((FileItem)item);
    }

    protected Object convertFilename(EntityItemStringType filename) {
	return windowsFactory.createFileItemFilename(filename);
    }

    protected ItemType createFileItem() {
	return windowsFactory.createFileItem();
    }

    protected List<? extends ItemType> getItems(ItemType base, ObjectType obj, IFile f) throws IOException {
	List<ItemType> list = new Vector<ItemType>();
	if (base instanceof FileItem) {
	    setItem((FileItem)base, f);
	    list.add(base);
	}
	return list;
    }

    // Private

    /**
     * Populate the FileItem with everything except the path, filename and filepath. 
     */
    private void setItem(FileItem fItem, IFile file) throws IOException {
	IRandomAccess ra = null;
	try {
	    //
	    // Get some useful information from the IFile
	    //
	    fItem.setStatus(StatusEnumeration.EXISTS);
	    EntityItemIntType sizeType = coreFactory.createEntityItemIntType();
	    sizeType.setValue(new Long(file.length()).toString());
	    sizeType.setDatatype(SimpleDatatypeEnumeration.INT.value());
	    fItem.setSize(sizeType);
	    EntityItemIntType aTimeType = coreFactory.createEntityItemIntType();
	    aTimeType.setStatus(StatusEnumeration.NOT_COLLECTED);
	    aTimeType.setDatatype(SimpleDatatypeEnumeration.INT.value());
	    fItem.setATime(aTimeType);
	    EntityItemIntType mTimeType = coreFactory.createEntityItemIntType();
	    mTimeType.setValue(toWindowsTimestamp(file.lastModified()));
	    mTimeType.setDatatype(SimpleDatatypeEnumeration.INT.value());
	    fItem.setMTime(mTimeType);
	    EntityItemIntType cTimeType = coreFactory.createEntityItemIntType();
	    cTimeType.setValue(toWindowsTimestamp(file.createTime()));
	    cTimeType.setDatatype(SimpleDatatypeEnumeration.INT.value());
	    fItem.setCTime(cTimeType);
	    EntityItemFileTypeType typeType = windowsFactory.createEntityItemFileTypeType();
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

	    EntityItemStringType ownerType = coreFactory.createEntityItemStringType();
	    if (wmi == null) {
		ownerType.setStatus(StatusEnumeration.NOT_COLLECTED);
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
		} catch (Exception e) {
		    MessageType msg = new MessageType();
		    msg.setLevel(MessageLevelEnumeration.INFO);
		    msg.setValue(e.getMessage());
		    fItem.getMessage().add(msg);
		    ownerType.setStatus(StatusEnumeration.ERROR);
		}
	    }
	    fItem.setOwner(ownerType);

	    if (file.length() > 0) {
		//
		// Read the PE-format headers
		//
		ra = file.getRandomAccess("r");
		ctx.log(Level.FINE, JOVALSystem.getMessage("STATUS_PE_READ", file.toString()));
		ImageDOSHeader dh = new ImageDOSHeader(ra);
		ra.seek((long)dh.getELFHeaderRVA());
		ImageNTHeaders nh = new ImageNTHeaders(ra);

		// Get the MS Checksum
		EntityItemStringType msChecksumType = coreFactory.createEntityItemStringType();
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
		EntityItemVersionType versionType = coreFactory.createEntityItemVersionType();
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
			    StringTable st = stringTables.get(0); //DAS
			    String key = st.getKey();
			    EntityItemStringType languageType = coreFactory.createEntityItemStringType();
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
		EntityItemStringType companyType = coreFactory.createEntityItemStringType();
		if (companyName == null) {
		    companyType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
		} else {
		    companyType.setValue(companyName);
		}
		fItem.setCompany(companyType);
		EntityItemStringType internalNameType = coreFactory.createEntityItemStringType();
		if (internalName == null) {
		    companyType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
		} else {
		    internalNameType.setValue(internalName);
		}
		fItem.setInternalName(internalNameType);
		EntityItemStringType productNameType = coreFactory.createEntityItemStringType();
		if (productName == null) {
		    companyType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
		} else {
		    productNameType.setValue(productName);
		}
		fItem.setProductName(productNameType);
		EntityItemStringType originalFilenameType = coreFactory.createEntityItemStringType();
		if (originalFilename == null) {
		    companyType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
		} else {
		    originalFilenameType.setValue(originalFilename);
		}
		fItem.setOriginalFilename(originalFilenameType);
		EntityItemVersionType productVersionType = coreFactory.createEntityItemVersionType();
		if (productVersion == null) {
		    companyType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
		} else {
		    productVersionType.setValue(productVersion);
		}
		productVersionType.setDatatype(SimpleDatatypeEnumeration.VERSION.value());
		fItem.setProductVersion(productVersionType);
		EntityItemStringType developmentClassType = coreFactory.createEntityItemStringType();
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
	    } else {
		ctx.log(Level.INFO, JOVALSystem.getMessage("STATUS_EMPTY_FILE", file.toString()));
		EntityItemVersionType versionType = coreFactory.createEntityItemVersionType();
		versionType.setDatatype(SimpleDatatypeEnumeration.VERSION.value());
		versionType.setStatus(StatusEnumeration.ERROR);  //DAS: this is what Ovaldi does
		fItem.setVersion(versionType);
		MessageType msg = new MessageType();
		msg.setLevel(MessageLevelEnumeration.INFO);
		msg.setValue(JOVALSystem.getMessage("STATUS_PE_EMPTY"));
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

    private ResultEnumeration match(EntityStateVersionType stateVer, EntityItemVersionType itemVer) throws OvalException {
	ResultEnumeration result = ResultEnumeration.UNKNOWN;

	switch(itemVer.getStatus()) {
	  case NOT_COLLECTED:
	    result = ResultEnumeration.NOT_EVALUATED;
	    break;

	  case ERROR:
	    result = ResultEnumeration.ERROR;
	    break;

	  case DOES_NOT_EXIST:
	    result = ResultEnumeration.FALSE;
	    break;

	  case EXISTS: {
	    String stateData = (String)stateVer.getValue();
	    String itemData = (String)itemVer.getValue();
	    OperationEnumeration op = stateVer.getOperation();
	    try {
		//
		// Use Strings for pattern matching, else use Version objects.
		//
		if (op == OperationEnumeration.PATTERN_MATCH) {
		    Pattern p = Pattern.compile(stateData);
		    if (p.matcher(itemData).find()) {
			result = ResultEnumeration.TRUE;
		    } else {
			result = ResultEnumeration.FALSE;
		    }
		} else if (versionCompare(new Version(itemData), op, new Version(stateData))) {
		    result = ResultEnumeration.TRUE;
		} else {
		    result = ResultEnumeration.FALSE;
		}
	    } catch (NumberFormatException e) {
		result = ResultEnumeration.ERROR;
	    }
	    break;
	  }
	}
	return result;
    }

    private boolean versionCompare(Version lhs, OperationEnumeration op, Version rhs) throws OvalException {
	switch(op) {
	  case EQUALS:
	    return lhs.equals(rhs);
	  case GREATER_THAN:
	    return lhs.greaterThan(rhs);
	  case LESS_THAN:
	    return rhs.greaterThan(lhs);
	  case LESS_THAN_OR_EQUAL:
	    return !lhs.greaterThan(rhs);
	  case GREATER_THAN_OR_EQUAL:
	    return !rhs.greaterThan(lhs);
	  case NOT_EQUAL:
	    return !lhs.equals(rhs);
	  default:
	    throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_OPERATION", op));
	}
    }

    static final BigInteger CNANOS_1601to1970	= new BigInteger("116444736000000000");
    static final BigInteger TEN_K		= new BigInteger("10000");

    /**
     * Given a Java timestamp, return a Windows-style decimal timestamp, converted to a String.  Note there is a loss of
     * precision in the last 4 digits of the resulting value (they'll always be 0s).
     */
    private String toWindowsTimestamp(long javaTS) {
	BigInteger tm = new BigInteger(new Long(javaTS).toString());
	tm = tm.multiply(TEN_K); // 10K 100 nanosecs in one millisec
	return tm.add(CNANOS_1601to1970).toString();
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
