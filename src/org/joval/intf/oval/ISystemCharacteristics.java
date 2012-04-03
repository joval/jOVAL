// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.oval;

import java.io.File;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

import oval.schemas.common.MessageType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.ObjectType;
import oval.schemas.systemcharacteristics.core.OvalSystemCharacteristics;
import oval.schemas.systemcharacteristics.core.SystemInfoType;
import oval.schemas.systemcharacteristics.core.VariableValueType;

import org.joval.intf.xml.ITransformable;
import org.joval.oval.OvalException;

/**
 * Interface defining OVAL System Characteristics.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface ISystemCharacteristics extends ITransformable {
    /**
     * Return a raw OVAL system characteristics object containing the underlying data.
     */
    OvalSystemCharacteristics getOvalSystemCharacteristics();

    /**
     * Serialize the OVAL system characteristics to the specified file.
     */
    void writeXML(File f);

    /**
     * Test whether an ObjectType with the specified ID is present.
     */
    boolean containsObject(String objectId);

    /**
     * Return a filtered OvalSystemCharacteristics, containing only objects and items pertaining to the specified variables
     * and objects.
     */
    OvalSystemCharacteristics getOvalSystemCharacteristics(Collection<String> vars, Collection<BigInteger> itemIds);

    /**
     * Store the ItemType in the itemTable and return the ID used to store it.
     */
    BigInteger storeItem(ItemType item) throws OvalException;

    /**
     * Add some information about an object to the store, without relating it to a variable or an item.  The last-set flag
     * always sticks.
     */
    void setObject(String objectId, String comment, BigInteger version, FlagEnumeration flag, MessageType message);

    /**
     * Fetch an existing ObjectType or create a new ObjectType and store it in the objectTable, and create a relation between
     * the ObjectType and ItemType (if such a relation does not already exist).
     */
    void relateItem(String objectId, BigInteger itemId) throws NoSuchElementException;

    void storeVariable(VariableValueType var);

    /**
     * Add a variable reference to an ObjectType.  Both must already exist.
     */
    void relateVariable(String objectId, String variableId) throws NoSuchElementException;

    List<VariableValueType> getVariablesByObjectId(String id) throws NoSuchElementException;

    ObjectType getObject(String id) throws NoSuchElementException;

    List<ItemType> getItemsByObjectId(String id) throws NoSuchElementException;

    SystemInfoType getSystemInfo();
}
