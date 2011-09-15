// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.independent;

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
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.MessageType;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.definitions.core.EntityObjectStringType;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.systemcharacteristics.core.EntityItemAnySimpleType;
import oval.schemas.systemcharacteristics.core.EntityItemIntType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.EntityItemVersionType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;

/* DAS: TBD -- adding view information to the base objects.
import oval.schemas.systemcharacteristics.independent.EntityItemWindowsViewType;
import oval.schemas.systemcharacteristics.windows.EntityItemWindowsViewType;
*/

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.ISession;
import org.joval.intf.util.tree.INode;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.oval.OvalException;
import org.joval.oval.ResolveException;
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

    public Collection<JAXBElement<? extends ItemType>> getItems(IRequestContext rc) throws OvalException {
	ObjectType obj = rc.getObject();
	String id = obj.getId();
	if (!obj.getClass().getName().equals(getObjectClass().getName())) {
	    throw new OvalException(JOVALSystem.getMessage("ERROR_INSTANCE",
							   getObjectClass().getName(), obj.getClass().getName()));
	}

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
	Collection<String> paths = getPathList(rc, fs, behaviors);
	for (String path : paths) {
	    IFile f = null;
	    try {
		ReflectedFileItem fItem = new ReflectedFileItem();

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
		} else if (fObj.isSetFilename() && fObj.getFilename() != null) {
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
		    throw new OvalException(JOVALSystem.getMessage("ERROR_TEXTFILECONTENT_SPEC", id));
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
		JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_REFLECTION", e.getMessage()));
	    } catch (IllegalArgumentException e) {
		JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_REFLECTION", e.getMessage()));
	    } catch (InvocationTargetException e) {
		JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_REFLECTION", e.getMessage()));
	    } catch (IOException e) {
		MessageType msg = JOVALSystem.factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		if (f == null) {
		    msg.setValue(e.getMessage());
		} else {
		    msg.setValue(JOVALSystem.getMessage("ERROR_IO", f.getLocalName(), e.getMessage()));
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
	getItems(ItemType it, IFile f, IRequestContext rc) throws IOException, OvalException;

    // Internal

    /**
     * Get a list of String paths for this object.  This accommodates searches (from pattern match operations),
     * singletons (from equals operations) and handles recursive searches specified by FileBehaviors.
     */
    final Collection<String> getPathList(IRequestContext rc, IFilesystem fs, ReflectedFileBehaviors fb) throws OvalException {
	ObjectType obj = rc.getObject();
	Collection<String> list = pathMap.get(obj.getId());
	if (list != null) {
	    return list;
	}

	list = new Vector<String>();
	try {
	    ReflectedFileObject fObj = new ReflectedFileObject(obj);

	    boolean patternMatch = false;
	    if (fObj.isSetFilepath()) {
		Collection<String> filepaths = new Vector<String>();
		EntityObjectStringType filepath = fObj.getFilepath();
		if (filepath.isSetVarRef()) {
		    filepaths.addAll(rc.resolve(filepath.getVarRef()));
		} else {
		    filepaths.add((String)filepath.getValue());
		}
		switch(filepath.getOperation()) {
		  case EQUALS:
		    list.addAll(filepaths);
		    break;
		  case PATTERN_MATCH:
		    patternMatch = true;
		    for (String value : filepaths) {
			list.addAll(fs.search(Pattern.compile(value), false));
		    }
		    break;
		  default:
		    throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_OPERATION", filepath.getOperation()));
		}
	    } else if (fObj.isSetPath()) {
		//
		// First, collect all possible matching paths (i.e., dirs)
		//
		Collection<String> paths = new Vector<String>();
		EntityObjectStringType path = fObj.getPath();
		if (path.isSetVarRef()) {
		    paths.addAll(rc.resolve(path.getVarRef()));
		} else if (path.isSetValue()) {
		    paths.add((String)path.getValue());
		}
		switch(path.getOperation()) {
		  case EQUALS:
		    list.addAll(paths);
		    break;
		  case PATTERN_MATCH:
		    patternMatch = true;
		    for (String value : paths) {
			for (String s : fs.search(Pattern.compile(value), false)) {
			    list.add(s);
			}
		    }
		    break;
		  default:
		    throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_OPERATION", path.getOperation()));
		}

		if (fb != null) {
		    list = getDirs(list, fb.getDepth(), fb.getRecurseDirection(), fb.getRecurse(), fs);
		} else if (patternMatch) {
		    //
		    // Wildcard pattern matches are really supposed to be recursive searches, unfortunately
		    //
		    Collection<String> newList = new Vector<String>();
		    for (String value : list) {
			if (((String)path.getValue()).indexOf(".*") != -1 ||
			    ((String)path.getValue()).indexOf(".+") != -1) {
			    Collection<String> l = new Vector<String>();
			    l.add(value);
			    newList.addAll(getDirs(l, -1, "down", "directories", fs));
			}
		    }
		    for (String value : newList) {
			if (!list.contains(value)) {
			    list.add(value);
			}
		    }
		}

		if (fObj.isSetFilename()) {
		    EntityObjectStringType filename = fObj.getFilename();
		    if (filename == null) {
			// False positive for isSetFilename -- happens with nil
		    } else {
			Collection<String> fnames = new Vector<String>();
			if (filename.isSetVarRef()) {
			    fnames.addAll(rc.resolve(filename.getVarRef()));
			} else {
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
					throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_OPERATION",
										       filename.getOperation()));
				    }
				} catch (IllegalArgumentException e) {
       				    JOVALSystem.getLogger().log(Level.WARNING, e.getMessage(), e);
				}
			    }
			}
			list = files;
		    }
		}
	    } else {
		throw new OvalException("ERROR_BAD_FILEOBJECT" + obj.getId());
	    }
	} catch (PatternSyntaxException e) {
       	    JOVALSystem.getLogger().log(Level.SEVERE, e.getMessage(), e);
	} catch (IOException e) {
       	    JOVALSystem.getLogger().log(Level.WARNING, e.getMessage(), e);
	} catch (NoSuchElementException e) {
       	    JOVALSystem.getLogger().log(Level.FINER, JOVALSystem.getMessage("STATUS_NOT_FOUND", e.getMessage(), obj.getId()));
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
     * Finds directories recursively based on FileBahaviors.
     */
    private Collection<String> getDirs(Collection<String> list, int depth, String direction, String recurse, IFilesystem fs) {
	if ("none".equals(direction) || depth == 0) {
	    return list;
	} else {
	    Collection<String> results = new Vector<String>();
	    for (String path : list) {
		if (!path.endsWith(fs.getDelimiter())) {
		    path = path + fs.getDelimiter();
		}
		JOVALSystem.getLogger().log(Level.FINE, JOVALSystem.getMessage("STATUS_FS_RECURSE", path));
		try {
		    IFile f = (IFile)fs.lookup(path);
		    if (!f.exists()) {
			// skip non-existent files
		    } else if (recurse != null && recurse.indexOf("symlinks") == -1 && f.isLink()) {
			// skip the symlink
		    } else if (recurse != null && recurse.indexOf("directories") == -1 && f.isDirectory()) {
			// skip the directory
		    } else if (f.isDirectory()) {
			results.add(f.getLocalName());
			if ("up".equals(direction)) {
			    int ptr = 0;
			    ptr = path.lastIndexOf(fs.getDelimiter(), path.lastIndexOf(fs.getDelimiter())-1);
			    if (ptr != -1) {
				Vector<String> v = new Vector<String>();
				v.add(path.substring(0, ptr));
				results.addAll(getDirs(v, --depth, direction, recurse, fs));
			    }
			} else { // recurse down
			    String[] children = f.list();
			    if (children != null) {
				Vector<String> v = new Vector<String>();
				for (int i=0; i < children.length; i++) {
				    v.add(path + children[i]);
				}
				results.addAll(getDirs(v, --depth, direction, recurse, fs));
			    }
			}
		    }
		} catch (NoSuchElementException e) {
		    // dir path doesn't exist.
		} catch (IOException e) {
		    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_IO", path, e.getMessage()));
		}
	    }
	    //
	    // Eliminate any duplicates, and remove any trailing slashes
	    //
	    HashSet<String> deduped = new HashSet<String>();
	    for (String s : results) {
		if (!deduped.contains(s)) {
		    deduped.add(s);
		}
	    }
	    return deduped;
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
			o = ((JAXBElement)o).getValue();
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
		    JOVALSystem.getLogger().log(Level.SEVERE,
						JOVALSystem.getMessage("ERROR_UNSUPPORTED_BEHAVIOR", recurseFS));
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
	    setFilename.invoke(it, convertFilename(filename));
	}

	void setFilepath(EntityItemStringType filepath)
		throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
	    setFilepath.invoke(it, filepath);
	}

	void setPath(EntityItemStringType path)
		throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
	    setPath.invoke(it, path);
	}

	void setStatus(StatusEnumeration status)
		throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
	    setStatus.invoke(it, status);
	}
    }
}
