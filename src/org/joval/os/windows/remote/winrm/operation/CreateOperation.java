// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.winrm.operation;

import java.io.IOException;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import org.w3c.dom.Node;

import org.dmtf.wsman.OptionSet;
import org.xmlsoap.ws.transfer.AnyXmlType;
import org.xmlsoap.ws.transfer.CreateResponseType;
import org.xmlsoap.ws.transfer.ResourceCreated;

import org.joval.os.windows.remote.winrm.IPort;
import org.joval.os.windows.remote.winrm.WSMFault;

public class CreateOperation extends BaseOperation<AnyXmlType, CreateResponseType> {
    public CreateOperation(AnyXmlType input) {
	super("http://schemas.xmlsoap.org/ws/2004/09/transfer/Create", input);
    }


    public void addOptionSet(OptionSet options) {
	headers.add(options);
    }

    static final String WSA04 = "http://schemas.xmlsoap.org/ws/2004/08/addressing";
    static final String WSA10 = "http://www.w3.org/2005/08/addressing";

    /**
     * The CreateResponseType is merely a container for one of the two EndpointReferenceType classes.  This confuses
     * JAXB, and forces it to unmarshal the XML into DOM Nodes.
     *
     * So, to make the API nicer, we put the appropriate EndpointReferenceType into the ResourceCreated's any contents.
     */
    @Override
    public CreateResponseType dispatch(IPort port) throws IOException, JAXBException, WSMFault {
        Object obj = dispatch0(port);
	if (obj instanceof ResourceCreated) {
	    //
	    // ResourceCreated is ambiguous, so its children will be Nodes.  We must determine the appropriate
	    // type and unmarshall the correct JAXB objects.
	    //
	    CreateResponseType response = Factories.TRANSFER.createCreateResponseType();
	    List<Object> list = ((ResourceCreated)obj).getAny();
	    String ns = ((Node)list.get(0)).getNamespaceURI();
	    if (WSA10.equals(ns)) {
		org.w3c.ws.addressing.EndpointReferenceType ref = Factories.WSADDRESS.createEndpointReferenceType();
		for (Object element : list) {
		    Node node = (Node)element;
		    if ("Address".equals(node.getLocalName())) {
			org.w3c.ws.addressing.AttributedURIType address = Factories.WSADDRESS.createAttributedURIType();
			int numAttrs = node.getAttributes().getLength();
			for (int i=0; i < numAttrs; i++) {
			    Node attr = node.getAttributes().item(i);
			    QName key = new QName(attr.getNamespaceURI(), attr.getLocalName());
			    address.getOtherAttributes().put(key, attr.getTextContent());
			}
			address.setValue(node.getTextContent());
			ref.setAddress(address);
		    } else if ("Metadata".equals(node.getLocalName())) {
			org.w3c.ws.addressing.MetadataType md = Factories.WSADDRESS.createMetadataType();
			int numAttrs = node.getAttributes().getLength();
			for (int i=0; i < numAttrs; i++) {
			    Node attr = node.getAttributes().item(i);
			    QName key = new QName(attr.getNamespaceURI(), attr.getLocalName());
			    md.getOtherAttributes().put(key, attr.getTextContent());
			}
			int numChildren = node.getChildNodes().getLength();
			for (int i=0; i < numChildren; i++) {
			    Node child = node.getChildNodes().item(i);
			    md.getAny().add(convert(child, port));
			}
			ref.setMetadata(md);
		    } else if ("ReferenceParameters".equals(node.getLocalName())) {
			org.w3c.ws.addressing.ReferenceParametersType params =
				Factories.WSADDRESS.createReferenceParametersType();
			int numAttrs = node.getAttributes().getLength();
			for (int i=0; i < numAttrs; i++) {
			    Node attr = node.getAttributes().item(i);
			    QName key = new QName(attr.getNamespaceURI(), attr.getLocalName());
			    params.getOtherAttributes().put(key, attr.getTextContent());
			}
			int numChildren = node.getChildNodes().getLength();
			for (int i=0; i < numChildren; i++) {
			    Node param = node.getChildNodes().item(i);
			    params.getAny().add(convert(param, port));
			}
			ref.setReferenceParameters(params);
		    } else {
			ref.getAny().add(node);
		    }
		}
		ResourceCreated res = Factories.TRANSFER.createResourceCreated();
		res.getAny().add(ref);
		response.setResourceCreated(res);
	    } else if (WSA04.equals(ns)) {
		org.xmlsoap.ws.addressing.EndpointReferenceType ref = Factories.ADDRESS.createEndpointReferenceType();
		for (Object element : list) {
		    Node node = (Node)element;
		    if ("Address".equals(node.getLocalName())) {
			org.xmlsoap.ws.addressing.AttributedURI address = Factories.ADDRESS.createAttributedURI();
			int numAttrs = node.getAttributes().getLength();
			for (int i=0; i < numAttrs; i++) {
			    Node attr = node.getAttributes().item(i);
			    QName key = new QName(attr.getNamespaceURI(), attr.getLocalName());
			    address.getOtherAttributes().put(key, attr.getTextContent());
			}
			address.setValue(node.getTextContent());
			ref.setAddress(address);
		    } else if ("ReferenceProperties".equals(node.getLocalName())) {
			org.xmlsoap.ws.addressing.ReferencePropertiesType props =
				Factories.ADDRESS.createReferencePropertiesType();
			int numChildren = node.getChildNodes().getLength();
			for (int i=0; i < numChildren; i++) {
			    Node prop = node.getChildNodes().item(i);
			    props.getAny().add(convert(prop, port));
			}
			ref.setReferenceProperties(props);
		    } else if ("ReferenceParameters".equals(node.getLocalName())) {
			org.xmlsoap.ws.addressing.ReferenceParametersType params =
				Factories.ADDRESS.createReferenceParametersType();
			int numChildren = node.getChildNodes().getLength();
			for (int i=0; i < numChildren; i++) {
			    Node param = node.getChildNodes().item(i);
			    params.getAny().add(convert(param, port));
			}
			ref.setReferenceParameters(params);
		    } else if ("PortType".equals(node.getLocalName())) {
			org.xmlsoap.ws.addressing.AttributedQName portType = Factories.ADDRESS.createAttributedQName();
			int numAttrs = node.getAttributes().getLength();
			for (int i=0; i < numAttrs; i++) {
			    Node attr = node.getAttributes().item(i);
			    QName key = new QName(attr.getNamespaceURI(), attr.getLocalName());
			    portType.getOtherAttributes().put(key, attr.getTextContent());
			}
			portType.setValue(new QName(node.getNamespaceURI(), node.getLocalName()));
			ref.setPortType(portType);
		    } else if ("ServiceName".equals(node.getLocalName())) {
			org.xmlsoap.ws.addressing.ServiceNameType svc = Factories.ADDRESS.createServiceNameType();
			int numAttrs = node.getAttributes().getLength();
			for (int i=0; i < numAttrs; i++) {
			    Node attr = node.getAttributes().item(i);
			    if (attr.getNamespaceURI().equals(XMLNS) && attr.getLocalName().equals("PortName")) {
				svc.setPortName(attr.getTextContent());
			    } else {
				QName key = new QName(attr.getNamespaceURI(), attr.getLocalName());
				svc.getOtherAttributes().put(key, attr.getTextContent());
			    }
			}
			svc.setValue(new QName(node.getNamespaceURI(), node.getLocalName()));
			ref.setServiceName(svc);
		    } else {
			ref.getAny().add(node);
		    }
		}
		ResourceCreated res = Factories.TRANSFER.createResourceCreated();
		res.getAny().add(ref);
		response.setResourceCreated(res);
	    } else {
        	response.setResourceCreated((ResourceCreated)obj);
	    }
	    return response;
        }
	throw new IllegalArgumentException(obj.getClass().getName());
    }

    // Private

    /**
     * Attempt to unmarshal DOM node(s).
     */
    private Object convert(Node node, IPort port) {
	try {
	    return port.unmarshal(node);
	} catch (JAXBException e) {
	    return node;
	}
    }
}
