// Copyright (C) 2014 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.independent.FamilyItem;

import org.joval.xml.SchemaRegistry;

/**
 * Tools for working with OVAL ItemType objects.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class ItemTools {
    private static Map<Class, Object> wrapperFactories = new HashMap<Class, Object>();
    private static Map<Class, Method> wrapperMethods = new HashMap<Class, Method>();

    /**
     * Wrap an ItemType in the appropriate JAXBElement.
     */
    public static <T extends ItemType> JAXBElement<T> wrapItem(T item) throws Exception {
	Class clazz = item.getClass();
	Method method = wrapperMethods.get(clazz);
	Object factory = wrapperFactories.get(clazz);
	if (method == null || factory == null) {
	    String packageName = clazz.getPackage().getName();
	    String unqualClassName = clazz.getName().substring(packageName.length()+1);
	    Class<?> factoryClass = Class.forName(packageName + ".ObjectFactory");
	    factory = factoryClass.newInstance();
	    wrapperFactories.put(clazz, factory);
	    method = factoryClass.getMethod("create" + unqualClassName, item.getClass());
	    wrapperMethods.put(clazz, method);
	}
	@SuppressWarnings("unchecked")
	JAXBElement<T> wrapped = (JAXBElement<T>)method.invoke(factory, item);
	return wrapped;
    }

    /**
     * Returns a Collection implementation for holding ItemType objects. This collection is safe to return
     * to the IOvalEngine from a call to IProvider.getItems. The collection can grow to threshold size in
     * memory, then it will serialize itself to a temp file in wsdir to protect the JVM from running out
     * of memory -- and can then grow without limit on disk, with no impact on memory utilization.
     */
    public static <T extends ItemType> Collection<T> safeCollection(int threshold, File wsdir) {
	return new ItemCollection<T>(threshold, wsdir);
    }

    // Internal

    static class MemoryCollection<T extends ItemType> extends ArrayList<T> {
	private ItemCollection<T> collection;
	private int threshold;

	MemoryCollection(int threshold, ItemCollection<T> collection) {
	    super();
	    this.collection = collection;
	    this.threshold = threshold;
	}

	@Override
	public boolean add(T e) {
	    if (threshold <= 0 || size() < threshold) {
		return super.add(e);
	    } else {
		collection.switchover();
		return collection.add(e);
	    }
	}
    }

    static class ItemCollection<T extends ItemType> implements Collection<T> {
	private int size = 0;
	private HashSet<Integer> removed;
	private Marshaller marshaller;
	private File wsdir, temp;
	private DataOutputStream out;
	private ItemIterator<T> iter;
	private boolean useMemory = true;
	private MemoryCollection<T> membuff;

	ItemCollection(int threshold, File wsdir) {
	    membuff = new MemoryCollection<T>(threshold, this);
	    this.wsdir = wsdir;
	}

	private void switchover() {
	    if (useMemory) {
		try {
		    temp = File.createTempFile("items_", ".tmp", wsdir);
		    temp.deleteOnExit();
		    removed = new HashSet<Integer>();
		    marshaller = SchemaRegistry.OVAL_SYSTEMCHARACTERISTICS.createMarshaller();
		    useMemory = false;
		    addAll(membuff);
		    membuff.clear();
		    membuff = null;
		} catch (Exception e) {
		    throw new RuntimeException(e);
		}
	    }
	}

	private byte[] toBytes(JAXBElement<T> item) throws JAXBException {
	    ByteArrayOutputStream out = new ByteArrayOutputStream();
	    marshaller.marshal(item, out);
	    return out.toByteArray();
	}

	private void close() {
	    if (out != null) {
		try {
		    out.close();
		} catch (IOException e) {
		}
	    }
	}

	@Override
	protected void finalize() {
	    close();
	    if (iter != null) {
		iter.close();
	    }
	    temp.delete();
	}

	// Implement Collection<T>

	public boolean add(T t) {
	    if (useMemory) {
		return membuff.add(t);
	    } else if (iter == null) {
		try {
		    if (out == null) {
			out = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(temp)));
		    }
		    byte[] buff = toBytes(wrapItem(t));
		    out.writeInt(buff.length);
		    out.write(buff, 0, buff.length);
		    size++;
		    return true;
		} catch (Exception e) {
		    throw new RuntimeException(e);
		}
	    } else {
		throw new ConcurrentModificationException();
	    }
	}

	public boolean addAll(Collection<? extends T> c) {
	    if (useMemory) {
		return membuff.addAll(c);
	    }
	    for (T item : c) {
		add(item);
	    }
	    return true;
	}

	public void clear() {
	    if (useMemory) {
		membuff.clear();
	    } else {
		throw new UnsupportedOperationException();
	    }
	}

	public boolean contains(Object o) {
	    if (useMemory) {
		return membuff.contains(o);
	    }
	    throw new UnsupportedOperationException();
	}

	public boolean containsAll(Collection<?> c) {
	    if (useMemory) {
		return membuff.containsAll(c);
	    }
	    throw new UnsupportedOperationException();
	}

	public boolean isEmpty() {
	    if (useMemory) {
		return membuff.isEmpty();
	    }
	    return size() == 0;
	}

	public Iterator<T> iterator() {
	    if (useMemory) {
		return membuff.iterator();
	    }
	    try {
		close();
		return iter = new ItemIterator<T>(this);
	    } catch (Exception e) {
		throw new RuntimeException(e);
	    }
	}

	public boolean remove(Object obj) {
	    if (useMemory) {
		return membuff.remove(obj);
	    }
	    throw new UnsupportedOperationException();
	}

	public boolean removeAll(Collection<?> c) {
	    if (useMemory) {
		return membuff.removeAll(c);
	    }
	    throw new UnsupportedOperationException();
	}

	public boolean retainAll(Collection<?> c) {
	    if (useMemory) {
		return membuff.retainAll(c);
	    }
	    throw new UnsupportedOperationException();
	}

	public int size() {
	    if (useMemory) {
		return membuff.size();
	    }
	    return size - removed.size();
	}

	public Object[] toArray() {
	    if (useMemory) {
		return membuff.toArray();
	    }
	    return toArray(new Object[size]);
	}

	public <E> E[] toArray(E[] a) {
	    if (useMemory) {
		return membuff.toArray(a);
	    }
	    if (size != a.length) {
		a = new ArrayList<E>(size()).toArray(a);
	    }
	    Iterator<T> iter = iterator();
	    for (int i=0; iter.hasNext(); i++) {
		@SuppressWarnings("unchecked")
		E elt = (E)iter.next();
		a[i] = elt;
	    }
	    return a;
	}
    }

    static class ItemIterator<T extends ItemType> implements Iterator<T> {
	private Unmarshaller unmarshaller;
	private ItemCollection<T> collection;
	private DataInputStream in;
	private T next;
	private int index = -1; // the index of the item returned by the last call to increment()
	private int lastIndex = -1; // the index of the item returned by the last call to next()

	ItemIterator(ItemCollection<T> collection) throws IOException, JAXBException {
	    unmarshaller = SchemaRegistry.OVAL_SYSTEMCHARACTERISTICS.getJAXBContext().createUnmarshaller();
	    this.collection = collection;
	    in = new DataInputStream(new GZIPInputStream(new FileInputStream(collection.temp)));
	    next = increment();
	}

	private T increment() {
	    if (collection.size > (1 + index)) {
		try {
		    while(collection.removed.contains(++index)) {
			int len = in.readInt();
			in.skip(len);
		    }
		    byte[] buff = new byte[in.readInt()];
		    in.readFully(buff);
		    JAXBElement elt = (JAXBElement)unmarshaller.unmarshal(new StreamSource(new ByteArrayInputStream(buff)));
		    @SuppressWarnings("unchecked")
		    T result = (T)elt.getValue();
		    return result;
		} catch (EOFException e) {
		    close();
		    return null;
		} catch (Exception e) {
		    try {
			in.close();
		    } catch (IOException e2) {
		    }
		    throw new RuntimeException(e);
		}
	    } else {
		close();
		return null;
	    }
	}

	private void close() {
	    if (in != null) {
		try {
		    in.close();
		} catch (IOException e) {
		}
		in = null;
	    }
	    collection.iter = null;
	}

	// Implement Iterator

	public boolean hasNext() {
	    return next != null;
	}

	public T next() {
	    if (next == null) {
		throw new NoSuchElementException();
	    } else {
		T result = next;
		lastIndex = index;
		next = increment();
		return result;
	    }
	}

	public void remove() {
	    if (lastIndex < 0) {
		throw new IllegalStateException("index " + lastIndex);
	    } else {
		collection.removed.add(lastIndex);
	    }
	}
    }
}
