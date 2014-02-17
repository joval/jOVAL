// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.windows;

import java.io.InputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jsaf.Message;
import jsaf.intf.system.ISession;
import jsaf.intf.windows.identity.IACE;
import jsaf.intf.windows.identity.IDirectory;
import jsaf.intf.windows.identity.IPrincipal;
import jsaf.intf.windows.powershell.IRunspace;
import jsaf.intf.windows.system.IWindowsSession;
import jsaf.util.Base64;
import jsaf.util.StringTools;

import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.MessageType;
import scap.oval.common.OperationEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.windows.PrintereffectiverightsObject;
import scap.oval.definitions.windows.PrinterEffectiveRightsBehaviors;
import scap.oval.systemcharacteristics.core.EntityItemBoolType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.windows.PrintereffectiverightsItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Collects items for Printereffectiverights objects.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class PrintereffectiverightsAdapter implements IAdapter {
    //
    // See: http://msdn.microsoft.com/en-us/library/cc244650.aspx
    //
    public static final int JOB_ACCESS_ADMINISTER	= 0x00000010;
    public static final int JOB_ACCESS_READ		= 0x00000020;
    public static final int PRINTER_ACCESS_ADMINISTER	= 0x00000004;
    public static final int PRINTER_ACCESS_USE		= 0x00000008;

    private IWindowsSession session;
    private HashSet<String> runspaceIds;
    private HashSet<String> printers;
    private CollectException error;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IWindowsSession) {
	    this.session = (IWindowsSession)session;
	    runspaceIds = new HashSet<String>();
	    classes.add(PrintereffectiverightsObject.class);
	} else {
	    notapplicable.add(PrintereffectiverightsObject.class);
	}
	return classes;
    }

    public Collection<PrintereffectiverightsItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	init();
	PrintereffectiverightsObject pObj = (PrintereffectiverightsObject)obj;

	//
	// First, get a list of matching printers matching the specified name
	//
	Collection<String> printerNames = new ArrayList<String>();
	OperationEnumeration op = pObj.getPrinterName().getOperation();
	switch(op) {
	  case EQUALS:
	  case CASE_INSENSITIVE_EQUALS:
	    for (String name : printers) {
		if (name.equalsIgnoreCase((String)pObj.getPrinterName().getValue())) {
		    printerNames.add(name);
		}
	    }
	    break;

	  case NOT_EQUAL:
	  case CASE_INSENSITIVE_NOT_EQUAL:
	    for (String name : printers) {
		if (!name.equalsIgnoreCase((String)pObj.getPrinterName().getValue())) {
		    printerNames.add(name);
		}
	    }
	    break;

	  case PATTERN_MATCH:
	    try {
		Pattern p = StringTools.pattern((String)pObj.getPrinterName().getValue());
		for (String name : printers) {
		    if (p.matcher(name).find()) {
			printerNames.add(name);
		    }
		}
	    } catch (PatternSyntaxException e) {
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_PATTERN, e.getMessage());
		throw new CollectException(msg, FlagEnumeration.ERROR);
	    }
	    break;

	  default:
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
	    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	}

	//
	// Now, collect effective rights for each printer
	//
	Collection<PrintereffectiverightsItem> items = new ArrayList<PrintereffectiverightsItem>();
	try {
	    IDirectory directory = session.getDirectory();
	    List<IPrincipal> principals = new ArrayList<IPrincipal>();
	    String sid = (String)pObj.getTrusteeSid().getValue();
	    op = pObj.getTrusteeSid().getOperation();
	    switch(op) {
	      case PATTERN_MATCH: {
		Pattern p = StringTools.pattern(sid);
		for (IPrincipal principal : directory.queryAllPrincipals()) {
		    if (p.matcher(principal.getSid()).find()) {
			principals.add(principal);
		    }
		}
		break;
	      }

	      case NOT_EQUAL:
		for (IPrincipal principal : directory.queryAllPrincipals()) {
		    if (!sid.equals(principal.getSid())) {
			principals.add(principal);
		    }
		}
		break;

	      case CASE_INSENSITIVE_EQUALS:
	      case EQUALS: {
		principals.add(directory.queryPrincipalBySid(sid));
		break;
	      }

	      default:
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	    }

	    Collection<IPrincipal> resolved = null;
	    if (pObj.isSetBehaviors()) {
		Map<String, IPrincipal> pmap = new HashMap<String, IPrincipal>();
		PrinterEffectiveRightsBehaviors behaviors = pObj.getBehaviors();
		boolean includeGroup = behaviors.getIncludeGroup();
		boolean resolveGroup = behaviors.getResolveGroup();
		for (IPrincipal principal : principals) {
		    for (IPrincipal p : directory.getAllPrincipals(principal, includeGroup, resolveGroup)) {
			pmap.put(p.getSid(), p);
		    }
		}
		resolved = pmap.values(); // de-duped
	    } else {
		resolved = principals;
	    }

	    //
	    // Create items
	    //
	    for (String name : printerNames) {
		Map<String, IPrincipal> principalMap = new HashMap<String, IPrincipal>();
		StringBuffer cmd = new StringBuffer();
		for (IPrincipal principal : resolved) {
		    if (cmd.length() > 0) {
			cmd.append(",");
		    }
		    sid = principal.getSid();
		    cmd.append("'").append(sid).append("'");
		    principalMap.put(sid, principal);
		}
		cmd.append(" | Get-EffectiveRights -ObjectType Printer");
		cmd.append(" -Name '").append(name).append("'");
		for (String line : getRunspace().invoke(cmd.toString()).split("\r\n")) {
		    int ptr = line.indexOf(":");
		    if (ptr != -1) {
			sid = line.substring(0,ptr);
			int mask = Integer.parseInt(line.substring(ptr+1).trim());
			items.add(makeItem(name, principalMap.get(sid), mask));
		    }
		}
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

    private PrintereffectiverightsItem makeItem(String name, IPrincipal principal, int mask) {
	PrintereffectiverightsItem item = Factories.sc.windows.createPrintereffectiverightsItem();

	EntityItemStringType printerName = Factories.sc.core.createEntityItemStringType();
	printerName.setValue(name);
	item.setPrinterName(printerName);

	EntityItemStringType trusteeSid = Factories.sc.core.createEntityItemStringType();
	trusteeSid.setValue(principal.getSid());
	item.setTrusteeSid(trusteeSid);

	EntityItemBoolType standardDelete = Factories.sc.core.createEntityItemBoolType();
	standardDelete.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	standardDelete.setValue((mask & IACE.DELETE) == IACE.DELETE ? "1" : "0");
	item.setStandardDelete(standardDelete);

	EntityItemBoolType standardReadControl = Factories.sc.core.createEntityItemBoolType();
	standardReadControl.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	standardReadControl.setValue((mask & IACE.READ_CONTROL) == IACE.READ_CONTROL ? "1" : "0");
	item.setStandardReadControl(standardReadControl);

	EntityItemBoolType standardWriteDac = Factories.sc.core.createEntityItemBoolType();
	standardWriteDac.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	standardWriteDac.setValue((mask & IACE.WRITE_DAC) == IACE.WRITE_DAC ? "1" : "0");
	item.setStandardWriteDac(standardWriteDac);

	EntityItemBoolType standardWriteOwner = Factories.sc.core.createEntityItemBoolType();
	standardWriteOwner.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	standardWriteOwner.setValue((mask & IACE.WRITE_OWNER) == IACE.WRITE_OWNER ? "1" : "0");
	item.setStandardWriteOwner(standardWriteOwner);

	EntityItemBoolType standardSynchronize = Factories.sc.core.createEntityItemBoolType();
	standardSynchronize.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	standardSynchronize.setValue((mask & IACE.SYNCHRONIZE) == IACE.SYNCHRONIZE ? "1" : "0");
	item.setStandardSynchronize(standardSynchronize);

	EntityItemBoolType genericRead = Factories.sc.core.createEntityItemBoolType();
	genericRead.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	genericRead.setValue((mask & IACE.GENERIC_READ) == IACE.GENERIC_READ ? "1" : "0");
	item.setGenericRead(genericRead);

	EntityItemBoolType genericWrite = Factories.sc.core.createEntityItemBoolType();
	genericWrite.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	genericWrite.setValue((mask & IACE.GENERIC_WRITE) == IACE.GENERIC_WRITE ? "1" : "0");
	item.setGenericWrite(genericWrite);

	EntityItemBoolType genericExecute = Factories.sc.core.createEntityItemBoolType();
	genericExecute.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	genericExecute.setValue((mask & IACE.GENERIC_EXECUTE) == IACE.GENERIC_EXECUTE ? "1" : "0");
	item.setGenericExecute(genericExecute);

	EntityItemBoolType genericAll = Factories.sc.core.createEntityItemBoolType();
	genericAll.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	genericAll.setValue((mask & IACE.GENERIC_ALL) == IACE.GENERIC_ALL ? "1" : "0");
	item.setGenericAll(genericAll);

	EntityItemBoolType printerAccessAdminister = Factories.sc.core.createEntityItemBoolType();
	printerAccessAdminister.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	printerAccessAdminister.setValue((mask & PRINTER_ACCESS_ADMINISTER) == PRINTER_ACCESS_ADMINISTER ? "1" : "0");
	item.setPrinterAccessAdminister(printerAccessAdminister);

	EntityItemBoolType printerAccessUse = Factories.sc.core.createEntityItemBoolType();
	printerAccessUse.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	printerAccessUse.setValue((mask & PRINTER_ACCESS_USE) == PRINTER_ACCESS_USE ? "1" : "0");
	item.setPrinterAccessUse(printerAccessUse);

	EntityItemBoolType jobAccessAdminister = Factories.sc.core.createEntityItemBoolType();
	jobAccessAdminister.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	jobAccessAdminister.setValue((mask & JOB_ACCESS_ADMINISTER) == JOB_ACCESS_ADMINISTER ? "1" : "0");
	item.setJobAccessAdminister(jobAccessAdminister);

	EntityItemBoolType jobAccessRead = Factories.sc.core.createEntityItemBoolType();
	jobAccessRead.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	jobAccessRead.setValue((mask & JOB_ACCESS_READ) == JOB_ACCESS_READ ? "1" : "0");
	item.setJobAccessRead(jobAccessRead);

	return item;
    }

    /**
     * Idempotent
     */
    private void init() throws CollectException {
	if (error != null) {
	    throw error;
	} else if (printers == null) {
	    try {
		printers = new HashSet<String>();
		String cmd = "[jOVAL.Printer.Probe]::List() | %{$_.pPrinterName} | Transfer-Encode";
		byte[] data = Base64.decode(getRunspace().invoke(cmd));
		for (String line : Arrays.asList(new String(data, StringTools.UTF8).split("\r\n"))) {
		    printers.add(line);
		}
	    } catch (Exception e) {
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		throw error = new CollectException(e, FlagEnumeration.ERROR);
	    }
	}
    }

    private IRunspace getRunspace() throws Exception {
	IRunspace runspace = session.getRunspacePool().getRunspace();
	if (!runspaceIds.contains(runspace.getId())) {
	    runspace.loadAssembly(getClass().getResourceAsStream("Printer.dll"));
	    runspace.loadAssembly(getClass().getResourceAsStream("Effectiverights.dll"));
	    runspace.loadModule(getClass().getResourceAsStream("Effectiverights.psm1"));
	    runspaceIds.add(runspace.getId());
	}
	return runspace;
    }
}
