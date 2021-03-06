/*
 * Copyright (C) 2006, 2007 XStream Committers.
 * All rights reserved.
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 * 
 * Created on 17. March 2006 by Joerg Schaible
 */
package com.thoughtworks.acceptance;

import com.thoughtworks.xstream.XStream;

public class AbsoluteXPathNestedCircularReferenceTest extends AbstractNestedCircularReferenceTest {

    // inherits test from superclass
    protected void setUp() throws Exception {
        super.setUp();
        xstream.setMode(XStream.XPATH_ABSOLUTE_REFERENCES);
    }

}
