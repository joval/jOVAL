// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.security.Key;
import java.security.KeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.CertSelector;
import java.security.cert.X509Certificate;
import java.security.cert.X509CertSelector;
import javax.security.auth.x500.X500Principal;
import javax.xml.XMLConstants;
import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.KeySelectorException;
import javax.xml.crypto.KeySelectorResult;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.NoSuchMechanismException;
import javax.xml.crypto.NodeSetData;
import javax.xml.crypto.OctetStreamData;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyName;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.dsig.keyinfo.RetrievalMethod;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.keyinfo.X509IssuerSerial;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Utility class for validating XML digital signatures.
 * @see http://docs.oracle.com/cd/E17802_01/webservices/webservices/docs/1.6/tutorial/doc/XMLDigitalSignatureAPI8.html
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SignatureValidator {
    private static KeyStore KEY_STORE;
    static {
	try {
	    KEY_STORE = KeyStore.getInstance(KeyStore.getDefaultType());
	    String s = System.getProperty("securityDir");
	    if (s != null) {
		File baseDir = new File(System.getProperty("securityDir"));
		File cacerts = new File(baseDir, "cacerts.jks");
		if (cacerts.exists()) {
		    KEY_STORE.load(new FileInputStream(cacerts), "jOVAL s3cure".toCharArray());
		} else {
		    KEY_STORE.load(null);
		}
	    }
	} catch (Exception e) {
	    System.out.println("WARNING: " + e.getMessage());
	}
    }

    private static final DocumentBuilderFactory DBF;
    static {
	try {
	    DBF = DocumentBuilderFactory.newInstance();
	    DBF.setNamespaceAware(true);
	} catch (FactoryConfigurationError e) {
	    throw new RuntimeException(e);
	}
    }

    private static final XMLSignatureFactory XSF;
    static {
	try {
	    XSF = XMLSignatureFactory.getInstance();
	} catch (NoSuchMechanismException e) {
	    throw new RuntimeException(e);
	}
    }

    private Document doc;
    private NodeList signatures;
    private KeyStore keyStore;

    /**
     * Create a new validator for an arbitrary XML file using the default KeyStore.
     */
    public SignatureValidator(File f) throws ParserConfigurationException, SAXException, IOException {
	this(f, KEY_STORE);
    }

    public SignatureValidator(File f, KeyStore keyStore) throws ParserConfigurationException, SAXException, IOException {
	doc = DBF.newDocumentBuilder().parse(f);
	signatures = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
	this.keyStore = keyStore;
    }

    /**
     * Assign a specific KeyStore to be used for validation.
     */
    public void setKeyStore(KeyStore keyStore) {
	this.keyStore = keyStore;
    }

    /**
     * Returns whether or not there is an XML signature node in the document.
     */
    public boolean containsSignature() {
	return signatures.getLength() > 0;
    }

    /**
     * Validate the document's XML digital signature.
     */
    public boolean validate() throws XMLSignatureException {
	int len = signatures.getLength();
	if (len == 0) {
	    throw new XMLSignatureException("Cannot find Signature element");
	} else {
	    try {
		for (int i=0; i < len; i++) {
		    DOMValidateContext ctx = new DOMValidateContext(new X509KeySelector(), signatures.item(i));
		    XMLSignature signature = XSF.unmarshalXMLSignature(ctx);
		    if (!signature.validate(ctx)) {
			return false;
		    }
		}
		return true;
	    } catch (ClassCastException e) {
		throw new XMLSignatureException(e);
	    } catch (MarshalException e) {
		throw new XMLSignatureException(e);
	    }
	}
    }

    /**
     * Validate the document's XML digital signature using the supplied public key.
     */
    public boolean validate(Key validationKey) throws XMLSignatureException {
	int len = signatures.getLength();
	if (len == 0) {
	    throw new XMLSignatureException("Cannot find Signature element");
	} else {
	    try {
		for (int i=0; i < len; i++) {
		    DOMValidateContext ctx = new DOMValidateContext(validationKey, signatures.item(i));
		    XMLSignature signature = XSF.unmarshalXMLSignature(ctx);
		    if (!signature.validate(ctx)) {
			return false;
		    }
		}
		return true;
	    } catch (ClassCastException e) {
		throw new XMLSignatureException(e);
	    } catch (MarshalException e) {
		throw new XMLSignatureException(e);
	    }
	}
    }

    /**
     * Obtain a Source for the document. This makes it possible to insure that the underlying data has been validated.
     */
    public Source getSource() {
	return new DOMSource(doc);
    }

    // Private

    /**
     * KeySelector implementation for X.509 certificates.
     */
    class X509KeySelector extends KeySelector {
	@Override
	public KeySelectorResult select(KeyInfo ki, KeySelector.Purpose p, AlgorithmMethod am, XMLCryptoContext ctx)
		throws KeySelectorException {

	    KeySelectorResult result = null;
	    SignatureMethod sm = (SignatureMethod)am;
	    for (Object obj : ki.getContent()) {
		XMLStructure xs = (XMLStructure)obj;
		if (xs instanceof X509Data) {
		    result = select((X509Data)xs, sm);
		} else if (xs instanceof KeyName) {
		    KeyName kn = (KeyName)xs;
		    try {
			Certificate cert = keyStore.getCertificate(kn.getName());
			if (cert != null && equals(sm.getAlgorithm(), cert.getPublicKey().getAlgorithm())) {
			    result = new SimpleKeySelectorResult(cert.getPublicKey());
			}
		    } catch (KeyStoreException e) {
			throw new KeySelectorException(e);
		    }
		} else if (xs instanceof RetrievalMethod) {
		    RetrievalMethod rm = (RetrievalMethod)xs;
		    try {
			if (rm.getType().equals(X509Data.RAW_X509_CERTIFICATE_TYPE)) {
			    OctetStreamData data = (OctetStreamData)rm.dereference(ctx);
			    CertificateFactory cf = CertificateFactory.getInstance("X.509");
			    X509Certificate cert = (X509Certificate)cf.generateCertificate(data.getOctetStream());
			    result = select(cert, sm);
			} else if (rm.getType().equals(X509Data.TYPE)) {
			    NodeSetData nd = (NodeSetData)rm.dereference(ctx);
			    System.out.println("DAS conversion is TBD");
			}
		    } catch (Exception e) {
			throw new KeySelectorException(e);
		    }
		} else if (xs instanceof KeyValue) {
		    PublicKey pk = null;
		    try {
			pk = ((KeyValue)xs).getPublicKey();
		    } catch (KeyException ke) {
			throw new KeySelectorException(ke);
		    }
		    if (equals(sm.getAlgorithm(), pk.getAlgorithm())) {
			result = new SimpleKeySelectorResult(pk);
		    }
		}
		if (result != null) {
		    return result;
		}
	    }
	    throw new KeySelectorException("Key not found");
	}

	// Internal

	KeySelectorResult select(X509Data data, SignatureMethod sm) throws KeySelectorException {
	    String oid = getOid(sm.getAlgorithm());
	    KeySelectorResult result = null;
	    for (Object obj : data.getContent()) {
		try {
		    if (obj instanceof X509Certificate) {
			return select((X509Certificate)obj, sm);
		    } else if (obj instanceof X509IssuerSerial) {
			X509IssuerSerial xis = (X509IssuerSerial)obj;
			X509CertSelector xcs = new X509CertSelector();
			xcs.setSubjectPublicKeyAlgID(oid);
			xcs.setSerialNumber(xis.getSerialNumber());
			xcs.setIssuer(new X500Principal(xis.getIssuerName()).getName());
			return select(xcs);
		    } else if (obj instanceof String) {
			String sn = (String)obj;
			X509CertSelector xcs = new X509CertSelector();
			xcs.setSubjectPublicKeyAlgID(oid);
			xcs.setSubject(new X500Principal(sn).getName());
			return select(xcs);
		    } else if (obj instanceof byte[]) {
			byte[] ski = (byte[])obj;
			X509CertSelector xcs = new X509CertSelector();
			xcs.setSubjectPublicKeyAlgID(oid);
			byte[] encodedSki = new byte[ski.length+2];
			encodedSki[0] = 0x04; // OCTET STRING tag value
			encodedSki[1] = (byte)ski.length;
			System.arraycopy(ski, 0, encodedSki, 2, ski.length);
			xcs.setSubjectKeyIdentifier(encodedSki);
			return select(xcs);
		    }
		} catch (IOException e) {
		    throw new KeySelectorException(e);
		} catch (KeyStoreException e) {
		    throw new KeySelectorException(e);
		} catch (NoSuchElementException e) {
		    throw new KeySelectorException(e);
		}
	    }
	    return null;
	}

	/**
	 * Return the certificate in the key store that matches the specified selector
	 *
	 * @throws NoSuchElementException if there is no match
	 */
	KeySelectorResult select(X509CertSelector cs) throws KeyStoreException, NoSuchElementException {
	    for (Enumeration e = keyStore.aliases(); e.hasMoreElements(); ) {
		Certificate cert = keyStore.getCertificate((String)e.nextElement());
		if (cert != null && cs.match(cert)) {
		    return new SimpleKeySelectorResult(cert.getPublicKey());
		}
	    }
	    throw new NoSuchElementException(cs.getSubject().toString());
	}

	/**
	 * Return the certificate in the key store that matches the specified X509 certificate (or null if there is no match).
	 *
	 * @throws NoSuchElementException if there is no match
	 */
	KeySelectorResult select(X509Certificate xc, SignatureMethod sm) throws KeyStoreException, NoSuchElementException {
	    boolean[] keyUsage = xc.getKeyUsage();
	    if (keyUsage[0]) {
		String alias = keyStore.getCertificateAlias(xc);
		if (alias != null) {
		    PublicKey pk = keyStore.getCertificate(alias).getPublicKey();
		    if (equals(sm.getAlgorithm(), pk.getAlgorithm())) {
			return new SimpleKeySelectorResult(pk);
		    }
		}
	    }
	    throw new NoSuchElementException(xc.getSubjectX500Principal().toString());
	}

	/**
	 * Return the OID corresponding to the Algorithm URI.
	 */
	String getOid(String uri) {
	    if (uri.equalsIgnoreCase(SignatureMethod.DSA_SHA1)) {
		return "1.2.840.10040.4.1";
	    } else if (uri.equalsIgnoreCase(SignatureMethod.RSA_SHA1)) {
		return "1.2.840.113549.1.1";
	    } else {
		return null;
	    }
	}

	/**
	 * Determine whether the algorithm URI matches the algorithm name.
	 */
	boolean equals(String uri, String name) {
	    if (name.equalsIgnoreCase("DSA") && uri.equalsIgnoreCase(SignatureMethod.DSA_SHA1)) {
		return true;
	    } else if (name.equalsIgnoreCase("RSA") && uri.equalsIgnoreCase(SignatureMethod.RSA_SHA1)) {
		return true;
	    } else {
		return false;
	    }
	}
    } 

    /**
     * A simple KeySelectorResult implementation.
     */
    static class SimpleKeySelectorResult implements KeySelectorResult {
	private Key key;

	SimpleKeySelectorResult(Key key) {
	    this.key = key;
	}

	// Implement KeySelectorResult

	public Key getKey() {
	    return key;
	}
    }
}
