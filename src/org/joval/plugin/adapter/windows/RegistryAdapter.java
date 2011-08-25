// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.windows;

import java.math.BigInteger;
import java.util.Hashtable;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.MessageType;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.definitions.windows.RegistryBehaviors;
import oval.schemas.definitions.windows.RegistryObject;
import oval.schemas.systemcharacteristics.core.EntityItemAnySimpleType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.windows.EntityItemRegistryHiveType;
import oval.schemas.systemcharacteristics.windows.EntityItemRegistryTypeType;
import oval.schemas.systemcharacteristics.windows.RegistryItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.windows.registry.IDwordValue;
import org.joval.intf.windows.registry.IExpandStringValue;
import org.joval.intf.windows.registry.IKey;
import org.joval.intf.windows.registry.IMultiStringValue;
import org.joval.intf.windows.registry.IStringValue;
import org.joval.intf.windows.registry.IRegistry;
import org.joval.intf.windows.registry.IValue;
import org.joval.oval.OvalException;
import org.joval.oval.ResolveException;
import org.joval.oval.TestException;
import org.joval.util.JOVALSystem;

/**
 * Evaluates RegistryTest OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class RegistryAdapter implements IAdapter {
    private static final String DATATYPE_INT	= "int";

    private IRegistry registry;
    private Hashtable<String, BigInteger> itemIds;
    private Hashtable<String, List<String>> pathMap;

    public RegistryAdapter(IRegistry registry) {
	this.registry = registry;
	itemIds = new Hashtable<String, BigInteger>();
	pathMap = new Hashtable<String, List<String>>();
    }

    // Implement IAdapter

    public Class getObjectClass() {
	return RegistryObject.class;
    }

    public boolean connect() {
	if (registry != null) {
	    return registry.connect();
	}
	return false;
    }

    public void disconnect() {
	if (registry != null) {
	    registry.disconnect();
	}
    }

    public List<JAXBElement<? extends ItemType>> getItems(IRequestContext rc) throws OvalException {
	List<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();
	RegistryObject rObj = (RegistryObject)rc.getObject();

	String id = rObj.getId();
	if (rObj.getHive() == null || rObj.getHive().getValue() == null) {
	    throw new OvalException(JOVALSystem.getMessage("ERROR_WINREG_HIVE_NAME", id));
	}
	String hive = (String)rObj.getHive().getValue();

	if (rObj.getKey().getValue() == null) {
	    try {
		for (RegistryItem item : getItems(rObj, hive, null, rc)) {
		    items.add(JOVALSystem.factories.sc.windows.createRegistryItem(item));
		}
	    } catch (NoSuchElementException e) {
		// No match.
	    }
	} else {
	    for (String path : getPathList(rObj, hive, rc)) {
		try {
		    for (RegistryItem item : getItems(rObj, hive, path, rc)) {
			items.add(JOVALSystem.factories.sc.windows.createRegistryItem(item));
		    }
		} catch (NoSuchElementException e) {
		    // No match.
		}
	    }
	}
	return items;
    }

    // Private

    /**
     * Return the list of all registry key paths corresponding to the given RegistryObject.  Handles searches (from
     * pattern match operations), singletons (from equals operations), and searches based on RegistryBehaviors.
     */
    private List<String> getPathList(RegistryObject rObj, String hive, IRequestContext rc) throws OvalException {
	List<String> list = pathMap.get(rObj.getId());
	if (list != null) {
	    return list;
	}

	list = new Vector<String>();
	if (rObj.getKey().getValue().isSetVarRef()) {
	    try {
		String variableId = rObj.getKey().getValue().getVarRef();
		list.addAll(rc.resolve(variableId));
	    } catch (NoSuchElementException e) {
		JOVALSystem.getLogger().log(Level.FINER,
					    JOVALSystem.getMessage("STATUS_NOT_FOUND", e.getMessage(), rObj.getId()));
	    } catch (ResolveException e) {
		MessageType msg = JOVALSystem.factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(JOVALSystem.getMessage("ERROR_RESOLVE_VAR", rObj.getKey().getValue().getVarRef(), e.getMessage()));
		rc.addMessage(msg);
	    }
	} else {
	    list.add((String)rObj.getKey().getValue().getValue());
	}

	boolean patternMatch = false;
	OperationEnumeration op = rObj.getKey().getValue().getOperation();
	switch(op) {
	  case EQUALS:
	    break;

	  case PATTERN_MATCH: {
	    patternMatch = true;
	    List<String> newList = new Vector<String>();
	    for (String value : list) {
		for (IKey key : registry.search(hive, value)) {
		    if (!newList.contains(key.getPath())) {
			newList.add(key.getPath());
		    }
		}
	    }
	    list = newList;
	    break;
	  }

	  default:
	    throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_OPERATION", op));
	}

	if (rObj.isSetBehaviors()) {
	    RegistryBehaviors behaviors = rObj.getBehaviors();
	    list = getPaths(hive, list, behaviors.getMaxDepth().intValue(), behaviors.getRecurseDirection());
	} else if (patternMatch) {
	    //
	    // Wildcard pattern matches are really supposed to be recursive searches, unfortunately
	    //
	    List<String> newList = new Vector<String>();
	    for (String value : list) {
		if (((String)rObj.getKey().getValue().getValue()).indexOf(".*") != -1) {
		    List<String> l = new Vector<String>();
		    l.add(value);
		    newList.addAll(getPaths(hive, l, -1, "down"));
		}
	    }
	    for (String value : newList) {
		if (!list.contains(value)) {
		    list.add(value);
		}
	    }
	}

	pathMap.put(rObj.getId(), list);
	return list;
    }

    /**
     * Recursively searchies for matches based on RegistryBehaviors.
     */
    private List<String> getPaths(String hive, List<String> list, int depth, String direction) {
	if ("none".equals(direction) || depth == 0) {
	    return list;
	} else {
	    List<String> results = new Vector<String>();
	    for (String path : list) {
		try {
		    IKey key = registry.fetchKey(hive, path);
		    results.add(path);
		    if ("up".equals(direction)) {
			int ptr = 0;
			if (path.endsWith(IRegistry.DELIM_STR)) {
			    path = path.substring(0, path.lastIndexOf(IRegistry.DELIM_STR));
			}
			ptr = path.lastIndexOf(IRegistry.DELIM_STR);
			if (ptr != -1) {
			    Vector<String> v = new Vector<String>();
			    v.add(path.substring(0, ptr + IRegistry.DELIM_STR.length()));
			    results.addAll(getPaths(hive, v, --depth, direction));
			}
		    } else { // recurse down
			String[] children = key.listSubkeys();
			if (children != null) {
			    Vector<String> v = new Vector<String>();
			    for (int i=0; i < children.length; i++) {
				if (path.endsWith(IRegistry.DELIM_STR)) {
				    v.add(path + children[i]);
				} else {
				    v.add(path + IRegistry.DELIM_STR + children[i]);
				}
			    }
			    results.addAll(getPaths(hive, v, --depth, direction));
			}
		    }
		} catch (NoSuchElementException e) {
		}
	    }
	    return results;
	}
    }

    /**
     * Get all items corresponding to a concrete path, given the hive and RegistryObject.
     */
    private List<RegistryItem> getItems(RegistryObject rObj, String hive, String path, IRequestContext rc)
		throws NoSuchElementException, OvalException {

	IKey key = null;
	if (path == null) {
	    key = registry.getHive(hive);
	} else {
	    key = registry.fetchKey(hive, path);
	}

	List<RegistryItem> items = new Vector<RegistryItem>();
	if (rObj.getName() == null || rObj.getName().getValue() == null) {
	    items.add(getItem(key, null));
	} else {
	    OperationEnumeration op = rObj.getName().getValue().getOperation();
	    switch(op) {
	      case EQUALS:
		if (rObj.getName().getValue() != null) {
		    items.add(getItem(key, (String)rObj.getName().getValue().getValue()));
		} else {
		    items.add(getItem(key, null));
		}
		break;
    
	      case PATTERN_MATCH:
		try {
		    String[] valueNames = key.listValues(Pattern.compile((String)rObj.getName().getValue().getValue()));
		    for (int i=0; i < valueNames.length; i++) {
			items.add(getItem(key, valueNames[i]));
		    }
		} catch (PatternSyntaxException e) {
		    MessageType msg = JOVALSystem.factories.common.createMessageType();
		    msg.setLevel(MessageLevelEnumeration.ERROR);
		    msg.setValue(JOVALSystem.getMessage("ERROR_PATTERN", e.getMessage()));
		    rc.addMessage(msg);
		    JOVALSystem.getLogger().log(Level.WARNING, e.getMessage(), e);
		}
		break;
    
	      default:
		throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_OPERATION", op));
	    }
	}

	return items;
    }

    /**
     * Get an item given a concrete hive, key path and value name.
     */
    private RegistryItem getItem(IKey key, String name) throws NoSuchElementException, OvalException {
	RegistryItem item = JOVALSystem.factories.sc.windows.createRegistryItem();
	EntityItemRegistryHiveType hiveType = JOVALSystem.factories.sc.windows.createEntityItemRegistryHiveType();
	hiveType.setValue(key.getHive());
	item.setHive(hiveType);

	if (key.getPath() == null) {
	    return item;
	}

	EntityItemStringType keyType = JOVALSystem.factories.sc.core.createEntityItemStringType();
	keyType.setValue(key.getPath());
	item.setKey(JOVALSystem.factories.sc.windows.createRegistryItemKey(keyType));

	if (name != null) {
	    EntityItemStringType nameType = JOVALSystem.factories.sc.core.createEntityItemStringType();
	    nameType.setValue(name);
	    item.setName(JOVALSystem.factories.sc.windows.createRegistryItemName(nameType));
	}

	if (name != null && !"".equals(name)) {
	    IValue val = registry.fetchValue(key, name);

	    List<EntityItemAnySimpleType> values = new Vector<EntityItemAnySimpleType>();
	    EntityItemRegistryTypeType typeType = JOVALSystem.factories.sc.windows.createEntityItemRegistryTypeType();
	    switch (val.getType()) {
	      case IValue.REG_SZ: {
		EntityItemAnySimpleType valueType = JOVALSystem.factories.sc.core.createEntityItemAnySimpleType();
		valueType.setValue(((IStringValue)val).getData());
		values.add(valueType);
		typeType.setValue("reg_sz");
		break;
	      }

	      case IValue.REG_EXPAND_SZ: {
		EntityItemAnySimpleType valueType = JOVALSystem.factories.sc.core.createEntityItemAnySimpleType();
		valueType.setValue(((IExpandStringValue)val).getExpandedData());
		values.add(valueType);
		typeType.setValue("reg_expand_sz");
		break;
	      }

	      case IValue.REG_DWORD: {
		EntityItemAnySimpleType valueType = JOVALSystem.factories.sc.core.createEntityItemAnySimpleType();
		valueType.setValue("" + ((IDwordValue)val).getData());
		valueType.setDatatype(DATATYPE_INT);
		values.add(valueType);
		typeType.setValue("reg_dword");
		break;
	      }

	      case IValue.REG_MULTI_SZ: {
		String[] sVals = ((IMultiStringValue)val).getData();
		for (int i=0; i < sVals.length; i++) {
		    EntityItemAnySimpleType valueType = JOVALSystem.factories.sc.core.createEntityItemAnySimpleType();
		    valueType.setValue(sVals[i]);
		    values.add(valueType);
		}
		typeType.setValue("reg_multi_sz");
		break;
	      }

	      default:
		throw new RuntimeException(JOVALSystem.getMessage("ERROR_WINREG_VALUETOSTR",
								  key.toString(), name, val.getClass().getName()));
	    }
	    item.getValue().addAll(values);
	    item.setType(typeType);
	}

	item.setStatus(StatusEnumeration.EXISTS);
	return item;
    }
}
