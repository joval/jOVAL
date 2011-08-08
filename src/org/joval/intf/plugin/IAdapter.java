// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.plugin;

import java.util.List;
import javax.xml.bind.JAXBElement;

import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.core.ObjectComponentType;
import oval.schemas.definitions.core.StateType;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.VariableValueType;
import oval.schemas.results.core.ResultEnumeration;
import oval.schemas.results.core.TestType;

import org.joval.oval.TestException;
import org.joval.oval.OvalException;

/**
 * The interface for implementing a jOVAL plug-in adapter.  An adapter operates on a set of classes: an ObjectType subclass,
 * a StateType subclass and an ItemType subclass.  The jOVAL engine uses adapters to retrieve item data about objects from
 * hosts, and to compare items to states.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IAdapter {
    /**
     * Initialize the plug-in.
     */
    public void init(IAdapterContext ctx);

    /**
     * Identify the class of a subclass of ObjectType for which this adapter knows how to retrieve item data.
     */
    public Class getObjectClass();

    /**
     * Identify the class of a subclass of definitions.StateType that this adapter knows how to compare to an item.
     */
    public Class getStateClass();

    /**
     * Identify the class of a subclass of systemcharacteristics.ItemType that this adapter creates and knows how to
     * compare to a state.
     */
    public Class getItemClass();

    /**
     * The adapter should open any special resources it's going to need in order to scan objects on the machine.  The engine
     * will call this method after init, but before any call to getItems.
     */
    public boolean connect();

    /**
     * The adapter should release any resource that it's opened for scanning purposes.  The engine will call this method
     * after all calls to getItems.
     */
    public void disconnect();

    /**
     * Retrieve items associated with the given object by scanning the machine.  Implementations should add variables
     * to the list as they are resolved.  (The ItemTypes must be wrapped in a JAXBElement so that they can be marshalled
     * into an OvalSystemCharacteristics.)
     */
    public List<JAXBElement<? extends ItemType>> getItems(ObjectType ot, List<VariableValueType> vars) throws OvalException;

    /**
     * Compare an item to a state.  The state and item are type-checked to insure that they match the types identified by
     * getStateClass and getItemClass before this method is invoked.  It should not be necessary for the adapter to interact
     * with a machine in order to compare an item to a state, as system data may be originating from a file.
     *
     * @throws OvalException if there is a fundamental issue with the OVAL structures, like a datatype mismatch.
     * @throws TestException if there is an error in evaluating the test, which should be attached to the TestedItemType.
     */
    public ResultEnumeration compare(StateType state, ItemType item) throws TestException, OvalException;
}
