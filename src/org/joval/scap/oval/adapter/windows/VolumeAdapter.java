// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.windows;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import jsaf.intf.io.IFilesystem.IMount;
import jsaf.intf.system.ISession;
import jsaf.intf.windows.io.IWindowsFilesystem;
import jsaf.intf.windows.io.IWindowsFilesystem.IVolume;
import jsaf.intf.windows.system.IWindowsSession;
import jsaf.intf.windows.wmi.IWmiProvider;
import jsaf.intf.windows.wmi.ISWbemObject;
import jsaf.intf.windows.wmi.ISWbemObjectSet;
import jsaf.intf.windows.wmi.ISWbemPropertySet;
import jsaf.provider.windows.wmi.WmiException;
import jsaf.util.StringTools;

import scap.oval.common.MessageType;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.OperationEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.windows.VolumeObject;
import scap.oval.systemcharacteristics.core.EntityItemBoolType;
import scap.oval.systemcharacteristics.core.EntityItemIntType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.windows.EntityItemDriveTypeType;
import scap.oval.systemcharacteristics.windows.VolumeItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Collects items for the volume_object.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class VolumeAdapter implements IAdapter {
    private IWindowsSession session;
    private Map<String, VolumeInfo> volumes;

    // Implement IAdapter

    @Override
    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IWindowsSession) {
	    this.session = (IWindowsSession)session;
	    classes.add(VolumeObject.class);
	} else {
	    notapplicable.add(VolumeObject.class);
	}
	return classes;
    }

    @Override
    public Collection<VolumeItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	Collection<VolumeItem> items = new ArrayList<VolumeItem>();
	VolumeObject vObj = (VolumeObject)obj;
	OperationEnumeration op = vObj.getRootpath().getOperation();
	String rootpath = (String)vObj.getRootpath().getValue();
	try {
	    init();
	    switch(op) {
	      case EQUALS:
	      case CASE_INSENSITIVE_EQUALS:
		for (Map.Entry<String, VolumeInfo> entry : volumes.entrySet()) {
		    if (entry.getKey().equalsIgnoreCase(rootpath)) {
			items.add(entry.getValue().toItem(rc));
		    }
		}
		break;

	      case CASE_INSENSITIVE_NOT_EQUAL:
	      case NOT_EQUAL:
		for (Map.Entry<String, VolumeInfo> entry : volumes.entrySet()) {
		    if (!entry.getKey().equalsIgnoreCase(rootpath)) {
			items.add(entry.getValue().toItem(rc));
		    }
		}
		break;

	      case PATTERN_MATCH:
		Pattern p = StringTools.pattern(rootpath);
		for (Map.Entry<String, VolumeInfo> entry : volumes.entrySet()) {
		    if (p.matcher(entry.getKey()).find()) {
			items.add(entry.getValue().toItem(rc));
		    }
		}
		break;
    
	      default:
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	    }
	} catch (CollectException e) {
	    throw e;
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    throw new CollectException(e, FlagEnumeration.ERROR);
	}
	return items;
    }

    // Private

    /**
     * Idempotent
     */
    private void init() throws WmiException, IOException {
	if (volumes == null) {
	    volumes = new HashMap<String, VolumeInfo>();
	    String wql = "select * from Win32_LogicalDisk";
	    ISWbemObjectSet rows = session.getWmiProvider().execQuery(IWmiProvider.CIMv2, wql);
	    for (IMount volume : session.getFilesystem().getMounts()) {
		volumes.put(volume.getPath(), new VolumeInfo(rows, (IVolume)volume));
	    }
	}
    }

    class VolumeInfo {
	private IVolume volume;
	private String name, serialNo, fileSystem, maxComponentLength;

	VolumeInfo(ISWbemObjectSet objects, IVolume volume) throws WmiException {
	    this.volume = volume;
	    for (ISWbemObject obj : objects) {
		ISWbemPropertySet props = obj.getProperties();

		String path = props.getItem("Name").getValueAsString();
		if (!path.endsWith(IWindowsFilesystem.DELIM_STR)) {
		    path = new StringBuffer(path).append(IWindowsFilesystem.DELIM_STR).toString();
		}
		if (volume.getPath().equalsIgnoreCase(path)) {
		    name = props.getItem("VolumeName").getValueAsString();
		    serialNo = props.getItem("VolumeSerialNumber").getValueAsString();
		    fileSystem = props.getItem("FileSystem").getValueAsString();
		    maxComponentLength = props.getItem("MaximumComponentLength").getValueAsString();
		    break;
		}
	    }
	}

	VolumeItem toItem(IRequestContext rc) {
	    VolumeItem item = Factories.sc.windows.createVolumeItem();

	    EntityItemStringType rootpathType = Factories.sc.core.createEntityItemStringType();
	    rootpathType.setValue(volume.getPath());
	    item.setRootpath(rootpathType);

	    EntityItemStringType nameType = Factories.sc.core.createEntityItemStringType();
	    if (name == null) {
		nameType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    } else {
		nameType.setValue(name);
	    }
	    item.setName(nameType);

	    EntityItemStringType fileSystemType = Factories.sc.core.createEntityItemStringType();
	    if (fileSystem == null) {
		fileSystemType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    } else {
		fileSystemType.setValue(fileSystem);
	    }
	    item.setFileSystem(fileSystemType);

	    EntityItemIntType serialNumber = Factories.sc.core.createEntityItemIntType();
	    serialNumber.setDatatype(SimpleDatatypeEnumeration.INT.value());
	    if (serialNo == null) {
		serialNumber.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    } else {
		serialNumber.setValue(new BigInteger(serialNo, 16).toString());
	    }
	    item.setSerialNumber(serialNumber);

	    EntityItemIntType maxCompLen = Factories.sc.core.createEntityItemIntType();
	    if (maxComponentLength == null) {
		maxCompLen.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    } else {
		maxCompLen.setValue(maxComponentLength);
	    }
	    item.setVolumeMaxComponentLength(maxCompLen);

	    EntityItemDriveTypeType driveType = Factories.sc.windows.createEntityItemDriveTypeType();
	    switch(IWindowsFilesystem.FsType.typeOf(volume.getType())) {
	      case UNKNOWN:
		driveType.setValue("DRIVE_UNKNOWN");
		break;
	      case REMOVABLE:
		driveType.setValue("DRIVE_REMOVABLE");
		break;
	      case FIXED:
		driveType.setValue("DRIVE_FIXED");
		break;
	      case REMOTE:
		driveType.setValue("DRIVE_REMOTE");
		break;
	      case CDROM:
		driveType.setValue("DRIVE_CDROM");
		break;
	      case RAMDISK:
		driveType.setValue("DRIVE_RAMDISK");
		break;
	    }
	    item.setDriveType(driveType);

	    try {
		int flags = volume.getFlags();

		// 0x1
		EntityItemBoolType fileCaseSensitiveSearch = Factories.sc.core.createEntityItemBoolType();
		fileCaseSensitiveSearch.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
		fileCaseSensitiveSearch.setValue(0x1 == (flags & 0x1) ? "1" : "0");
		item.setFileCaseSensitiveSearch(fileCaseSensitiveSearch);

		// 0x2
		EntityItemBoolType fileCasePreservedNames = Factories.sc.core.createEntityItemBoolType();
		fileCasePreservedNames.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
		fileCasePreservedNames.setValue(0x2 == (flags & 0x2) ? "1" : "0");
		item.setFileCasePreservedNames(fileCasePreservedNames);

		// 0x4
		EntityItemBoolType fileUnicodeOnDisk = Factories.sc.core.createEntityItemBoolType();
		fileUnicodeOnDisk.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
		fileUnicodeOnDisk.setValue(0x4 == (flags & 0x4) ? "1" : "0");
		item.setFileUnicodeOnDisk(fileUnicodeOnDisk);

		// 0x8
		EntityItemBoolType filePersistentAcls = Factories.sc.core.createEntityItemBoolType();
		filePersistentAcls.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
		filePersistentAcls.setValue(0x8 == (flags & 0x8) ? "1" : "0");
		item.setFilePersistentAcls(filePersistentAcls);

		// 0x10
		EntityItemBoolType fileFileCompression = Factories.sc.core.createEntityItemBoolType();
		fileFileCompression.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
		fileFileCompression.setValue(0x10 == (flags & 0x10) ? "1" : "0");
		item.setFileFileCompression(fileFileCompression);

		// 0x20
		EntityItemBoolType fileVolumeQuotas = Factories.sc.core.createEntityItemBoolType();
		fileVolumeQuotas.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
		fileVolumeQuotas.setValue(0x20 == (flags & 0x20) ? "1" : "0");
		item.setFileVolumeQuotas(fileVolumeQuotas);

		// 0x40
		EntityItemBoolType fileSupportsSparseFiles = Factories.sc.core.createEntityItemBoolType();
		fileSupportsSparseFiles.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
		fileSupportsSparseFiles.setValue(0x40 == (flags & 0x40) ? "1" : "0");
		item.setFileSupportsSparseFiles(fileSupportsSparseFiles);

		// 0x80
		EntityItemBoolType fileSupportsReparsePoints = Factories.sc.core.createEntityItemBoolType();
		fileSupportsReparsePoints.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
		fileSupportsReparsePoints.setValue(0x80 == (flags & 0x80) ? "1" : "0");
		item.setFileSupportsReparsePoints(fileSupportsReparsePoints);

		// 0x8000
		EntityItemBoolType fileVolumeIsCompressed = Factories.sc.core.createEntityItemBoolType();
		fileVolumeIsCompressed.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
		fileVolumeIsCompressed.setValue(0x8000 == (flags & 0x8000) ? "1" : "0");
		item.setFileVolumeIsCompressed(fileVolumeIsCompressed);

		// 0x10000
		EntityItemBoolType fileSupportsObjectIds = Factories.sc.core.createEntityItemBoolType();
		fileSupportsObjectIds.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
		fileSupportsObjectIds.setValue(0x10000 == (flags & 0x10000) ? "1" : "0");
		item.setFileSupportsObjectIds(fileSupportsObjectIds);

		// 0x20000
		EntityItemBoolType fileSupportsEncryption = Factories.sc.core.createEntityItemBoolType();
		fileSupportsEncryption.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
		fileSupportsEncryption.setValue(0x20000 == (flags & 0x20000) ? "1" : "0");
		item.setFileSupportsEncryption(fileSupportsEncryption);

		// 0x40000
		EntityItemBoolType fileNamedStreams = Factories.sc.core.createEntityItemBoolType();
		fileNamedStreams.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
		fileNamedStreams.setValue(0x40000 == (flags & 0x40000) ? "1" : "0");
		item.setFileNamedStreams(fileNamedStreams);

		// 0x80000
		EntityItemBoolType fileReadOnlyVolume = Factories.sc.core.createEntityItemBoolType();
		fileReadOnlyVolume.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
		fileReadOnlyVolume.setValue(0x80000 == (flags & 0x80000) ? "1" : "0");
		item.setFileReadOnlyVolume(fileReadOnlyVolume);

		/*

		REMIND (DAS): The following flags are not addressed by the OVAL volume_test:

		0x00100000 FILE_SEQUENTIAL_WRITE_ONCE
		0x00200000 FILE_SUPPORTS_TRANSACTIONS
		0x00400000 FILE_SUPPORTS_HARD_LINKS
		0x00800000 FILE_SUPPORTS_EXTENDED_ATTRIBUTES
		0x01000000 FILE_SUPPORTS_OPEN_BY_FILE_ID
		0x02000000 FILE_SUPPORTS_USN_JOURNAL

		*/
	    } catch (IOException e) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.WARNING);
		msg.setValue(e.getMessage());
		rc.addMessage(msg);
	    }

	    return item;
	}
    }
}
