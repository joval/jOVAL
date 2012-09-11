// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.adapter.windows;

import java.io.IOException;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Vector;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.windows.PasswordpolicyObject;
import oval.schemas.systemcharacteristics.core.EntityItemBoolType;
import oval.schemas.systemcharacteristics.core.EntityItemIntType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.windows.PasswordpolicyItem;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.util.IProperty;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.oval.CollectException;
import org.joval.oval.Factories;
import org.joval.oval.OvalException;
import org.joval.util.IniFile;
import org.joval.util.JOVALMsg;
import org.joval.util.SafeCLI;
import org.joval.util.StringTools;

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

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IWindowsSession) {
	    this.session = (IWindowsSession)session;
	    classes.add(PasswordpolicyObject.class);
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
		    file = session.getFilesystem().getFile(secpol, IFile.READWRITE);
		    IniFile config = new IniFile(file.getInputStream(), StringTools.UTF16LE);
		    items = new Vector<PasswordpolicyItem>();
		    PasswordpolicyItem item = Factories.sc.windows.createPasswordpolicyItem();
		    IProperty prop = config.getSection("System Access");
		    for (String key : prop) {
			if ("MaximumPasswordAge".equals(key)) {
			    EntityItemIntType type = Factories.sc.core.createEntityItemIntType();
			    type.setDatatype(SimpleDatatypeEnumeration.INT.value());
			    type.setValue(prop.getProperty(key));
			    item.setMaxPasswdAge(type);
			} else if ("MinimumPasswordAge".equals(key)) {
			    EntityItemIntType type = Factories.sc.core.createEntityItemIntType();
			    type.setDatatype(SimpleDatatypeEnumeration.INT.value());
			    type.setValue(prop.getProperty(key));
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
			    item.setReversibleEncryption(type);
			}
		    }
		    items.add(item);
		} catch (NoSuchElementException e) {
		    error = new CollectException(e.getMessage(), FlagEnumeration.NOT_APPLICABLE);
		    throw error;
		} catch (IOException e) {
		    session.getLogger().warn(JOVALMsg.ERROR_IO, secpol, e.getMessage());
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
	    session.getLogger().warn(JOVALMsg.ERROR_PROCESS_CREATE, e.getMessage());
	    error = new CollectException(e.getMessage(), FlagEnumeration.ERROR);
	    throw error;
	}
    }
}
