// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.scap.oval;

import java.util.Collection;
import java.util.NoSuchElementException;

import scap.oval.results.ResultEnumeration;

import org.joval.intf.util.IProducer;
import org.joval.scap.oval.OvalException;
import org.joval.util.Version;

/**
 * Engine that evaluates OVAL tests on remote hosts.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IOvalEngine extends Runnable {
    /**
     * The version of the OVAL schema supported by the engine.
     */
    Version SCHEMA_VERSION = new Version("5.10.1");

    enum Message {
	/**
	 * Message indicating that the engine has begun probing for object items.
	 */
	OBJECT_PHASE_START,

	/**
	 * Message indicating that the engine has started collecting items for an object.  The argument is the String value
	 * of the object ID.
	 */
	OBJECT,

	/**
	 * Message indicating that the engine has started collecting items for a group of objects in batch.  The argument
	 * is a String[] containing the object IDs being collected.
	 */
	OBJECTS,

	/**
	 * Message indicating that the engine has finished probing for object items.
	 */
	OBJECT_PHASE_END,

	/**
	 * Message indicating that the engine has collected a complete set of system characteristics.  The argument is the
	 * ISystemCharacteristics containing the data.
	 *
	 * @see org.joval.intf.scap.oval.ISystemCharacteristics
	 */
	SYSTEMCHARACTERISTICS,

	/**
	 * Message indicating that the engine has started evaluating the logical values of the OVAL definitions.
	 */
	DEFINITION_PHASE_START,

	/**
	 * Message indicating that the engine is evaluating an OVAL definition.  The argument is the String value of the
	 * definition ID.
	 */
	DEFINITION,

	/**
	 * Message indicating that the engine has finished evaluating the logical values of the OVAL definitions.
	 */
	DEFINITION_PHASE_END;
    }

    enum Mode {
	/**
	 * Signifies an exhaustive mode, wherein all the objects defined in the IDefinitions are probed, regardless of
	 * whether or not they are required by the definitions that will be evaluated.
	 */
	EXHAUSTIVE,

	/**
	 * Signifies a mode in which the engine will probe all the objects that are referenced by the IDefinitions that
	 * are to be evaluated.
	 */
	DIRECTED;
    }

    enum Result {
	/**
	 * Specifies a nominal result
	 */
	OK,

	/**
	 * Specifies that an exception has been raised
	 */
	ERR;
    }

    /**
     * Stop the engine's processing and close all open resources.  This will leave the engine in an error state.
     */
    void destroy();

    /**
     * Set the source from which to read external variable definitions.
     *
     * @throws OvalException if there was an exception parsing the file.
     */
    void setExternalVariables(IVariables variables) throws IllegalThreadStateException;

    /**
     * Set the IDefinitions that the engine will process.
     *
     * @throws IllegalThreadStateException if the engine has already started.
     */
    void setDefinitions(IDefinitions definitions) throws IllegalThreadStateException;

    /**
     * Set a list of definition IDs to evaluate during the run phase.
     *
     * @throws IllegalThreadStateException if the engine has already started.
     */
    void setDefinitionFilter(IDefinitionFilter filter) throws IllegalThreadStateException;

    /**
     * Set some previously-collected SystemCharacteristics data that will be used as input to the definition evaluation phase.
     * Specifying a pre-existing System Characteristics file will cause the engine to skip any data collection.
     *
     * @throws IllegalThreadStateException if the engine has already started.
     * @throws OvalException if there was an exception parsing the file.
     */
    void setSystemCharacteristics(ISystemCharacteristics sc) throws IllegalThreadStateException, OvalException;

    /**
     * Get an IProducer associated with the IOvalEngine.  This IProducer can be observed for notifications while the
     * engine is running.
     */
    IProducer<Message> getNotificationProducer();

    /**
     * Returns Result.OK or Result.ERR
     *
     * @throws IllegalThreadStateException if the engine hasn't run, or is running.
     */
    Result getResult() throws IllegalThreadStateException;

    /**
     * Return the scan IResults (valid if getResult returned Result.OK).  Only valid after the run() method has finished.
     *
     * @throws IllegalThreadStateException if the engine hasn't run, or is running.
     */
    IResults getResults() throws IllegalThreadStateException;

    /**
     * Return the error (valid if getResult returned Result.ERR).  Only valid after the run() method has finished.
     *
     * @throws IllegalThreadStateException if the engine hasn't run, or is running.
     */
    Exception getError() throws IllegalThreadStateException;

    /**
     * Instead of running the engine to evaluate definitions in bulk, it is possible to evaluate individual definitions
     * singly using this method.  The engine state will always be set to Result.COMPLETE_OK after calling this method.
     * Calling getResults will return the accumulated OVAL results to that point in time.
     *
     * @throws IllegalStateException if definitions have not been set
     * @throws NoSuchElementException if the ID is not found in the OVAL definitions
     */
    ResultEnumeration evaluateDefinition(String id) throws IllegalStateException, NoSuchElementException, OvalException;
}
