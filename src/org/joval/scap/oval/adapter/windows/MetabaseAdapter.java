// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.windows;

import java.io.IOException;
import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.xml.bind.JAXBElement;

import jsaf.intf.system.ISession;
import jsaf.intf.windows.powershell.IRunspace;
import jsaf.intf.windows.system.IWindowsSession;
import jsaf.intf.windows.wmi.IWmiProvider;
import jsaf.provider.windows.powershell.PowershellException;
import jsaf.provider.windows.wmi.WmiException;
import jsaf.util.Base64;
import jsaf.util.SafeCLI;
import jsaf.util.StringTools;

import scap.oval.common.MessageType;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.OperationEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.core.EntityObjectIntType;
import scap.oval.definitions.core.EntityObjectStringType;
import scap.oval.definitions.windows.MetabaseObject;
import scap.oval.systemcharacteristics.core.EntityItemAnySimpleType;
import scap.oval.systemcharacteristics.core.EntityItemIntType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.windows.EntityItemSystemMetricIndexType;
import scap.oval.systemcharacteristics.windows.MetabaseItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;
import org.joval.xml.XSITools;

/**
 * Retrieves windows:metabase_items.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class MetabaseAdapter implements IAdapter {
    private IWindowsSession session;
    private CollectException error = null;
    private IRunspace runspace = null;
    private Properties idNames;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IWindowsSession) {
	    //
	    // Initially, any Windows machine will be considered applicable.  Then, if there are any Metabase
	    // tests in the content, the idempotent checkApplicable() method will be invoked to perform a deeper
	    // analysis of the machine's applicability.
	    //
	    this.session = (IWindowsSession)session;
	    classes.add(MetabaseObject.class);
	} else {
	    notapplicable.add(MetabaseObject.class);
	}
	return classes;
    }

    public Collection<? extends ItemType> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	init();
	MetabaseObject mObj = (MetabaseObject)obj;
	Collection<MetabaseItem> items = new ArrayList<MetabaseItem>();
	try {
	    //
	    // First, determine the matching key(s)
	    //
	    OperationEnumeration op = mObj.getKey().getOperation();
	    String s = (String)mObj.getKey().getValue();
	    List<String> keys = new ArrayList<String>();
	    switch(op) {
	      case CASE_INSENSITIVE_EQUALS:
	      case EQUALS: {
		StringBuffer sb = new StringBuffer("[jOVAL.Metabase.Probe]::TestKey(");
		sb.append("\"").append(SafeCLI.checkArgument(s, session)).append("\")");
		if ("true".equalsIgnoreCase(runspace.invoke(sb.toString()))) {
		    keys.add(s);
		}
		break;
	      }

	      case NOT_EQUAL: {
		StringBuffer sb = new StringBuffer("Find-MetabaseKeys");
		sb.append(" -Path \"").append("/").append("\"");
		sb.append(" | Transfer-Encode");
		String data = new String(Base64.decode(runspace.invoke(sb.toString())), StringTools.UTF8);
		for (String subkey : data.split("\r\n")) {
		    if (!s.equals(subkey)) {
			keys.add(subkey);
		    }
		}
		break;
	      }

	      case CASE_INSENSITIVE_NOT_EQUAL: {
		StringBuffer sb = new StringBuffer("Find-MetabaseKeys");
		sb.append(" -Path \"").append("/").append("\"");
		sb.append(" | Transfer-Encode");
		String data = new String(Base64.decode(runspace.invoke(sb.toString())), StringTools.UTF8);
		for (String subkey : data.split("\r\n")) {
		    if (!s.equalsIgnoreCase(subkey)) {
			keys.add(subkey);
		    }
		}
		break;
	      }

	      case PATTERN_MATCH: {
		StringBuffer sb = new StringBuffer("Find-MetabaseKeys");
		String safe = SafeCLI.checkArgument(s, session);
		String path = getSearchRoot(safe);
		if (path != null) {
		    sb.append(" -Path \"").append(path).append("\"");
		    sb.append(" -Pattern \"").append(StringTools.regexPosix2Powershell(safe)).append("\"");
		    sb.append(" | Transfer-Encode");
		    String data = new String(Base64.decode(runspace.invoke(sb.toString())), StringTools.UTF8);
		    for (String subkey : data.split("\r\n")) {
			keys.add(subkey);
		    }
		}
		break;
	      }

	      default:
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	    }

	    //
	    // Now, collect matching objects for each key
	    //
	    JAXBElement<EntityObjectIntType> metabaseId = mObj.getMetabaseId();
	    if (XSITools.isNil(metabaseId)) {
		// Key Only
		for (String key : keys) {
		    items.add(makeItem(key));
		}
	    } else {
		op = metabaseId.getValue().getOperation();
		switch(op) {
		  case EQUALS: {
		    String id = (String)metabaseId.getValue().getValue();
		    Map<String, Map<String, String>> data = getData(keys, id);
		    for (Map.Entry<String, Map<String, String>> entry : data.entrySet()) {
			items.add(makeItem(entry.getKey(), entry.getValue()));
		    }
		    break;
		  }

		  case NOT_EQUAL: {
		    Map<String, List<Map<String, String>>> data = getData(keys);
		    for (Map.Entry<String, List<Map<String, String>>> entry : data.entrySet()) {
			for (Map<String, String> datum : entry.getValue()) {
			    if (!((String)metabaseId.getValue().getValue()).equals(datum.get("DATA_ID"))) {
				items.add(makeItem(entry.getKey(), datum));
			    }
			}
		    }
		    break;
		  }

		  default:
		    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
		}
	    }
	} catch (PatternSyntaxException e) {
	    throw new CollectException(e, FlagEnumeration.ERROR);
	} catch (IOException e) {
	    throw new CollectException(e, FlagEnumeration.ERROR);
	} catch (PowershellException e) {
	    throw new CollectException(e, FlagEnumeration.ERROR);
	}

	return items;
    }

    // Private

    /**
     * Make an item for just a Metabase key.
     */
    private MetabaseItem makeItem(String key) {
	MetabaseItem item = Factories.sc.windows.createMetabaseItem();
	EntityItemStringType keyType = Factories.sc.core.createEntityItemStringType();
	keyType.setValue(key);
	item.setKey(keyType);
	return item;
    }

    /**
     * Make an item for a key and datum.
     */
    private MetabaseItem makeItem(String key, Map<String, String> datum) {
	MetabaseItem item = makeItem(key);

	String dataId = datum.get("DATA_ID");
	EntityItemIntType metabaseIdType = Factories.sc.core.createEntityItemIntType();
	metabaseIdType.setValue(dataId);
	metabaseIdType.setDatatype(SimpleDatatypeEnumeration.INT.value());
	item.setMetabaseId(Factories.sc.windows.createMetabaseItemId(metabaseIdType));

	EntityItemStringType nameType = Factories.sc.core.createEntityItemStringType();
	nameType.setValue(idNames.get(dataId));
	item.setName(nameType);

	EntityItemStringType userType = Factories.sc.core.createEntityItemStringType();
	userType.setValue(datum.get("USER_TYPE"));
	item.setUserType(userType);

	EntityItemStringType datatypeType = Factories.sc.core.createEntityItemStringType();
	String datatype = datum.get("DATA_TYPE");
	datatypeType.setValue(datatype);
	item.setDataType(datatypeType);

	if ("BINARY_METADATA".equals(datatype)) {
	    EntityItemAnySimpleType dataType = Factories.sc.core.createEntityItemAnySimpleType();
	    dataType.setValue(datum.get("VALUE"));
	    dataType.setDatatype(SimpleDatatypeEnumeration.BINARY.value());
	    item.getData().add(dataType);
	} else if ("STRING_METADATA".equals(datatype)) {
	    EntityItemAnySimpleType dataType = Factories.sc.core.createEntityItemAnySimpleType();
	    dataType.setValue(datum.get("VALUE"));
	    dataType.setDatatype(SimpleDatatypeEnumeration.STRING.value());
	    item.getData().add(dataType);
	} else if ("DWORD_METADATA".equals(datatype)) {
	    EntityItemAnySimpleType dataType = Factories.sc.core.createEntityItemAnySimpleType();
	    dataType.setValue(datum.get("VALUE"));
	    dataType.setDatatype(SimpleDatatypeEnumeration.INT.value());
	    item.getData().add(dataType);
	} else if ("MULTISZ_METADATA".equals(datatype)) {
	    for (String value : datum.get("VALUE").split("\0")) {
		EntityItemAnySimpleType dataType = Factories.sc.core.createEntityItemAnySimpleType();
		dataType.setValue(datum.get("VALUE"));
		dataType.setDatatype(SimpleDatatypeEnumeration.STRING.value());
		item.getData().add(dataType);
	    }
	}

	return item;
    }

    /**
     * Get the specified datum (ID) for all the specified keys.
     */
    private Map<String, Map<String, String>> getData(List<String> keys, String id) throws IOException, PowershellException {
	StringBuffer sb = new StringBuffer();
	for (String key : keys) {
	    if (sb.length() > 0) {
		sb.append(",");
	    }
	    sb.append("\"").append(key).append("\"");
	}
	sb.append(" | Get-MetabaseData -ID ").append(id);
	sb.append(" | Transfer-Encode");
	String s = runspace.invoke(sb.toString(), session.getTimeout(IWindowsSession.Timeout.L));
	Map<String, Map<String, String>> result = new HashMap<String, Map<String, String>>();
	String key = null;
	for (String line : new String(Base64.decode(s), StringTools.UTF8).split("\r\n")) {
	    if (line.startsWith("Key:")) {
		key = line.substring(4).trim();
	    } else if (key != null) {
		int ptr = line.indexOf(":");
		if (ptr != -1) {
		    String name = line.substring(0,ptr);
		    String value = line.substring(ptr+1).trim();
		    if (!result.containsKey(key)) {
			result.put(key, new HashMap<String, String>());
		    }
		    result.get(key).put(name, value);
		}
	    }
	}
	return result;
    }

    private static final String OPEN = "{";
    private static final String CLOSE = "}";

    /**
     * Get all data for all the specified keys.
     */
    private Map<String, List<Map<String, String>>> getData(List<String> keys) throws IOException, PowershellException {
	StringBuffer sb = new StringBuffer();
	for (String key : keys) {
	    if (sb.length() > 0) {
		sb.append(",");
	    }
	    sb.append("\"").append(SafeCLI.checkArgument(key, session)).append("\"");
	}
	sb.append(" | Get-MetabaseData | Transfer-Encode");
	String s = runspace.invoke(sb.toString(), session.getTimeout(IWindowsSession.Timeout.L));
	Map<String, List<Map<String, String>>> result = new HashMap<String, List<Map<String, String>>>();
	String key = null;
	Map<String, String> current = null;
	boolean open = false;
	for (String line : new String(Base64.decode(s), StringTools.UTF8).split("\r\n")) {
	    if (open) {
		if (line.equals(CLOSE)) {
		    result.get(key).add(current);
		    current = null;
		    open = false;
		} else {
		    int ptr = line.indexOf(":");
		    if (ptr != -1) {
			String name = line.substring(0,ptr);
			String value = line.substring(ptr+1).trim();
			current.put(name, value);
		    }
		}
	    } else if (line.startsWith("Key:")) {
		key = line.substring(4).trim();
		result.put(key, new ArrayList<Map<String, String>>());
	    } else if (line.equals(OPEN)) {
		current = new HashMap<String, String>();
		open = true;
	    }
	}
	return result;
    }

    /**
     * Returns null if the pattern cannot possibly be found in a Metabase.
     */
    private String getSearchRoot(String s) {
	if (s.startsWith("^")) {
	    s = s.substring(1);
	    if (s.startsWith("/")) {
		StringBuffer sb = new StringBuffer("/");
		StringTokenizer tok = new StringTokenizer(s.substring(1), "/");
		while(tok.hasMoreTokens()) {
		    String token = tok.nextToken();
		    if (StringTools.containsRegex(token)) {
			return sb.toString();
		    } else if (sb.length() > 1) {
			sb.append("/").append(token);
		    } else {
			sb.append(token);
		    }
		}
		return sb.toString();
	    } else {
		return null;
	    }
	}
	return "/";
    }

    /**
     * Idempotent initialization, first determines whether IIS2-6 is installed (or if IIS7 is installed in IIS6-
     * compatibility mode).
     *
     * For non-privileged users, the object will not be collected, because we can't even make the applicability
     * determination unless we're running in a privileged mode.
     */
    private void init() throws CollectException {
	if (error != null) {
	    throw error;
	} else if (runspace == null) {
	    try {
		idNames = new Properties();
		idNames.load(getClass().getResourceAsStream("Metabase.properties"));

		//
		// Get a runspace if there are any in the pool, or create a new one, and load the Metabase
		// Powershell module code.
		//
		IWindowsSession.View view = session.getNativeView();
		for (IRunspace rs : session.getRunspacePool().enumerate()) {
		    if (rs.getView() == view) {
			runspace = rs;
			break;
		    }
		}
		if (runspace == null) {
		    runspace = session.getRunspacePool().spawn(view);
		}
		runspace.loadAssembly(getClass().getResourceAsStream("Metabase.dll"));
		runspace.loadModule(getClass().getResourceAsStream("Metabase.psm1"));
	    } catch (Exception e) {
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		error = new CollectException(e.getMessage(), FlagEnumeration.ERROR);
		throw error;
	    }

	    //
	    // Deep applicability check
	    //
	    try {
		IWmiProvider wmi = this.session.getWmiProvider();
		boolean privileged = "true".equalsIgnoreCase(runspace.invoke("Check-Privileged"));
		if (privileged) {
		    wmi.execQuery("root\\MicrosoftIISv2", "Select Name from IISComputer");
		} else {
		    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_METABASE_PRIVILEGE);
		    error = new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
		    throw error;
		}
	    } catch (IOException e) {
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		error = new CollectException(e.getMessage(), FlagEnumeration.NOT_APPLICABLE);
		throw error;
	    } catch (PowershellException e) {
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		error = new CollectException(e.getMessage(), FlagEnumeration.NOT_APPLICABLE);
		throw error;
	    } catch (WmiException e) {
		//
		// Assume that the namespace was not found.
		//
		session.getLogger().warn(JOVALMsg.ERROR_WINWMI_GENERAL, e.getMessage());
		error = new CollectException(e.getMessage(), FlagEnumeration.NOT_APPLICABLE);
		throw error;
	    }
	}
	// If we're here, then the host is applicable.
    }
}
