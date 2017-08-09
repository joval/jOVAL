About the scap-extensions component
=============

The purpose of the scap-extensions component is to illustrate how you can extend jOVAL's OVAL data model. To extend the model, you must first create XSD files containing the OVAL tests, objects, states and items (and any enumerations) that you want to add to the OVAL language. Your custom ObjectTypes, StateTypes and TestTypes must appear together in a single namespace, with your ItemTypes in a separate namespace.

The examples here, under the [schemas](schemas/) directory, were taken from the [OVAL Sandbox on Github](https://github.com/OVALProject/Sandbox).

Once you have schema definitions, you will need to generate JAXB model classes.  You should create a TR9401 catalog to map namespaces to XSD source files, and use it with the JAXB code-generation process. There must be no &lt;xsd:include/&gt; tags in your schema files, and any &lt;xsd:import/&gt; tags should have no schemaLocation attribute. The catalog must be used by XJC to associate namespace URIs with the XSD files. The classes you produce with XJC must follow these naming conventions:

1. The JAXB types for every \[Object/State/Test\]Type tuple must appear in a common package whose name follows the pattern: \[anything-a\].definitions.\[anything-b\]
2. The corresponding JAXB type for every ItemType must be placed in another package whose name follows the correspending pattern: \[anything-a\].systemcharacteristics.\[anything-b\]
3. The class names for your Object/State/Test/Item types must all derive from a common stem, i.e., MyCustomTypenameStem\[Object/State/Test/Item\].

Next, create a <registry.ini> file that follows the same conventions as the scap component's registry.ini file. See the documentation inside of [scap/rsrc/registry.ini](../scap/rsrc/registry.ini).

You must also choose a unique "basename" for registering your extensions. This example uses a basename of "scapx" (the scap component uses a basename of "scap"). To create an extension JAR, you must package up your registry.ini and schemas.cat files together in under a top-level directory whose name matches your desired basename. The basename can then used to tell the SchemaRegistry class -- via the public static register(String basename) method -- where it can find the extensions. The catalog file that you bundle in basename/schemas.cat should specify relative URIs within the same JAR file where you have decided to bundle your XSD schema files. This is how JAXB will be able to find those files when creating a validation schema.


=============

Copyright (C) 2013 jOVAL.org.  All rights reserved.
