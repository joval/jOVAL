// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util;

import java.util.HashMap;

import ch.qos.cal10n.BaseName;
import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.Locale;
import ch.qos.cal10n.LocaleData;
import ch.qos.cal10n.MessageConveyor;
import ch.qos.cal10n.MessageConveyorException;
import ch.qos.cal10n.MessageParameterObj;

import org.slf4j.cal10n.LocLogger;
import org.slf4j.cal10n.LocLoggerFactory;

/**
 * Uses cal10n to define localized messages for jOVAL.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
@BaseName("jovalmsg")
@LocaleData(
  defaultCharset="ASCII",
  value = { @Locale("en_US") }
)
public enum JOVALMsg {
    ERROR_AD_DOMAIN_UNKNOWN,
    ERROR_ADAPTER_MISSING,
    ERROR_AIX_EMGR_STATE,
    ERROR_ARF_BAD_SOURCE,
    ERROR_ARF_CATALOG,
    ERROR_BAD_COMPONENT,
    ERROR_BAD_FILE_OBJECT,
    ERROR_BAD_PROCESS58_OBJECT,
    ERROR_BAD_TIMEDIFFERENCE,
    ERROR_BINARY_LENGTH,
    ERROR_CHECKSUM_ALGORITHM,
    ERROR_CMDLET,
    ERROR_CMDLET_FIELD,
    ERROR_CMDLET_GUID,
    ERROR_CMDLET_MODULE,
    ERROR_CMDLET_VERB,
    ERROR_CMDLET_VERSION,
    ERROR_COMPONENT_FILTER,
    ERROR_CONFIG_OVERLAY,
    ERROR_CPE_BAD_SOURCE,
    ERROR_DATASTREAM_BAD_SOURCE,
    ERROR_DATASTREAM_COMP_TYPE,
    ERROR_DATASTREAM_COMPONENT,
    ERROR_DEFINITION_FILTER_BAD_SOURCE,
    ERROR_DEFINITIONS_BAD_SOURCE,
    ERROR_DEFINITIONS_NONE,
    ERROR_DIRECTIVES_BAD_SOURCE,
    ERROR_DPKGINFO_LINE,
    ERROR_ENGINE_ABORT,
    ERROR_ENGINE_STATE,
    ERROR_EXCEPTION,
    ERROR_EXTERNAL_VARIABLE,
    ERROR_EXTERNAL_VARIABLE_SOURCE,
    ERROR_FILE_CLOSE,
    ERROR_FILE_GENERATE,
    ERROR_FILE_STREAM_CLOSE,
    ERROR_FLAG,
    ERROR_FMRI,
    ERROR_ILLEGAL_TIME,
    ERROR_INETD_LINE,
    ERROR_INSTANCE,
    ERROR_IOS_TRAIN_COMPARISON,
    ERROR_LINUX_PARTITION,
    ERROR_LSOF,
    ERROR_MESSAGE_CONVEYOR,
    ERROR_METABASE_PRIVILEGE,
    ERROR_MISSING_RESOURCE,
    ERROR_OBJECT_PERMUTATION,
    ERROR_OCIL_REQUIRED,
    ERROR_OCIL_VARS,
    ERROR_OVAL,
    ERROR_OVAL_STATES,
    ERROR_PASSWD_LINE,
    ERROR_PATTERN,
    ERROR_PE,
    ERROR_PLIST_APPID,
    ERROR_PLIST_PARSE,
    ERROR_PLIST_UNSUPPORTED_TYPE,
    ERROR_PLUGIN_CLASSPATH,
    ERROR_PLUGIN_CLASSPATH_ELT,
    ERROR_PLUGIN_INTERFACE,
    ERROR_PLUGIN_MAIN,
    ERROR_POWERSHELL,
    ERROR_REF_DEFINITION,
    ERROR_REF_ITEM,
    ERROR_REF_OBJECT,
    ERROR_REF_STATE,
    ERROR_REF_TEST,
    ERROR_REF_VARIABLE,
    ERROR_REFLECTION,
    ERROR_RESOLVE_ITEM_FIELD,
    ERROR_RESOLVE_VAR,
    ERROR_RESULTS_BAD_SOURCE,
    ERROR_RESULTS_SC_COUNT,
    ERROR_RPMINFO_SIGKEY,
    ERROR_SC_BAD_SOURCE,
    ERROR_SC_MAP_OVERFLOW,
    ERROR_SCAP_CHECKCONTENT,
    ERROR_SCE_PLATFORM,
    ERROR_SCE_PLATFORMLANG,
    ERROR_SCE_RESULT_BAD_SOURCE,
    ERROR_SCE_VARS,
    ERROR_SCHEMATRON_TYPE,
    ERROR_SCHEMATRON_VALIDATION,
    ERROR_SELINUX_BOOL,
    ERROR_SELINUX_SC,
    ERROR_SESSION_CONNECT,
    ERROR_SESSION_NONE,
    ERROR_SET_COMPLEMENT,
    ERROR_SHADOW_LINE,
    ERROR_SMF,
    ERROR_SOLPKGCHK_BEHAVIORS,
    ERROR_SUBSTRING,
    ERROR_SYSINFO_ARCH,
    ERROR_SYSINFO_INTERFACE,
    ERROR_SYSINFO_OSNAME,
    ERROR_SYSINFO_OSVERSION,
    ERROR_SYSINFO_TYPE,
    ERROR_TAILORING_BAD_SOURCE,
    ERROR_TEST_INCOMPARABLE,
    ERROR_TEST_NOOBJREF,
    ERROR_TESTEXCEPTION,
    ERROR_TIMESTAMP,
    ERROR_TYPE_CIDR_MASKS,
    ERROR_TYPE_CONVERSION,
    ERROR_TYPE_INCOMPATIBLE,
    ERROR_UNIX_FILE,
    ERROR_UNIX_FILEATTR,
    ERROR_UNSUPPORTED_CHECK,
    ERROR_UNSUPPORTED_COMPONENT,
    ERROR_UNSUPPORTED_DATATYPE,
    ERROR_UNSUPPORTED_ENTITY,
    ERROR_UNSUPPORTED_EXISTENCE,
    ERROR_UNSUPPORTED_ITEM,
    ERROR_UNSUPPORTED_OBJECT,
    ERROR_UNSUPPORTED_OPERATION,
    ERROR_UNSUPPORTED_OS_VERSION,
    ERROR_UNSUPPORTED_SESSION_TYPE,
    ERROR_UNSUPPORTED_UNIX_FLAVOR,
    ERROR_VARIABLE_MISSING,
    ERROR_VARIABLE_NO_VALUES,
    ERROR_VARIABLES_BAD_SOURCE,
    ERROR_VERSION_CLASS,
    ERROR_VERSION_STR,
    ERROR_WIN_ACCESSTOKEN_PRINCIPAL,
    ERROR_WIN_ACCESSTOKEN_TOKEN,
    ERROR_WIN_AUDITPOL_SUBCATEGORY,
    ERROR_WIN_FILESACL,
    ERROR_WIN_IDENTITY,
    ERROR_WIN_LOCKOUTPOLICY_VALUE,
    ERROR_WIN_NOPRINCIPAL,
    ERROR_WIN_SECEDIT_CODE,
    ERROR_WIN_SYSTEMMETRIC_VALUE,
    ERROR_WIN_UAC,
    ERROR_WIN_WUA_SEARCH,
    ERROR_WINREG_HIVE_NAME,
    ERROR_WINREG_VALUETOSTR,
    ERROR_WINWMI_GENERAL,
    ERROR_XCCDF_BAD_SOURCE,
    ERROR_XCCDF_BENCHMARK,
    ERROR_XCCDF_MISSING_PART,
    ERROR_XCCDF_VALUE,
    ERROR_XINETD_FILE,
    ERROR_XINETD_FORMAT,
    ERROR_XML_PARSE,
    ERROR_XML_TRANSFORM,
    ERROR_XML_XPATH,
    STATUS_ADAPTER_COLLECTION,
    STATUS_AIX_FILESET,
    STATUS_AIX_FIX,
    STATUS_AIX_FSTYPE,
    STATUS_AIX_JFS2EAFORMAT,
    STATUS_CHECK_NONE_EXIST,
    STATUS_CPE_TARGET,
    STATUS_DEFINITION,
    STATUS_EMPTY_ENTITY,
    STATUS_EMPTY_FILE,
    STATUS_EMPTY_RECORD,
    STATUS_EMPTY_SET,
    STATUS_FILEHASH,
    STATUS_FILTER,
    STATUS_INETD_NOCONFIG,
    STATUS_INETD_SERVICE,
    STATUS_NO_PROCESS,
    STATUS_NOT_APPLICABLE,
    STATUS_NOT_FILE,
    STATUS_OBJECT,
    STATUS_OBJECT_BATCH,
    STATUS_OBJECT_ITEM_MAP,
    STATUS_OBJECT_NOITEM_MAP,
    STATUS_OBJECT_QUEUE,
    STATUS_PE_DEVCLASS,
    STATUS_PE_EMPTY,
    STATUS_PE_READ,
    STATUS_PRO_DB,
    STATUS_RPMINFO_LIST,
    STATUS_RPMINFO_RPM,
    STATUS_SMF,
    STATUS_SMF_SERVICE,
    STATUS_SOLPKG_LIST,
    STATUS_SOLPKG_PKGINFO,
    STATUS_TEST,
    STATUS_UNIX_FILE,
    STATUS_VARIABLE_CREATE,
    STATUS_VARIABLE_RECYCLE,
    STATUS_WIN_ACCESSTOKEN,
    STATUS_WIN_WUA,
    STATUS_XCCDF_CONVERT,
    STATUS_XCCDF_NOPROFILE,
    STATUS_XCCDF_RULE,
    STATUS_XCCDF_RULES,
    STATUS_XCCDF_SCORE,
    STATUS_XINETD_FILE,
    STATUS_XINETD_NOCONFIG,
    STATUS_XINETD_SERVICE,
    WARNING_CPE_MULTIDICTIONARY,
    WARNING_CPE_NODICTIONARY,
    WARNING_CPE_TARGET,
    WARNING_CPE_URI,
    WARNING_FIELD_STATUS,
    WARNING_FILEHASH_LINES,
    WARNING_REGEX_GROUP,
    WARNING_RPMVERIFY_LINE,
    WARNING_SERVICE,
    WARNING_SYSCTL,
    WARNING_WINDOWS_VIEW,
    WARNING_XCCDF_PLATFORM,
    WARNING_XCCDF_RULES;

    private static IMessageConveyor baseConveyor;
    private static Conveyor conveyor;
    private static LocLoggerFactory loggerFactory;
    private static LocLogger sysLogger;

    static {
	baseConveyor = new MessageConveyor(java.util.Locale.getDefault());
	try {
	    //
	    // Get a message to test whether localized messages are available for the default Locale
	    //
	    baseConveyor.getMessage(ERROR_EXCEPTION);
	} catch (MessageConveyorException e) {
	    //
	    // The test failed, so set the message Locale to English
	    //
	    baseConveyor = new MessageConveyor(java.util.Locale.ENGLISH);
	}
	conveyor = new Conveyor();
	loggerFactory = new LocLoggerFactory(conveyor);
	sysLogger = loggerFactory.getLocLogger(JOVALMsg.class);
    }

    /**
     * Extend JOVALMsg to be able to provide messages for the specified Enum class, using the specified IMessageConveyor.
     */
    public static void extend(Class<? extends Enum<?>> clazz, IMessageConveyor mc) {
	conveyor.conveyors.put(clazz, mc);
    }

    /**
     * Retrieve the default localized system logger used by the jOVAL library.
     */
    public static LocLogger getLogger() {
	return sysLogger;
    }

    /**
     * Retrieve/create a localized jOVAL logger with a particular name.  This is useful for passing to an IPlugin, if you
     * want all of the plugin's log messages routed to a specific logger.
     */
    public static LocLogger getLogger(String name) {
	return loggerFactory.getLocLogger(name);
    }

    /**
     * Retrieve a localized String, given the key and substitution arguments.
     */
    public static String getMessage(Enum<?> key, Object... args) {
	return conveyor.getMessage(key, args);
    }

    // Internal

    /**
     * An IMessageConveyor that consolidates multiple IMessageConveyors.
     */
    static class Conveyor implements IMessageConveyor {
	HashMap<Class, IMessageConveyor> conveyors;

	Conveyor() {
	    conveyors = new HashMap<Class, IMessageConveyor>();
	    conveyors.put(JOVALMsg.class, baseConveyor);
	}

	public <E extends Enum<?>>String getMessage(E key, Object... args) throws MessageConveyorException {
	    IMessageConveyor mc = conveyors.get(key.getDeclaringClass());
	    if (mc == null) {
		throw new MessageConveyorException(baseConveyor.getMessage(ERROR_MESSAGE_CONVEYOR, key.getClass().getName()));
	    } else {
		return mc.getMessage(key, args);
	    }
	}

	public String getMessage(MessageParameterObj mpo) throws MessageConveyorException {
	    return getMessage(mpo.getKey(), mpo.getArgs());
	}
     }
}
