// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.io.fs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.cal10n.LocLogger;

import org.apache.jdbm.DB;
import org.apache.jdbm.DBMaker;
import org.apache.jdbm.Serializer;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFileEx;
import org.joval.intf.io.IFileMetadata;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.io.IRandomAccess;
import org.joval.intf.util.ILoggable;
import org.joval.intf.util.IProperty;
import org.joval.intf.util.ISearchable;
import org.joval.intf.system.ISession;
import org.joval.intf.system.IEnvironment;
import org.joval.util.JOVALMsg;
import org.joval.util.StringTools;

/**
 * An abstract IFilesystem implementation with caching and convenience methods.
 *
 * All IFilesystem implementations extend this base class.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public abstract class AbstractFilesystem implements IFilesystem {
    /**
     * Static map of filesystem instances indexed by hashcode for reference by deserialized serializers.
     */
    public static Map<Integer, AbstractFilesystem> instances = new HashMap<Integer, AbstractFilesystem>();

    protected boolean autoExpand = true;
    protected IProperty props;
    protected ISession session;
    protected IEnvironment env;
    protected LocLogger logger;
    protected DB db;

    protected final String ESCAPED_DELIM;
    protected final String DELIM;

    protected AbstractFilesystem(ISession session, String delim, String dbkey) {
	ESCAPED_DELIM = Matcher.quoteReplacement(delim);
	DELIM = delim;
	this.session = session;
	logger = session.getLogger();
	props = session.getProperties();
	env = session.getEnvironment();

	if (session.getProperties().getBooleanProperty(IFilesystem.PROP_CACHE_JDBM)) {
	    for (File f : session.getWorkspace().listFiles()) {
		if (f.getName().startsWith(dbkey)) {
		    f.delete();
		}
	    }
	    DBMaker dbm = DBMaker.openFile(new File(session.getWorkspace(), dbkey).toString());
	    dbm.disableTransactions();
	    dbm.closeOnExit();
	    dbm.deleteFilesAfterClose();
	    db = dbm.make();
	    Integer instanceKey = new Integer(hashCode());
	    instances.put(instanceKey, this);
	    cache = db.createHashMap("files", null, getFileSerializer(instanceKey));
	    index = db.createHashSet("index");
	} else {
	    cache = new HashMap<String, IFile>();
	    index = cache.keySet();
	}
    }

    public void setAutoExpand(boolean autoExpand) {
	this.autoExpand = autoExpand;
    }

    public void dispose() {
	if (db != null) {
	    try {
		db.close();
	    } catch (Exception e) {
		logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	    db = null;
	}
	instances.remove(new Integer(hashCode()));
    }

    /**
     * Create a hash-map. If JDBM caching is enabled, the map will be backed by a b-tree file.
     */
    public <J, K> Map<J, K> createHashMap(String name, Serializer<J> keySerializer, Serializer<K> valueSerializer) {
	if (db == null) {
	    return new HashMap<J, K>();
	} else if (valueSerializer == null) {
	    return db.createHashMap(name);
	} else {
	    return db.createHashMap(name, keySerializer, valueSerializer);
	}
    }

    /**
     * For use by the ISearchable.
     */
    public String[] guessParent(Pattern p) {
	String path = p.pattern();
	if (!path.startsWith("^")) {
	    return null;
	}
	path = path.substring(1);

	int ptr = path.indexOf(ESCAPED_DELIM);
	if (ptr == -1) {
	    return Arrays.asList(path).toArray(new String[1]);
	}

	StringBuffer sb = new StringBuffer(path.substring(0,ptr)).append(DELIM);
	ptr += ESCAPED_DELIM.length();
	int next = ptr;
	while((next = path.indexOf(ESCAPED_DELIM, ptr)) != -1) {
	    String token = path.substring(ptr, next);
	    if (StringTools.containsRegex(token)) {
		break;
	    } else {
		if (!sb.toString().endsWith(DELIM)) {
		    sb.append(DELIM);
		}
		sb.append(token);
		ptr = next + ESCAPED_DELIM.length();
	    }
	}
	if (sb.length() == 0) {
	    return null;
	} else {
	    String parent = sb.toString();

	    // One of the children of parent should match...
	    StringBuffer prefix = new StringBuffer("^");
	    String token = path.substring(ptr);
	    for (int i=0; i < token.length(); i++) {
		char c = token.charAt(i);
		boolean isRegexChar = false;
		for (char ch : StringTools.REGEX_CHARS) {
		    if (c == ch) {
			isRegexChar = true;
			break;
		    }
		}
		if (isRegexChar) {
		    break;
		} else {
		    prefix.append(c);
		}
	    }
	    try {
		if (prefix.length() > 1) {
		    IFile base = getFile(parent);
		    List<String> candidates = new ArrayList<String>();
		    for (IFile f : base.listFiles(Pattern.compile(prefix.toString()))) {
			if (f.isDirectory()) {
			    candidates.add(f.getPath());
			}
		    }
		    if (candidates.size() > 0) {
			return candidates.toArray(new String[candidates.size()]);
		    }
		}
	    } catch (Exception e) {
	    }

	    return Arrays.asList(parent).toArray(new String[1]);
	}
    }

    /**
     * Return an implementation-specific IFile serializer for JDBM. The instanceKey is used to re-associate
     * deserialized IFile instances back with this AbstractFilesystem instance.
     */
    public abstract Serializer<IFile> getFileSerializer(Integer instanceKey);

    /**
     * Return an implementation-specific IFile instance based on an IAccessor. The AbstractFilesystem base class invokes
     * this method if it cannot return the file from the cache.
     */
    protected abstract IFile getPlatformFile(String path, IFile.Flags flags) throws IllegalArgumentException, IOException;

    /**
     * For use during deserialization.  A serialized IFile will only consist of metadata.  So, to reconstitute an IFile
     * capable of accessing the underlying file, use:
     *
     * <code>AbstractFilesystem.instances.get(instanceKey).createFileFromInfo(...)</code>
     */
    public IFile createFileFromInfo(IFileMetadata info) {
	IFile f = new DefaultFile(info, IFile.Flags.READONLY);
	return f;
    }

    // Implement ILoggable

    public void setLogger(LocLogger logger) {
	this.logger = logger;
    }

    public LocLogger getLogger() {
	return logger;
    }

    // Implement IFilesystem (partially)

    public String getDelimiter() {
	return DELIM;
    }

    public final IFile getFile(String path) throws IOException {
	return getFile(path, IFile.Flags.READONLY);
    }

    public final IFile getFile(String path, IFile.Flags flags) throws IOException {
        if (autoExpand) {
            path = env.expand(path);
        }
        switch(flags) {
          case READONLY:
	    try {
                return getCache(path);
	    } catch (NoSuchElementException e) {
	    }
	    // fall-thru

	  default:
	    return getPlatformFile(path, flags);
	}
    }

    public final IRandomAccess getRandomAccess(IFile file, String mode) throws IllegalArgumentException, IOException {
	return file.getRandomAccess(mode);
    }

    public final IRandomAccess getRandomAccess(String path, String mode) throws IllegalArgumentException, IOException {
	IFile.Flags flags = "rw".equals(mode) ? flags = IFile.Flags.READWRITE : IFile.Flags.READONLY;
	return getFile(path, flags).getRandomAccess(mode);
    }

    public final InputStream getInputStream(String path) throws IllegalArgumentException, IOException {
	return getFile(path).getInputStream();
    }

    // Inner Classes

    /**
     * The default implementation of an IFile -- works with Java (local) Files.
     */
    public class DefaultFile implements IFile {
	protected String path;
	protected IAccessor accessor;
	protected IFileMetadata info;
	protected Flags flags;

	/**
	 * Create a file from an accessor.
	 */
	public DefaultFile(String path, IAccessor accessor, Flags flags) {
	    this.path = path;
	    this.accessor = accessor;
	    this.flags = flags;
	}

	/**
	 * Create a file from metadata. Note that subclasses must ALWAYS delegate to this constructor when initializing
	 * from IFileMetadata, or else the metadata will not be cached.
	 */
	public DefaultFile(IFileMetadata info, Flags flags) {
	    path = info.getPath();
	    this.info = info;
	    this.flags = flags;

	    //
	    // Place in the cache if READONLY
	    //
	    if (flags == IFile.Flags.READONLY) {
		putCache(this);
	    }
	}

	// Implement IFileMetadata

	public Type getType() throws IOException {
	    return getInfo().getType();
	}

	public String getLinkPath() throws IllegalStateException, IOException {
	    return getInfo().getLinkPath();
	}

	public long accessTime() throws IOException {
	    return getInfo().accessTime();
	}

	public long createTime() throws IOException {
	    return getInfo().createTime();
	}

	public long lastModified() throws IOException {
	    if (info == null) {
		return accessor.getMtime();
	    } else {
		return info.lastModified();
	    }
	}

	public long length() throws IOException {
	    if (info == null) {
		return accessor.getLength();
	    } else {
		return info.length();
	    }
	}

	public String getPath() {
	    return path;
	}

	public String getCanonicalPath() throws IOException {
	    if (info == null) {
		return accessor.getCanonicalPath();
	    } else {
		return info.getCanonicalPath();
	    }
	}

	public IFileEx getExtended() throws IOException {
	    return getInfo().getExtended();
	}

	// Implement IFile

	public String getName() {
	    if (path.equals(DELIM)) {
		return path;
	    } else {
		int ptr = path.lastIndexOf(DELIM);
		if (ptr == -1) {
		    return path;
		} else {
		    return path.substring(ptr + DELIM.length());
		}
	    }
	}

	public String getParent() {
	    if (path.equals(DELIM)) {
		return path;
	    } else {
		int ptr = path.lastIndexOf(DELIM);
		if (ptr == -1) {
		    return path;
		} else {
		    return path.substring(0, ptr);
		}
	    }
	}

	public boolean isDirectory() throws IOException {
	    try {
		return getInfo().getType() == Type.DIRECTORY;
	    } catch (FileNotFoundException e) {
		return false;
	    }
	}

	public boolean isFile() throws IOException {
	    try {
		return getInfo().getType() == Type.FILE;
	    } catch (FileNotFoundException e) {
		return false;
	    }
	}

	public boolean isLink() throws IOException {
	    try {
		return getInfo().getType() == Type.LINK;
	    } catch (FileNotFoundException e) {
		return false;
	    }
	}

	public boolean exists() {
	    if (info == null) {
		try {
		    return accessor.exists();
		} catch (Exception e) {
		    return false;
		}
	    } else {
		return true;
	    }
	}

	public final boolean mkdir() {
	    switch(flags) {
	      case READWRITE:
		try {
		    return getAccessor().mkdir();
		} catch (IOException e) {
		    return false;
		}
	      default:
		return false;
	    }
	}

	public InputStream getInputStream() throws IOException {
	    return getAccessor().getInputStream();
	}

	public final OutputStream getOutputStream(boolean append) throws IOException {
	    switch(flags) {
	      case READWRITE:
		return getAccessor().getOutputStream(append);
	      default:
		throw new AccessControlException("Method: getOutputStream, Flags: " + flags);
	    }
	}

	public final IRandomAccess getRandomAccess(String mode) throws IllegalArgumentException, IOException {
	    if ("rw".equals(mode) && flags != IFile.Flags.READWRITE) {
		throw new AccessControlException("Method: getRandomAccess, Mode: " + mode + ", Flags: " + flags);
	    }
	    return getAccessor().getRandomAccess(mode);
	}

	public String[] list() throws IOException {
	    if (isDirectory()) {
		String[] list = getAccessor().list();
		if (list == null) {
		    return new String[0];
		} else {
		    return list;
		}
	    } else {
		return null;
	    }
	}

	public IFile[] listFiles() throws IOException {
	    return listFiles(null);
	}

	public IFile[] listFiles(Pattern p) throws IOException {
	    String[] fnames = list();
	    if (fnames == null) {
		return null;
	    }
	    List<IFile> result = new ArrayList<IFile>();
	    for (String fname : fnames) {
		if (p == null || p.matcher(fname).find()) {
		    if (path.endsWith(DELIM)) {
			result.add(getFile(path + fname, flags));
		    } else {
			result.add(getFile(path + DELIM + fname, flags));
		    }
		}
	    }
	    return result.toArray(new IFile[result.size()]);
	}

	public IFile getChild(String name) throws IOException {
	    if (isDirectory()) {
		return getFile(path + DELIM + name, flags);
	    } else {
		throw new IOException("Not a directory: " + path);
	    }
	}

	public void delete() throws IOException {
	    switch(flags) {
	      case READWRITE:
		getAccessor().delete();
		break;
	      default:
		throw new AccessControlException("Method: delete, Flags: " + flags);
	    }
	}

	// Internal

	protected IAccessor getAccessor() throws IOException {
	    if (accessor == null) {
		// Info must not be null
		accessor = new DefaultAccessor(new File(info.getPath()));
	    }
	    return accessor;
	}

	protected final IFileMetadata getInfo() throws IOException {
	    if (info == null) {
		// Accessor must not be null
		info = getAccessor().getInfo();

		//
		// Now that we have info, cache it if this is a READONLY IFile
		//
		if (flags == IFile.Flags.READONLY) {
		    putCache(this);
		}
	    }
	    return info;
	}
    }

    /**
     * A FileAccessor implementation for Java (local) Files.
     */
    public class DefaultAccessor implements IAccessor {
	protected File file;

	protected DefaultAccessor(File file) {
	    this.file = file;
	}

	public String toString() {
	    return file.toString();
	}

	// Implement IAccessor

	public boolean exists() {
	    return file.exists();
	}

	public void delete() {
	    file.delete();
	}

	public DefaultMetadata getInfo() throws IOException {
	    if (exists()) {
		long ctime = getCtime();
		long mtime = getMtime();
		long atime = getAtime();
		long length = getLength();

		// Determine the file type...

		IFileMetadata.Type type = IFileMetadata.Type.FILE;
		String path = file.getPath();
		String canonicalPath = file.getCanonicalPath();
		String linkPath = null;

		File canon;
		if (file.getParent() == null) {
		    canon = file;
		} else {
		    File canonDir = file.getParentFile().getCanonicalFile();
		    canon = new File(canonDir, file.getName());
		}

		if (!canon.getCanonicalFile().equals(canon.getAbsoluteFile())) {
		    type = IFileMetadata.Type.LINK;
		    linkPath = canonicalPath;
		} else if (file.isDirectory()) {
		    type = IFileMetadata.Type.DIRECTORY;
		}

		return new DefaultMetadata(type, path, linkPath, canonicalPath, this);
	    } else {
		throw new FileNotFoundException(file.getPath());
	    }
	}

	public long getCtime() throws IOException {
	    return IFile.UNKNOWN_TIME;
	}

	public long getAtime() throws IOException {
	    return IFile.UNKNOWN_TIME;
	}

	public long getMtime() throws IOException {
	    return file.lastModified();
	}

	public long getLength() throws IOException {
	    return file.length();
	}

	public IRandomAccess getRandomAccess(String mode) throws IOException {
	    return new RandomAccessImpl(new RandomAccessFile(file, mode));
	}

	public InputStream getInputStream() throws IOException {
	    return new FileInputStream(file);
	}

	public OutputStream getOutputStream(boolean append) throws IOException {
	    return new FileOutputStream(file, append);
	}

	public String getCanonicalPath() throws IOException {
	    return file.getCanonicalPath();
	}

	public String[] list() throws IOException {
	    return file.list();
	}

	public boolean mkdir() {
	    return file.mkdir();
	}

	// Internal

	/**
	 * Default implementation of IRandomAccess
	 */
	class RandomAccessImpl implements IRandomAccess {
	    private RandomAccessFile raf;

	    public RandomAccessImpl(RandomAccessFile raf) {
		this.raf = raf;
	    }

	    // Implement IRandomAccess

	    public void readFully(byte[] buff) throws IOException {
		raf.readFully(buff);
	    }

	    public void close() throws IOException {
		raf.close();
	    }

	    public void seek(long pos) throws IOException {
		raf.seek(pos);
	    }

	    public int read() throws IOException {
		return raf.read();
	    }

	    public int read(byte[] buff) throws IOException {
		return raf.read(buff);
	    }

	    public int read(byte[] buff, int offset, int len) throws IOException {
		return raf.read(buff, offset, len);
	    }

	    public long length() throws IOException {
		return raf.length();
	    }

	    public long getFilePointer() throws IOException {
		return raf.getFilePointer();
	    }
	}
    }

    // Private

    private Map<String, IFile> cache;
    private Set<String> index;

    /**
     * Attempt to retrieve an IFile from the cache.
     *
     * TBD: expire objects that get too old
     */
    private IFile getCache(String path) throws NoSuchElementException {
        if (cache.containsKey(path)) {
	    logger.trace(JOVALMsg.STATUS_FS_CACHE_RETRIEVE, path);
            return cache.get(path);
	}
	throw new NoSuchElementException(path);
    }

    /**
     * Put an IFile in the cache.
     *
     */
    private void putCache(IFile file) {
	String path = file.getPath();

	//
	// Check the index rather than the cache, to avoid a potential infinite loop in JDBM deserialization.
	// TBD: see if the data is newer than what's already in the cache?
	//
	if (!index.contains(path)) {
	    logger.trace(JOVALMsg.STATUS_FS_CACHE_STORE, path);
	    cache.put(path, file);
	    index.add(path);
	}
    }
}
