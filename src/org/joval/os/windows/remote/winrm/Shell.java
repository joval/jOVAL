// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.winrm;

import java.io.IOException;
import java.math.BigInteger;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.dmtf.wsman.AttributableEmpty;
import org.dmtf.wsman.AttributablePositiveInteger;
import org.dmtf.wsman.OptionSet;
import org.dmtf.wsman.OptionType;
import org.dmtf.wsman.SelectorSetType;
import org.dmtf.wsman.SelectorType;
import org.xmlsoap.ws.addressing.EndpointReferenceType;
import org.xmlsoap.ws.addressing.ReferenceParametersType;
import org.xmlsoap.ws.enumeration.Enumerate;
import org.xmlsoap.ws.enumeration.EnumerateResponse;
import org.xmlsoap.ws.enumeration.Pull;
import org.xmlsoap.ws.enumeration.PullResponse;
import org.xmlsoap.ws.transfer.AnyXmlOptionalType;
import org.xmlsoap.ws.transfer.AnyXmlType;
import com.microsoft.wsman.shell.CommandLine;
import com.microsoft.wsman.shell.CommandResponse;
import com.microsoft.wsman.shell.EnvironmentVariable;
import com.microsoft.wsman.shell.EnvironmentVariableList;
import com.microsoft.wsman.shell.ShellType;

import org.joval.intf.system.IEnvironment;
import org.joval.intf.system.IProcess;
import org.joval.intf.windows.wsmv.IWSMVConstants;
import org.joval.intf.ws.IPort;
import org.joval.os.windows.remote.wsmv.operation.CommandOperation;
import org.joval.os.windows.remote.wsmv.operation.CreateOperation;
import org.joval.os.windows.remote.wsmv.operation.DeleteOperation;
import org.joval.os.windows.remote.wsmv.operation.EnumerateOperation;
import org.joval.os.windows.remote.wsmv.operation.PullOperation;
import org.joval.ws.WSFault;

