// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.windows;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.xml.bind.JAXBElement;

import jsaf.intf.util.ISearchable;
import jsaf.intf.windows.registry.IKey;
import jsaf.intf.windows.registry.IRegistry;
import jsaf.intf.windows.powershell.IRunspace;
import jsaf.intf.windows.system.IWindowsSession;
import jsaf.util.SafeCLI;

import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.MessageType;
import scap.oval.common.OperationEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.EntityObjectStringType;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.windows.EntityObjectRegistryHiveType;
import scap.oval.definitions.windows.RegistryBehaviors;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.windows.EntityItemRegistryHiveType;
import scap.oval.systemcharacteristics.windows.EntityItemWindowsViewType;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Base class for IRegistry/IKey-based IAdapters. Subclasses need only implement getItemClass and getItems
 * methods. The base class handles searches and caching of search results.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public abstract class BaseRegkeyAdapter<T extends ItemType> implements IAdapter {
    protected static final int TYPE_EQUALITY		= ISearchable.TYPE_EQUALITY;
    protected static final int TYPE_PATTERN		= ISearchable.TYPE_PATTERN;
    protected static final int FIELD_FROM		= ISearchable.FIELD_FROM;
    protected static final int FIELD_DEPTH		= ISearchable.FIELD_DEPTH;
    protected static final int FIELD_HIVE		= IRegistry.FIELD_HIVE;
    protected static final int FIELD_KEY		= IRegistry.FIELD_KEY;
    protected static final int FIELD_VALUE		= IRegistry.FIELD_VALUE;
    protected static final int FIELD_VALUE_BASE64	= IRegistry.FIELD_VALUE_BASE64;

    protected IWindowsSession session;

    private IRunspace rs, rs32;

    /**
     * Subclasses can call this method to initialize the session variable.
     */
    protected void init(IWindowsSession session) {
	this.session = session;
    }

    // Implement IAdapter

    public final Collection<T> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	Collection<T> items = new ArrayList<T>();
	ReflectedRegistryObject rObj = new ReflectedRegistryObject(obj);
	IWindowsSession.View view = getView(rObj.getBehaviors());
	if (session.supports(view)) {
	    //
	    // Find all the matching top-level hives
	    //
	    List<IRegistry.Hive> hives = new ArrayList<IRegistry.Hive>();
	    OperationEnumeration op = rObj.getHive().getOperation();
	    String name = (String)rObj.getHive().getValue();
	    switch(op) {
	      case EQUALS:
		for (IRegistry.Hive hive : IRegistry.Hive.values()) {
		    if (name.equals(hive.getName())) {
			hives.add(hive);
			break;
		    }
		}
		break;
	      case NOT_EQUAL:
		for (IRegistry.Hive hive : IRegistry.Hive.values()) {
		    if (!name.equals(hive.getName())) {
			hives.add(hive);
			break;
		    }
		}
		break;
	      case CASE_INSENSITIVE_EQUALS:
		for (IRegistry.Hive hive : IRegistry.Hive.values()) {
		    if (name.equalsIgnoreCase(hive.getName())) {
			hives.add(hive);
			break;
		    }
		}
		break;
	      case CASE_INSENSITIVE_NOT_EQUAL:
		for (IRegistry.Hive hive : IRegistry.Hive.values()) {
		    if (!name.equalsIgnoreCase(hive.getName())) {
			hives.add(hive);
			break;
		    }
		}
		break;
	      case PATTERN_MATCH: {
		Pattern p = Pattern.compile(name);
		for (IRegistry.Hive hive : IRegistry.Hive.values()) {
		    if (p.matcher(hive.getName()).find()) {
			hives.add(hive);
		    }
		}
		break;
	      }
	      default:
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	    }

	    //
	    // For each matching top-level hive, get items.
	    //
	    for (IRegistry.Hive hive : hives) {
		for (IKey key : getKeys(rObj, hive, rc)) {
		    try {
			//
			// Create the base ItemType for the path
			//
			ReflectedRegkeyItem rItem = new ReflectedRegkeyItem();
			EntityItemRegistryHiveType hiveType = Factories.sc.windows.createEntityItemRegistryHiveType();
			hiveType.setValue(hive.getName());
			rItem.setHive(hiveType);
			EntityItemStringType keyType = Factories.sc.core.createEntityItemStringType();
			keyType.setValue(key.getPath());
			rItem.setKey(keyType);
			switch(view) {
			  case _32BIT:
			    rItem.setWindowsView("32_bit");
			    break;
			  case _64BIT:
			    rItem.setWindowsView("64_bit");
			    break;
			}

			//
			// Add items retrieved by the subclass
			//
			items.addAll(getItems(obj, rItem.it, key, rc));
		    } catch (NoSuchElementException e) {
			// No match.
		    } catch (CollectException e) {
			throw e;
		    } catch (Exception e) {
			MessageType msg = Factories.common.createMessageType();
			msg.setLevel(MessageLevelEnumeration.ERROR);
			msg.setValue(e.getMessage());
			rc.addMessage(msg);
			session.getLogger().debug(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		    }
		}
	    }
	} else {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.INFO);
	    msg.setValue(JOVALMsg.getMessage(JOVALMsg.WARNING_WINDOWS_VIEW, view.toString()));
	    rc.addMessage(msg);
	}
	return items;
    }

    // Protected

    /**
     * Return the Class of the ItemTypes generated by the subclass.
     */
    protected abstract Class getItemClass();

    /**
     * Subclasses can use this method to apply additional search conditions. This base class only applies conditions related
     * to keys (so, for instance, it doesn't create any FIELD_VALUE conditions).
     */
    protected List<ISearchable.ICondition> getConditions(ObjectType obj) throws PatternSyntaxException, CollectException {
	@SuppressWarnings("unchecked")
	List<ISearchable.ICondition> empty = (List<ISearchable.ICondition>)Collections.EMPTY_LIST;
	return empty;
    }

    /**
     * Return a list of items to associate with the given ObjectType, based on information gathered from the IKey.
     *
     * @arg it the base ItemType containing hive and key information already populated
     *
     * @throws NoSuchElementException if no matching item is found
     * @throws CollectException collection cannot take place and should be halted
     */
    protected abstract Collection<T> getItems(ObjectType obj, ItemType it, IKey key, IRequestContext rc) throws Exception;

    /**
     * Subclasses should override by supplying streams to any modules that must be loaded into requested runspaces using
     * the getRunspace method, below.
     */
    protected List<InputStream> getPowershellModules() {
	@SuppressWarnings("unchecked")
	List<InputStream> empty = (List<InputStream>)Collections.EMPTY_LIST;
	return empty;
    }

    /**
     * Get a runspace with the specified view, or create it if there isn't one yet.
     */
    protected IRunspace getRunspace(IWindowsSession.View view) throws Exception {
	switch(view) {
	  case _32BIT:
	    if (rs32 == null) {
		rs32 = createRunspace(view);
	    }
	    return rs32;

	  default:
	    if (rs == null) {
		rs = createRunspace(view);
	    }
	    return rs;
	}
    }

    /**
     * Returns the view suggested by the RegistryBehaviors.
     */
    protected IWindowsSession.View getView(RegistryBehaviors behaviors) {
	if (behaviors != null && behaviors.isSetWindowsView()) {
	    String s = behaviors.getWindowsView();
	    if ("32_bit".equals(s)) {
		return IWindowsSession.View._32BIT;
	    } else if ("64_bit".equals(s)) {
		return IWindowsSession.View._64BIT;
	    }
	}
	return session.getNativeView();
    }

    // Private

    /**
     * Create a runspace with the specified view. Modules supplied by getPowershellModules() will be auto-loaded before
     * the runspace is returned.
     */
    private IRunspace createRunspace(IWindowsSession.View view) throws Exception {
	IRunspace result = null;
	for (IRunspace runspace : session.getRunspacePool().enumerate()) {
	    if (view == runspace.getView()) {
		result = runspace;
		break;
	    }
	}
	if (result == null) {
	    result = session.getRunspacePool().spawn(view);
	}
	for (InputStream in : getPowershellModules()) {
	    result.loadModule(in);
	}
	return result;
    }

    /**
     * Return the list of all registry IKeys corresponding to the given RegistryObject.  Handles searches (from
     * pattern match operations), singletons (from equals operations), and searches based on RegistryBehaviors.
     */
    private List<IKey> getKeys(ReflectedRegistryObject rObj, IRegistry.Hive hive, IRequestContext rc) throws CollectException {
	RegistryBehaviors behaviors = rObj.getBehaviors();
	IRegistry registry = session.getRegistry(getView(behaviors));
	ISearchable<IKey> searcher = registry.getSearcher();
	List<ISearchable.ICondition> conditions = new ArrayList<ISearchable.ICondition>();
	conditions.add(searcher.condition(FIELD_HIVE, TYPE_EQUALITY, hive));

	boolean search = false;
	String keypath = null;
	if (!rObj.isKeyNil()) {
	    keypath = SafeCLI.checkArgument((String)rObj.getKey().getValue(), session);
	    OperationEnumeration op = rObj.getKey().getOperation();
	    switch(op) {
	      case EQUALS:
		conditions.add(searcher.condition(FIELD_KEY, TYPE_EQUALITY, keypath));
		break;

	      case PATTERN_MATCH:
		search = true;
		conditions.add(searcher.condition(FIELD_KEY, TYPE_PATTERN, Pattern.compile(keypath)));
		break;

	      default:
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	    }
	}

	//
	// If the subclass has any search conditions to add, go ahead and add them
	//
	conditions.addAll(getConditions(rObj.getObject()));

	int maxDepth = ISearchable.DEPTH_UNLIMITED;
	if (behaviors != null) {
	    maxDepth = behaviors.getMaxDepth().intValue();
	    if (maxDepth != 0 && "down".equals(behaviors.getRecurseDirection())) {
		search = true;
		conditions.add(searcher.condition(FIELD_DEPTH, TYPE_EQUALITY, new Integer(maxDepth)));
	    }
	}

	List<IKey> results = new ArrayList<IKey>();
	try {
	    if (search) {
		results.addAll(searcher.search(conditions));
	    } else if (behaviors != null && "up".equals(behaviors.getRecurseDirection())) {
		IKey key = null;
		if (keypath == null) {
		    key = registry.getHive(hive);
		} else {
		    key = registry.getKey(hive, keypath);
		}
		do {
		    results.add(key);
		    String path = key.getPath();
		    int ptr = path.lastIndexOf(IRegistry.DELIM_STR);
		    if (ptr == -1) {
			break;
		    } else {
			key = registry.getKey(hive, path.substring(0, ptr));
		    }
		} while(maxDepth-- > 0);
	    } else {
		if (keypath == null) {
		    results.add(registry.getHive(hive));
		} else {
		    results.add(registry.getKey(hive, keypath));
		}
	    }
	} catch (NoSuchElementException e) {
	    // ignore
	} catch (CollectException e) {
	    throw e;
	} catch (Exception e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(e.getMessage());
	    rc.addMessage(msg);
	    session.getLogger().debug(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return results;
    }

    /**
     * A reflection proxy for:
     *     scap.oval.definitions.windows.RegistryObject
     *     scap.oval.definitions.windows.RegkeyEffectiverights53Object
     *     scap.oval.definitions.windows.RegkeyEffectiverightsObject
     */
    class ReflectedRegistryObject {
	ObjectType obj;
	String id = null;
	boolean keyNil = false;
	EntityObjectRegistryHiveType hive = null;
	EntityObjectStringType key = null, name = null;
	RegistryBehaviors behaviors = null;

	ReflectedRegistryObject(ObjectType obj) throws CollectException {
	    this.obj = obj;

	    try {
		Method getId = obj.getClass().getMethod("getId");
		Object o = getId.invoke(obj);
		if (o != null) {
		    id = (String)o;
		}
	    } catch (NoSuchMethodException e) {
	    } catch (IllegalAccessException e) {
	    } catch (IllegalArgumentException e) {
	    } catch (InvocationTargetException e) {
	    }

	    try {
		Method getHive = obj.getClass().getMethod("getHive");
		Object o = getHive.invoke(obj);
		if (o == null) {
		    throw new CollectException(JOVALMsg.getMessage(JOVALMsg.ERROR_WINREG_HIVE_NAME, id), FlagEnumeration.ERROR);
		} else {
		    hive = (EntityObjectRegistryHiveType)o;
		}
	    } catch (NoSuchMethodException e) {
	    } catch (IllegalAccessException e) {
	    } catch (IllegalArgumentException e) {
	    } catch (InvocationTargetException e) {
	    }

	    try {
		Method getKey = obj.getClass().getMethod("getKey");
		Object o = getKey.invoke(obj);
		if (o != null) {
		    if (o instanceof JAXBElement) {
			JAXBElement j = (JAXBElement)o;
			keyNil = j.isNil();
			o = j.getValue();
		    }
		    key = (EntityObjectStringType)o;
		}
	    } catch (NoSuchMethodException e) {
	    } catch (IllegalAccessException e) {
	    } catch (IllegalArgumentException e) {
	    } catch (InvocationTargetException e) {
	    }

	    try {
		Method getBehaviors = obj.getClass().getMethod("getBehaviors");
		Object o = getBehaviors.invoke(obj);
		if (o != null) {
		    behaviors = (RegistryBehaviors)o;
		}
	    } catch (NoSuchMethodException e) {
	    } catch (IllegalAccessException e) {
	    } catch (IllegalArgumentException e) {
	    } catch (InvocationTargetException e) {
	    }
	}

	public ObjectType getObject() {
	    return obj;
	}

	public String getId() {
	    return id;
	}

	public boolean isSetHive() {
	    return hive != null;
	}

	public EntityObjectRegistryHiveType getHive() {
	    return hive;
	}

	public boolean isKeyNil() {
	    return keyNil;
	}

	public boolean isSetKey() {
	    return key != null;
	}

	public EntityObjectStringType getKey() {
	    return key;
	}

	public boolean isSetBehaviors() {
	    return behaviors != null;
	}

	public RegistryBehaviors getBehaviors() {
	    return behaviors;
	}
    }

    /**
     * A reflection proxy for:
     *     scap.oval.systemcharacteristics.windows.RegistryItem
     *     scap.oval.systemcharacteristics.windows.RegkeyEffectiverightsItem
     */
    class ReflectedRegkeyItem {
	ItemType it;
	Method setHive, setKey, setStatus, setWindowsView, wrapKey=null;
	Object factory;

	ReflectedRegkeyItem() throws ClassNotFoundException, InstantiationException, NoSuchMethodException,
		IllegalAccessException, InvocationTargetException {

	    Class clazz = getItemClass();
	    String className = clazz.getName();
	    String packageName = clazz.getPackage().getName();
	    String unqualClassName = className.substring(packageName.length() + 1);
	    Class<?> factoryClass = Class.forName(packageName + ".ObjectFactory");
	    factory = factoryClass.newInstance();
	    Method createType = factoryClass.getMethod("create" + unqualClassName);
	    it = (ItemType)createType.invoke(factory);

	    Method[] methods = it.getClass().getMethods();
	    for (int i=0; i < methods.length; i++) {
		String name = methods[i].getName();
		if ("setHive".equals(name)) {
		    setHive = methods[i];
		} else if ("setKey".equals(name)) {
		    setKey = methods[i];
		    String keyClassName = setKey.getParameterTypes()[0].getName();
		    if (keyClassName.equals(JAXBElement.class.getName())) {
			String methodName = "create" + unqualClassName + "Key";
			wrapKey = factoryClass.getMethod(methodName, EntityItemStringType.class);
		    }
		} else if ("setStatus".equals(name)) {
		    setStatus = methods[i];
		} else if ("setWindowsView".equals(name)) {
		    setWindowsView = methods[i];
		}
	    }
	}

	void setWindowsView(String view) {
	    try {
		if (setWindowsView != null) {
		    Class[] types = setWindowsView.getParameterTypes();
		    if (types.length == 1) {
			Class type = types[0];
			Object instance = Class.forName(type.getName()).newInstance();
			@SuppressWarnings("unchecked")
			Method setValue = type.getMethod("setValue", Object.class);
			setValue.invoke(instance, view);
			setWindowsView.invoke(it, instance);
		    }
		}
	    } catch (NoSuchMethodException e) {
	    } catch (IllegalAccessException e) {
	    } catch (IllegalArgumentException e) {
	    } catch (InvocationTargetException e) {
	    } catch (InstantiationException e) {
	    } catch (ClassNotFoundException e) {
	    }
	}

	void setKey(EntityItemStringType key)
		throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
	    if (setKey != null) {
		if (wrapKey == null) {
		    setKey.invoke(it, key);
		} else {
		    setKey.invoke(it, wrapKey.invoke(factory, key));
		}
	    }
	}

	void setHive(EntityItemRegistryHiveType hive)
		throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
	    if (setHive != null) {
		setHive.invoke(it, hive);
	    }
	}

	void setStatus(StatusEnumeration status)
		throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
	    if (setStatus != null) {
		setStatus.invoke(it, status);
	    }
	}
    }
}
