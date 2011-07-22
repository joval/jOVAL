// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.windows;

import java.math.BigInteger;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.regex.Pattern;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.CheckEnumeration;
import oval.schemas.common.ExistenceEnumeration;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.MessageType;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.definitions.core.EntityObjectStringType;
import oval.schemas.definitions.core.EntityStateAnySimpleType;
import oval.schemas.definitions.core.ObjectComponentType;
import oval.schemas.definitions.core.ObjectRefType;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.core.StateRefType;
import oval.schemas.definitions.core.StateType;
import oval.schemas.definitions.windows.EntityObjectRegistryHiveType;
import oval.schemas.definitions.windows.RegistryObject;
import oval.schemas.definitions.windows.RegistryState;
import oval.schemas.definitions.windows.RegistryTest;
import oval.schemas.systemcharacteristics.core.EntityItemAnySimpleType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.core.VariableValueType;
import oval.schemas.systemcharacteristics.windows.EntityItemRegistryHiveType;
import oval.schemas.systemcharacteristics.windows.EntityItemRegistryTypeType;
import oval.schemas.systemcharacteristics.windows.RegistryItem;
import oval.schemas.results.core.ResultEnumeration;
import oval.schemas.results.core.TestedItemType;
import oval.schemas.results.core.TestedVariableType;
import oval.schemas.results.core.TestType;

import org.joval.intf.oval.IDefinitions;
import org.joval.intf.oval.ISystemCharacteristics;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IAdapterContext;
import org.joval.intf.windows.registry.IDwordValue;
import org.joval.intf.windows.registry.IExpandStringValue;
import org.joval.intf.windows.registry.IKey;
import org.joval.intf.windows.registry.IStringValue;
import org.joval.intf.windows.registry.IRegistry;
import org.joval.intf.windows.registry.IValue;
import org.joval.oval.OvalException;
import org.joval.util.JOVALSystem;
import org.joval.util.Version;

