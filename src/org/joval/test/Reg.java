// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.logging.*;

import org.joval.intf.system.ISession;
import org.joval.intf.windows.registry.IRegistry;
import org.joval.intf.windows.registry.IKey;
import org.joval.intf.windows.registry.IValue;
import org.joval.intf.windows.registry.IBinaryValue;
import org.joval.intf.windows.registry.IDwordValue;
import org.joval.intf.windows.registry.IExpandStringValue;
import org.joval.intf.windows.registry.IMultiStringValue;
import org.joval.intf.windows.registry.IStringValue;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.io.LittleEndian;

public class Reg {
    IWindowsSession session;

    public Reg(ISession session) {
	if (session instanceof IWindowsSession) {
	    this.session = (IWindowsSession)session;
	}
    }

    public void test(String keyName, String valueName) {
	try {
	    IRegistry r = session.getRegistry(IWindowsSession.View._64BIT);
	    if (r.connect()) {
		IKey key = r.fetchKey(keyName);

		if (valueName == null) {
		    String[] sa = key.listValues();
		    for (int i=0; i < sa.length; i++) {
			System.out.println("Value name: " + sa[i] + " val: " + key.getValue(sa[i]).toString());
		    }
		} else {
		    IValue value = r.fetchValue(key, valueName);
		    switch(value.getType()) {
		      case IValue.REG_DWORD:
			System.out.println("Dword: " + ((IDwordValue)value).getData());
			break;

		      case IValue.REG_BINARY: {
			byte[] buff = ((IBinaryValue)value).getData();
			StringBuffer sb = new StringBuffer();
			System.out.println("Binary:");
			for (int i=0; i < buff.length; i++) {
			    if (i > 0 && (i % 16) == 0) {
				sb.append("\n");
			    }
			    sb.append(" ").append(LittleEndian.toHexString(buff[i]));
			}
			System.out.println(sb.toString());
			break;
		      }

		      case IValue.REG_SZ:
			System.out.println("String: " + ((IDwordValue)value).getData());
			break;

		      case IValue.REG_EXPAND_SZ:
			System.out.println("Expand String: " + ((IDwordValue)value).getData());
			break;

		      case IValue.REG_MULTI_SZ: {
			String[] sa = ((IMultiStringValue)value).getData();
			System.out.println("Multi String: (len " + sa.length + ")");
			for (int i=0; i < sa.length; i++) {
			    System.out.println("    " + sa[i]);
			}
			break;
		      }

		      default:
			System.out.println("Unexpected type: " + value.getType());
			break;
		    }
		}

		key.closeAll();
		r.disconnect();
	    } else {
	 	System.out.println("Failed to connect to registry");
	    }
	} catch (NoSuchElementException e) {
	    e.printStackTrace();
	}
    }
}

