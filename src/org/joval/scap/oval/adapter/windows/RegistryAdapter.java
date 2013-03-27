// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.windows;

import java.io.InputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jsaf.intf.system.ISession;
import jsaf.intf.util.ISearchable;
import jsaf.intf.windows.registry.IBinaryValue;
import jsaf.intf.windows.registry.IDwordValue;
import jsaf.intf.windows.registry.IExpandStringValue;
import jsaf.intf.windows.registry.IKey;
import jsaf.intf.windows.registry.IMultiStringValue;
import jsaf.intf.windows.registry.IQwordValue;
import jsaf.intf.windows.registry.IRegistry;
import jsaf.intf.windows.registry.IStringValue;
import jsaf.intf.windows.registry.IValue;
import jsaf.intf.windows.system.IWindowsSession;
import jsaf.io.LittleEndian;
import jsaf.util.Base64;
import jsaf.util.SafeCLI;
import jsaf.util.StringTools;

import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.MessageType;
import scap.oval.common.OperationEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.windows.RegistryBehaviors;
import scap.oval.definitions.windows.RegistryObject;
import scap.oval.systemcharacteristics.core.EntityItemAnySimpleType;
import scap.oval.systemcharacteristics.core.EntityItemIntType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.windows.EntityItemRegistryHiveType;
import scap.oval.systemcharacteristics.windows.EntityItemRegistryTypeType;
import scap.oval.systemcharacteristics.windows.EntityItemWindowsViewType;
import scap.oval.systemcharacteristics.windows.RegistryItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.Batch;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;
import org.joval.xml.XSITools;