/**
 * Evaluates RegistryTest OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class RegistryAdapter implements IAdapter {
    private static final String DATATYPE_INT	= "int";

    private IAdapterContext ctx;
    private IDefinitions definitions;
    private IRegistry registry;
    private Hashtable<String, BigInteger> itemIds;

    private oval.schemas.systemcharacteristics.core.ObjectFactory coreFactory;
    private oval.schemas.systemcharacteristics.windows.ObjectFactory windowsFactory;

    public RegistryAdapter(IRegistry registry) {
	this.registry = registry;
	coreFactory = new oval.schemas.systemcharacteristics.core.ObjectFactory();
	windowsFactory = new oval.schemas.systemcharacteristics.windows.ObjectFactory();
	itemIds = new Hashtable<String, BigInteger>();
    }

    // Implement IAdapter

    public void init(IAdapterContext ctx) {
	this.ctx = ctx;
	definitions = ctx.getDefinitions();
    }

    public Class getObjectClass() {
	return RegistryObject.class;
    }

    public Class getTestClass() {
	return RegistryTest.class;
    }

    public Class getStateClass() {
	return RegistryState.class;
    }

    public Class getItemClass() {
	return RegistryItem.class;
    }

    public void scan(ISystemCharacteristics sc) throws OvalException {
	try {
	    registry.connect();
	    Iterator <ObjectType>iter = definitions.iterateLeafObjects(RegistryObject.class);
	    while (iter.hasNext()) {
		RegistryObject rObj = (RegistryObject)iter.next();
		ctx.status(rObj.getId());
		List<VariableValueType> variableValueTypes = new Vector<VariableValueType>();
		List<ItemWrapper> items = getItems(rObj, variableValueTypes);
		if (items.size() == 0) {
		    MessageType msg = new MessageType();
		    msg.setLevel(MessageLevelEnumeration.INFO);
		    msg.setValue(JOVALSystem.getMessage("ERROR_WINREG_KEY"));
		    sc.setObject(rObj.getId(), rObj.getComment(), rObj.getVersion(), FlagEnumeration.DOES_NOT_EXIST, msg);
		} else {
		    sc.setObject(rObj.getId(), rObj.getComment(), rObj.getVersion(), FlagEnumeration.COMPLETE, null);
		    for (ItemWrapper wrapper : items) {
			BigInteger itemId = itemIds.get(wrapper.identifier);
			if (itemId == null) {
			    itemId = sc.storeItem(windowsFactory.createRegistryItem(wrapper.item));
			    itemIds.put(wrapper.identifier, itemId);
			}
			sc.relateItem(rObj.getId(), itemId);
			for (VariableValueType var : variableValueTypes) {
			    sc.storeVariable(var);
			    sc.relateVariable(rObj.getId(), var.getVariableId());
			}
		    }
		}
	    }
	} finally {
	    registry.disconnect();
	}
    }

    public List<? extends ItemType> getItems(ObjectType ot) throws OvalException {
	return getItems((RegistryObject)ot);
    }

    public ResultEnumeration compare(StateType st, ItemType it) throws OvalException {
	if (match((RegistryState)st, (RegistryItem)it)) {
	    return ResultEnumeration.TRUE;
	} else {
	    return ResultEnumeration.FALSE;
	}
    }

    // Private

    private List<ItemType> getItems(RegistryObject rObj) throws OvalException {
	List<VariableValueType> temp = new Vector<VariableValueType>();
	Vector<ItemType> items = new Vector<ItemType>();
	for (ItemWrapper wrapper : getItems(rObj, temp)) {
	    items.add(wrapper.item);
	}
	return items;
    }

    private List<ItemWrapper> getItems(RegistryObject rObj, List<VariableValueType> variableValueTypes) throws OvalException {
	List<ItemWrapper> list = new Vector<ItemWrapper>();
	String id = rObj.getId();
	String hive=null, path=null;
	if (rObj.getKey().getValue().isSetVarRef()) {
	    try {
		String variableId = rObj.getKey().getValue().getVarRef();
		path = ctx.resolve(variableId, variableValueTypes);
	    } catch (NoSuchElementException e) {
       		ctx.log(Level.FINER, JOVALSystem.getMessage("STATUS_NOT_FOUND", e.getMessage(), id));
	    }
	} else {
	    path = (String)rObj.getKey().getValue().getValue();
	}
	if (path != null && rObj.isSetHive()) {
	    try {
		hive = (String)rObj.getHive().getValue();
		OperationEnumeration op = rObj.getKey().getValue().getOperation();
		if (OperationEnumeration.EQUALS == op) {
		    list.add(getItem(rObj, hive, path));
		} else if (OperationEnumeration.PATTERN_MATCH == op) {
		    for (IKey key : registry.search(hive, path)) {
			ItemWrapper wrapper = getItem(rObj, key.getHive(), key.getPath());
			if (wrapper.item.getStatus() == StatusEnumeration.EXISTS) {
			    list.add(wrapper);
			}
		    }
		} else {
		    throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_OPERATION", op));
		}
	    } catch (NoSuchElementException e) {
		// This can really only happen if the hive of a search key was invalid
       		ctx.log(Level.FINER, JOVALSystem.getMessage("STATUS_NOT_FOUND", e.getMessage(), id));
	    }
	}
	return list;
    }

    private ItemWrapper getItem(RegistryObject rObj, String hive, String path) throws OvalException {
	RegistryItem item = windowsFactory.createRegistryItem();
	EntityItemRegistryHiveType hiveType = windowsFactory.createEntityItemRegistryHiveType();
	hiveType.setValue(hive);
	item.setHive(hiveType);
	EntityItemStringType keyType = coreFactory.createEntityItemStringType();
	EntityItemAnySimpleType valueType = coreFactory.createEntityItemAnySimpleType();

	IKey key = null;
	String name = null;
	try {
	    key = registry.fetchKey(hive, path);
	    keyType.setValue(path);
	    item.setKey(windowsFactory.createRegistryItemKey(keyType));
	    name = addNameAndValue(rObj, item, valueType, key);
	    item.setStatus(StatusEnumeration.EXISTS);
	} catch (NoSuchElementException e) {
       	    ctx.log(Level.FINER, JOVALSystem.getMessage("STATUS_NOT_FOUND", e.getMessage(), rObj.getId()));
	    item.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    if (key == null) {
		keyType.setValue(path);
		keyType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
		item.setKey(windowsFactory.createRegistryItemKey(keyType));
	    } else if (item.isSetName()) {
		item.getName().getValue().setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    }
	}

	return new ItemWrapper(hive, path, name, item);
    }

    /**
     * @return the registry value name, or null if none is specified (i.e., it's just a Key).
     */
    private String addNameAndValue(RegistryObject rObj, RegistryItem item,
			 EntityItemAnySimpleType valueType, IKey key) throws NoSuchElementException, OvalException {
	String name = null;
	if (rObj.isSetName() && rObj.getName().getValue() != null) {
	    name = (String)rObj.getName().getValue().getValue();
	}
	if (name == null || "".equals(name)) {
	    item.unsetValue(); // Just a key without a name/value
	} else {
	    EntityItemStringType nameType = coreFactory.createEntityItemStringType();
	    nameType.setValue(name);
	    item.setName(windowsFactory.createRegistryItemName(nameType));

	    IValue val = null;
	    switch(rObj.getName().getValue().getOperation()) {
	      case EQUALS:
		val = registry.fetchValue(key, name);
		break;
	      case PATTERN_MATCH:
		val = registry.fetchValue(key, Pattern.compile(name));
		name = val.getName();
		break;
	      default:
		throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_OPERATION",
							       rObj.getName().getValue().getOperation()));
	    }

	    EntityItemRegistryTypeType typeType = windowsFactory.createEntityItemRegistryTypeType();
	    switch (val.getType()) {
	      case IValue.REG_SZ: {
		valueType.setValue(((IStringValue)val).getData());
		typeType.setValue("reg_sz");
		break;
	      }
	      case IValue.REG_EXPAND_SZ: {
		valueType.setValue(((IExpandStringValue)val).getExpandedData());
		typeType.setValue("reg_expand_sz");
		break;
	      }
	      case IValue.REG_DWORD: {
		valueType.setValue("" + ((IDwordValue)val).getData());
		valueType.setDatatype(DATATYPE_INT);
		typeType.setValue("reg_dword");
		break;
	      }
	      default:
		throw new RuntimeException(JOVALSystem.getMessage("ERROR_WINREG_VALUETOSTR",
								  key.toString(), name, val.getClass().getName()));
	    }
	    item.getValue().add(valueType);
	    item.setType(typeType);
	}
	return name;
    }

    String getItemString(RegistryItem item) {
	StringBuffer sb = new StringBuffer();
	if (item.isSetHive()) {
	    sb.append((String)item.getHive().getValue());
	} else {
	    sb.append("UNDEFINED");
	}
	sb.append(IRegistry.DELIM_CH);
	if (item.isSetKey()) {
	    sb.append((String)item.getKey().getValue().getValue());
	} else {
	    sb.append("undefined");
	}
	if (item.isSetName()) {
	    sb.append(" {");
	    sb.append((String)item.getName().getValue().getValue());
	    sb.append('}');
	}
	return sb.toString();
    }

    /**
     * Does the item match the state?
     */
    boolean match(RegistryState state, RegistryItem item) throws OvalException {
	if (!item.isSetValue()) {
	    return false; // Value doesn't exist
	}
	String itemValue = (String)item.getValue().get(0).getValue();
	String stateValue = (String)state.getValue().getValue();
	OperationEnumeration op = state.getValue().getOperation();

	switch(op) {
	  case EQUALS:
	    return stateValue.equals(itemValue); // Check for the value to match the state
	  case CASE_INSENSITIVE_EQUALS:
	    return stateValue.equalsIgnoreCase(itemValue);
	  case PATTERN_MATCH: {
	    Pattern p = Pattern.compile(stateValue);
	    return itemValue == null ? false : p.matcher(itemValue).find();
	  }
	  case NOT_EQUAL:
	    return !stateValue.equals(itemValue);

	  default:
	    if (!Version.isVersion(itemValue) && !Version.isVersion(stateValue)) {
		throw new RuntimeException(JOVALSystem.getMessage("ERROR_WINREG_MATCH", op, itemValue, stateValue));
	    }
	    Version iv = new Version(itemValue);
	    Version sv = new Version(stateValue);
	    switch(op) {
	      case GREATER_THAN:
		return iv.greaterThan(sv);
	      case LESS_THAN:
		return sv.greaterThan(iv);
	      case LESS_THAN_OR_EQUAL:
		return !iv.greaterThan(sv);
	      case GREATER_THAN_OR_EQUAL:
		return !sv.greaterThan(iv);
	    }
	    throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_OPERATION", op));
	}
    }

    class ItemWrapper {
	String identifier;
	RegistryItem item;

	ItemWrapper (String hive, String path, String name, RegistryItem item) {
	    identifier = "" + hive + IRegistry.DELIM_STR + path + " (Name=" + name + ")";
	    this.item = item;
	}
    }
}
