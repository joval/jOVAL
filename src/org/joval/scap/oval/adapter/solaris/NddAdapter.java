// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.solaris;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import jsaf.intf.system.ISession;
import jsaf.intf.unix.system.IUnixSession;
import jsaf.util.SafeCLI;
import jsaf.util.StringTools;

import scap.oval.common.MessageType;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.OperationEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.solaris.NddObject;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.EntityItemAnySimpleType;
import scap.oval.systemcharacteristics.core.EntityItemIntType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.solaris.NddItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Collects items for NddObjects.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class NddAdapter implements IAdapter {
    private IUnixSession session;
    private Collection<String> drivers;
    private Map<String, Collection<String>> parameters;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IUnixSession && ((IUnixSession)session).getFlavor() == IUnixSession.Flavor.SOLARIS) {
	    this.session = (IUnixSession)session;
	    classes.add(NddObject.class);
	    parameters = new HashMap<String, Collection<String>>();
	} else {
	    notapplicable.add(NddObject.class);
	}
	return classes;
    }

    public Collection<NddItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	NddObject nObj = (NddObject)obj;
	try {
	    //
	    // First get devices matching the object entity
	    //
	    List<String> devices = new ArrayList<String>();
	    OperationEnumeration op = nObj.getDevice().getOperation();
	    switch(op) {
	      case EQUALS:
		if (getDevices().contains((String)nObj.getDevice().getValue())) {
		    devices.add((String)nObj.getDevice().getValue());
		}
		break;

	      case PATTERN_MATCH:
		Pattern p = StringTools.pattern((String)nObj.getDevice().getValue());
		for (String device : getDevices()) {
		    if (p.matcher(device).find()) {
			devices.add(device);
		    }
		}
		break;

	      case NOT_EQUAL:
		for (String device : getDevices()) {
		    if (!device.equals((String)nObj.getDevice().getValue())) {
			devices.add(device);
		    }
		}
		break;

	      default:
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	    }

	    //
	    // Next, get parameters matching those available for the devices
	    //
	    Map<String, List<String>> deviceParams = new HashMap<String, List<String>>();
	    for (String device : devices) {
		try {
		    op = nObj.getParameter().getOperation();
		    switch(op) {
		      case EQUALS:
			if (getParameters(device).contains((String)nObj.getParameter().getValue())) {
			    if (!deviceParams.containsKey(device)) {
				deviceParams.put(device, new ArrayList<String>());
			    }
			    String parameter = SafeCLI.checkArgument((String)nObj.getParameter().getValue(), session);
			    deviceParams.get(device).add(parameter);
			}
			break;

		      case PATTERN_MATCH:
			Pattern p = StringTools.pattern((String)nObj.getParameter().getValue());
			for (String parameter : getParameters(device)) {
			    if (p.matcher(parameter).find()) {
				if (!deviceParams.containsKey(device)) {
				    deviceParams.put(device, new ArrayList<String>());
				}
				deviceParams.get(device).add(parameter);
			    }
			}
			break;

		      case NOT_EQUAL:
			for (String parameter : getParameters(device)) {
			    if (!parameter.equals((String)nObj.getParameter().getValue())) {
				if (!deviceParams.containsKey(device)) {
				    deviceParams.put(device, new ArrayList<String>());
				}
				deviceParams.get(device).add(parameter);
			    }
			}
			break;

		      default:
			String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
			throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
		    }
		} catch (NoSuchElementException e) {
		    // Turns out there is no such device, so skip it
		}
	    }

	    Collection<NddItem> items = new ArrayList<NddItem>();
	    for (Map.Entry<String, List<String>> entry : deviceParams.entrySet()) {
                try {
                    items.addAll(makeItems(entry.getKey(), entry.getValue()));
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

    private Collection<NddItem> makeItems(String device, List<String> parameters) throws Exception {
	Collection<NddItem> items = new ArrayList<NddItem>();
	for (String parameter : parameters) {
	    NddItem item = Factories.sc.solaris.createNddItem();

	    EntityItemStringType deviceType = Factories.sc.core.createEntityItemStringType();
	    deviceType.setValue(device);
	    item.setDevice(deviceType);

	    EntityItemStringType parameterType = Factories.sc.core.createEntityItemStringType();
	    parameterType.setValue(parameter);
	    item.setParameter(parameterType);

	    EntityItemAnySimpleType valueType = Factories.sc.core.createEntityItemAnySimpleType();
	    String cmd = "/usr/sbin/ndd -get '" + device + "' '" + parameter + "'";
	    byte[] data = SafeCLI.execData(cmd, null, session, session.getTimeout(IUnixSession.Timeout.S)).getData();
	    valueType.setValue(new String(data, StringTools.UTF8).trim());
	    item.setValue(valueType);

	    items.add(item);
	}
	return items;
    }

    /**
     * Lists all the drivers under the /dev directory.
     */
    private Collection<String> getDevices() throws Exception {
	if (drivers == null) {
	    drivers = new ArrayList<String>();
	    for (String driverName : session.getFilesystem().getFile("/dev").list()) {
		drivers.add("/dev/" + driverName);
	    }
	}
	return drivers;
    }

    /**
     * Get a list of parameters relevant for the specified driver.
     *
     * @throws NoSuchElementException if the driver is not valid
     */
    private Collection<String> getParameters(String driver) throws Exception {
	if (parameters.containsKey(driver)) {
	    return parameters.get(driver);
	} else if (getDevices().contains(driver)) {
	    parameters.put(driver, new ArrayList<String>());
	    String cmd = "/usr/sbin/ndd '" + driver + "' ?";
	    SafeCLI.ExecData ed = SafeCLI.execData(cmd, null, session, session.getTimeout(IUnixSession.Timeout.S));
	    switch(ed.getExitCode()) {
	      case 0:
		for (String line : ed.getLines()) {
		    StringTokenizer tok = new StringTokenizer(line);
		    if (tok.countTokens() > 0) {
			parameters.get(driver).add(tok.nextToken());
		    }
		}
		return parameters.get(driver);

	      //
	      // NDD doesn't work with all devices in /dev, so if there's an error, we remove the device from the list
	      // and return as if there is no such device.
	      //
	      default:
		drivers.remove(driver);
		throw new NoSuchElementException(driver);
	    }
	} else {
	    throw new NoSuchElementException(driver);
	}
    }
}
