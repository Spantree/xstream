/*
 * Copyright (C) 2004, 2005, 2006 Joe Walnes.
 * Copyright (C) 2006, 2007 XStream Committers.
 * All rights reserved.
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 * 
 * Created on 03. September 2004 by Joe Walnes
 */
package com.thoughtworks.xstream.io.xml;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;

import java.util.List;

/**
 * @author Laurent Bihanic
 */
public class JDomReader extends AbstractDocumentReader {

    private Element currentElement;

    public JDomReader(Element root) {
        super(root);
    }

    public JDomReader(Document document) {
        super(document.getRootElement());
    }

    /**
     * @since 1.2
     */
    public JDomReader(Element root, XmlFriendlyReplacer replacer) {
        super(root, replacer);
    }

    /**
     * @since 1.2
     */
    public JDomReader(Document document, XmlFriendlyReplacer replacer) {
        super(document.getRootElement(), replacer);
    }
    
    protected void reassignCurrentElement(Object current) {
        currentElement = (Element) current;
    }

    protected Object getParent() {
        // JDOM 1.0:
        return currentElement.getParentElement();

        // JDOM b10:
        // Parent parent = currentElement.getParent();
        // return (parent instanceof Element) ? (Element)parent : null;

        // JDOM b9 and earlier:
        // return currentElement.getParent();
    }

    public String peekNextChild() {
        List list = currentElement.getChildren();
        if(list.isEmpty())  return null;
        return unescapeXmlName(((Element)list.get(0)).getName());
    }

    protected Object getChild(int index) {
        return currentElement.getChildren().get(index);
    }

    protected int getChildCount() {
        return currentElement.getChildren().size();
    }

    public String getNodeName() {
        return unescapeXmlName(currentElement.getName());
    }

    public String getValue() {
        return currentElement.getText();
    }

    public String getAttribute(String name) {
        return currentElement.getAttributeValue(name);
    }

    public String getAttribute(int index) {
        return ((Attribute) currentElement.getAttributes().get(index)).getValue();
    }

    public int getAttributeCount() {
        return currentElement.getAttributes().size();
    }

    public String getAttributeName(int index) {
        return unescapeXmlName(((Attribute) currentElement.getAttributes().get(index)).getQualifiedName());
    }

}

