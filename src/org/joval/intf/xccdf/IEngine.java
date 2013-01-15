// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.xccdf;

import java.util.Collection;
import java.util.NoSuchElementException;

import org.joval.intf.arf.IReport;
import org.joval.intf.ocil.IChecklist;
import org.joval.intf.ocil.IVariables;
import org.joval.intf.scap.IView;
import org.joval.intf.util.IProducer;
import org.joval.scap.xccdf.XccdfException;
import org.joval.util.Version;

/**
 * Engine that evaluates a host against an XCCDF benchmark.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IEngine extends Runnable {
    /**
     * The version of the SCAP schema supported by the engine.
     */
    Version SCHEMA_VERSION = new Version("1.2");

    /**
     * The minimum message value that will be produced by the engine's IProducer.
     */
    int MESSAGE_MIN				= 200;

    /**
     * The maximum message value that will be produced by the engine's IProducer.
     */
    int MESSAGE_MAX				= 299;

    /**
     * Message ID indicating that the engine has begun probing for platform applicability.
     */
    int MESSAGE_PLATFORM_PHASE_START		= 210;

    /**
     * Message ID indicating that the engine is probing for platform applicability. Argument is the String CPE ID of
     * the platform that is about to be tested.
     */
    int MESSAGE_PLATFORM_CPE			= 220;

    /**
     * Message ID indicating that the engine has finished probing for object items. Argument is Boolean.TRUE if the
     * target is applicable, or Boolean.FALSE if not.
     */
    int MESSAGE_PLATFORM_PHASE_END		= 230;

    /**
     * Message ID indicating that the engine has created an OVAL engine instance and is about to run it. The argument
     * is the OVAL IEngine instance.
     *
     * @see org.joval.intf.oval.IEngine
     */
    int MESSAGE_OVAL				= 240;

    /**
     * Message ID indicating that the engine is missing information about an OCIL checklist result. The argument is
     * an OcilMessageArgument.
     *
     * @see org.joval.intf.ocil.IChecklist
     */
    int MESSAGE_OCIL				= 250;

    /**
     * Message ID indicating that the engine is about to run an SCE script. The argument is the String href for the script.
     */
    int MESSAGE_SCE				= 260;

    /**
     * Specification for the argument accompanying a MESSAGE_OCIL notification message.
     */
    interface OcilMessageArgument {
	String getHref();
	IChecklist getChecklist();
	IVariables getVariables();
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
     * Set the SCAP view (i.e., the selected profile of a benchmark in a datastream collection) that will be processed
     * by the engine.
     *
     * @throws IllegalStateException if the engine has already started.
     */
    void setView(IView view) throws IllegalStateException, XccdfException;

    /**
     * Add an OCIL checklist (containing result information) to the engine as input.
     *
     * @throws IllegalStateException if the engine has already started.
     */
    void addChecklist(String href, IChecklist checklist) throws IllegalStateException;

    /**
     * Get an IProducer associated with the IEngine.  This IProducer can be observed for MESSAGE_ notifications while the
     * engine is running.
     */
    IProducer getNotificationProducer();

    /**
     * Returns Result.OK or Result.ERR
     *
     * @throws IllegalThreadStateException if the engine hasn't run, or is running.
     */
    Result getResult() throws IllegalThreadStateException;

    /**
     * Return the ARF report (valid if getResult returned Result.OK).  Only valid after the run() method has finished.
     *
     * @throws IllegalThreadStateException if the engine hasn't run, or is running.
     */
    IReport getReport() throws IllegalThreadStateException;

    /**
     * Return the error (valid if getResult returned Result.ERR).  Only valid after the run() method has finished.
     *
     * @throws IllegalThreadStateException if the engine hasn't run, or is running.
     */
    Exception getError() throws IllegalThreadStateException;
}
