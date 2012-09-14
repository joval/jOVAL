// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.adapter.windows;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.EntityObjectFieldType;
import oval.schemas.definitions.core.EntityObjectRecordType;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.windows.CmdletObject;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemFieldType;
import oval.schemas.systemcharacteristics.core.EntityItemRecordType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.EntityItemVersionType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.windows.CmdletItem;
import oval.schemas.systemcharacteristics.windows.EntityItemGUIDType;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.windows.powershell.IRunspace;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.os.windows.powershell.PowershellException;
import org.joval.oval.CollectException;
import org.joval.oval.OvalException;
import org.joval.oval.SystemCharacteristics;
import org.joval.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Retrieves windows:cmdlet_items.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class CmdletAdapter implements IAdapter {
    public static HashSet<String> VERB_WHITELIST = new HashSet<String>();
    static {
	VERB_WHITELIST.add("Approve");
	VERB_WHITELIST.add("Assert");
	VERB_WHITELIST.add("Compare");
	VERB_WHITELIST.add("Confirm");
	VERB_WHITELIST.add("Find");
	VERB_WHITELIST.add("Get");
	VERB_WHITELIST.add("Import");
	VERB_WHITELIST.add("Measure");
	VERB_WHITELIST.add("Read");
	VERB_WHITELIST.add("Request");
	VERB_WHITELIST.add("Resolve");
	VERB_WHITELIST.add("Search");
	VERB_WHITELIST.add("Select");
	VERB_WHITELIST.add("Show");
	VERB_WHITELIST.add("Test");
	VERB_WHITELIST.add("Trace");
	VERB_WHITELIST.add("Watch");
	VERB_WHITELIST.add("");
    }

    private IWindowsSession session;
    private IRunspace runspace;
    private HashMap<String, ModuleInfo> modules;

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IWindowsSession) {
	    this.session = (IWindowsSession)session;
	    classes.add(CmdletObject.class);
	}
	return classes;
    }

    public Collection<? extends ItemType> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	initialize();
	if (runspace == null) {
	    throw new CollectException(JOVALMsg.getMessage(JOVALMsg.ERROR_POWERSHELL), FlagEnumeration.NOT_COLLECTED);
	}

	CmdletObject cObj = (CmdletObject)obj;
	validateOperation(cObj);

	//
	// Validate that the verb is in the whitelist
	//
	String verb = (String)cObj.getVerb().getValue();
	if (!VERB_WHITELIST.contains(verb)) {
	    throw new CollectException(JOVALMsg.getMessage(JOVALMsg.ERROR_CMDLET_VERB, verb), FlagEnumeration.ERROR);
	}

	//
	// Validate that the module is available, and that the available module matches the specified module version
	// and GUID.
	//
	String moduleName=null, moduleId=null, moduleVersion=null;
	if (cObj.isSetModuleName()) {
	    moduleName = (String)cObj.getModuleName().getValue().getValue();
	}
	if (cObj.isSetModuleId()) {
	    moduleId = (String)cObj.getModuleId().getValue().getValue();
	}
	if (cObj.isSetModuleVersion()) {
	    moduleVersion = (String)cObj.getModuleVersion().getValue().getValue();
	}
	ModuleInfo info = null;
	if (moduleName != null) {
	    if (modules.containsKey(moduleName)) {
		info = modules.get(moduleName);
	    } else {
		throw new CollectException(JOVALMsg.getMessage(JOVALMsg.ERROR_CMDLET_MODULE,
			moduleName), FlagEnumeration.NOT_APPLICABLE);
	    }
	}
	if (moduleId != null) {
	    if (info == null) {
		for (ModuleInfo m : modules.values()) {
		    if (moduleId.equals(m.getModuleId())) {
			moduleName = m.getModuleName();
			info = m;
			break;
		    }
		}
		if (moduleName == null) {
		    throw new CollectException(JOVALMsg.getMessage(JOVALMsg.ERROR_CMDLET_MODULE,
			moduleId), FlagEnumeration.NOT_APPLICABLE);
		}
	    } else if (!moduleId.equals(info.getModuleId())) {
		throw new CollectException(JOVALMsg.getMessage(JOVALMsg.ERROR_CMDLET_GUID,
			moduleName, moduleId, info.getModuleId()), FlagEnumeration.NOT_APPLICABLE);
	    }
	}
	if (moduleVersion != null) {
	    if (!moduleVersion.equals(info.getModuleVersion())) {
		throw new CollectException(JOVALMsg.getMessage(JOVALMsg.ERROR_CMDLET_VERSION,
			moduleName, moduleVersion, info.getModuleVersion()), FlagEnumeration.NOT_APPLICABLE);
	    }
	}

	Collection<CmdletItem> items = new ArrayList<CmdletItem>();
	CmdletItem item = Factories.sc.windows.createCmdletItem();
	try {
	    //
	    // Now that we have validated that the module is available and correct, we import it if it's not
	    // already loaded.
	    //
	    if (!info.isLoaded()) {
		runspace.invoke("Import-Module " + moduleName);
		info.setLoaded(true);
	    }

	    String noun = (String)cObj.getNoun().getValue();
	    StringBuffer command = new StringBuffer(verb).append("-").append(noun);
	    if (cObj.isSetParameters() && cObj.getParameters().getValue() != null) {
		command.append(toParameterString(cObj.getParameters().getValue()));
	    }
	    if (cObj.isSetSelect() && cObj.getSelect().getValue() != null) {
		command.append(" | Select-Object");
		command.append(toSelectString(cObj.getSelect().getValue()));
	    }
	    command.append(" | ConvertTo-OVAL");
	    String data = runspace.invoke(command.toString());
	    if (data != null) {
		//
		// ConvertTo-OVAL outputs one huge line, so it's necessary to join together any line-splitting that
		// Powershell did "for" us.
		//
		StringBuffer sb = new StringBuffer();
		for (String line : data.split("\r\n")) {
		    sb.append(line);
		}
		Object result = SystemCharacteristics.parse(new ByteArrayInputStream(sb.toString().getBytes()));
		if (result instanceof JAXBElement) {
		    result = ((JAXBElement)result).getValue();
		}
		if (result instanceof CmdletItem) {
		    item = (CmdletItem)result;
		}
	    }

	    EntityItemGUIDType moduleIdType = Factories.sc.windows.createEntityItemGUIDType();
	    moduleIdType.setValue(moduleId);
	    item.setModuleId(Factories.sc.windows.createCmdletItemModuleId(moduleIdType));

	    EntityItemStringType moduleNameType = Factories.sc.core.createEntityItemStringType();
	    moduleNameType.setValue(moduleName);
	    item.setModuleName(Factories.sc.windows.createCmdletItemModuleName(moduleNameType));

	    EntityItemVersionType moduleVersionType = Factories.sc.core.createEntityItemVersionType();
	    moduleVersionType.setValue(moduleVersion);
	    item.setModuleVersion(Factories.sc.windows.createCmdletItemModuleVersion(moduleVersionType));

	    if (cObj.isSetParameters()) {
		EntityItemRecordType record = Factories.sc.core.createEntityItemRecordType();
		if (cObj.getParameters().getValue() != null) {
		    record = toItemRecord(cObj.getParameters().getValue());
		}
		item.setParameters(Factories.sc.windows.createCmdletItemParameters(record));
	    }

	    if (cObj.isSetSelect()) {
		EntityItemRecordType record = Factories.sc.core.createEntityItemRecordType();
		if (cObj.getSelect().getValue() != null) {
		    record = toItemRecord(cObj.getSelect().getValue());
		}
		item.setSelect(Factories.sc.windows.createCmdletItemSelect(record));
	    }

	    items.add(item);

	    // DAS: should we remove the module from the runspace when the query is complete?
	    // runspace.invoke("Remove-Module " + moduleName);
	} catch (Exception e) {
	    String s = JOVALMsg.getMessage(JOVALMsg.ERROR_CMDLET, e.getMessage());
	    session.getLogger().warn(s);
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(s);
	    item.getMessage().add(msg);
	    item.setStatus(StatusEnumeration.ERROR);
	}

	return items;
    }

    // Private

    /**
     * Idempotent
     */
    private void initialize() {
	if (modules == null) {
	    modules = new HashMap<String, ModuleInfo>();
	} else {
	    return; // previously initialized
	}

	//
	// Get a runspace if there are any in the pool, or create a new one, and load the Get-AccessTokens
	// Powershell module code.
	//
	for (IRunspace rs : session.getRunspacePool().enumerate()) {
	    runspace = rs;
	    break;
	}
	try {
	    if (runspace == null) {
		runspace = session.getRunspacePool().spawn();
	    }
	    if (runspace != null) {
		runspace.loadModule(getClass().getResourceAsStream("Cmdlet.psm1"));
	    }

	    //
	    // Enumerate available modules with manifest information.
	    //
	    String data = runspace.invoke("Get-ModuleInfo");
	    if (data != null) {
		for (String moduleData : data.split("ModuleName=")) {
		    ModuleInfo info = new ModuleInfo(moduleData);
		    modules.put(info.getModuleName(), info);
		}
	    }
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }

    /**
     * The CmdletObject only makes sense if all its entities use the EQUALS OperationEnumeration member.
     */
    void validateOperation(CmdletObject cObj) throws CollectException {
	OperationEnumeration op = OperationEnumeration.EQUALS;
	do {
	    if (cObj.isSetModuleName() &&
		(op = cObj.getModuleName().getValue().getOperation()) != OperationEnumeration.EQUALS) {
		break;
	    }
	    if (cObj.isSetModuleId() &&
		(op = cObj.getModuleId().getValue().getOperation()) != OperationEnumeration.EQUALS) {
		break;
	    }
	    if (cObj.isSetModuleVersion() &&
		(op = cObj.getModuleVersion().getValue().getOperation()) != OperationEnumeration.EQUALS) {
		break;
	    }
	    if (cObj.isSetVerb() &&
		(op = cObj.getVerb().getOperation()) != OperationEnumeration.EQUALS) {
		break;
	    }
	    if (cObj.isSetNoun() &&
		(op = cObj.getNoun().getOperation()) != OperationEnumeration.EQUALS) {
		break;
	    }
	    if (cObj.isSetParameters() && cObj.getParameters().getValue() != null &&
		(op = cObj.getParameters().getValue().getOperation()) != OperationEnumeration.EQUALS) {
		break;
	    }
	    if (cObj.isSetSelect() && cObj.getSelect().getValue() != null &&
		(op = cObj.getSelect().getValue().getOperation()) != OperationEnumeration.EQUALS) {
		break;
	    }
	} while(false);
	if (op != OperationEnumeration.EQUALS) {
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
	    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	}
    }

    /**
     * Convert an EntityItemRecordType into a validated cmdlet parameter String. The result always begins with
     * a space, if there are any parameters defined in the record at all.
     */
    private String toParameterString(EntityObjectRecordType record) throws IllegalArgumentException {
	StringBuffer sb = new StringBuffer();
	for (EntityObjectFieldType field : record.getField()) {
	    if (field.getName().indexOf(" ") == -1) {
		sb.append(" ").append(field.getName());
	    } else {
		throw new IllegalArgumentException(JOVALMsg.getMessage(JOVALMsg.ERROR_CMDLET_FIELD, field.getName()));
	    }
	    if (field.isSetValue()) {
		String val = (String)field.getValue();
		// TBD: validate the datatype using the type classes?

		//
		// quote the value if it contains a space, unless it's already quoted
		//
		if (val.indexOf(" ") != -1 && !(val.startsWith("\"") && val.endsWith("\""))) {
		    val = new StringBuffer("\"").append(val).append("\"").toString();
		}
		sb.append(" ").append(val);
	    }
	}
	return sb.toString();
    }

    /**
     * Convert an EntityItemRecordType into a validated parameter String for the Select-Object cmdlet. The result always
     * begins with a space, if there are any parameters defined in the record at all.
     */
    private String toSelectString(EntityObjectRecordType record) throws IllegalArgumentException {
	StringBuffer sb = new StringBuffer();
	for (EntityObjectFieldType field : record.getField()) {
	    if ("*".equals(field.getName()) || field.getName().indexOf(" ") != -1) {
		throw new IllegalArgumentException(JOVALMsg.getMessage(JOVALMsg.ERROR_CMDLET_FIELD, field.getName()));
	    } else {
		if (sb.length() == 0) {
		    sb.append(" ");
		} else {
		    sb.append(",");
		}
		sb.append(field.getName());
	    }
	}
	return sb.toString();
    }

    private EntityItemRecordType toItemRecord(EntityObjectRecordType record) {
	EntityItemRecordType irecord = Factories.sc.core.createEntityItemRecordType();
	for (EntityObjectFieldType field : record.getField()) {
	    EntityItemFieldType ifield = Factories.sc.core.createEntityItemFieldType();
	    ifield.setName(field.getName());
	    if (field.isSetDatatype()) {
		ifield.setDatatype(field.getDatatype());
	    }
	    if (field.isSetValue()) {
		ifield.setValue(field.getValue());
	    }
	    irecord.getField().add(ifield);
	}
	return irecord;
    }

    class ModuleInfo {
	private String moduleName;
	private Properties props;
	private boolean loaded;

	ModuleInfo(String data) {
	    loaded = false;
	    props = new Properties();
	    if (data != null) {
		boolean open = false;
		for (String line : data.split("\n")) {
		    line = line.trim();
		    if (!open && line.equals("@{")) {
			open = true;
		    } else if (!open) {
			if (moduleName == null) {
			    moduleName = line;
			} else if (line.startsWith("Status=")) {
			    loaded = line.endsWith("=loaded");
			}
		    } else if (open && line.equals("}")) {
			break;
		    } else if (open && !line.startsWith("#")) {
			int ptr = line.indexOf("=");
			if (ptr > 0) {
			    String key = line.substring(0,ptr);
			    String val = line.substring(ptr+1);
			    if (val.startsWith("\"") && val.endsWith("\"")) {
				val = val.substring(1, val.length()-1);
			    }
			    props.setProperty(key, val);
			}
		    }
		}
	    }
	}

	void setLoaded(boolean loaded) {
	    this.loaded = loaded;
	}

	boolean isLoaded() {
	    return loaded;
	}

	String getModuleId() {
	    return props.getProperty("GUID");
	}

	String getModuleVersion() {
	    return props.getProperty("ModuleVersion");
	}

	String getModuleName() {
	    return moduleName;
	}
    }
}
