// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.windows;

import java.io.IOException;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Vector;

import jsaf.Message;
import jsaf.intf.io.IFile;
import jsaf.intf.io.IFilesystem;
import jsaf.intf.system.IBaseSession;
import jsaf.intf.util.IProperty;
import jsaf.intf.windows.system.IWindowsSession;
import jsaf.util.IniFile;
import jsaf.util.SafeCLI;
import jsaf.util.StringTools;

import scap.oval.common.MessageType;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.OperationEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.windows.AuditeventpolicyObject;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.windows.AuditeventpolicyItem;
import scap.oval.systemcharacteristics.windows.EntityItemAuditType;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Retrieves the unary windows:passwordpolicy_item.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class AuditeventpolicyAdapter implements IAdapter {
    protected IWindowsSession session;
    private Collection<AuditeventpolicyItem> items = null;
    private CollectException error = null;

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IWindowsSession) {
	    this.session = (IWindowsSession)session;
	    classes.add(AuditeventpolicyObject.class);
	}
	return classes;
    }

    public Collection<? extends ItemType> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	if (error != null) {
	    throw error;
	} else if (items == null) {
	    makeItem();
	}
	return items;
    }

    // Private

    private void makeItem() throws CollectException {
	try {
	    long timeout = session.getTimeout(IBaseSession.Timeout.M);
	    String tempDir = session.getTempDir();
	    StringBuffer sb = new StringBuffer(tempDir);
	    if (!tempDir.endsWith(session.getFilesystem().getDelimiter())) {
		sb.append(session.getFilesystem().getDelimiter());
	    }
	    sb.append("secpol.inf");
	    String secpol = sb.toString();
	    String cmd = "secedit.exe /export /areas SECURITYPOLICY /cfg " + secpol;
	    SafeCLI.ExecData data = SafeCLI.execData(cmd, null, session, timeout);
	    int code = data.getExitCode();
	    switch(code) {
	      case 0: // success
		IFile file = null;
		try {
		    file = session.getFilesystem().getFile(secpol, IFile.Flags.READWRITE);
		    IniFile config = new IniFile(file.getInputStream(), StringTools.UTF16LE);
		    items = new Vector<AuditeventpolicyItem>();
		    AuditeventpolicyItem item = Factories.sc.windows.createAuditeventpolicyItem();
		    IProperty prop = config.getSection("Event Audit");
		    for (String key : prop) {
			try {
			    if ("AuditAccountLogon".equals(key)) {
				EntityItemAuditType type = Factories.sc.windows.createEntityItemAuditType();
				type.setValue(getPolicyValue(prop.getIntProperty(key)));
				item.setAccountLogon(type);
			    } else if ("AuditAccountManage".equals(key)) {
				EntityItemAuditType type = Factories.sc.windows.createEntityItemAuditType();
				type.setValue(getPolicyValue(prop.getIntProperty(key)));
				item.setAccountManagement(type);
			    } else if ("AuditProcessTracking".equals(key)) {
				EntityItemAuditType type = Factories.sc.windows.createEntityItemAuditType();
				type.setValue(getPolicyValue(prop.getIntProperty(key)));
				item.setDetailedTracking(type);
			    } else if ("AuditDSAccess".equals(key)) {
				EntityItemAuditType type = Factories.sc.windows.createEntityItemAuditType();
				type.setValue(getPolicyValue(prop.getIntProperty(key)));
				item.setDirectoryServiceAccess(type);
			    } else if ("AuditLogonEvents".equals(key)) {
				EntityItemAuditType type = Factories.sc.windows.createEntityItemAuditType();
				type.setValue(getPolicyValue(prop.getIntProperty(key)));
				item.setLogon(type);
			    } else if ("AuditObjectAccess".equals(key)) {
				EntityItemAuditType type = Factories.sc.windows.createEntityItemAuditType();
				type.setValue(getPolicyValue(prop.getIntProperty(key)));
				item.setObjectAccess(type);
			    } else if ("AuditPolicyChange".equals(key)) {
				EntityItemAuditType type = Factories.sc.windows.createEntityItemAuditType();
				type.setValue(getPolicyValue(prop.getIntProperty(key)));
				item.setPolicyChange(type);
			    } else if ("AuditPrivilegeUse".equals(key)) {
				EntityItemAuditType type = Factories.sc.windows.createEntityItemAuditType();
				type.setValue(getPolicyValue(prop.getIntProperty(key)));
				item.setPrivilegeUse(type);
			    } else if ("AuditSystemEvents".equals(key)) {
				EntityItemAuditType type = Factories.sc.windows.createEntityItemAuditType();
				type.setValue(getPolicyValue(prop.getIntProperty(key)));
				item.setSystem(type);
			    }
			} catch (IllegalArgumentException e) {
			    session.getLogger().warn(JOVALMsg.ERROR_WIN_SECEDIT_VALUE, e.getMessage(), key);
			}
		    }
		    items.add(item);
		} catch (NoSuchElementException e) {
		    error = new CollectException(e.getMessage(), FlagEnumeration.NOT_APPLICABLE);
		    throw error;
		} catch (IOException e) {
		    session.getLogger().warn(Message.ERROR_IO, secpol, e.getMessage());
		} finally {
		    if (file != null) {
			file.delete();
		    }
		}
		break;

	      default:
		String output = new String(data.getData());
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_WIN_SECEDIT_CODE, Integer.toString(code), output);
		throw new Exception(msg);
	    }
	} catch (CollectException e) {
	    throw e;
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e.getMessage());
	    error = new CollectException(e.getMessage(), FlagEnumeration.ERROR);
	    throw error;
	}
    }

    /**
     * Convert the policy int in the INF file to the OVAL String value.
     */
    private String getPolicyValue(int val) throws IllegalArgumentException {
	switch(val) {
	  case 0:
	    return "AUDIT_NONE";
	  case 1:
	    return "AUDIT_SUCCESS";
	  case 2:
	    return "AUDIT_FAILURE";
	  case 3:
	    return "AUDIT_SUCCESS_FAILURE";
	  default:
	    throw new IllegalArgumentException(Integer.toString(val));
	}
    }
}
