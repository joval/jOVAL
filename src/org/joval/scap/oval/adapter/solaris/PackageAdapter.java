// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.solaris;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jsaf.intf.io.IReader;
import jsaf.intf.io.IReaderGobbler;
import jsaf.intf.system.ISession;
import jsaf.intf.unix.system.IUnixSession;
import jsaf.util.SafeCLI;
import jsaf.util.StringTools;

import scap.oval.common.MessageType;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.core.EntityObjectStringType;
import scap.oval.definitions.solaris.PackageCheckBehaviors;
import scap.oval.definitions.solaris.PackagecheckObject;
import scap.oval.definitions.solaris.PackageObject;
import scap.oval.systemcharacteristics.core.EntityItemBoolType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.core.EntityItemEVRStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.solaris.EntityItemPermissionCompareType;
import scap.oval.systemcharacteristics.solaris.PackagecheckItem;
import scap.oval.systemcharacteristics.solaris.PackageItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Collects items for package and packagecheck objects.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class PackageAdapter implements IAdapter {
    private IUnixSession session;
    private Map<String, PackageItem> packageMap;
    private Collection<String> packageList;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IUnixSession && ((IUnixSession)session).getFlavor() == IUnixSession.Flavor.SOLARIS) {
	    this.session = (IUnixSession)session;
	    packageMap = new HashMap<String, PackageItem>();
	    classes.add(PackageObject.class);
	    classes.add(PackagecheckObject.class);
	} else {
	    notapplicable.add(PackageObject.class);
	    notapplicable.add(PackagecheckObject.class);
	}
	return classes;
    }

    public Collection<? extends ItemType> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	if (obj instanceof PackageObject) {
	    return getPackageItems(obj, rc);
	} else if (obj instanceof PackagecheckObject) {
	    return getPackagecheckItems(obj, rc);
	} else {
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OBJECT, obj.getClass().getName(), obj.getId());
	    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	}
    }

    // Private

    enum PermissionCompareEnum {
	MORE("more"),
	LESS("less"),
	SAME("same");

	private String value;

	private PermissionCompareEnum(String value) {
	    this.value = value;
	}

	String value() {
	    return value;
	}
    }

    private Collection <PackagecheckItem> getPackagecheckItems(ObjectType obj, IRequestContext rc) throws CollectException {
	PackagecheckObject pObj = (PackagecheckObject)obj;
	try {
	    List<String> packages = new ArrayList<String>();
	    switch(pObj.getPkginst().getOperation()) {
	      case EQUALS:
		if (getPackageList().contains((String)pObj.getPkginst().getValue())) {
		    packages.add((String)pObj.getPkginst().getValue());
		}
		break;

	      case PATTERN_MATCH:
		Pattern p = StringTools.pattern((String)pObj.getPkginst().getValue());
		for (String pkginst : getPackageList()) {
		    if (p.matcher(pkginst).find()) {
			packages.add(pkginst);
		    }
		}
		break;

	      case NOT_EQUAL: {
		String pkginst = (String)pObj.getPkginst().getValue();
		for (String pkg : getPackageList()) {
		    if (!pkginst.equals(pkg)) {
			packages.add(pkg);
		    }
		}
		break;
	      }

	      default:
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, pObj.getPkginst().getOperation());
		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	    }
	    return getPackagecheckItems(packages, pObj, rc);
	} catch (Exception e) {
	    throw new CollectException(e, FlagEnumeration.ERROR);
	}
    }

    private Collection <PackagecheckItem> getPackagecheckItems(List<String> packages, PackagecheckObject pObj,
		IRequestContext rc) throws CollectException {

	PackageCheckBehaviors behaviors = pObj.getBehaviors();
	if (behaviors == null) {
	    behaviors = new PackageCheckBehaviors();
	}
	StringBuffer sb = new StringBuffer("/usr/sbin/pkgchk -v");
	if (behaviors.getFileattributesOnly() && behaviors.getFilecontentsOnly()) {
	    throw new CollectException(JOVALMsg.getMessage(JOVALMsg.ERROR_SOLPKGCHK_BEHAVIORS), FlagEnumeration.ERROR);
	}
	if (behaviors.getFileattributesOnly()) {
	    sb.append("a");
	}
	if (behaviors.getFilecontentsOnly()) {
	    sb.append("c");
	}
	if (behaviors.getNoVolatileeditable()) {
	    sb.append("n");
	}
	String prefix = sb.toString();
	long timeout = session.getTimeout(IUnixSession.Timeout.S);
	Collection<PackagecheckItem> items = new ArrayList<PackagecheckItem>();
	for (String pkginst : packages) {
	    try {
		String cmd = new StringBuffer(prefix).append(" ").append(pkginst).toString();
		PkgchkReader reader = new PkgchkReader(pkginst, pObj);
		SafeCLI.exec(cmd, null, null, session, timeout, reader, null);
		items.addAll(reader.getItems());
	    } catch (CollectException e) {
		throw e;
	    } catch (Exception e) {
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(e.getMessage());
		rc.addMessage(msg);
	    }
	}
	return items;
    }

    class PkgchkReader implements IReaderGobbler {
	private String pkginst;
	private Collection<String> files;
	private Map<String, List<String>> errors;
	private EntityObjectStringType filepath;
	private PackageCheckBehaviors behaviors;

	PkgchkReader(String pkginst, PackagecheckObject pObj) {
	    this.pkginst = pkginst;
	    filepath = pObj.getFilepath();
	    behaviors = pObj.getBehaviors();
	    if (behaviors == null) {
		behaviors = new PackageCheckBehaviors();
	    }
	    files = new HashSet<String>();
	    errors = new HashMap<String, List<String>>();
	}

	Collection<PackagecheckItem> getItems() throws CollectException, PatternSyntaxException {
	    Collection<PackagecheckItem> items = new ArrayList<PackagecheckItem>();
	    switch(filepath.getOperation()) {
	      case EQUALS:
		if (files.contains((String)filepath.getValue())) {
		    items.add(makeItem((String)filepath.getValue()));
		}
		break;

	      case PATTERN_MATCH:
		Pattern p = StringTools.pattern((String)filepath.getValue());
		for (String path : files) {
		    if (p.matcher(path).find()) {
			items.add(makeItem(path));
		    }
		}
		break;

	      case NOT_EQUAL:
		for (String path : files) {
		    if (!path.equals((String)filepath.getValue())) {
			items.add(makeItem(path));
		    }
		}
		break;

	      default:
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, filepath.getOperation());
		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	    }
	    return items;
	}

	// Implement IReaderGobbler

	public void gobble(IReader reader) {
	    try {
		String line = null;
		String errFile = null;
		while((line = reader.readLine()) != null) {
		    if (line.startsWith("/")) {
			files.add(line);
			errFile = null;
		    } else if (line.startsWith("ERROR:")) {
			errFile = line.substring(6).trim();
			errors.put(errFile, new ArrayList<String>());
		    } else if (errFile != null) {
			errors.get(errFile).add(line.trim());
		    }
		}
	    } catch (IOException e) {
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	}

	// Private

	private PackagecheckItem makeItem(String filepath) {
	    PackagecheckItem item = Factories.sc.solaris.createPackagecheckItem();

	    EntityItemStringType pkginstType = Factories.sc.core.createEntityItemStringType();
	    pkginstType.setValue(pkginst);
	    item.setPkginst(pkginstType);

	    EntityItemStringType filepathType = Factories.sc.core.createEntityItemStringType();
	    filepathType.setValue(filepath);
	    item.setFilepath(filepathType);

	    if (!behaviors.getFileattributesOnly()) {
		EntityItemBoolType sizeDiffers = Factories.sc.core.createEntityItemBoolType();
		sizeDiffers.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
		sizeDiffers.setValue("0");
		item.setSizeDiffers(sizeDiffers);

		EntityItemBoolType checksumDiffers = Factories.sc.core.createEntityItemBoolType();
		checksumDiffers.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
		checksumDiffers.setValue("0");
		item.setChecksumDiffers(checksumDiffers);

		EntityItemBoolType mtimeDiffers = Factories.sc.core.createEntityItemBoolType();
		mtimeDiffers.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
		mtimeDiffers.setValue("0");
		item.setMtimeDiffers(mtimeDiffers);
	    }

	    if (!behaviors.getFilecontentsOnly()) {
		EntityItemPermissionCompareType uexec = Factories.sc.solaris.createEntityItemPermissionCompareType();
		uexec.setValue(PermissionCompareEnum.SAME.value());
		item.setUexec(uexec);

		EntityItemPermissionCompareType uread = Factories.sc.solaris.createEntityItemPermissionCompareType();
		uread.setValue(PermissionCompareEnum.SAME.value());
		item.setUread(uread);

		EntityItemPermissionCompareType uwrite = Factories.sc.solaris.createEntityItemPermissionCompareType();
		uwrite.setValue(PermissionCompareEnum.SAME.value());
		item.setUwrite(uwrite);

		EntityItemPermissionCompareType gexec = Factories.sc.solaris.createEntityItemPermissionCompareType();
		gexec.setValue(PermissionCompareEnum.SAME.value());
		item.setGexec(gexec);

		EntityItemPermissionCompareType gread = Factories.sc.solaris.createEntityItemPermissionCompareType();
		gread.setValue(PermissionCompareEnum.SAME.value());
		item.setGread(gread);

		EntityItemPermissionCompareType gwrite = Factories.sc.solaris.createEntityItemPermissionCompareType();
		gwrite.setValue(PermissionCompareEnum.SAME.value());
		item.setGwrite(gwrite);

		EntityItemPermissionCompareType oexec = Factories.sc.solaris.createEntityItemPermissionCompareType();
		oexec.setValue(PermissionCompareEnum.SAME.value());
		item.setOexec(oexec);

		EntityItemPermissionCompareType oread = Factories.sc.solaris.createEntityItemPermissionCompareType();
		oread.setValue(PermissionCompareEnum.SAME.value());
		item.setOread(oread);

		EntityItemPermissionCompareType owrite = Factories.sc.solaris.createEntityItemPermissionCompareType();
		owrite.setValue(PermissionCompareEnum.SAME.value());
		item.setOwrite(uexec);
	    }

	    if (errors.containsKey(filepath)) {
		for (String error : errors.get(filepath)) {
		    if (error.startsWith("modtime")) {
			item.getMtimeDiffers().setValue("1");
		    } else if (error.startsWith("file size")) {
			item.getSizeDiffers().setValue("1");
		    } else if (error.startsWith("file cksum")) {
			item.getChecksumDiffers().setValue("1");
		    } else if (error.startsWith("permissions")) {
			int begin = error.indexOf("<") + 1;
			int end = error.indexOf("> expected");
			int[] expected = toModeBits(error.substring(begin, end));

			begin = error.indexOf("<", end) + 1;
			end = error.indexOf("> actual");
			int[] actual = toModeBits(error.substring(begin, end));

			if (expected[0] != actual[0]) {
			    if (1 == (expected[0] & 0x1)) {
				if (0 == (actual[0] & 0x1)) {
				    item.getUexec().setValue(PermissionCompareEnum.MORE.value());
				}
			    } else if (1 == (actual[0] & 0x1)) {
				item.getUexec().setValue(PermissionCompareEnum.LESS.value());
			    }
			    if (2 == (expected[0] & 0x2)) {
				if (0 == (actual[0] & 0x2)) {
				    item.getUwrite().setValue(PermissionCompareEnum.MORE.value());
				}
			    } else if (2 == (actual[0] & 0x2)) {
				item.getUwrite().setValue(PermissionCompareEnum.LESS.value());
			    }
			    if (4 == (expected[0] & 0x4)) {
				if (0 == (actual[0] & 0x4)) {
				    item.getUread().setValue(PermissionCompareEnum.MORE.value());
				}
			    } else if (4 == (actual[0] & 0x4)) {
				item.getUread().setValue(PermissionCompareEnum.LESS.value());
			    }
			}
			if (expected[1] != actual[1]) {
			    if (1 == (expected[1] & 0x1)) {
				if (0 == (actual[1] & 0x1)) {
				    item.getGexec().setValue(PermissionCompareEnum.MORE.value());
				}
			    } else if (1 == (actual[1] & 0x1)) {
				item.getGexec().setValue(PermissionCompareEnum.LESS.value());
			    }
			    if (2 == (expected[1] & 0x2)) {
				if (0 == (actual[1] & 0x2)) {
				    item.getGwrite().setValue(PermissionCompareEnum.MORE.value());
				}
			    } else if (2 == (actual[1] & 0x2)) {
				item.getGwrite().setValue(PermissionCompareEnum.LESS.value());
			    }
			    if (4 == (expected[1] & 0x4)) {
				if (0 == (actual[1] & 0x4)) {
				    item.getGread().setValue(PermissionCompareEnum.MORE.value());
				}
			    } else if (4 == (actual[1] & 0x4)) {
				item.getGread().setValue(PermissionCompareEnum.LESS.value());
			    }
			}
			if (expected[2] != actual[2]) {
			    if (1 == (expected[2] & 0x1)) {
				if (0 == (actual[2] & 0x1)) {
				    item.getOexec().setValue(PermissionCompareEnum.MORE.value());
				}
			    } else if (1 == (actual[2] & 0x1)) {
				item.getOexec().setValue(PermissionCompareEnum.LESS.value());
			    }
			    if (2 == (expected[2] & 0x2)) {
				if (0 == (actual[2] & 0x2)) {
				    item.getOwrite().setValue(PermissionCompareEnum.MORE.value());
				}
			    } else if (2 == (actual[2] & 0x2)) {
				item.getOwrite().setValue(PermissionCompareEnum.LESS.value());
			    }
			    if (4 == (expected[2] & 0x4)) {
				if (0 == (actual[2] & 0x4)) {
				    item.getOread().setValue(PermissionCompareEnum.MORE.value());
				}
			    } else if (4 == (actual[2] & 0x4)) {
				item.getOread().setValue(PermissionCompareEnum.LESS.value());
			    }
			}
		    }
		}
	    }

	    return item;
	}

	private int[] toModeBits(String s) throws IllegalArgumentException {
	    if (s.length() == 4) {
		s = s.substring(1);
	    }
	    if (s.length() == 3) {
		int[] mode = new int[3];
		mode[0] = Integer.parseInt(s.substring(0,1));
		mode[1] = Integer.parseInt(s.substring(1,2));
		mode[2] = Integer.parseInt(s.substring(2));
		return mode;
	    } else {
		throw new IllegalArgumentException(s);
	    }
	}
    }

    private Collection<PackageItem> getPackageItems(ObjectType obj, IRequestContext rc) throws CollectException {
	PackageObject pObj = (PackageObject)obj;
	Collection<PackageItem> items = new ArrayList<PackageItem>();
	switch(pObj.getPkginst().getOperation()) {
	  case EQUALS:
	    try {
		String pkginst = SafeCLI.checkArgument((String)pObj.getPkginst().getValue(), session);
		if (packageMap.containsKey(pkginst)) {
		    items.add(packageMap.get(pkginst));
		} else if (getPackageList().contains(pkginst)) {
		    String cmd = "pkginfo -l '" + SafeCLI.checkArgument(pkginst, session) + "'";
		    PackageItem item = nextPackageItem(SafeCLI.multiLine(cmd, session, IUnixSession.Timeout.M).iterator());
		    packageMap.put(pkginst, item);
		    items.add(item);
		}
	    } catch (Exception e) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(e.getMessage());
		rc.addMessage(msg);
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	    break;

	  case PATTERN_MATCH:
	    loadPackageMap();
	    try {
		Pattern p = StringTools.pattern((String)pObj.getPkginst().getValue());
		for (String pkginst : packageMap.keySet()) {
		    if (p.matcher(pkginst).find()) {
			items.add(packageMap.get(pkginst));
		    }
		}
	    } catch (PatternSyntaxException e) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_PATTERN, e.getMessage()));
		rc.addMessage(msg);
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	    break;

	  case NOT_EQUAL: {
	    loadPackageMap();
	    String pkginst = (String)pObj.getPkginst().getValue();
	    for (String key : packageMap.keySet()) {
		if (!pkginst.equals(key)) {
		    items.add(packageMap.get(key));
		}
	    }
	    break;
	  }

	  default: {
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, pObj.getPkginst().getOperation());
	    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	  }
	}

	return items;
    }

    private Collection<String> getPackageList() {
	if (packageList == null) {
	    packageList = new ArrayList<String>();
	    try {
		for (String line : SafeCLI.multiLine("pkginfo", session, IUnixSession.Timeout.M)) {
		    StringTokenizer tok = new StringTokenizer(line);
		    if (tok.countTokens() > 1) {
			tok.nextToken();
			packageList.add(tok.nextToken());
		    }
		}
	    } catch (Exception e) {
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	}
	return packageList;
    }

    private boolean loaded = false;
    private void loadPackageMap() {
	if (loaded) return;

	String cmd = "pkginfo | awk '{print $2}' | xargs -I{} pkginfo -l '{}'";
	try {
	    Iterator<String> lines = SafeCLI.manyLines(cmd, null, session);
	    PackageItem item = null;
	    while((item = nextPackageItem(lines)) != null) {
		packageMap.put((String)item.getPkginst().getValue(), item);
	    }
	    if (packageList == null) {
		packageList = packageMap.keySet();
	    }
	    loaded = true;
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }

    private static final String PKGINST		= "PKGINST:";
    private static final String NAME		= "NAME:";
    private static final String CATEGORY	= "CATEGORY:";
    private static final String ARCH		= "ARCH:";
    private static final String VERSION		= "VERSION:";
    private static final String BASEDIR		= "BASEDIR:";
    private static final String VENDOR		= "VENDOR:";
    private static final String DESC		= "DESC:";
    private static final String PSTAMP		= "PSTAMP:";
    private static final String INSTDATE	= "INSTDATE:";
    private static final String HOTLINE		= "HOTLINE:";
    private static final String STATUS		= "STATUS:";
    private static final String FILES		= "FILES:";
    private static final String ERROR		= "ERROR:";

    private PackageItem nextPackageItem(Iterator<String> lines) {
	PackageItem item = null;
	while(lines.hasNext()) {
	    String line = lines.next().trim();
	    if (line.length() == 0) {
		break;
	    } else if (line.startsWith(PKGINST)) {
		item = Factories.sc.solaris.createPackageItem();
		EntityItemStringType type = Factories.sc.core.createEntityItemStringType();
		String pkginst = line.substring(PKGINST.length()).trim();
		type.setValue(pkginst);
		session.getLogger().debug(JOVALMsg.STATUS_SOLPKG_PKGINFO, pkginst);
		item.setPkginst(type);
	    } else if (line.startsWith(NAME)) {
		EntityItemStringType type = Factories.sc.core.createEntityItemStringType();
		type.setValue(line.substring(NAME.length()).trim());
		item.setName(type);
	    } else if (line.startsWith(DESC)) {
		EntityItemStringType type = Factories.sc.core.createEntityItemStringType();
		type.setValue(line.substring(DESC.length()).trim());
		item.setDescription(type);
	    } else if (line.startsWith(CATEGORY)) {
		EntityItemStringType type = Factories.sc.core.createEntityItemStringType();
		type.setValue(line.substring(CATEGORY.length()).trim());
		item.setCategory(type);
	    } else if (line.startsWith(VENDOR)) {
		EntityItemStringType type = Factories.sc.core.createEntityItemStringType();
		type.setValue(line.substring(VENDOR.length()).trim());
		item.setVendor(type);
	    } else if (line.startsWith(VERSION)) {
		EntityItemStringType type = Factories.sc.core.createEntityItemStringType();
		type.setValue(line.substring(VERSION.length()).trim());
		item.setPackageVersion(type);
	    }
	}
	return item;
    }
}
