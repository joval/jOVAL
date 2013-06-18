// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.windows;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.NoSuchElementException;

import jsaf.Message;
import jsaf.intf.io.IFile;
import jsaf.intf.io.IFilesystem;
import jsaf.intf.system.ISession;
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
import scap.oval.definitions.windows.PasswordpolicyObject;
import scap.oval.systemcharacteristics.core.EntityItemBoolType;
import scap.oval.systemcharacteristics.core.EntityItemIntType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.windows.PasswordpolicyItem;

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
public class PasswordpolicyAdapter implements IAdapter {
    protected IWindowsSession session;
    private Collection<PasswordpolicyItem> items = null;
    private CollectException error = null;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IWindowsSession) {
	    this.session = (IWindowsSession)session;
	    classes.add(PasswordpolicyObject.class);
	} else {
	    notapplicable.add(PasswordpolicyObject.class);
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
	    long timeout = session.getTimeout(ISession.Timeout.M);
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
		    PasswordpolicyItem item = Factories.sc.windows.createPasswordpolicyItem();
		    file = session.getFilesystem().getFile(secpol, IFile.Flags.READWRITE);
		    IniFile config = new IniFile(file.getInputStream(), StringTools.UTF16LE);
		    IProperty prop = config.getSection("System Access");
		    for (String key : prop) {
			if ("MaximumPasswordAge".equals(key)) {
			    EntityItemIntType type = Factories.sc.core.createEntityItemIntType();
			    type.setDatatype(SimpleDatatypeEnumeration.INT.value());
			    long days = Long.parseLong(prop.getProperty(key));
			    long secs = days * 86400;
			    type.setValue(Long.toString(secs));
			    item.setMaxPasswdAge(type);
			} else if ("MinimumPasswordAge".equals(key)) {
			    EntityItemIntType type = Factories.sc.core.createEntityItemIntType();
			    type.setDatatype(SimpleDatatypeEnumeration.INT.value());
			    long days = Long.parseLong(prop.getProperty(key));
			    long secs = days * 86400;
			    type.setValue(Long.toString(secs));
			    item.setMinPasswdAge(type);
			} else if ("MinimumPasswordLength".equals(key)) {
			    EntityItemIntType type = Factories.sc.core.createEntityItemIntType();
			    type.setDatatype(SimpleDatatypeEnumeration.INT.value());
			    type.setValue(prop.getProperty(key));
			    item.setMinPasswdLen(type);
			} else if ("PasswordHistorySize".equals(key)) {
			    EntityItemIntType type = Factories.sc.core.createEntityItemIntType();
			    type.setDatatype(SimpleDatatypeEnumeration.INT.value());
			    type.setValue(prop.getProperty(key));
			    item.setPasswordHistLen(type);
			} else if ("ClearTextPassword".equals(key)) {
			    EntityItemBoolType type = Factories.sc.core.createEntityItemBoolType();
			    type.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
			    type.setValue(prop.getProperty(key));
			    item.setReversibleEncryption(type);
			} else if ("PasswordComplexity".equals(key)) {
			    EntityItemBoolType type = Factories.sc.core.createEntityItemBoolType();
			    type.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
			    type.setValue(prop.getProperty(key));
			    item.setPasswordComplexity(type);
			}
		    }
		    items = Arrays.asList(item);
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
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    error = new CollectException(e.getMessage(), FlagEnumeration.ERROR);
	    throw error;
	}
    }
}
