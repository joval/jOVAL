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

    public void scan(ISystemCharacteristics sc) throws OvalException {
	try {
	    registry.connect();
	    Iterator <ObjectType>iter = definitions.iterateObjects(RegistryObject.class);
	    while (iter.hasNext()) {
		RegistryObject rObj = (RegistryObject)iter.next();
		if (rObj.isSetSet()) {
		    // DAS: Set objects are just collections, the object references they contain will be scanned elsewhere.
		    continue;
		} else {
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
	    }
	} finally {
	    registry.disconnect();
	}
    }

    public String getItemData(ObjectComponentType oc, ISystemCharacteristics sc) throws OvalException {
	RegistryObject rObj = definitions.getObject(oc.getObjectRef(), RegistryObject.class);
	List<ItemType> items = null;
	try {
	    items = sc.getItemsByObjectId(rObj.getId());
	} catch (NoSuchElementException e) {
	    items = getItems(rObj);
	}
	if (items.size() == 0) {
	    return null;
	} else if (items.size() > 1) {
	    throw new OvalException(JOVALSystem.getMessage("ERROR_OBJECT_ITEM_CHOICE",
							   new Integer(items.size()), rObj.getId()));
	}
	RegistryItem item = (RegistryItem)items.get(0);
	if ("value".equals(oc.getItemField())) {
	    if (item.isSetValue()) {
		return (String)item.getValue().get(0).getValue();
	    } else {
		return null;
	    }
	} else if ("key".equals(oc.getItemField())) {
	    if (item.isSetKey()) {
		return (String)item.getKey().getValue().getValue();
	    } else {
		return null;
	    }
	} else {
	    throw new OvalException(JOVALSystem.getMessage("ERROR_OBJECT_ITEM_FIELD", oc.getItemField()));
	}
    }

    public void evaluate(TestType testResult, ISystemCharacteristics sc) throws OvalException {
	String testId = testResult.getTestId();
	RegistryTest test = definitions.getTest(testId, RegistryTest.class);
	String objectId = test.getObject().getObjectRef();

	RegistryState state = null;
	if (test.isSetState() && test.getState().get(0).isSetStateRef()) {
	    String stateId = test.getState().get(0).getStateRef();
	    state = definitions.getState(stateId, RegistryState.class);
	}

	for (VariableValueType var : sc.getVariablesByObjectId(objectId)) {
	    TestedVariableType testedVariable = JOVALSystem.resultsFactory.createTestedVariableType();
	    testedVariable.setVariableId(var.getVariableId());
	    testedVariable.setValue(var.getValue());
	    testResult.getTestedVariable().add(testedVariable);
	}

	boolean result = false;
	int trueCount=0, falseCount=0, errorCount=0;
	if (sc.getObject(objectId).getFlag() == FlagEnumeration.ERROR) {
	    errorCount++;
	}
	Iterator<ItemType> items = sc.getItemsByObjectId(objectId).iterator();
	switch(test.getCheckExistence()) {
	  case NONE_EXIST: {
	    while(items.hasNext()) {
		ItemType it = items.next();
		if (it instanceof RegistryItem) {
		    RegistryItem item = (RegistryItem)it;
		    TestedItemType testedItem = JOVALSystem.resultsFactory.createTestedItemType();
		    testedItem.setItemId(item.getId());
		    switch(item.getStatus()) {
		      case EXISTS:
			trueCount++;
			testedItem.setResult(ResultEnumeration.NOT_EVALUATED); // just an existence check
			break;
		      case DOES_NOT_EXIST:
			falseCount++;
			testedItem.setResult(ResultEnumeration.NOT_EVALUATED); // just an existence check
			break;
		      case ERROR:
			errorCount++;
			testedItem.setResult(ResultEnumeration.ERROR);
			break;
		      case NOT_COLLECTED:
			testedItem.setResult(ResultEnumeration.NOT_EVALUATED);
			break;
		    }
		    testResult.getTestedItem().add(testedItem);
		} else {
		    throw new OvalException(JOVALSystem.getMessage("ERROR_INSTANCE",
								   RegistryItem.class.getName(), it.getClass().getName()));
		}
	    }
	    result = trueCount == 0;
	    break;
	  }

	  case AT_LEAST_ONE_EXISTS: {
	    while(items.hasNext()) {
		ItemType it = items.next();
		if (it instanceof RegistryItem) {
		    RegistryItem item = (RegistryItem)it;
		    TestedItemType testedItem = JOVALSystem.resultsFactory.createTestedItemType();
		    testedItem.setItemId(item.getId());
		    switch(item.getStatus()) {
		      case EXISTS:
			if (state == null) {
			    trueCount++;
			    testedItem.setResult(ResultEnumeration.TRUE);
			} else {
			    if(match(state, item)) {
				trueCount++;
				testedItem.setResult(ResultEnumeration.TRUE);
			    } else {
				falseCount++;
				testedItem.setResult(ResultEnumeration.FALSE);
			    }
			}
			break;
		      case DOES_NOT_EXIST:
			falseCount++;
			testedItem.setResult(ResultEnumeration.FALSE);
			break;
		      case ERROR:
			errorCount++;
			testedItem.setResult(ResultEnumeration.ERROR);
			break;
		      case NOT_COLLECTED:
			testedItem.setResult(ResultEnumeration.NOT_EVALUATED);
			break;
		    }
		    testResult.getTestedItem().add(testedItem);
		} else {
		    throw new OvalException(JOVALSystem.getMessage("ERROR_INSTANCE",
								   RegistryItem.class.getName(), it.getClass().getName()));
		}
	    }

	    switch(test.getCheck()) {
	      case ALL:
		result = falseCount == 0 && trueCount > 0;
		break;
	      case NONE_SATISFY:
		result = trueCount == 0;
		break;
	      case AT_LEAST_ONE:
		result = trueCount > 0;
		break;
	      default:
		throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_CHECK", test.getCheck()));
	    }
	    break;
	  }

          default:
            throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_EXISTENCE", test.getCheckExistence()));
	}
	if (errorCount > 0) {
	    testResult.setResult(ResultEnumeration.ERROR);
	} else if (result) {
	    testResult.setResult(ResultEnumeration.TRUE);
	} else {
	    testResult.setResult(ResultEnumeration.FALSE);
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
