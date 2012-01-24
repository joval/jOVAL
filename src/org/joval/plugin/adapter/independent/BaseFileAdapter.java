// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.independent;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.MessageType;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.definitions.core.EntityObjectStringType;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.macos.Plist510Object;
import oval.schemas.definitions.macos.PlistObject;
import oval.schemas.systemcharacteristics.core.EntityItemAnySimpleType;
import oval.schemas.systemcharacteristics.core.EntityItemIntType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.EntityItemVersionType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.ISession;
import org.joval.intf.util.tree.INode;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.oval.NotCollectableException;
import org.joval.oval.OvalException;
import org.joval.oval.ResolveException;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.Version;

/**
 * Base class for IFile-based IAdapters. Subclasses need only implement getObjectClass, createFileItem, convertFilename
 * and getItems methods.  The base class handles searches and caching of search results.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public abstract class BaseFileAdapter implements IAdapter {
    protected ISession session;
    protected Hashtable<String, Collection<String>> pathMap;

    protected BaseFileAdapter(ISession session) {
	this.session = session;
	pathMap = new Hashtable<String, Collection<String>>();
    }

    // Implement IAdapter

    public boolean connect() {
	return true;
    }

    public void disconnect() {
    }

    public Collection<JAXBElement<? extends ItemType>> getItems(IRequestContext rc)
	    throws OvalException, NotCollectableException {

	ObjectType obj = rc.getObject();
	String id = obj.getId();

	//
	// Get the appropriate IFilesystem
	//
	ReflectedFileObject fObj = new ReflectedFileObject(obj);
	ReflectedFileBehaviors behaviors = null;
	if (fObj.isSetBehaviors()) {
	    behaviors = fObj.getBehaviors();
	}
	IFilesystem fs = null;
	int winView = 0;
	if (session instanceof IWindowsSession) {
	    if (behaviors != null) {
		if ("32_bit".equals(behaviors.getWindowsView())) {
		    fs = ((IWindowsSession)session).getFilesystem(IWindowsSession.View._32BIT);
		    winView = 32;
		}
	    }
	    if (winView == 0 && ((IWindowsSession)session).supports(IWindowsSession.View._64BIT)) {
		winView = 64;
	    }
	}
	if (fs == null) {
	    fs = session.getFilesystem();
	}

	Collection<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();
	for (String path : getPathList(rc, fs, behaviors)) {
	    IFile f = null;
	    try {
		f = fs.getFile(path);
		if (!f.exists()) {
		    throw new NoSuchElementException(path);
		}
		String dirPath = null;
		boolean isDirectory = f.isDirectory();
		if (isDirectory) {
		    dirPath = path;
		} else {
		    dirPath = path.substring(0, path.lastIndexOf(fs.getDelimiter()));
		}
		ReflectedFileItem fItem = new ReflectedFileItem();
		if (fObj.isSetFilepath()) {
		    if (isDirectory) {
			//
			// Object is looking for files, so skip over this directory
			//
			continue;
		    } else {
			EntityItemStringType filepathType = JOVALSystem.factories.sc.core.createEntityItemStringType();
			filepathType.setValue(f.getLocalName());
			EntityItemStringType pathType = JOVALSystem.factories.sc.core.createEntityItemStringType();
			pathType.setValue(getPath(f));
			EntityItemStringType filenameType = JOVALSystem.factories.sc.core.createEntityItemStringType();
			filenameType.setValue(f.getName());
			fItem.setFilepath(filepathType);
			fItem.setPath(pathType);
			fItem.setFilename(filenameType);
		    }
		} else if (fObj.isSetFilename() && fObj.getFilename().getValue() != null) {
		    if (isDirectory) {
			//
			// Object is looking for files, so skip over this directory
			//
			continue;
		    } else {
			EntityItemStringType filepathType = JOVALSystem.factories.sc.core.createEntityItemStringType();
			filepathType.setValue(f.getLocalName());
			EntityItemStringType pathType = JOVALSystem.factories.sc.core.createEntityItemStringType();
			pathType.setValue(getPath(f));
			EntityItemStringType filenameType = JOVALSystem.factories.sc.core.createEntityItemStringType();
			filenameType.setValue(f.getName());
			fItem.setFilepath(filepathType);
			fItem.setPath(pathType);
			fItem.setFilename(filenameType);
		    }
		} else if (fObj.isSetPath()) {
		    if (!isDirectory && fObj.isSetFilename() && fObj.getFilename().getValue() == null) {
			//
			// If xsi:nil is set for the filename element, we only want directories...
			//
			continue;
		    }

		    EntityItemStringType pathType = JOVALSystem.factories.sc.core.createEntityItemStringType();
		    pathType.setValue(dirPath);
		    fItem.setPath(pathType);
		    if (!isDirectory) {
			EntityItemStringType filenameType = JOVALSystem.factories.sc.core.createEntityItemStringType();
			filenameType.setValue(f.getName());
			fItem.setFilename(filenameType);
			EntityItemStringType filepathType = JOVALSystem.factories.sc.core.createEntityItemStringType();
			filepathType.setValue(getPath(f));
			fItem.setFilepath(filepathType);
		    }
		} else {
		    throw new NotCollectableException(JOVALSystem.getMessage(JOVALMsg.ERROR_TEXTFILECONTENT_SPEC, id));
		}

		switch(winView) {
		  case 32:
		    fItem.setWindowsView("32_bit");
		    break;
		  case 64:
		    fItem.setWindowsView("64_bit");
		    break;
		}
		items.addAll(getItems(fItem.it, f, rc));
	    } catch (NoSuchElementException e) {
		// skip it
	    } catch (IllegalAccessException e) {
		session.getLogger().warn(JOVALMsg.ERROR_REFLECTION, e.getMessage());
	    } catch (InvocationTargetException e) {
		session.getLogger().warn(JOVALMsg.ERROR_REFLECTION, e.getMessage());
	    } catch (IllegalArgumentException e) {
		session.getLogger().warn(JOVALMsg.ERROR_IO, path, e.getMessage());
	    } catch (IOException e) {
		MessageType msg = JOVALSystem.factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		if (f == null) {
		    msg.setValue(e.getMessage());
		} else {
		    msg.setValue(JOVALSystem.getMessage(JOVALMsg.ERROR_IO, path, e.getMessage()));
		}
		rc.addMessage(msg);
	    }
	}
	return items;
    }

    // Protected

    /**
     * Return either an EntityItemStringType or a JAXBElement<EntityItemStringType>, as appropriate for the relevant ItemType.
     */
    protected abstract Object convertFilename(EntityItemStringType filename);

    /**
     * Create and return an instance of the appropriate ItemType.
     */
    protected abstract ItemType createFileItem();

    /**
     * Return a list of items to associate with the given ObjectType, based on information gathered from the IFile.
     *
     * @arg it the base ItemType containing filepath, path and filename information already populated
     */
    protected abstract Collection<JAXBElement<? extends ItemType>>
	getItems(ItemType it, IFile f, IRequestContext rc) throws IOException, NotCollectableException, OvalException;

    // Internal

    /**
     * Get a list of String paths for this object.  This accommodates searches (from pattern match operations),
     * singletons (from equals operations) and handles recursive searches specified by FileBehaviors.
     *
     * The resulting collection may contain extraneous objects.  For instance, it may contain files that match the
     * object path pattern even though the filename may have been nilled.  Hence, the results should be filtered.
     */
    final Collection<String> getPathList(IRequestContext rc, IFilesystem fs, ReflectedFileBehaviors fb) throws OvalException {
	ObjectType obj = rc.getObject();
	Collection<String> list = pathMap.get(obj.getId());
	if (list != null) {
	    return list;
	}

	list = new HashSet<String>();
	try {
	    ReflectedFileObject fObj = new ReflectedFileObject(obj);

	    if (fObj.isSetFilepath()) {
		//
		// Windows view is already handled, and FileBehaviors recursion is ignored in the Filepath case, so
		// all we need to do is add the discovered paths to the return list.
		//
		Collection<String> filepaths = new HashSet<String>();
		EntityObjectStringType filepath = fObj.getFilepath();
		if (filepath.isSetVarRef()) {
		    filepaths.addAll(rc.resolve(filepath.getVarRef()));
		} else {
		    filepaths.add((String)filepath.getValue());
		}
		OperationEnumeration op = filepath.getOperation();
		switch(op) {
		  case EQUALS:
		    list.addAll(filepaths);
		    break;

		  case PATTERN_MATCH:
		    for (String value : filepaths) {
			list.addAll(fs.search(Pattern.compile(value)));
		    }
		    break;

		  default:
		    throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op));
		}
	    } else if (fObj.isSetPath()) {
		//
		// First, collect all possible matching paths (i.e., dirs)
		//
		Collection<String> paths = new HashSet<String>();
		EntityObjectStringType path = fObj.getPath();
		if (path.isSetVarRef()) {
		    paths.addAll(rc.resolve(path.getVarRef()));
		} else if (path.isSetValue()) {
		    paths.add((String)path.getValue());
		}
		OperationEnumeration op = path.getOperation();
		switch(op) {
		  case EQUALS:
		    list.addAll(paths);
		    if (fb != null) {
			//
			// Recursive search File Behaviors always only apply exclusively to well-defined paths, never
			// patterns.
			//
			list = getDirs(list, fb.getDepth(), fb.getRecurseDirection(), fb.getRecurse(), fs);
		    }
		    break;

		  case PATTERN_MATCH:
		    for (String value : paths) {
			list.addAll(fs.search(Pattern.compile(value)));
		    }
		    break;

		  default:
		    throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op));
		}

		if (fObj.isSetFilename()) {
		    EntityObjectStringType filename = fObj.getFilename();
		    Collection<String> fnames = new Vector<String>();
		    if (filename.isSetVarRef()) {
			fnames.addAll(rc.resolve(filename.getVarRef()));
		    } else if (filename.getValue() != null) {
			fnames.add((String)filename.getValue());
		    }
		    Collection<String> files = new Vector<String>();
		    for (String pathString : list) {
			if (!pathString.endsWith(fs.getDelimiter())) {
			    pathString = pathString + fs.getDelimiter();
			}
			for (String fname : fnames) {
			    try {
				switch(filename.getOperation()) {
				  case PATTERN_MATCH: {
				    IFile f = fs.getFile(pathString);
				    if (f.exists() && f.isDirectory() && f.hasChildren()) {
					for (INode child : f.getChildren(Pattern.compile(fname))) {
					    files.add(child.getPath());
					}
				    }
				    break;
				  }
 
				  case EQUALS:
				    files.add(pathString + fname);
				    break;

				  case NOT_EQUAL: {
				    IFile f = fs.getFile(pathString);
				    if (f.exists() && f.isDirectory() && f.hasChildren()) {
					for (INode child : f.getChildren(Pattern.compile(fname))) {
					    files.add(child.getPath());
					}
				    }
				    break;
				  }
 
				  default:
				    throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION,
										   filename.getOperation()));
				}
			    } catch (UnsupportedOperationException e) {
				// ignore -- not a directory
			    } catch (IllegalArgumentException e) {
				session.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
			    } catch (IOException e) {
				MessageType msg = JOVALSystem.factories.common.createMessageType();
				msg.setLevel(MessageLevelEnumeration.ERROR);
				msg.setValue(e.getMessage());
				rc.addMessage(msg);
			    }
			}
		    }
		    list = files;
		}
	    } else if (isPlistObject(obj)) {
		for (String path : getPlistPaths(obj, rc)) {
		    list.add(path);
		}
	    } else {
		throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_BAD_FILE_OBJECT, obj.getId()));
	    }
	} catch (PatternSyntaxException e) {
       	    session.getLogger().error(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (NoSuchElementException e) {
       	    session.getLogger().trace(JOVALMsg.STATUS_NOT_FOUND, e.getMessage(), obj.getId());
	} catch (ResolveException e) {
	    MessageType msg = JOVALSystem.factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(e.getMessage());
	    rc.addMessage(msg);
	}
	pathMap.put(obj.getId(), list);
	return list;
    }

    /**
     * Does the object represent a MacOS X plist object?
     */
    private boolean isPlistObject(ObjectType obj) {
	return obj instanceof PlistObject || obj instanceof Plist510Object;
    }

    /**
     * Get the path of the plist file based on the app_id.
     */
    private Collection<String> getPlistPaths(ObjectType obj, IRequestContext rc) throws OvalException, ResolveException {
	EntityObjectStringType appIdType = null;
	if (obj instanceof PlistObject) {
	    appIdType = ((PlistObject)obj).getAppId();
	} else if (obj instanceof Plist510Object) {
	    appIdType = ((Plist510Object)obj).getAppId();
	}
	if (appIdType == null) {
	    throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_BAD_PLIST_OBJECT, obj.getId()));
	} else {
	    Collection<String> paths = new Vector<String>();

	    StringBuffer sb = new StringBuffer(session.getEnvironment().getenv("HOME"));
	    sb.append("/Library/Preferences/");
	    if (appIdType.isSetValue()) {
		paths.add(sb.append((String)appIdType.getValue()).append(".plist").toString());
	    } else if (appIdType.isSetVarRef()) {
		for (String appId : rc.resolve(appIdType.getVarRef())) {
		    paths.add(new StringBuffer(sb.toString()).append(appId).append(".plist").toString());
		}
	    } else {
		throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_BAD_PLIST_OBJECT, obj.getId()));
	    }

	    return paths;
	}
    }

    /**
     * Finds directories recursively based on FileBahaviors.
     */
    private Collection<String> getDirs(Collection<String> list, int depth, String direction, String recurse, IFilesystem fs) {
	if ("none".equals(direction) || depth == 0) {
	    return list;
	} else {
	    Collection<String> results = new HashSet<String>();
	    for (String path : list) {
		if (!path.endsWith(fs.getDelimiter())) {
		    path = path + fs.getDelimiter();
		}
		session.getLogger().trace(JOVALMsg.STATUS_FS_RECURSE, path);
		try {
		    IFile f = (IFile)fs.lookup(path);
		    if (f == null) {
			// skip permission denied (or other access error)
		    } else if (!f.exists()) {
			// skip non-existent files
		    } else if (recurse != null && recurse.indexOf("symlinks") == -1 && f.isLink()) {
			// skip the symlink
		    } else if (recurse != null && recurse.indexOf("directories") == -1 && f.isDirectory()) {
			// skip the directory
		    } else if (f.isDirectory()) {
			results.add(f.getLocalName());
			if ("up".equals(direction)) {
			    int ptr = 0;
			    ptr = path.lastIndexOf(fs.getDelimiter(),
						   path.lastIndexOf(fs.getDelimiter()) - fs.getDelimiter().length());
			    if (ptr != -1) {
				Collection<String> c = new HashSet<String>();
				c.add(path.substring(0, ptr));
				results.addAll(getDirs(c, --depth, direction, recurse, fs));
			    }
			} else { // recurse down
			    Collection<String> c = new HashSet<String>();
			    for (INode child : f.getChildren()) {
				c.add(child.getPath());
			    }
			    results.addAll(getDirs(c, --depth, direction, recurse, fs));
			}
		    }
		} catch (UnsupportedOperationException e) {	
		    // ignore -- not a directory
		} catch (NoSuchElementException e) {
		    // dir path doesn't exist.
		} catch (FileNotFoundException e) {
		    // link is not a link to a directory
		} catch (IOException e) {
		    session.getLogger().warn(JOVALMsg.ERROR_IO, path, e.getMessage());
		    session.getLogger().debug(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		}
	    }
	    return results;
	}
    }

    private String getPath(IFile f) {
	String name = f.getName();
	String path = f.getLocalName();
	int len = path.length();
	if (len > 1) {
	    return path.substring(0, len - name.length() - 1);
	} else {
	    return path;
	}
    }

    /**
     * A reflection proxy for:
     *     oval.schemas.definitions.independent.Textfilecontent54Object
     *     oval.schemas.definitions.independent.TextfilecontentObject
     *     oval.schemas.definitions.unix.FileObject
     *     oval.schemas.definitions.windows.FileObject
     */
    class ReflectedFileObject {
	ObjectType obj;
	String id = null;
	boolean filenameNil = false;
	EntityObjectStringType filepath = null, path = null, filename = null;
	ReflectedFileBehaviors behaviors = null;

	ReflectedFileObject(ObjectType obj) {
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
		Method getFilepath = obj.getClass().getMethod("getFilepath");
		Object o = getFilepath.invoke(obj);
		if (o != null) {
		    filepath = (EntityObjectStringType)o;
		}
	    } catch (NoSuchMethodException e) {
	    } catch (IllegalAccessException e) {
	    } catch (IllegalArgumentException e) {
	    } catch (InvocationTargetException e) {
	    }

	    try {
		Method getFilename = obj.getClass().getMethod("getFilename");
		Object o = getFilename.invoke(obj);
		if (o != null) {
		    if (o instanceof JAXBElement) {
			JAXBElement j = (JAXBElement)o;
			filenameNil = j.isNil();
			o = j.getValue();
		    }
		    filename = (EntityObjectStringType)o;
		}
	    } catch (NoSuchMethodException e) {
	    } catch (IllegalAccessException e) {
	    } catch (IllegalArgumentException e) {
	    } catch (InvocationTargetException e) {
	    }

	    try {
		Method getPath = obj.getClass().getMethod("getPath");
		Object o = getPath.invoke(obj);
		if (o != null) {
		    path = (EntityObjectStringType)o;
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
		    behaviors = new ReflectedFileBehaviors(o);
		}
	    } catch (NoSuchMethodException e) {
	    } catch (IllegalAccessException e) {
	    } catch (IllegalArgumentException e) {
	    } catch (InvocationTargetException e) {
	    }
	}

	String getId() {
	    return id;
	}

	boolean isSetFilepath() {
	    return filepath != null;
	}

	EntityObjectStringType getFilepath() {
	    return filepath;
	}

	boolean isFilenameNil() {
	    return filenameNil;
	}

	boolean isSetFilename() {
	    return filename != null;
	}

	EntityObjectStringType getFilename() {
	    return filename;
	}

	boolean isSetPath() {
	    return path != null;
	}

	EntityObjectStringType getPath() {
	    return path;
	}

	boolean isSetBehaviors() {
	    return behaviors != null;
	}

	ReflectedFileBehaviors getBehaviors() {
	    return behaviors;
	}
    }

    /**
     * A reflection proxy for:
     *     oval.schemas.definitions.independent.FileBehaviors
     *     oval.schemas.definitions.unix.FileBehaviors
     *     oval.schemas.definitions.windows.FileBehaviors
     */
    class ReflectedFileBehaviors {
	BigInteger maxDepth = new BigInteger("-1");
	String recurseDirection = "none";
	String recurse = "symlinks and directories";
	String recurseFS = "all";
	String windowsView = "64_bit";

	ReflectedFileBehaviors(Object obj)
		throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
	    if (obj != null) {
		Method getMaxDepth = obj.getClass().getMethod("getMaxDepth");
		maxDepth = (BigInteger)getMaxDepth.invoke(obj);

		Method getRecurseDirection = obj.getClass().getMethod("getRecurseDirection");
		recurseDirection = (String)getRecurseDirection.invoke(obj);

		try {
		    //
		    // Not applicable to Unix FileBehaviors
		    //
		    Method getWindowsView = obj.getClass().getMethod("getWindowsView");
		    windowsView = (String)getWindowsView.invoke(obj);
		} catch (NoSuchMethodException e) {
		    recurse = null;
		}

		try {
		    //
		    // Not applicable to Windows FileBehaviors
		    //
		    Method getRecurse = obj.getClass().getMethod("getRecurse");
		    recurse = (String)getRecurse.invoke(obj);
		} catch (NoSuchMethodException e) {
		    recurse = null;
		}

		Method getRecurseFileSystem = obj.getClass().getMethod("getRecurseFileSystem");
		recurseFS = (String)getRecurseFileSystem.invoke(obj);
		if (!"all".equals(recurseFS)) {
		    session.getLogger().error(JOVALMsg.ERROR_UNSUPPORTED_BEHAVIOR, recurseFS);
		}
	    }
	    if ("none".equals(recurseDirection)) {
		maxDepth = BigInteger.ZERO;
	    }
	}

	String getRecurse() {
	    return recurse;
	}

	String getRecurseDirection() {
	    return recurseDirection;
	}

	int getDepth() {
	    return Integer.parseInt(maxDepth.toString());
	}

	String getRecurseFileSystem() {
	    return recurseFS;
	}

	String getWindowsView() {
	    return windowsView;
	}
    }

    /**
     * A reflection proxy for:
     *     oval.schemas.systemcharacteristics.independent.TextfilecontentItem
     *     oval.schemas.systemcharacteristics.unix.FileItem
     *     oval.schemas.systemcharacteristics.windows.FileItem
     */
    class ReflectedFileItem {
	ItemType it;
	Method setFilepath, setFilename, setPath, setStatus, setWindowsView;

	ReflectedFileItem() {
	    it = createFileItem();
	    Method[] methods = it.getClass().getMethods();
	    for (int i=0; i < methods.length; i++) {
		String name = methods[i].getName();
		if ("setFilepath".equals(name)) {
		    setFilepath = methods[i];
		} else if ("setFilename".equals(name)) {
		    setFilename = methods[i];
		} else if ("setPath".equals(name)) {
		    setPath = methods[i];
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
			Object instance = Class.forName(type.getName());
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
	    } catch (ClassNotFoundException e) {
	    }
	}

	void setFilename(EntityItemStringType filename)
		throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
	    if (setFilename != null) {
		setFilename.invoke(it, convertFilename(filename));
	    }
	}

	void setFilepath(EntityItemStringType filepath)
		throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
	    if (setFilepath != null) {
		setFilepath.invoke(it, filepath);
	    }
	}

	void setPath(EntityItemStringType path)
		throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
	    if (setPath != null) {
		setPath.invoke(it, path);
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
