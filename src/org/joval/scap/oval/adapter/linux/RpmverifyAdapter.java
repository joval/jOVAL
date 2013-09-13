// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.linux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jsaf.intf.system.ISession;
import jsaf.intf.unix.system.IUnixSession;
import jsaf.util.SafeCLI;
import jsaf.util.StringTools;

import scap.oval.common.MessageType;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.OperationEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.linux.RpmVerifyBehaviors;
import scap.oval.definitions.linux.RpmverifyObject;
import scap.oval.systemcharacteristics.core.EntityItemBoolType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.linux.EntityItemRpmVerifyResultType;
import scap.oval.systemcharacteristics.linux.RpmverifyItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Evaluates Rpmverify OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class RpmverifyAdapter extends BaseRpmAdapter {
    @Override
    Class getObjectClass() {
	return RpmverifyObject.class;
    }

    // Implement IAdapter

    public Collection<RpmverifyItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	try {
	    RpmverifyObject rObj = (RpmverifyObject)obj;

	    //
	    // First, get information about all the RPMs matching the specified package name
	    //
	    List<RpmData> data = new ArrayList<RpmData>();
	    switch(rObj.getName().getOperation()) {
	      case EQUALS:
		data.addAll(getRpmData((String)rObj.getName().getValue()));
		break;

	      case PATTERN_MATCH:
		loadPackageMap();
		Pattern p = StringTools.pattern((String)rObj.getName().getValue());
		for (RpmData datum : packageMap.values()) {
		    if (p.matcher(datum.name).find()) {
			data.add(datum);
		    }
		}
		break;

	      case NOT_EQUAL:
		loadPackageMap();
		String name = (String)rObj.getName().getValue();
		for (RpmData datum : packageMap.values()) {
		    if (!datum.name.equals(name)) {
			data.add(datum);
		    }
		}
		break;

	      default:
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, rObj.getName().getOperation());
		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	    }

	    //
	    // Next, filter the RPM list down to only those that include files matching the specified filename, and build
	    // a map of those file matches.
	    //
	    Map<String, Collection<String>> filepaths = new HashMap<String, Collection<String>>();
	    Iterator<RpmData> iter = data.iterator();
	    switch(rObj.getFilepath().getOperation()) {
	      case EQUALS:
		while(iter.hasNext()) {
		    RpmData datum = iter.next();
		    if (datum.filepaths.contains((String)rObj.getFilepath().getValue())) {
			filepaths.put(datum.name, Arrays.asList((String)rObj.getFilepath().getValue()));
		    } else {
			iter.remove();
		    }
		}
		break;

	      case PATTERN_MATCH:
		Pattern p = StringTools.pattern((String)rObj.getFilepath().getValue());
		while(iter.hasNext()) {
		    RpmData datum = iter.next();
		    for (String filepath : datum.filepaths) {
			if (p.matcher(filepath).find()) {
			    if (!filepaths.containsKey(datum.name)) {
				filepaths.put(datum.name, new ArrayList<String>());
			    }
			    filepaths.get(datum.name).add(filepath);
			}
		    }
		    if (!filepaths.containsKey(datum.name)) {
			iter.remove();
		    }
		}
		break;

	      case NOT_EQUAL:
		while(iter.hasNext()) {
		    RpmData datum = iter.next();
		    if (datum.filepaths.contains((String)rObj.getFilepath().getValue())) {
			iter.remove();
		    } else {
			filepaths.put(datum.name, new ArrayList<String>());
			for (String filepath : datum.filepaths) {
			    if (!filepath.equals((String)rObj.getFilepath().getValue())) {
				filepaths.get(datum.name).add(filepath);
			    }
			}
		    }
		}
		break;

	      default:
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, rObj.getFilepath().getOperation());
		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	    }

	    Collection<RpmverifyItem> items = new ArrayList<RpmverifyItem>();
	    for (Map.Entry<String, Collection<String>> entry : filepaths.entrySet()) {
		try {
		    items.addAll(getItems(entry.getKey(), entry.getValue(), rObj.getBehaviors(), rc));
		} catch (Exception e) {
		    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		    MessageType msg = Factories.common.createMessageType();
		    msg.setLevel(MessageLevelEnumeration.ERROR);
		    msg.setValue(e.getMessage());
		    rc.addMessage(msg);
		}
	    }
	    return items;
	} catch (CollectException e) {
	    throw e;
	} catch (Exception e) {
	    throw new CollectException(e, FlagEnumeration.ERROR);
	}
    }

    // Private

    private static final String PASS = "pass";
    private static final String FAIL = "fail";
    private static final String NP = "not performed";

    /**
     * NOTE: In some versions of RPM, the "not performed" check may be broken because of the following defect in RPM:
     *   https://bugzilla.redhat.com/show_bug.cgi?id=803765
     */
    private Collection<RpmverifyItem> getItems(String packageName, Collection<String> filepaths, RpmVerifyBehaviors behaviors,
		IRequestContext rc) throws Exception {

	StringBuffer cmd = new StringBuffer("rpm -V ");
	cmd.append("'").append(packageName).append("'");
	if (behaviors == null) {
	    behaviors = Factories.definitions.linux.createRpmVerifyBehaviors();
	}
	if (behaviors.getNodeps()) {
	    cmd.append(" --nodeps");
	}
	if (behaviors.getNodigest()) {
	    cmd.append(" --nodigest");
	}
	if (behaviors.getNogroup()) {
	    cmd.append(" --nogroup");
	}
	if (behaviors.getNolinkto()) {
	    cmd.append(" --nolinkto");
	}
	if (behaviors.getNomd5()) {
	    cmd.append(" --nofiledigest");
	}
	if (behaviors.getNomode()) {
	    cmd.append(" --nomode");
	}
	if (behaviors.getNomtime()) {
	    cmd.append(" --nomtime");
	}
	if (behaviors.getNordev()) {
	    cmd.append(" --nordev");
	}
	if (behaviors.getNoscripts()) {
	    cmd.append(" --noscripts");
	}
	if (behaviors.getNosignature()) {
	    cmd.append(" --nosignature");
	}
	if (behaviors.getNosize()) {
	    cmd.append(" --nosize");
	}
	if (behaviors.getNouser()) {
	    cmd.append(" --nouser");
	}
	cmd.append(" -v");

	Collection<RpmverifyItem> items = new ArrayList<RpmverifyItem>();
	for (String line : SafeCLI.multiLine(cmd.toString(), session, IUnixSession.Timeout.M)) {
	    int ptr = line.indexOf("/");
	    if (ptr == -1) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.WARNING);
		msg.setValue(JOVALMsg.getMessage(JOVALMsg.WARNING_RPMVERIFY_LINE, line));
		rc.addMessage(msg);
		continue;
	    }
	    String filepath = line.substring(ptr);

	    if (filepaths.contains(filepath)) {
		String type = line.substring(9, ptr).trim();
		RpmverifyItem item = Factories.sc.linux.createRpmverifyItem();

		EntityItemStringType filepathType = Factories.sc.core.createEntityItemStringType();
		filepathType.setValue(filepath);
		item.setFilepath(filepathType);

		EntityItemStringType name = Factories.sc.core.createEntityItemStringType();
		name.setValue(packageName);
		item.setName(name);

		EntityItemBoolType config = Factories.sc.core.createEntityItemBoolType();
		config.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
		config.setValue("0");
		item.setConfigurationFile(config);

		EntityItemBoolType doc = Factories.sc.core.createEntityItemBoolType();
		doc.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
		doc.setValue("0");
		item.setDocumentationFile(doc);

		EntityItemBoolType ghost = Factories.sc.core.createEntityItemBoolType();
		ghost.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
		ghost.setValue("0");
		item.setGhostFile(ghost);

		EntityItemBoolType license = Factories.sc.core.createEntityItemBoolType();
		license.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
		license.setValue("0");
		item.setLicenseFile(license);

		EntityItemBoolType readme = Factories.sc.core.createEntityItemBoolType();
		readme.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
		readme.setValue("0");
		item.setReadmeFile(license);

		if (type.length() > 0) {
		    if ("c".equals(type)) {
			if (behaviors.getNoconfigfiles()) {
			    continue;
			} else {
			    config.setValue("1");
			}
		    } else if ("d".equals(type)) {
			doc.setValue("1");
		    } else if ("g".equals(type)) {
			if (behaviors.getNoghostfiles()) {
			    continue;
			} else {
			    ghost.setValue("1");
			}
		    } else if ("l".equals(type)) {
			license.setValue("1");
		    } else if ("r".equals(type)) {
			readme.setValue("1");
		    }
		}

		String tests = line.substring(0, 9);

		EntityItemRpmVerifyResultType sizeDiffers = Factories.sc.linux.createEntityItemRpmVerifyResultType();
		if (tests.indexOf("S") != -1) {
		    sizeDiffers.setValue(FAIL);
		} else if (tests.charAt(0) == '?' || behaviors.getNosize()) {
		    sizeDiffers.setValue(NP);
		} else {
		    sizeDiffers.setValue(PASS);
		}
		item.setSizeDiffers(sizeDiffers);

		EntityItemRpmVerifyResultType modeDiffers = Factories.sc.linux.createEntityItemRpmVerifyResultType();
		if (tests.indexOf("M") != -1) {
		    modeDiffers.setValue(FAIL);
		} else if (tests.charAt(1) == '?' || behaviors.getNomode()) {
		    modeDiffers.setValue(NP);
		} else {
		    modeDiffers.setValue(PASS);
		}
		item.setModeDiffers(modeDiffers);

		EntityItemRpmVerifyResultType md5Differs = Factories.sc.linux.createEntityItemRpmVerifyResultType();
		if (tests.indexOf("5") != -1) {
		    md5Differs.setValue(FAIL);
		} else if (tests.charAt(2) == '?' || behaviors.getNomd5()) {
		    md5Differs.setValue(NP);
		} else {
		    md5Differs.setValue(PASS);
		}
		item.setMd5Differs(md5Differs);

		EntityItemRpmVerifyResultType deviceDiffers = Factories.sc.linux.createEntityItemRpmVerifyResultType();
		if (tests.indexOf("D") != -1) {
		    deviceDiffers.setValue(FAIL);
		} else if (tests.charAt(3) == '?' || behaviors.getNordev()) {
		    deviceDiffers.setValue(NP);
		} else {
		    deviceDiffers.setValue(PASS);
		}
		item.setDeviceDiffers(deviceDiffers);

		EntityItemRpmVerifyResultType linkMismatch = Factories.sc.linux.createEntityItemRpmVerifyResultType();
		if (tests.indexOf("L") != -1) {
		    linkMismatch.setValue(FAIL);
		} else if (tests.charAt(4) == '?' || behaviors.getNolinkto()) {
		    linkMismatch.setValue(NP);
		} else {
		    linkMismatch.setValue(PASS);
		}
		item.setLinkMismatch(linkMismatch);

		EntityItemRpmVerifyResultType ownershipDiffers = Factories.sc.linux.createEntityItemRpmVerifyResultType();
		if (tests.indexOf("U") != -1) {
		    ownershipDiffers.setValue(FAIL);
		} else if (tests.charAt(5) == '?' || behaviors.getNouser()) {
		    ownershipDiffers.setValue(NP);
		} else {
		    ownershipDiffers.setValue(PASS);
		}
		item.setOwnershipDiffers(ownershipDiffers);

		EntityItemRpmVerifyResultType groupDiffers = Factories.sc.linux.createEntityItemRpmVerifyResultType();
		if (tests.indexOf("G") != -1) {
		    groupDiffers.setValue(FAIL);
		} else if (tests.charAt(6) == '?' || behaviors.getNogroup()) {
		    groupDiffers.setValue(NP);
		} else {
		    groupDiffers.setValue(PASS);
		}
		item.setGroupDiffers(groupDiffers);

		EntityItemRpmVerifyResultType mtimeDiffers = Factories.sc.linux.createEntityItemRpmVerifyResultType();
		if (tests.indexOf("T") != -1) {
		    mtimeDiffers.setValue(FAIL);
		} else if (tests.charAt(7) == '?' || behaviors.getNomtime()) {
		    mtimeDiffers.setValue(NP);
		} else {
		    mtimeDiffers.setValue(PASS);
		}
		item.setMtimeDiffers(mtimeDiffers);

		EntityItemRpmVerifyResultType capabilitiesDiffer = Factories.sc.linux.createEntityItemRpmVerifyResultType();
		if (tests.indexOf("P") != -1) {
		    capabilitiesDiffer.setValue(FAIL);
		} else if (tests.charAt(8) == '?') {
		    capabilitiesDiffer.setValue(NP);
		} else {
		    capabilitiesDiffer.setValue(PASS);
		}
		item.setCapabilitiesDiffer(capabilitiesDiffer);

		items.add(item);
	    }
	}
	return items;
    }
}
