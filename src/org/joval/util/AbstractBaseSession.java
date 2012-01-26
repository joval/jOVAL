// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util;

import java.util.Iterator;

import org.slf4j.cal10n.LocLogger;

import oval.schemas.common.FamilyEnumeration;
import oval.schemas.systemcharacteristics.core.SystemInfoType;

import org.joval.intf.system.IProcess;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.intf.util.IProperty;

/**
 * Base class for the SSH IBaseSession implementation, and the AbstractSession.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public abstract class AbstractBaseSession implements IBaseSession {
    protected LocLogger logger;
    protected boolean debug = false;
    protected InternalProperties internalProps;

    protected AbstractBaseSession() {
	logger = JOVALSystem.getLogger();
	internalProps = new InternalProperties();
    }

    /**
     * Subclasses may override this method in order to define an override key for any key that should specify an override
     * property key.  In the event the override key does not exist, the original key is used to fetch the value.
     */
    protected String getOverrideKey(String key) {
        return null;
    }

    /**
     * Subclasses may override this method if changes made to the IProperty should cause some immediate effect to occur
     * on the operation of the subclass.
     */
    protected void handlePropertyChange(String key, String value) {}

    // Implement ILoggable

    public LocLogger getLogger() {
	return logger;
    }

    public void setLogger(LocLogger logger) {
	this.logger = logger;
    }

    // Implement IBaseSession

    public long getTimeout(Timeout to) {
	switch(to) {
	  case M:
	    return internalProps.getLongProperty(PROP_READ_TIMEOUT_M);

	  case L:
	    return internalProps.getLongProperty(PROP_READ_TIMEOUT_L);

	  case XL:
	    return internalProps.getLongProperty(PROP_READ_TIMEOUT_XL);

	  case S:
	  default:
	    return internalProps.getLongProperty(PROP_READ_TIMEOUT_S);
	}
    }

    public IProperty getProperties() {
	return internalProps;
    }

    public boolean isDebug() {
	return internalProps.getBooleanProperty(PROP_DEBUG);
    }

    public FamilyEnumeration getFamily() {
	switch(getType()) {
	  case WINDOWS:
	    return FamilyEnumeration.WINDOWS;

	  case UNIX:
	    switch(((IUnixSession)this).getFlavor()) {
	      case MACOSX:
		return FamilyEnumeration.MACOS;
	      default:
		return FamilyEnumeration.UNIX;
	    }

	  case CISCO_IOS:
	    return FamilyEnumeration.IOS;

	  default:
	    return FamilyEnumeration.UNDEFINED;
	}
    }

    // All the abstract methods, for reference

    public abstract boolean connect();

    public abstract void disconnect();

    public abstract String getHostname();

    public abstract Type getType();

    public abstract SystemInfoType getSystemInfo();

    public abstract IProcess createProcess(String command) throws Exception;

    // Private

    protected class InternalProperties implements IProperty {
	private PropertyUtil props;

	protected InternalProperties() {
	    props = new PropertyUtil();
	}

	// Implement IProperty

	public void setProperty(String key, String value) {
	    props.setProperty(key, value);
	    handlePropertyChange(key, value);
	}

	public String getProperty(String key) {
	    String ok = getOverrideKey(key);
	    if (ok != null) {
		String val =  props.getProperty(ok);
		if (val != null) {
		    return val;
		}
	    }
	    return props.getProperty(key);
	}

	public long getLongProperty(String key) {
	    long l = 0L;
	    try {
		String val = getProperty(key);
		if (val != null) {
		    l = Long.parseLong(val);
		}
	    } catch (NumberFormatException e) {
	    }
	    return l;
	}

	public int getIntProperty(String key) {
	    int i = 0;
	    try {
		String val = getProperty(key);
		if (val != null) {
		    i = Integer.parseInt(val);
		}
	    } catch (NumberFormatException e) {
	    }
	    return i;
	}

	public boolean getBooleanProperty(String key) {
	    return "true".equalsIgnoreCase(getProperty(key));
	}

	public Iterator<String> iterator() {
	    return props.iterator();
	}
    }
}
