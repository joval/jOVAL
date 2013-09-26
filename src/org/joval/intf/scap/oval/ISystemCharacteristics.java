// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.scap.oval;

import java.io.File;
import java.math.BigInteger;
import java.util.Collection;
import java.util.NoSuchElementException;
import javax.xml.bind.JAXBElement;

import scap.oval.common.MessageType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.ObjectType;
import scap.oval.systemcharacteristics.core.OvalSystemCharacteristics;
import scap.oval.systemcharacteristics.core.SystemInfoType;
import scap.oval.systemcharacteristics.core.VariableValueType;

import org.joval.intf.xml.ITransformable;
import org.joval.scap.oval.OvalException;

/**
 * Interface defining OVAL System Characteristics.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface ISystemCharacteristics extends ITransformable {
    SystemInfoType getSystemInfo();

    /**
     * Return a raw OVAL system characteristics object containing the underlying data.
     *
     * @param mask Set to true to apply mask attributes from the EntityAttributeGroup to the result.
     */
    OvalSystemCharacteristics getOvalSystemCharacteristics(boolean mask);

    /**
     * Store the ItemType in the itemTable and return the ID used to store it.
     *
     * Note, the items you store in a given ISystemCharacteristics should either never have an ID (in which case one
     * will be assigned when it's stored), or they should always have an ID (in which case they should guarantee the
     * uniqueness of each item). Otherwise, ID collisions can result.
     */
    BigInteger storeItem(ItemType item) throws OvalException;

    /**
     * Store the specified VariableValueType.
     */
    void storeVariable(VariableValueType var);

    /**
     * Add some information about an object to the store, without relating it to a variable or an item.  The last-set flag
     * always sticks.
     */
    void setObject(String objectId, String comment, BigInteger version, FlagEnumeration flag, MessageType message);

    /**
     * Get a collected object record given its ID.
     */
    ObjectType getObject(String objectId) throws NoSuchElementException;

    /**
     * Fetch an existing ObjectType or create a new ObjectType and store it in the objectTable, and create a relation between
     * the ObjectType and ItemType (if such a relation does not already exist).
     */
    void relateItem(String objectId, BigInteger itemId) throws NoSuchElementException;

    /**
     * Add a variable reference to an ObjectType.  Both must already exist.
     */
    void relateVariable(String objectId, String variableId) throws NoSuchElementException;

    /**
     * Get the flag for an object, given the specified object ID.
     */
    FlagEnumeration getObjectFlag(String id) throws NoSuchElementException;

    /**
     * Test whether an ObjectType with the specified ID is present.
     */
    boolean containsObject(String objectId);

    /**
     * Return a copy of this ISystemCharacteristics which has pruned out all items that do not relate directly to the
     * specified object IDs.
     */
    ISystemCharacteristics prune(Collection<String> objectIds);

    /**
     * Get all the items associated with the specified object.
     */
    Collection<JAXBElement<? extends ItemType>> getItemsByObjectId(String id) throws NoSuchElementException;

    /**
     * Serialize the OVAL system characteristics to the specified file.
     */
    void writeXML(File f);
}
