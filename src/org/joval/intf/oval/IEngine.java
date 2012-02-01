// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.oval;

import java.io.File;
import java.util.Collection;

import org.joval.intf.plugin.IPlugin;
import org.joval.intf.util.IProducer;
import org.joval.oval.OvalException;
import org.joval.util.Version;

/**
 * Engine that evaluates OVAL tests on remote hosts.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IEngine extends Runnable {
    /**
     * The version of the OVAL schema supported by the engine.
     */
    Version SCHEMA_VERSION = new Version("5.10.1");

    /**
     * The minimum message value that will be produced by the engine's IProducer.
     */
    int MESSAGE_MIN				= 100;

    /**
     * The maximum message value that will be produced by the engine's IProducer.
     */
    int MESSAGE_MAX				= 199;

    /**
     * Message ID indicating that the engine has begun probing for object items.
     */
    int MESSAGE_OBJECT_PHASE_START		= 110;

    /**
     * Message ID indicating that the engine has started collecting items for an object.  The argument is the String value
     * of the object ID.
     */
    int MESSAGE_OBJECT				= 120;

    /**
     * Message ID indicating that the engine has finished probing for object items.
     */
    int MESSAGE_OBJECT_PHASE_END		= 130;

    /**
     * Message ID indicating that the engine has collected a complete set of system characteristics.  The argument is the
     * ISystemCharacteristics containing the data.
     *
     * @see org.joval.intf.oval.ISystemCharacteristics
     */
    int MESSAGE_SYSTEMCHARACTERISTICS		= 140;

    /**
     * Message ID indicating that the engine has started evaluating the logical values of the OVAL definitions.
     */
    int MESSAGE_DEFINITION_PHASE_START		= 150;

    /**
     * Message ID indicating that the engine is evaluating an OVAL definition.  The argument is the String value of the
     * definition ID.
     */
    int MESSAGE_DEFINITION			= 160;

    /**
     * Message ID indicating that the engine has finished evaluating the logical values of the OVAL definitions.
     */
    int MESSAGE_DEFINITION_PHASE_END		= 170;

    public enum Result {
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
    public void destroy();

    /**
     * Set the file from which to read external variable definitions.
     *
     * @throws IllegalThreadStateException if the engine has already started.
     * @throws OvalException if there was an exception parsing the file.
     */
    public void setExternalVariablesFile(File f) throws IllegalThreadStateException, OvalException;

    /**
     * Set the file from which to read the OVAL definitions.
     *
     * @throws IllegalThreadStateException if the engine has already started.
     * @throws OvalException if there was an exception parsing the file.
     */
    public void setDefinitionsFile(File f) throws IllegalThreadStateException, OvalException;

    /**
     * Set the IDefinitions that the engine will process.
     *
     * @throws IllegalThreadStateException if the engine has already started.
     */
    public void setDefinitions(IDefinitions definitions) throws IllegalThreadStateException;

    /**
     * Set a list of definition IDs to evaluate during the run phase.
     *
     * @throws IllegalThreadStateException if the engine has already started.
     * @throws OvalException if there was an exception parsing the file.
     */
    public void setDefinitionFilterFile(File f) throws IllegalThreadStateException, OvalException;

    /**
     * Set a list of definition IDs to evaluate during the run phase.
     *
     * @throws IllegalThreadStateException if the engine has already started.
     */
    public void setDefinitionFilter(IDefinitionFilter filter) throws IllegalThreadStateException;

    /**
     * Set the file containing SystemCharacteristics data that will be used as input to the definition evaluation phase.
     * Specifying a pre-existing System Characteristics file will cause the engine to skip the object data collection phase.
     *
     * @throws IllegalThreadStateException if the engine has already started.
     * @throws OvalException if there was an exception parsing the file.
     */
    public void setSystemCharacteristicsFile(File f) throws IllegalThreadStateException, OvalException;

    /**
     * Get an IProducer associated with the IEngine.  This IProducer can be observed for MESSAGE_ notifications while the
     * engine is running.
     */
    public IProducer getNotificationProducer();

    /**
     * Returns Result.OK or Result.ERR
     *
     * @throws IllegalThreadStateException if the engine hasn't run, or is running.
     */
    public Result getResult() throws IllegalThreadStateException;

    /**
     * Return the scan IResults (valid if getResult returned Result.OK).  Only valid after the run() method has finished.
     *
     * @throws IllegalThreadStateException if the engine hasn't run, or is running.
     */
    public IResults getResults() throws IllegalThreadStateException;

    /**
     * Return the error (valid if getResult returned Result.ERR).  Only valid after the run() method has finished.
     *
     * @throws IllegalThreadStateException if the engine hasn't run, or is running.
     */
    public Exception getError() throws IllegalThreadStateException;
}
