// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.windows;

import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jsaf.intf.system.ISession;
import jsaf.intf.windows.identity.IACE;
import jsaf.intf.windows.identity.IDirectory;
import jsaf.intf.windows.identity.IPrincipal;
import jsaf.intf.windows.powershell.IRunspace;
import jsaf.intf.windows.system.IWindowsSession;
import jsaf.provider.windows.powershell.PowershellException;
import jsaf.provider.windows.wmi.WmiException;
import jsaf.util.StringTools;

import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.MessageType;
import scap.oval.common.OperationEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.windows.ServiceeffectiverightsObject;
import scap.oval.systemcharacteristics.core.EntityItemBoolType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.windows.ServiceeffectiverightsItem;

import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Collects items for Serviceeffectiverights objects.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class ServiceeffectiverightsAdapter extends BaseServiceAdapter<ServiceeffectiverightsItem> {
    private IDirectory directory;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IWindowsSession) {
	    super.init((IWindowsSession)session);
	    directory = this.session.getDirectory();
	    classes.add(ServiceeffectiverightsObject.class);
	} else {
	    notapplicable.add(ServiceeffectiverightsObject.class);
	}
	return classes;
    }

    // Protected

    protected Class getItemClass() {
	return ServiceeffectiverightsItem.class;
    }

    protected Collection<ServiceeffectiverightsItem> getItems(ObjectType obj, ItemType base, IRequestContext rc)
		throws Exception {

	Collection<ServiceeffectiverightsItem> items = new ArrayList<ServiceeffectiverightsItem>();
	ServiceeffectiverightsItem baseItem = (ServiceeffectiverightsItem)base;
	try {
	    List<IPrincipal> principals = new ArrayList<IPrincipal>();
	    ServiceeffectiverightsObject sObj = (ServiceeffectiverightsObject)obj;
	    String sid = (String)sObj.getTrusteeSid().getValue();
	    OperationEnumeration op = sObj.getTrusteeSid().getOperation();
	    switch(op) {
	      case PATTERN_MATCH: {
		Pattern p = Pattern.compile(sid);
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

	    //
	    // Create items
	    //
	    String serviceName = (String)baseItem.getServiceName().getValue();
	    for (IPrincipal principal : principals) {
		StringBuffer cmd = new StringBuffer("Get-EffectiveRights -ObjectType Service -Name ");
		cmd.append("\"").append(serviceName).append("\"");
		cmd.append(" -SID ").append(principal.getSid());
		int mask = Integer.parseInt(getRunspace().invoke(cmd.toString()));
		items.add(makeItem(baseItem, principal, mask));
	    }
	} catch (NoSuchElementException e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.INFO);
	    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_WIN_NOPRINCIPAL, e.getMessage()));
	    rc.addMessage(msg);
	} catch (PatternSyntaxException e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_PATTERN, e.getMessage()));
	    rc.addMessage(msg);
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (WmiException e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_WINWMI_GENERAL, obj.getId(), e.getMessage()));
	    rc.addMessage(msg);
	} catch (CollectException e) {
	    throw e;
	} catch (Exception e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(e.getMessage());
	    rc.addMessage(msg);
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return items;
    }

    @Override
    protected List<InputStream> getPowershellModules() {
	return Arrays.asList(getClass().getResourceAsStream("Effectiverights.psm1"));
    }

    // Private

    /**
     * Create a new ServiceeffectiverightsItem based on the base ServiceeffectiverightsItem, IPrincipal and mask.
     */
    private ServiceeffectiverightsItem makeItem(ServiceeffectiverightsItem base, IPrincipal p, int mask) {
	ServiceeffectiverightsItem item = Factories.sc.windows.createServiceeffectiverightsItem();
	item.setServiceName(base.getServiceName());

	EntityItemStringType trusteeSid = Factories.sc.core.createEntityItemStringType();
	trusteeSid.setValue(p.getSid());
	item.setTrusteeSid(trusteeSid);

	boolean test = IACE.DELETE == (IACE.DELETE & mask);
	EntityItemBoolType standardDelete = Factories.sc.core.createEntityItemBoolType();
	standardDelete.setValue(Boolean.toString(test));
	standardDelete.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setStandardDelete(standardDelete);

	test = IACE.READ_CONTROL == (IACE.READ_CONTROL & mask);
	EntityItemBoolType standardReadControl = Factories.sc.core.createEntityItemBoolType();
	standardReadControl.setValue(Boolean.toString(test));
	standardReadControl.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setStandardReadControl(standardReadControl);

	test = IACE.WRITE_DAC == (IACE.WRITE_DAC & mask);
	EntityItemBoolType standardWriteDac = Factories.sc.core.createEntityItemBoolType();
	standardWriteDac.setValue(Boolean.toString(test));
	standardWriteDac.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setStandardWriteDac(standardWriteDac);

	test = IACE.WRITE_OWNER == (IACE.WRITE_OWNER & mask);
	EntityItemBoolType standardWriteOwner = Factories.sc.core.createEntityItemBoolType();
	standardWriteOwner.setValue(Boolean.toString(test));
	standardWriteOwner.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setStandardWriteOwner(standardWriteOwner);

	test = IACE.FILE_GENERIC_READ == (IACE.FILE_GENERIC_READ & mask);
	EntityItemBoolType genericRead = Factories.sc.core.createEntityItemBoolType();
	genericRead.setValue(Boolean.toString(test));
	genericRead.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setGenericRead(genericRead);

	test = IACE.FILE_GENERIC_WRITE == (IACE.FILE_GENERIC_WRITE & mask);
	EntityItemBoolType genericWrite = Factories.sc.core.createEntityItemBoolType();
	genericWrite.setValue(Boolean.toString(test));
	genericWrite.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setGenericWrite(genericWrite);

	test = IACE.FILE_GENERIC_EXECUTE == (IACE.FILE_GENERIC_EXECUTE & mask);
	EntityItemBoolType genericExecute = Factories.sc.core.createEntityItemBoolType();
	genericExecute.setValue(Boolean.toString(test));
	genericExecute.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setGenericExecute(genericExecute);

	test = IACE.SERVICE_QUERY_CONFIG == (IACE.SERVICE_QUERY_CONFIG & mask);
	EntityItemBoolType serviceQueryConf = Factories.sc.core.createEntityItemBoolType();
	serviceQueryConf.setValue(Boolean.toString(test));
	serviceQueryConf.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setServiceQueryConf(serviceQueryConf);

	test = IACE.SERVICE_CHANGE_CONFIG == (IACE.SERVICE_CHANGE_CONFIG & mask);
	EntityItemBoolType serviceChangeConf = Factories.sc.core.createEntityItemBoolType();
	serviceChangeConf.setValue(Boolean.toString(test));
	serviceChangeConf.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setServiceChangeConf(serviceChangeConf);

	test = IACE.SERVICE_QUERY_STATUS == (IACE.SERVICE_QUERY_STATUS & mask);
	EntityItemBoolType serviceQueryStat = Factories.sc.core.createEntityItemBoolType();
	serviceQueryStat.setValue(Boolean.toString(test));
	serviceQueryStat.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setServiceQueryStat(serviceQueryStat);

	test = IACE.SERVICE_ENUMERATE_DEPENDENTS == (IACE.SERVICE_ENUMERATE_DEPENDENTS & mask);
	EntityItemBoolType serviceEnumDependents = Factories.sc.core.createEntityItemBoolType();
	serviceEnumDependents.setValue(Boolean.toString(test));
	serviceEnumDependents.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setServiceEnumDependents(serviceEnumDependents);

	test = IACE.SERVICE_START == (IACE.SERVICE_START & mask);
	EntityItemBoolType serviceStart = Factories.sc.core.createEntityItemBoolType();
	serviceStart.setValue(Boolean.toString(test));
	serviceStart.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setServiceStart(serviceStart);

	test = IACE.SERVICE_PAUSE_CONTINUE == (IACE.SERVICE_PAUSE_CONTINUE & mask);
	EntityItemBoolType servicePause = Factories.sc.core.createEntityItemBoolType();
	servicePause.setValue(Boolean.toString(test));
	servicePause.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setServicePause(servicePause);

	test = IACE.SERVICE_INTERROGATE == (IACE.SERVICE_INTERROGATE & mask);
	EntityItemBoolType serviceInterrogate = Factories.sc.core.createEntityItemBoolType();
	serviceInterrogate.setValue(Boolean.toString(test));
	serviceInterrogate.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setServiceInterrogate(serviceInterrogate);

	test = IACE.SERVICE_USER_DEFINED_CONTROL == (IACE.SERVICE_USER_DEFINED_CONTROL & mask);
	EntityItemBoolType serviceUserDefined = Factories.sc.core.createEntityItemBoolType();
	serviceUserDefined.setValue(Boolean.toString(test));
	serviceUserDefined.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setServiceUserDefined(serviceUserDefined);
	return item;
    }
}