/**
 * A WinRM client.  To use it, you must first do this on the target machine:
 *
 *   winrm set winrm/config/service @{AllowUnencrypted="true"}
 *   winrm set winrm/config/service/auth @{Basic="true"}
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Shell implements IWSMVConstants {
    public static final String STDOUT	= "stdout";
    public static final String STDERR	= "stderr";
    public static final String STDIN	= "stdin";

    private IPort port;
    private String id;

    /**
     * Create a new Shell.
     */
    public Shell(IPort port, IEnvironment env, String cwd) throws JAXBException, IOException, WSFault {
	this.port = port;

	//
	// Create the WS-Create input parameter
	//
	AnyXmlType arg = Factories.TRANSFER.createAnyXmlType();
	ShellType shell = Factories.SHELL.createShellType();
	if (env != null) {
	    EnvironmentVariableList envList = Factories.SHELL.createEnvironmentVariableList();
	    for (String varName : env) {
		EnvironmentVariable var = Factories.SHELL.createEnvironmentVariable();
		var.setName(varName);
		var.setValue(env.getenv(varName));
		envList.getVariable().add(var);
	    }
	    shell.setEnvironment(envList);
	}
	if (cwd != null) {
	    shell.setWorkingDirectory(cwd);
	}
//	shell.setLifetime(Factories.XMLDT.newDuration(1000000));
	shell.getOutputStreams().add(STDOUT);
	shell.getOutputStreams().add(STDERR);
	shell.getInputStreams().add(STDIN);
	arg.setAny(Factories.SHELL.createShell(shell));

	//
	// Create the CreateOperation and set options
	//
	CreateOperation createOperation = new CreateOperation(arg);
	createOperation.addResourceURI(SHELL_URI);
	createOperation.setTimeout(60000);

	//
	// If set to TRUE, this option specifies that the user profile does not exist on the remote system
	// and that the default profile SHOULD be used. By default, the value is TRUE.
	//
	OptionType winrsNoProfile = Factories.WSMAN.createOptionType();
	winrsNoProfile.setName("WINRS_NOPROFILE");
	winrsNoProfile.setValue("TRUE");

	//
	// The value of the options specifies the client's console output code page. The value is returned
	// by GetConsoleOutputCP API; on the server side, this value is set as input and output code page
	// to display the number of the active character set (code page) or to change the active character set.
	//
	// @see http://en.wikipedia.org/wiki/Code_page_437
	//
	OptionType winrsCodepage = Factories.WSMAN.createOptionType();
	winrsCodepage.setName("WINRS_CODEPAGE");
	winrsCodepage.setValue("437");

	OptionSet options = Factories.WSMAN.createOptionSet();
	options.getOption().add(winrsNoProfile);
	options.getOption().add(winrsCodepage);
	createOperation.addOptionSet(options);

	//
	// Dispatch the call to the target, and get the ID of the new shell.
	//
	Object response = createOperation.dispatch(port);
	if (response instanceof EndpointReferenceType) {
	    for (Object param : ((EndpointReferenceType)response).getReferenceParameters().getAny()) {
		if (param instanceof JAXBElement) {
		    param = ((JAXBElement)param).getValue();
		}
		if (param instanceof SelectorSetType) {
		    for (SelectorType sel : ((SelectorSetType)param).getSelector()) {
			if ("ShellId".equals(sel.getName())) {
			    id = (String)sel.getContent().get(0);
			    break;
			}
		   }
		}
		if (id != null) break;
	    }
	}
    }

    /**
     * Grab an existing shell on the target. Uses WS-Transfer Enumerate to validate the existence of the ID.
     */
    public Shell(IPort port, String id) throws JAXBException, IOException, WSFault {
	this.port = port;
	this.id = id;

/*
	Enumerate enumerate = Factories.ENUMERATION.createEnumerate();
	AttributableEmpty optimize = Factories.WSMAN.createAttributableEmpty();
	enumerate.getAny().add(Factories.WSMAN.createOptimizeEnumeration(optimize));
	AttributablePositiveInteger maxElements = Factories.WSMAN.createAttributablePositiveInteger();
	maxElements.setValue(new BigInteger("1"));
	enumerate.getAny().add(Factories.WSMAN.createMaxElements(maxElements));
	EnumerateOperation operation = new EnumerateOperation(new Enumerate());
	operation.addResourceURI(SHELL_URI);

	EnumerateResponse response = operation.dispatch(port);

	Pull pull = Factories.ENUMERATION.createPull();
	pull.setEnumerationContext(response.getEnumerationContext());
	pull.setMaxElements(new BigInteger("1"));
	PullOperation pullOperation = new PullOperation(pull);
	pullOperation.addResourceURI(SHELL_URI);

	boolean endOfSequence = false;
	while(!endOfSequence) {
	    PullResponse pullResponse = pullOperation.dispatch(port);
	    if (pullResponse.isSetItems()) {
	        int itemNum = 0;
	        for (Object obj : pullResponse.getItems().getAny()) {
	            System.out.println("Enumerate item " + itemNum++ + " is a " + obj.getClass().getName());
	        }
	    }
	    if (pullResponse.isSetEndOfSequence()) {
	        endOfSequence = true;
	    }
	}
*/
    }

    public IProcess createProcess(String command, String[] args) {
	return new ShellCommand(port, id, command, args);
    }

    /**
     * Get the ID of the shell.
     */
    public String getId() {
	return id;
    }

    /**
     * Delete the Shell on the target machine.
     */
    @Override
    public void finalize() {
	try {
	    DeleteOperation deleteOperation = new DeleteOperation();
	    deleteOperation.addResourceURI(SHELL_URI);
	    SelectorSetType set = Factories.WSMAN.createSelectorSetType();
	    SelectorType sel = Factories.WSMAN.createSelectorType();
	    sel.setName("ShellId");
	    sel.getContent().add(id);
	    set.getSelector().add(sel);
	    deleteOperation.addSelectorSet(set);
	    deleteOperation.dispatch(port);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
