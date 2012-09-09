// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.winrm;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.dmtf.wsman.AnyListType;
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
import org.xmlsoap.ws.enumeration.EnumerationContextType;
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
import org.joval.util.JOVALMsg;
import org.joval.ws.WSFault;

/**
 * An implementation of MS-WSMV Shell, using WS-Management.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Shell implements IWSMVConstants {
    public static final String STDOUT	= "stdout";
    public static final String STDERR	= "stderr";
    public static final String STDIN	= "stdin";

    private boolean disposed = false;

    IPort port;
    String id;

    /**
     * Return an Iterator of all the remote shells available at the specified port.
     */
    public static Iterable<Shell> enumerate(IPort port) throws JAXBException, IOException, WSFault {
	//
	// Build an optimized enumerate operation.
	//
	Enumerate enumerate = Factories.ENUMERATION.createEnumerate();
	AttributableEmpty optimize = Factories.WSMAN.createAttributableEmpty();
	enumerate.getAny().add(Factories.WSMAN.createOptimizeEnumeration(optimize));
	AttributablePositiveInteger maxElements = Factories.WSMAN.createAttributablePositiveInteger();
	maxElements.setValue(new BigInteger("4"));
	enumerate.getAny().add(Factories.WSMAN.createMaxElements(maxElements));
	EnumerateOperation operation = new EnumerateOperation(enumerate);
	operation.addResourceURI(SHELL_BASE_URI);

	//
	// Capture shells listed in the enumerate response.
	//
	ArrayList<Shell> shells = new ArrayList<Shell>();
	boolean endOfSequence = false;
	List<Object> items = null;
	EnumerateResponse response = operation.dispatch(port);
	EnumerationContextType enumContext = response.getEnumerationContext();
	if (response.isSetAny()) {
	    for (Object obj : response.getAny()) {
		if (obj instanceof JAXBElement) {
		    JAXBElement elt = (JAXBElement)obj;
		    if ("EndOfSequence".equals(elt.getName().getLocalPart())) {
			endOfSequence = true;
		    } else {
			obj = ((JAXBElement)obj).getValue();
			if (obj instanceof AnyListType) {
			    items = ((AnyListType)obj).getAny();
			} else {
			    System.out.println("Ignoring EnumerateResponse child: " + obj.getClass().getName());
			}
		    }
		} else {
		    System.out.println("Ignoring EnumerateResponse child: " + obj.getClass().getName());
		}
	    }
	}

	while(true) {
	    //
	    // Process items captured in the last operation.
	    //
	    if (items != null) {
		for (Object obj : items) {
		    if (obj instanceof JAXBElement) {
			obj = ((JAXBElement)obj).getValue();
		    }
		    if (obj instanceof ShellType) {
			shells.add(new Shell(port, ((ShellType)obj).getShellId()));
		    } else {
	        	System.out.println("Ignoring item: " + obj.getClass().getName());
		    }
	        }
	    }

	    //
	    // Pull down additional shell lists until the end of the enumeration has been reached.
	    //
	    if (endOfSequence) {
		break;
	    } else {
		Pull pull = Factories.ENUMERATION.createPull();
		pull.setEnumerationContext(enumContext);
		pull.setMaxElements(new BigInteger("4"));
		PullOperation pullOperation = new PullOperation(pull);
		pullOperation.addResourceURI(SHELL_BASE_URI);

		PullResponse pullResponse = pullOperation.dispatch(port);
		if (pullResponse.isSetEndOfSequence()) {
		    endOfSequence = true;
		} else if (pullResponse.isSetEnumerationContext()) {
		    enumContext = pullResponse.getEnumerationContext();
		}
		if (pullResponse.isSetItems()) {
		    items = pullResponse.getItems().getAny();
		} else {
		    items = null;
		}
	    }
	}
	return shells;
    }

    /**
     * Create a new Shell on the specified port.
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
	shell.setLifetime(Factories.XMLDT.newDuration(1800000)); // 30 min.
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
     * Create an IProcess using this shell.
     */
    public IProcess createProcess(String command) throws IllegalArgumentException {
	ArrayList<String> args = new ArrayList<String>();
	ArgumentTokenizer tok = new ArgumentTokenizer(command);
	String arg = null;
	while((arg = tok.nextArg()) != null) {
	    args.add(arg);
	}
	String[] argv = new String[args.size() - 1];
	for (int i=0; i < argv.length; i++) {
	    argv[i] = args.get(i+1);
	}
	return new ShellCommand(this, args.get(0), argv);
    }

    /**
     * Get the ID of the shell.
     */
    public String getId() {
	return id;
    }

    /**
     * Delete the Shell on the target machine (idempotent).
     */
    @Override
    protected void finalize() {
	if (!disposed) {
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
		disposed = true;
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	}
    }

    // Private

    /**
     * Grab an existing shell on the target. Uses WS-Transfer Enumerate to validate the existence of the ID.
     *
     * @throws NoSuchElementException if no shell with the given ID is found.
     */
    private Shell(IPort port, String id) {
	this.port = port;
	this.id = id;
    }

    /**
     * The argument tokenizer ...
     */
    class ArgumentTokenizer {
	String command;
	int ptr;

	/**
	 * Create a tokenizer for a command string.
	 */
	ArgumentTokenizer(String command) {
	    this.command = command;
	    ptr = 0;
	}

	/**
	 * Get the next argument from the command string.
	 */
	String nextArg() {
	    return nextArg(ptr);
	}

	// Private

	/**
	 * Determine the next argument after ptr, starting the search for a space at the specified index.
	 */
	private String nextArg(int index) {
	    if (ptr >= command.length()) {
		return null;
	    } else if (index == -1) {
		StringBuffer sb = new StringBuffer();
		int unclosed = unescapedIndexOf("\"", command, ptr);
		while(unclosed-- > 0) {
		    sb.append(" ");
		}
		sb.append("^");
		throw new IllegalArgumentException("Unclosed quote in command:\n    " + command + "\n" +
						   "    " + sb.toString());
	    }

	    int nextQuote = unescapedIndexOf("\"", command, index);
	    if (nextQuote == -1) {
		nextQuote = command.length();
	    }
	    String arg = null;
	    int nextSpace = unescapedIndexOf(" ", command, index);
	    if (nextSpace == -1) {
		arg = command.substring(ptr);
		ptr = command.length();
	    } else if (nextSpace < nextQuote) {
		arg = command.substring(ptr, nextSpace);
		ptr = nextSpace+1;
	    } else {
		int nextIndex = unescapedIndexOf("\"", command, nextQuote+1);
		if (nextIndex == -1) {
		    arg = nextArg(-1);
		} else {
		    arg = nextArg(nextIndex + 1);
		}
	    }

	    arg = arg.trim();
	    if (arg.length() == 0) {
		arg = nextArg();
	    }
	    return arg;
	}

	/**
	 * Get the index of s in target starting from fromIndex, which is not preceded by an unescaped escape character.
	 */
	private int unescapedIndexOf(String s, String target, int fromIndex) {
	    int candidate = target.indexOf(s, fromIndex);
	    if (candidate == -1) {
		return -1;
	    } else if (escapedChar(candidate, target)) {
		return unescapedIndexOf(s, target, candidate+1);
	    } else {
		return candidate;
	    }
	}

	/**
	 * Is the character at index of s escaped?
	 */
	private boolean escapedChar(int index, String s) throws IllegalArgumentException {
	    if (index < 0) {
		throw new IllegalArgumentException(Integer.toString(index));
	    } else if (index == 0) {
		return false;
	    } else {
		int escapes = 0;
		for (int i=index-1; i >= 0; i--) {
		    char c = s.charAt(i);
		    if (c == '\\') {
			escapes++;
		    } else {
			break;
		    }
		}
		return (escapes % 2) == 1;
	    }
	}
    }
}
