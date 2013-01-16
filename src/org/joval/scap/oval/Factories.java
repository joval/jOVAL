// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval;

/**
 * This class provides a consolidated access point for accessing all of the OVAL schema JAXB object factories.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Factories {
    /**
     * Facilitates access to the OVAL common schema ObjectFactory.
     */
    public static scap.oval.common.ObjectFactory common = new scap.oval.common.ObjectFactory();

    /**
     * Facilitates access to the OVAL directives schema ObjectFactory.
     */
    public static scap.oval.directives.ObjectFactory directives = new scap.oval.directives.ObjectFactory();

    /**
     * Facilitates access to the OVAL evaluation schema ObjectFactory.
     */
    public static scap.oval.evaluation.ObjectFactory evaluation = new scap.oval.evaluation.ObjectFactory();

    /**
     * Facilitates access to the OVAL results schema ObjectFactory.
     */
    public static scap.oval.results.ObjectFactory results = new scap.oval.results.ObjectFactory();

    /**
     * Facilitates access to the OVAL variables schema ObjectFactory.
     */
    public static scap.oval.variables.ObjectFactory variables = new scap.oval.variables.ObjectFactory();

    /**
     * Facilitates access to the ObjectFactories of the OVAL definitions schema.
     */
    public static DefinitionFactories definitions = new DefinitionFactories();

    /**
     * Facilitates access to the ObjectFactories of the OVAL system-characteristics schema.
     */
    public static SystemCharacteristicsFactories sc = new SystemCharacteristicsFactories();

    /**
     * A data structure containing fields for all the OVAL definition object factories.
     */
    public static class DefinitionFactories {
	public scap.oval.definitions.aix.ObjectFactory		aix;
	public scap.oval.definitions.apache.ObjectFactory	apache;
	public scap.oval.definitions.catos.ObjectFactory	catos;
	public scap.oval.definitions.core.ObjectFactory		core;
	public scap.oval.definitions.esx.ObjectFactory		esx;
	public scap.oval.definitions.freebsd.ObjectFactory	freebsd;
	public scap.oval.definitions.hpux.ObjectFactory		hpux;
	public scap.oval.definitions.independent.ObjectFactory	independent;
	public scap.oval.definitions.ios.ObjectFactory		ios;
	public scap.oval.definitions.junos.ObjectFactory	junos;
	public scap.oval.definitions.linux.ObjectFactory	linux;
	public scap.oval.definitions.macos.ObjectFactory	macos;
	public scap.oval.definitions.netconf.ObjectFactory	netconf;
	public scap.oval.definitions.pixos.ObjectFactory	pixos;
	public scap.oval.definitions.sharepoint.ObjectFactory	sharepoint;
	public scap.oval.definitions.solaris.ObjectFactory	solaris;
	public scap.oval.definitions.unix.ObjectFactory		unix;
	public scap.oval.definitions.windows.ObjectFactory	windows;

	private DefinitionFactories() {
	    aix		= new scap.oval.definitions.aix.ObjectFactory();
	    apache	= new scap.oval.definitions.apache.ObjectFactory();
	    catos	= new scap.oval.definitions.catos.ObjectFactory();
	    core	= new scap.oval.definitions.core.ObjectFactory();
	    esx		= new scap.oval.definitions.esx.ObjectFactory();
	    freebsd	= new scap.oval.definitions.freebsd.ObjectFactory();
	    hpux	= new scap.oval.definitions.hpux.ObjectFactory();
	    independent	= new scap.oval.definitions.independent.ObjectFactory();
	    ios		= new scap.oval.definitions.ios.ObjectFactory();
	    junos	= new scap.oval.definitions.junos.ObjectFactory();
	    linux	= new scap.oval.definitions.linux.ObjectFactory();
	    macos	= new scap.oval.definitions.macos.ObjectFactory();
	    netconf	= new scap.oval.definitions.netconf.ObjectFactory();
	    pixos	= new scap.oval.definitions.pixos.ObjectFactory();
	    sharepoint	= new scap.oval.definitions.sharepoint.ObjectFactory();
	    solaris	= new scap.oval.definitions.solaris.ObjectFactory();
	    unix	= new scap.oval.definitions.unix.ObjectFactory();
	    windows	= new scap.oval.definitions.windows.ObjectFactory();
	}
    }

    /**
     * A data structure containing fields for all the OVAL system characteristics object factories.
     */
    public static class SystemCharacteristicsFactories {
	public scap.oval.systemcharacteristics.aix.ObjectFactory	 aix;
	public scap.oval.systemcharacteristics.apache.ObjectFactory	 apache;
	public scap.oval.systemcharacteristics.catos.ObjectFactory	 catos;
	public scap.oval.systemcharacteristics.core.ObjectFactory	 core;
	public scap.oval.systemcharacteristics.esx.ObjectFactory	 esx;
	public scap.oval.systemcharacteristics.freebsd.ObjectFactory	 freebsd;
	public scap.oval.systemcharacteristics.hpux.ObjectFactory	 hpux;
	public scap.oval.systemcharacteristics.independent.ObjectFactory independent;
	public scap.oval.systemcharacteristics.ios.ObjectFactory	 ios;
	public scap.oval.systemcharacteristics.junos.ObjectFactory	 junos;
	public scap.oval.systemcharacteristics.linux.ObjectFactory	 linux;
	public scap.oval.systemcharacteristics.macos.ObjectFactory	 macos;
	public scap.oval.systemcharacteristics.netconf.ObjectFactory	 netconf;
	public scap.oval.systemcharacteristics.pixos.ObjectFactory	 pixos;
	public scap.oval.systemcharacteristics.sharepoint.ObjectFactory	 sharepoint;
	public scap.oval.systemcharacteristics.solaris.ObjectFactory	 solaris;
	public scap.oval.systemcharacteristics.unix.ObjectFactory	 unix;
	public scap.oval.systemcharacteristics.windows.ObjectFactory	 windows;

	private SystemCharacteristicsFactories() {
	    aix		= new scap.oval.systemcharacteristics.aix.ObjectFactory();
	    apache	= new scap.oval.systemcharacteristics.apache.ObjectFactory();
	    catos	= new scap.oval.systemcharacteristics.catos.ObjectFactory();
	    core	= new scap.oval.systemcharacteristics.core.ObjectFactory();
	    esx		= new scap.oval.systemcharacteristics.esx.ObjectFactory();
	    freebsd	= new scap.oval.systemcharacteristics.freebsd.ObjectFactory();
	    hpux	= new scap.oval.systemcharacteristics.hpux.ObjectFactory();
	    independent	= new scap.oval.systemcharacteristics.independent.ObjectFactory();
	    ios		= new scap.oval.systemcharacteristics.ios.ObjectFactory();
	    junos	= new scap.oval.systemcharacteristics.junos.ObjectFactory();
	    linux	= new scap.oval.systemcharacteristics.linux.ObjectFactory();
	    macos	= new scap.oval.systemcharacteristics.macos.ObjectFactory();
	    netconf	= new scap.oval.systemcharacteristics.netconf.ObjectFactory();
	    pixos	= new scap.oval.systemcharacteristics.pixos.ObjectFactory();
	    sharepoint	= new scap.oval.systemcharacteristics.sharepoint.ObjectFactory();
	    solaris	= new scap.oval.systemcharacteristics.solaris.ObjectFactory();
	    unix	= new scap.oval.systemcharacteristics.unix.ObjectFactory();
	    windows	= new scap.oval.systemcharacteristics.windows.ObjectFactory();
	}
    }
}
