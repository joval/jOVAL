// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.adapter.unix;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.EntityObjectStringType;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.unix.ShadowObject;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemIntType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.unix.EntityItemEncryptMethodType;
import oval.schemas.systemcharacteristics.unix.ShadowItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.os.unix.macos.DsclTool;
import org.joval.oval.CollectException;
import org.joval.oval.Factories;
import org.joval.oval.OvalException;
import org.joval.util.JOVALMsg;
import org.joval.util.SafeCLI;
import org.joval.util.StringTools;

/**
 * Collects items for unix:shadow_objects.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class ShadowAdapter implements IAdapter {
    private IUnixSession session;
    private Hashtable<String, ShadowItem> shadowMap;
    private String error;
    private boolean initialized;

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IUnixSession) {
	    this.session = (IUnixSession)session;
	    shadowMap = new Hashtable<String, ShadowItem>();
	    error = null;
	    initialized = false;
	    classes.add(ShadowObject.class);
	}
	return classes;
    }

    public Collection<ShadowItem> getItems(ObjectType obj, IRequestContext rc) throws OvalException, CollectException {
	if (session.getFlavor() == IUnixSession.Flavor.MACOSX) {
	    //
	    // shadow_objects should be considered not applicable on MacOS X.
	    //
	    throw new CollectException("", FlagEnumeration.NOT_APPLICABLE);
	} else if (!initialized) {
	    loadShadow();
	}

	if (error != null) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(error);
	    rc.addMessage(msg);
	}

	Collection<ShadowItem> items = new Vector<ShadowItem>();
	ShadowObject sObj = (ShadowObject)obj;
	EntityObjectStringType usernameType = sObj.getUsername();
	try {
	    String username = (String)usernameType.getValue();
	    OperationEnumeration op = usernameType.getOperation();
	    switch(op) {
	      case EQUALS:
		if (shadowMap.containsKey(username)) {
		    items.add(shadowMap.get(username));
		}
		break;

	      case NOT_EQUAL:
		for (String s : shadowMap.keySet()) {
		    if (!s.equals(username)) {
			items.add(shadowMap.get(s));
		    }
		}
		break;

	      case PATTERN_MATCH: {
		Pattern p = Pattern.compile(username);
		for (String s : shadowMap.keySet()) {
		    if (p.matcher(s).find()) {
			items.add(shadowMap.get(s));
		    }
		}
		break;
	      }

	      default:
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	    }
	} catch (PatternSyntaxException e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_PATTERN, e.getMessage()));
	    rc.addMessage(msg);
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return items;
    }

    // Internal

    private static final int USERNAME	= 0;
    private static final int PASSWORD	= 1;
    private static final int CHG_LST	= 2;
    private static final int CHG_ALLOW	= 3;
    private static final int CHG_REQ	= 4;
    private static final int EXP_WARN	= 5;
    private static final int EXP_INACT	= 6;
    private static final int EXP_DATE	= 7;
    private static final int FLAG	= 8;

    private void loadShadow() {
	try {
	    List<String> lines = null;
	    switch(session.getFlavor()) {
	      //
	      // On Linux, use getent to interact with NIS, LDAP or just the /etc/shadow file, as appropriate.
	      //
	      case LINUX:
		lines = SafeCLI.multiLine("getent shadow", session, IUnixSession.Timeout.S);
		break;

	      //
	      // By default, cat the contents of /etc/shadow, to leverage elevated privileges if necessary.
	      //
	      default:
		lines = SafeCLI.multiLine("cat /etc/shadow", session, IUnixSession.Timeout.S);
		break;
	    }

	    //
	    // Create the basic PasswordItems
	    //
	    for (String line : lines) {
		if (line.startsWith("#")) {
		    continue;
		}
		List<String> tokens = StringTools.toList(StringTools.tokenize(line, ":", false));
		if (tokens.size() == 9) {
		    ShadowItem item = Factories.sc.unix.createShadowItem();
		    EntityItemStringType username = Factories.sc.core.createEntityItemStringType();
		    String usernameString = tokens.get(USERNAME);
		    username.setValue(usernameString);
		    item.setUsername(username);

		    String pw = tokens.get(PASSWORD);
		    if (pw.length() > 0) {
			EntityItemStringType password = Factories.sc.core.createEntityItemStringType();
			password.setValue(pw);
			item.setPassword(password);

			EntityItemEncryptMethodType encryptMethod = Factories.sc.unix.createEntityItemEncryptMethodType();
			if (pw.startsWith("$")) {
			    List<String> elements = StringTools.toList(StringTools.tokenize(pw, "$", true));
			    if (elements.size() == 3) {
				String s = elements.get(0);
				if ("1".equals(s)) {
				    encryptMethod.setValue("MD5");
				} else if ("2".equals(s) || "2a".equals(s)) {
				    encryptMethod.setValue("Blowfish");
				} else if ("md5".equals(s)) {
				    encryptMethod.setValue("Sun MD5");
				} else if ("5".equals(s)) {
				    encryptMethod.setValue("SHA-256");
				} else if ("6".equals(s)) {
				    encryptMethod.setValue("SHA-512");
				} else {
				    encryptMethod.setStatus(StatusEnumeration.ERROR);
				}
			    }
			} else if (pw.startsWith("_")) {
			    encryptMethod.setValue("BSDi");
			} else if ("NP".equals(pw) || "!".equals(pw) || "LK".equals(pw) || "*".equals(pw) || "!!".equals(pw)) {
			    encryptMethod.setStatus(StatusEnumeration.DOES_NOT_EXIST);
			} else {
			    encryptMethod.setValue("DES");
			}
			item.setEncryptMethod(encryptMethod);
		    }

		    try {
			EntityItemIntType chgAllow = Factories.sc.core.createEntityItemIntType();
			chgAllow.setValue(Integer.toString(Integer.parseInt(tokens.get(CHG_ALLOW))));
			chgAllow.setDatatype(SimpleDatatypeEnumeration.INT.value());
			item.setChgAllow(chgAllow);
		    } catch (NumberFormatException e) {
		    }

		    try {
			EntityItemIntType chgLst = Factories.sc.core.createEntityItemIntType();
			chgLst.setValue(Integer.toString(Integer.parseInt(tokens.get(CHG_LST))));
			chgLst.setDatatype(SimpleDatatypeEnumeration.INT.value());
			item.setChgLst(chgLst);
		    } catch (NumberFormatException e) {
		    }

		    try {
			EntityItemIntType chgReq = Factories.sc.core.createEntityItemIntType();
			chgReq.setValue(Integer.toString(Integer.parseInt(tokens.get(CHG_REQ))));
			chgReq.setDatatype(SimpleDatatypeEnumeration.INT.value());
			item.setChgReq(chgReq);
		    } catch (NumberFormatException e) {
		    }

		    try {
			EntityItemIntType expDate = Factories.sc.core.createEntityItemIntType();
			expDate.setValue(Integer.toString(Integer.parseInt(tokens.get(EXP_DATE))));
			expDate.setDatatype(SimpleDatatypeEnumeration.INT.value());
			item.setExpDate(expDate);
		    } catch (NumberFormatException e) {
		    }

		    try {
			EntityItemIntType expInact = Factories.sc.core.createEntityItemIntType();
			expInact.setValue(Integer.toString(Integer.parseInt(tokens.get(EXP_INACT))));
			expInact.setDatatype(SimpleDatatypeEnumeration.INT.value());
			item.setExpInact(expInact);
		    } catch (NumberFormatException e) {
		    }

		    try {
			EntityItemIntType expWarn = Factories.sc.core.createEntityItemIntType();
			expWarn.setValue(Integer.toString(Integer.parseInt(tokens.get(EXP_WARN))));
			expWarn.setDatatype(SimpleDatatypeEnumeration.INT.value());
			item.setExpWarn(expWarn);
		    } catch (NumberFormatException e) {
		    }

		    try {
			EntityItemStringType flag = Factories.sc.core.createEntityItemStringType();
			flag.setValue(tokens.get(FLAG));
			item.setFlag(flag);
		    } catch (NumberFormatException e) {
		    }

		    shadowMap.put(usernameString, item);
		} else {
		    session.getLogger().warn(JOVALMsg.ERROR_SHADOW_LINE, line);
		}
	    }
	} catch (Exception e) {
	    error = e.getMessage();
	    session.getLogger().error(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	initialized = true;
    }
}