/**
 * Evaluates RegistryTest OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class RegistryAdapter extends BaseRegkeyAdapter<RegistryItem> {
    private Map<String, BigInteger> writeTimes;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IWindowsSession) {
	    super.init((IWindowsSession)session);
	    writeTimes = new HashMap<String, BigInteger>();
	    classes.add(RegistryObject.class);
	} else {
	    notapplicable.add(RegistryObject.class);
	}
	return classes;
    }

    // Implement IBatch

    Collection<IRequest> queue;

    @Override
    public boolean queue(IRequest request) {
	if (batchable(request)) {
	    if (queue == null) {
		queue = new ArrayList<IRequest>();
	    }
	    queue.add(request);
	    return true;
	} else {
	    return false;
	}
    }

    @Override
    public Collection<IResult> exec() {
	Collection<IResult> results = new ArrayList<IResult>();
	if (queue != null) {
	    HashSet<String> subkeys = new HashSet<String>();
	    for (IRequest request : queue) {
		RegistryObject rObj = (RegistryObject)request.getObject();
		subkeys.add((String)rObj.getKey().getValue().getValue());
	    }
	    StringBuffer sb = new StringBuffer();
	    for (String subkey : subkeys) {
		if (sb.length() > 0) {
		    sb.append(",");
		}
		sb.append("\"").append(subkey).append("\"");
	    }
	    sb.append(" | Get-RegKeyLastWriteTime | Transfer-Encode");
	    try {
		//
		// Cache all the last_write_times all at once
		//
		byte[] buff = Base64.decode(getRunspace(session.getNativeView()).invoke(sb.toString()));
		String data = new String(buff, StringTools.UTF8);
		for (String line : data.split("\r\n")) {
		    int ptr = line.lastIndexOf(":");
		    if (ptr > 0) {
			try {
			    String fullPath = line.substring(0,ptr).trim().toUpperCase();
			    writeTimes.put(fullPath, new BigInteger(line.substring(ptr+1).trim()));
			} catch (IllegalArgumentException e) {
			}
		    }
		}

		//
		// Now, iterate through the requests normally
		//
		for (IRequest request : queue) {
		    IRequestContext rc = request.getContext();
		    try {
			Collection<ItemType> items = new ArrayList<ItemType>();
			for (Arguments args : getArguments(request.getObject(), rc)) {
			    items.addAll(getItems(args));
			}
			results.add(new Batch.Result(items, rc));
		    } catch (NoSuchElementException e) {
			// no results
			results.add(new Batch.Result(new ArrayList<ItemType>(), rc));
		    } catch (CollectException e) {
			results.add(new Batch.Result(e, rc));
		    } catch (Exception e) {
			results.add(new Batch.Result(new CollectException(e, FlagEnumeration.ERROR), rc));
		    }
		}
	    } catch (Exception e) {
		for (IRequest request : queue) {
		    results.add(new Batch.Result(new CollectException(e, FlagEnumeration.ERROR), request.getContext()));
		}
	    }
	}
	queue = null;
	return results;
    }

    // Protected

    protected Class getItemClass() {
	return RegistryItem.class;
    }

    @Override
    protected List<ISearchable.ICondition> getConditions(ObjectType obj) throws CollectException, PatternSyntaxException {
	List<ISearchable.ICondition> conditions = new ArrayList<ISearchable.ICondition>();
	RegistryObject rObj = (RegistryObject)obj;
	if (!XSITools.isNil(rObj.getName())) {
	    String name = (String)rObj.getName().getValue().getValue();
	    OperationEnumeration op = rObj.getName().getValue().getOperation();
	    switch(op) {
	      case EQUALS:
		try {
		    SafeCLI.checkArgument(name, session);
		    conditions.add(new ISearchable.GenericCondition(FIELD_VALUE, TYPE_EQUALITY, name));
		} catch (IllegalArgumentException e) {
		    //
		    // The name is unsafe to pass directly into a search because it contains dangerous characters, so
		    // we pass it in as a Base-64 encoded value.
		    //
		    String encoded = Base64.encodeBytes(name.getBytes());
		    conditions.add(new ISearchable.GenericCondition(FIELD_VALUE_BASE64, TYPE_EQUALITY, encoded));
		}
		break;
	      case PATTERN_MATCH:
		conditions.add(new ISearchable.GenericCondition(FIELD_VALUE, TYPE_PATTERN, Pattern.compile(name)));
		break;
	      default:
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	    }
	}
	return conditions;
    }

    protected Collection<RegistryItem> getItems(Arguments args) throws Exception {
	ObjectType obj = args.obj;
	ItemType it = args.base;
	IKey key = args.key;
	IRequestContext rc = args.rc;

	if (it instanceof RegistryItem) {
	    RegistryItem base = (RegistryItem)it;
	    RegistryObject rObj = (RegistryObject)obj;
	    Collection<RegistryItem> items = new ArrayList<RegistryItem>();

	    if (XSITools.isNil(rObj.getName())) {
		items.add(getItem(base, key));
	    } else {
		OperationEnumeration op = rObj.getName().getValue().getOperation();
		switch(op) {
		  case EQUALS:
		    items.add(getItem(base, key.getValue((String)rObj.getName().getValue().getValue())));
		    break;

		  case PATTERN_MATCH:
		    try {
			Pattern p = Pattern.compile((String)rObj.getName().getValue().getValue());
			for (IValue value : key.listValues(p)) {
			    items.add(getItem(base, value));
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
	String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_ITEM, it.getClass().getName());
	throw new CollectException(msg, FlagEnumeration.ERROR);
    }

    @Override
    protected List<InputStream> getPowershellModules() {
	return Arrays.asList(getClass().getResourceAsStream("Registry.psm1"));
    }

    // Private

    /**
     * Any exact (EQUALS) native subkey of HKLM is batchable.
     */
    private boolean batchable(IRequest request) {
	RegistryObject rObj = (RegistryObject)request.getObject();
	return	!rObj.isSetBehaviors() &&
		rObj.getHive().getOperation() == OperationEnumeration.EQUALS &&
		IRegistry.Hive.HKLM.getName().equals(rObj.getHive().getValue()) &&
		!XSITools.isNil(rObj.getKey()) &&
		rObj.getKey().getValue().getOperation() == OperationEnumeration.EQUALS;
    }

    private RegistryItem getItem(RegistryItem base, IKey key) throws Exception {
	RegistryItem item = Factories.sc.windows.createRegistryItem();
	item.setHive(base.getHive());
	boolean win32 = false;
	if (base.isSetWindowsView()) {
	    win32 = "32_bit".equals(base.getWindowsView().getValue());
	    item.setWindowsView(base.getWindowsView());
	}

	EntityItemIntType lastWriteTimeType = Factories.sc.core.createEntityItemIntType();
	lastWriteTimeType.setDatatype(SimpleDatatypeEnumeration.INT.value());
	try {
	    lastWriteTimeType.setValue(getLastWriteTime(key, win32).toString());
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    lastWriteTimeType.setStatus(StatusEnumeration.ERROR);
	}
	item.setLastWriteTime(lastWriteTimeType);

	if (key.getPath() == null) {
	    return item;
	}
	item.setKey(base.getKey());
	item.setStatus(StatusEnumeration.EXISTS);
	return item;
    }

    /**
     * Get an item given an IKey and name.
     */
    private RegistryItem getItem(RegistryItem base, IValue value) throws Exception {
	RegistryItem item = getItem(base, value.getKey());

	EntityItemStringType nameType = Factories.sc.core.createEntityItemStringType();
	nameType.setValue(value.getName());
	item.setName(Factories.sc.windows.createRegistryItemName(nameType));

	Collection<EntityItemAnySimpleType> values = new ArrayList<EntityItemAnySimpleType>();
	EntityItemRegistryTypeType typeType = Factories.sc.windows.createEntityItemRegistryTypeType();
	switch (value.getType()) {
	  case REG_SZ: {
	    EntityItemAnySimpleType valueType = Factories.sc.core.createEntityItemAnySimpleType();
	    valueType.setValue(((IStringValue)value).getData());
	    valueType.setDatatype(SimpleDatatypeEnumeration.STRING.value());
	    values.add(valueType);
	    typeType.setValue("reg_sz");
	    break;
	  }

	  case REG_EXPAND_SZ: {
	    EntityItemAnySimpleType valueType = Factories.sc.core.createEntityItemAnySimpleType();
	    valueType.setValue(((IExpandStringValue)value).getData());
	    valueType.setDatatype(SimpleDatatypeEnumeration.STRING.value());
	    values.add(valueType);
	    typeType.setValue("reg_expand_sz");
	    break;
	  }

	  case REG_DWORD: {
	    EntityItemAnySimpleType valueType = Factories.sc.core.createEntityItemAnySimpleType();
	    valueType.setValue(((IDwordValue)value).getData().toString());
	    valueType.setDatatype(SimpleDatatypeEnumeration.INT.value());
	    values.add(valueType);
	    typeType.setValue("reg_dword");
	    break;
	  }

	  case REG_QWORD: {
	    EntityItemAnySimpleType valueType = Factories.sc.core.createEntityItemAnySimpleType();
	    valueType.setValue(((IQwordValue)value).getData().toString());
	    valueType.setDatatype(SimpleDatatypeEnumeration.INT.value());
	    values.add(valueType);
	    typeType.setValue("reg_qword");
	    break;
	  }

	  case REG_BINARY: {
	    EntityItemAnySimpleType valueType = Factories.sc.core.createEntityItemAnySimpleType();
	    byte[] data = ((IBinaryValue)value).getData();
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

	  case REG_MULTI_SZ: {
	    String[] sVals = ((IMultiStringValue)value).getData();
	    if (sVals == null) {
		EntityItemAnySimpleType valueType = Factories.sc.core.createEntityItemAnySimpleType();
		valueType.setDatatype(SimpleDatatypeEnumeration.STRING.value());
		valueType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
		values.add(valueType);
	    } else {
		for (int i=0; i < sVals.length; i++) {
		    EntityItemAnySimpleType valueType = Factories.sc.core.createEntityItemAnySimpleType();
		    valueType.setDatatype(SimpleDatatypeEnumeration.STRING.value());
		    valueType.setValue(sVals[i]);
		    values.add(valueType);
		}
	    }
	    typeType.setValue("reg_multi_sz");
	    break;
	  }

	  case REG_NONE:
	    typeType.setValue("reg_none");
	    break;

	  default:
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_WINREG_VALUETOSTR, value.getKey().toString(),
					     value.getName(), value.getClass().getName());
	    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	}
	item.setType(typeType);
	if (values.size() > 0) {
	    item.getValue().addAll(values);
	}
	return item;
    }

    private BigInteger getLastWriteTime(IKey key, boolean win32) throws Exception {
	String fullPath = new StringBuffer(win32 ? "32:" : "").append(key.toString()).toString().toUpperCase();
	if (!writeTimes.containsKey(fullPath)) {
	    StringBuffer sb = new StringBuffer("Get-RegKeyLastWriteTime -Hive ").append(key.getHive().getName());
	    if (key.getPath() != null) {
		sb.append(" -Subkey \"").append(key.getPath()).append("\"");
	    }
	    IWindowsSession.View view = win32 ? IWindowsSession.View._32BIT : session.getNativeView();
	    String data = getRunspace(view).invoke(sb.toString());
	    int ptr = data.lastIndexOf(":");
	    if (ptr > 0) {
		BigInteger ticks = new BigInteger(data.substring(ptr+1).trim());
		writeTimes.put(fullPath, ticks);
	    } else {
		throw new IllegalArgumentException(data);
	    }
	}
	return writeTimes.get(fullPath);
    }
}
