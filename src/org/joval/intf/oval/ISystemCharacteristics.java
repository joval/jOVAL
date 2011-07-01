// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.oval;

import java.io.File;
import java.math.BigInteger;
import java.util.List;
import java.util.NoSuchElementException;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.MessageType;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ObjectType;
import oval.schemas.systemcharacteristics.core.OvalSystemCharacteristics;
import oval.schemas.systemcharacteristics.core.VariableValueType;

/**
 * The interface for implementing an OVAL store, for caching/storing ItemTypes.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface ISystemCharacteristics {
    /**
     * Put an object into the store.  If the object is already in the store, this method can be used to add another message
     * to it, or to change its flag or comment.
     */
    public void setObject(String objectId, String comment, BigInteger version, FlagEnumeration flag, MessageType message);

    /**
     * Store the ItemType in the itemTable and return the ID used to store it.  The plugin should retain this ID in case the
     * item is shared by multiple objects (and use the relateItem method to store those associations).
     *
     * @returns the itemId in which the ItemType was stored.
     */
    public BigInteger storeItem(JAXBElement<? extends ItemType> item);

    /**
     * Relate an object to an item.
     */
    public void relateItem(String objectId, BigInteger itemId) throws NoSuchElementException;

    /**
     * Return a list of ItemTypes that are related to the given OVAL object ID.
     */
    public List<ItemType> getItemsByObjectId(String id) throws NoSuchElementException;

    /**
     * Return an ObjectType given its ID.
     */
    public ObjectType getObject(String id);

    /**
     * Store a VariableValueType in the SystemCharacteristics.
     */
    public void storeVariable(VariableValueType variableValueType);

    /**
     * Relate an object to a variable.
     */
    public void relateVariable(String objectId, String variableId) throws NoSuchElementException;

    /**
     * Return a list of VariableValueTypes that are referenced by the given OVAL object ID.
     */
    public List<VariableValueType> getVariablesByObjectId(String id) throws NoSuchElementException;

    /**
     * Serialize the contents of this structure to an XML file.
     */
    public void write(File f, boolean noNamespaces);
}
