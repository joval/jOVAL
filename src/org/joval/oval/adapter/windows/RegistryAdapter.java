// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.adapter.windows;

import java.math.BigInteger;
import java.util.Hashtable;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.HashSet;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.MessageType;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.windows.RegistryBehaviors;
import oval.schemas.definitions.windows.RegistryObject;
import oval.schemas.systemcharacteristics.core.EntityItemAnySimpleType;
import oval.schemas.systemcharacteristics.core.EntityItemIntType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.windows.EntityItemRegistryHiveType;
import oval.schemas.systemcharacteristics.windows.EntityItemRegistryTypeType;
import oval.schemas.systemcharacteristics.windows.EntityItemWindowsViewType;
import oval.schemas.systemcharacteristics.windows.RegistryItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.system.ISession;
import org.joval.intf.util.IPathRedirector;
import org.joval.intf.windows.registry.IBinaryValue;
import org.joval.intf.windows.registry.IDwordValue;
import org.joval.intf.windows.registry.IExpandStringValue;
import org.joval.intf.windows.registry.IKey;
import org.joval.intf.windows.registry.IMultiStringValue;
import org.joval.intf.windows.registry.IQwordValue;
import org.joval.intf.windows.registry.IStringValue;
import org.joval.intf.windows.registry.IRegistry;
import org.joval.intf.windows.registry.IValue;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.io.LittleEndian;
import org.joval.oval.CollectException;
import org.joval.oval.Factories;
import org.joval.oval.OvalException;
import org.joval.util.JOVALMsg;

