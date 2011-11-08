// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.oval;

import java.io.File;
import java.util.Collection;

import org.joval.intf.oval.IDefinitionFilter;
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
    Version SCHEMA_VERSION = new Version("5.10");

    int MESSAGE_MIN				= 0;
    int MESSAGE_OBJECT_PHASE_START		= 0;
    int MESSAGE_OBJECT				= 1;
    int MESSAGE_OBJECT_PHASE_END		= 2;
    int MESSAGE_SYSTEMCHARACTERISTICS		= 3;
    int MESSAGE_DEFINITION_PHASE_START		= 5;
    int MESSAGE_DEFINITION			= 6;
    int MESSAGE_DEFINITION_PHASE_END		= 7;
    int MESSAGE_MAX				= 8;

    public enum Result {
	OK,
	ERR;
    }

    /**
     * Set the file from which to read external variable definitions.
     */
    public void setExternalVariablesFile(File f) throws IllegalThreadStateException, OvalException;

    /**
     * Set the file from which to read the OVAL definitions.
     */
    public void setDefinitionsFile(File f) throws IllegalThreadStateException, OvalException;

    /**
     * Set the IDefinitions that the engine will process.
     */
    public void setDefinitions(IDefinitions definitions) throws IllegalThreadStateException;

    /**
     * Set a list of definition IDs to evaluate during the run phase.
     */
    public void setDefinitionFilterFile(File f) throws IllegalThreadStateException, OvalException;

    /**
     * Set a list of definition IDs to evaluate during the run phase.
     */
    public void setDefinitionFilter(IDefinitionFilter filter) throws IllegalThreadStateException;

    public void setSystemCharacteristicsFile(File f) throws IllegalThreadStateException, OvalException;

    public void setPlugin(IPlugin plugin) throws IllegalThreadStateException;

    /**
     * Get an IProducer associated with the IEngine.
     */
    public IProducer getNotificationProducer();

    /**
     * Returns Result.OK or Result.ERR, or throws an exception if the engine hasn't run, or is running.
     */
    public Result getResult() throws IllegalThreadStateException;

    /**
     * Return the scan IResults (valid if getResult returned COMPLETE_OK).  Only valid after the run() method has finished.
     */
    public IResults getResults() throws IllegalThreadStateException;

    /**
     * Return the error (valid if getResult returned COMPLETE_ERR).  Only valid after the run() method has finished.
     */
    public OvalException getError() throws IllegalThreadStateException;
}
