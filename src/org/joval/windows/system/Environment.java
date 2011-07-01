// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.windows.system;

import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import org.joval.intf.system.IEnvironment;
import org.joval.intf.windows.registry.IKey;
import org.joval.intf.windows.registry.IRegistry;
import org.joval.intf.windows.registry.IValue;
import org.joval.intf.windows.registry.IStringValue;
import org.joval.intf.windows.registry.IExpandStringValue;
import org.joval.util.JOVALSystem;

/**
 * A representation of the Windows SYSTEM environment, retrieved from the registry.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Environment implements IEnvironment {
    public static final String ARCHITECTURE = "PROCESSOR_ARCHITECTURE";

    static final String SYSTEMROOT	= "SYSTEMROOT";
    static final String PATH		= "PATH";

    static final String[] SYSROOT_ENV	= {IRegistry.HKLM, "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion"};
    static final String[] SYSTEM_ENV	= {IRegistry.HKLM, "SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Environment"};
    static final String[] USER_ENV	= {IRegistry.HKCU, "Environment"};
    static final String[] VOLATILE_ENV	= {IRegistry.HKCU, "Volatile Environment"};

    private Properties props;

    public Environment(IRegistry registry) {
	props = new Properties();
	Vector <String>toExpand = new Vector <String>();

	IKey cv = null, env = null;
	try {
	    cv = registry.fetchKey(SYSROOT_ENV[0], SYSROOT_ENV[1]);
	    IValue sysRootValue = cv.getValue("SystemRoot");
	    if (sysRootValue.getType() == IValue.REG_SZ) {
		String sysRoot = ((IStringValue)sysRootValue).getData();
		props.setProperty(SYSTEMROOT, sysRoot);
		sysRoot = Matcher.quoteReplacement(sysRoot);
	    } else {
		throw new RuntimeException(JOVALSystem.getMessage("ERROR_WINENV_SYSROOT"));
	    }

	    env = registry.fetchKey(SYSTEM_ENV[0], SYSTEM_ENV[1]);
	    for (Iterator <IValue> i = env.values(); i.hasNext(); ) {
		IValue val = i.next();
		String name = val.getName().toUpperCase();
		if (val.getType() == IValue.REG_SZ) {
		    props.setProperty(name,  ((IStringValue)val).getData());
		} else if (val.getType() == IValue.REG_EXPAND_SZ) {
		    toExpand.addElement(name);
		    props.setProperty(name, ((IExpandStringValue)val).getData());
		} else {
		    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_WINENV_NONSTR" + val.getName()));
		}
	    }
	} catch (NoSuchElementException e) {
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_WINENV_SYSENV"));
	} finally {
	    if (cv != null) {
		cv.closeAll();
	    }
	    if (env != null) {
		env.closeAll();
	    }
	}

	IKey userEnv = null;
	try {
	    userEnv = registry.fetchKey(USER_ENV[0], USER_ENV[1]);
	    for (Iterator <IValue> i = userEnv.values(); i.hasNext(); ) {
		IValue val = i.next();
		String name = val.getName().toUpperCase();
		if (val.getType() == IValue.REG_SZ) {
		    props.setProperty(name, ((IStringValue)val).getData());
		} else if (val.getType() == IValue.REG_EXPAND_SZ) {
		    String s = ((IExpandStringValue)val).getData();

		    //
		    // Special case of a user-defined PATH, which must be expanded immediately so as to include the
		    // SYSTEM-defined PATH.
		    //
		    if (name.equals(PATH)) {
			props.setProperty(name, s.replaceAll("(?i)%PATH%", Matcher.quoteReplacement(props.getProperty(PATH))));
		    } else {
			if (!toExpand.contains(name)) {
			    toExpand.addElement(name);
			}
			props.setProperty(name, s);
		    }
		} else {
		    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_WINENV_NONSTR" + val.getName()));
		}
	    }
	} catch (NoSuchElementException e) {
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_WINENV_USRENV"));
	} finally {
	    if (userEnv != null) {
		userEnv.closeAll();
	    }
	}

	IKey volatileEnv = null;
	try {
	    volatileEnv = registry.fetchKey(VOLATILE_ENV[0], VOLATILE_ENV[1]);
	    for (Iterator <IValue> i = volatileEnv.values(); i.hasNext(); ) {
		IValue val = i.next();
		String name = val.getName().toUpperCase();
		if (val.getType() == IValue.REG_SZ) {
		    props.setProperty(name, ((IStringValue)val).getData());
		} else if (val.getType() == IValue.REG_EXPAND_SZ) {
		    if (!toExpand.contains(name)) {
			toExpand.addElement(name);
		    }
		    props.setProperty(name, ((IExpandStringValue)val).getData());
		} else {
		    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_WINENV_NONSTR" + val.getName()));
		}
	    }
	} catch (NoSuchElementException e) {
	    JOVALSystem.getLogger().log(Level.INFO, JOVALSystem.getMessage("ERROR_WINENV_VOLENV"));
	} finally {
	    if (volatileEnv != null) {
		volatileEnv.closeAll();
	    }
	}

	Enumeration <String>list = toExpand.elements();
	while (list.hasMoreElements()) {
	    String name = list.nextElement();
	    props.setProperty(name, expand(props.getProperty(name)));
	}
    }

    // Implement IEnvironment

    public String expand(String data) {
	if (data.indexOf('%') < 0) {
	    return data;
	}
	String originalData = data;
	Iterator <String>names = props.stringPropertyNames().iterator();
	while (names.hasNext()) {
	    String name = names.next();
	    String pattern = new StringBuffer("(?i)%").append(name).append('%').toString();
	    data = data.replaceAll(pattern, Matcher.quoteReplacement(props.getProperty(name)));
	}
	if (data.equals(originalData)) {
	    return data; // Some unexpandable pattern exists in there
	} else {
	    return expand(data); // Recurse, in case a variable includes another
	}
    }

    /**
     * Get an environment variable!
     */
    public String getenv(String var) {
	return props.getProperty(var.toUpperCase());
    }

    public Iterator<String> iterateVariables() {
	return props.stringPropertyNames().iterator();
    }
}