/**
 * Evaluates RegistryTest OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class RegistryAdapter implements IAdapter {
    private IWindowsSession session;
    private Hashtable<String, BigInteger> itemIds;
    private IRegistry reg32, reg;

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IWindowsSession) {
	    this.session = (IWindowsSession)session;
	    itemIds = new Hashtable<String, BigInteger>();
	    classes.add(RegistryObject.class);
	}
	return classes;
    }

    public Collection<RegistryItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException, OvalException {
	if (reg32 == null) {
	    reg32 = session.getRegistry(IWindowsSession.View._32BIT);
	    if (session.supports(IWindowsSession.View._64BIT)) {
		reg = session.getRegistry(IWindowsSession.View._64BIT);
	    } else {
		reg = reg32;
	    }
	}
	Collection<RegistryItem> items = new Vector<RegistryItem>();
	RegistryObject rObj = (RegistryObject)obj;

	String id = rObj.getId();
	if (rObj.getHive() == null || rObj.getHive().getValue() == null) {
	    throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_WINREG_HIVE_NAME, id));
	}
	String hive = (String)rObj.getHive().getValue();
	for (String path : getPathList(rObj, hive, rc)) {
	    try {
		items.addAll(getItems(rObj, hive, path, rc));
	    } catch (NoSuchElementException e) {
		// No match.
	    }
	}
	return items;
    }

    // Private

    /**
     * Return the list of all registry key paths corresponding to the given RegistryObject.  Handles searches (from
     * pattern match operations), singletons (from equals operations), and searches based on RegistryBehaviors.
     */
    private Collection<String> getPathList(RegistryObject rObj, String hive, IRequestContext rc)
		throws CollectException, OvalException {

	boolean win32 = false;
	if (rObj.isSetBehaviors()) {
	    RegistryBehaviors behaviors = rObj.getBehaviors();
	    win32 = "32_bit".equals(behaviors.getWindowsView());
	}

	Collection<String> list = new HashSet<String>();
	boolean patternMatch = false;
	if (rObj.getKey().getValue() == null) {
	    list.add(""); // special case
	} else {
	    String keypath = (String)rObj.getKey().getValue().getValue();
	    OperationEnumeration op = rObj.getKey().getValue().getOperation();
	    switch(op) {
	      case EQUALS:
		list.add(keypath);
		break;

	      case PATTERN_MATCH: {
		patternMatch = true;
		try {
		    for (IKey key : (win32 ? reg32 : reg).search(hive, keypath)) {
			if (!list.contains(key.getPath())) {
			    list.add(key.getPath());
			}
		    }
		} catch (NoSuchElementException e) {
		}
		break;
	      }

	      default:
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	    }
	}

	if (rObj.isSetBehaviors()) {
	    RegistryBehaviors behaviors = rObj.getBehaviors();
	    list = getPaths(hive, list, behaviors.getMaxDepth().intValue(), behaviors.getRecurseDirection(), win32);
	} else if (patternMatch) {
	    //
	    // Wildcard pattern matches are really supposed to be recursive searches, unfortunately
	    //
	    Collection<String> newList = new Vector<String>();
	    for (String value : list) {
		String keypath = (String)rObj.getKey().getValue().getValue();
		if (keypath.indexOf(".*") != -1 || keypath.indexOf(".+") != -1) {
		    Collection<String> l = new Vector<String>();
		    l.add(value);
		    newList.addAll(getPaths(hive, l, -1, "down", win32));
		}
	    }
	    for (String value : newList) {
		if (!list.contains(value)) {
		    list.add(value);
		}
	    }
	}

	return list;
    }

    /**
     * Recursively searchies for matches based on RegistryBehaviors.
     */
    private Collection<String> getPaths(String hive, Collection<String> list, int depth, String direction, boolean win32) {
	if ("none".equals(direction) || depth == 0) {
	    return list;
	} else {
	    Collection<String> results = new Vector<String>();
	    for (String path : list) {
		try {
		    IKey key = null;
		    if (path.length() > 0) {
			key = (win32 ? reg32 : reg).fetchKey(hive, path);
			results.add(path);
		    } else {
			key = (win32 ? reg32 : reg).fetchKey(hive);
		    }
		    if ("up".equals(direction)) {
			int ptr = 0;
			if (path.endsWith(IRegistry.DELIM_STR)) {
			    path = path.substring(0, path.lastIndexOf(IRegistry.DELIM_STR));
			}
			ptr = path.lastIndexOf(IRegistry.DELIM_STR);
			if (ptr != -1) {
			    Vector<String> v = new Vector<String>();
			    v.add(path.substring(0, ptr));
			    results.addAll(getPaths(hive, v, --depth, direction, win32));
			}
		    } else { // recurse down
			String[] children = key.listSubkeys();
			if (children != null) {
			    Vector<String> v = new Vector<String>();
			    for (int i=0; i < children.length; i++) {
				if (path.length() == 0) {
				    v.add(children[i]);
				} else {
				    v.add(path + IRegistry.DELIM_STR + children[i]);
				}
			    }
			    results.addAll(getPaths(hive, v, --depth, direction, win32));
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
    private Collection<RegistryItem> getItems(RegistryObject rObj, String hive, String path, IRequestContext rc)
		throws NoSuchElementException, CollectException {

	boolean win32 = false;
	if (rObj.isSetBehaviors()) {
	    RegistryBehaviors behaviors = rObj.getBehaviors();
	    win32 = "32_bit".equals(behaviors.getWindowsView());
	}

	IKey key = null;
	if (path == null) {
	    key = (win32 ? reg32 : reg).getHive(hive);
	} else {
	    key = (win32 ? reg32 : reg).fetchKey(hive, path);
	}

	Collection<RegistryItem> items = new Vector<RegistryItem>();
	if (rObj.getName() == null || rObj.getName().getValue() == null) {
	    items.add(getItem(key, null, win32));
	} else {
	    OperationEnumeration op = rObj.getName().getValue().getOperation();
	    switch(op) {
	      case EQUALS:
		items.add(getItem(key, (String)rObj.getName().getValue().getValue(), win32));
		break;
    
	      case PATTERN_MATCH:
		try {
		    String[] valueNames = key.listValues(Pattern.compile((String)rObj.getName().getValue().getValue()));
		    for (int i=0; i < valueNames.length; i++) {
			items.add(getItem(key, valueNames[i], win32));
		    }
		} catch (PatternSyntaxException e) {
		    MessageType msg = Factories.common.createMessageType();
		    msg.setLevel(MessageLevelEnumeration.ERROR);
		    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_PATTERN, e.getMessage()));
		    rc.addMessage(msg);
		    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		}
		break;
    
	      default:
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	    }
	}

	return items;
    }

    /**
     * Get an item given a concrete hive, key path and value name.
     */
    private RegistryItem getItem(IKey key, String name, boolean win32) throws NoSuchElementException, CollectException {
	RegistryItem item = Factories.sc.windows.createRegistryItem();
	EntityItemRegistryHiveType hiveType = Factories.sc.windows.createEntityItemRegistryHiveType();
	hiveType.setValue(key.getHive());
	item.setHive(hiveType);

	// REMIND (DAS): lastWriteTime implementation is TBD
	EntityItemIntType lastWriteTimeType = Factories.sc.core.createEntityItemIntType();
	lastWriteTimeType.setStatus(StatusEnumeration.NOT_COLLECTED);
	lastWriteTimeType.setDatatype(SimpleDatatypeEnumeration.INT.value());
	item.setLastWriteTime(lastWriteTimeType);

	EntityItemWindowsViewType viewType = Factories.sc.windows.createEntityItemWindowsViewType();
	if (session.supports(IWindowsSession.View._64BIT)) {
	    if (win32) {
		viewType.setValue("32_bit");
	    } else {
		viewType.setValue("64_bit");
	    }
	} else {
	    viewType.setValue("32_bit");
	}
	item.setWindowsView(viewType);

	if (key.getPath() == null) {
	    return item;
	}

	EntityItemStringType keyType = Factories.sc.core.createEntityItemStringType();
	keyType.setValue(key.getPath());
	item.setKey(Factories.sc.windows.createRegistryItemKey(keyType));

	if (name != null) {
	    EntityItemStringType nameType = Factories.sc.core.createEntityItemStringType();
	    nameType.setValue(name);
	    item.setName(Factories.sc.windows.createRegistryItemName(nameType));
	}

	if (name != null && !"".equals(name)) {
	    IValue val = (win32 ? reg32 : reg).fetchValue(key, name);

	    Collection<EntityItemAnySimpleType> values = new Vector<EntityItemAnySimpleType>();
	    EntityItemRegistryTypeType typeType = Factories.sc.windows.createEntityItemRegistryTypeType();
	    switch (val.getType()) {
	      case IValue.REG_SZ: {
		EntityItemAnySimpleType valueType = Factories.sc.core.createEntityItemAnySimpleType();
		valueType.setValue(((IStringValue)val).getData());
		valueType.setDatatype(SimpleDatatypeEnumeration.STRING.value());
		values.add(valueType);
		typeType.setValue("reg_sz");
		break;
	      }

	      case IValue.REG_EXPAND_SZ: {
		EntityItemAnySimpleType valueType = Factories.sc.core.createEntityItemAnySimpleType();
		valueType.setValue(((IExpandStringValue)val).getExpandedData());
		valueType.setDatatype(SimpleDatatypeEnumeration.STRING.value());
		values.add(valueType);
		typeType.setValue("reg_expand_sz");
		break;
	      }

	      case IValue.REG_DWORD: {
		EntityItemAnySimpleType valueType = Factories.sc.core.createEntityItemAnySimpleType();
		valueType.setValue(Integer.toString(((IDwordValue)val).getData()));
		valueType.setDatatype(SimpleDatatypeEnumeration.INT.value());
		values.add(valueType);
		typeType.setValue("reg_dword");
		break;
	      }

	      case IValue.REG_QWORD: {
		EntityItemAnySimpleType valueType = Factories.sc.core.createEntityItemAnySimpleType();
		valueType.setValue(Long.toString(((IQwordValue)val).getData()));
		valueType.setDatatype(SimpleDatatypeEnumeration.INT.value());
		values.add(valueType);
		typeType.setValue("reg_qword");
		break;
	      }

	      case IValue.REG_BINARY: {
		EntityItemAnySimpleType valueType = Factories.sc.core.createEntityItemAnySimpleType();
		byte[] data = ((IBinaryValue)val).getData();
		StringBuffer sb = new StringBuffer();
		for (int i=0; i < data.length; i++) {
		    sb.append(LittleEndian.toHexString(data[i]));
		}
		valueType.setValue(sb.toString());
		valueType.setDatatype(SimpleDatatypeEnumeration.BINARY.value());
		values.add(valueType);
		typeType.setValue("reg_binary");
		break;
	      }

	      case IValue.REG_MULTI_SZ: {
		String[] sVals = ((IMultiStringValue)val).getData();
		for (int i=0; i < sVals.length; i++) {
		    EntityItemAnySimpleType valueType = Factories.sc.core.createEntityItemAnySimpleType();
		    valueType.setDatatype(SimpleDatatypeEnumeration.STRING.value());
		    valueType.setValue(sVals[i]);
		    values.add(valueType);
		}
		typeType.setValue("reg_multi_sz");
		break;
	      }

	      case IValue.REG_NONE:
		typeType.setValue("reg_none");
		break;

	      default:
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_WINREG_VALUETOSTR,
						    key.toString(), name, val.getClass().getName());
		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	    }
	    item.setType(typeType);
	    if (values.size() > 0) {
		item.getValue().addAll(values);
	    }
	}

	item.setStatus(StatusEnumeration.EXISTS);
	return item;
    }
}
