/*
 * Copyright (C) 2004, 2005, 2006 Joe Walnes.
 * Copyright (C) 2006, 2007, 2008 XStream Committers.
 * All rights reserved.
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 * 
 * Created on 14. May 2004 by Joe Walnes
 */
package com.thoughtworks.xstream.converters.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.thoughtworks.xstream.core.JVM;
import com.thoughtworks.xstream.core.util.OrderRetainingMap;
import com.thoughtworks.xstream.core.util.ConcurrentWeakHashMap;
import com.thoughtworks.xstream.decorators.FieldDecorator;


/**
 * A field dictionary instance caches information about classes fields.
 * 
 * @author Joe Walnes
 * @author J&ouml;rg Schaible
 * @author Guilherme Silveira
 */
public class FieldDictionary {

    static final class Entry {
        final Map<String,FieldDecorator> keyedByFieldName;
        final Map<FieldKey,FieldDecorator> keyedByFieldKey;

        Entry(Map<String,FieldDecorator> keyedByFieldName, Map<FieldKey,FieldDecorator> keyedByFieldKey) {
            this.keyedByFieldName = keyedByFieldName;
            this.keyedByFieldKey = keyedByFieldKey;
        }
    }

    private transient Map<Class,Entry> cache;
//    private transient Map keyedByFieldNameCache;
//    private transient Map keyedByFieldKeyCache;
    private final FieldKeySorter sorter;

    public FieldDictionary() {
        this(new ImmutableFieldKeySorter());
    }

    public FieldDictionary(FieldKeySorter sorter) {
        this.sorter = sorter;
        init();
    }

    private void init() {
//        keyedByFieldNameCache = new WeakHashMap();
//        keyedByFieldKeyCache = new WeakHashMap();
//        keyedByFieldNameCache.put(Object.class, Collections.EMPTY_MAP);
//        keyedByFieldKeyCache.put(Object.class, Collections.EMPTY_MAP);
        cache = new ConcurrentWeakHashMap<Class,Entry>();
        cache.put(Object.class, new Entry(
                Collections.<String,FieldDecorator>emptyMap(),
                Collections.<FieldKey,FieldDecorator>emptyMap()));
    }

    /**
     * Returns an iterator for all fields for some class
     * 
     * @param cls the class you are interested on
     * @return an iterator for its fields
     * @deprecated since 1.3, use {@link #fieldsFor(Class)} instead
     */
    public Iterator serializableFieldsFor(Class cls) {
        return fieldsFor(cls);
    }

    /**
     * Returns an iterator for all fields for some class
     * 
     * @param cls the class you are interested on
     * @return an iterator for its fields
     */
    public Iterator fieldsFor(Class cls) {
        return buildMap(cls, true).values().iterator();
    }

    /**
     * Returns an specific field of some class. If definedIn is null, it searches for the field
     * named 'name' inside the class cls. If definedIn is different than null, tries to find the
     * specified field name in the specified class cls which should be defined in class
     * definedIn (either equals cls or a one of it's superclasses)
     * 
     * @param cls the class where the field is to be searched
     * @param name the field name
     * @param definedIn the superclass (or the class itself) of cls where the field was defined
     * @return the field itself
     */
    public Field field(Class cls, String name, Class definedIn) {
        Field field = fieldOrNull(cls,name,definedIn);
        if (field == null) {
            throw new NonExistentFieldException("No such field " + cls.getName() + "." + name,name);
        } else {
            return field;
        }
    }

    /**
     * Works like {@link #field(Class, String, Class)} but returns null instead of throwing exception.
     */
    public Field fieldOrNull(Class cls, String name, Class definedIn) {
        FieldDecorator d = fieldDecoratorOrNull(cls, name, definedIn);
        return d==null ? null : d.target;
    }

    public FieldDecorator fieldDecoratorOrNull(Class cls, String name, Class definedIn) {
        Map fields = buildMap(cls, definedIn != null);
        FieldDecorator field = (FieldDecorator)fields.get(definedIn != null ? (Object)new FieldKey(
            name, definedIn, 0) : (Object)name);
        return field;
    }

    private Map buildMap(final Class type, boolean tupleKeyed) {
        Class cls = type;
//        synchronized (this) {
            if (!cache.containsKey(type)) {
                final List<Class> superClasses = new ArrayList<Class>();
                while (!Object.class.equals(cls)) {
                    superClasses.add(0, cls);
                    cls = cls.getSuperclass();
                }
                Map<String,FieldDecorator> lastKeyedByFieldName = Collections.emptyMap();
                Map lastKeyedByFieldKey = Collections.emptyMap();
                for (Class superClass : superClasses) {
                    cls = superClass;
                    if (!cache.containsKey(cls)) {
                        final Map<String,FieldDecorator> keyedByFieldName = new HashMap<String,FieldDecorator>(lastKeyedByFieldName);
                        final Map<FieldKey,FieldDecorator> keyedByFieldKey = new OrderRetainingMap(lastKeyedByFieldKey);
                        Field[] fields = cls.getDeclaredFields();
                        if (JVM.reverseFieldDefinition()) {
                            for (int i = fields.length >> 1; i-- > 0;) {
                                final int idx = fields.length - i - 1;
                                final Field field = fields[i];
                                fields[i] = fields[idx];
                                fields[idx] = field;
                            }
                        }
                        for (int i = 0; i < fields.length; i++) {
                            Field field = fields[i];
                            FieldKey fieldKey = new FieldKey(field.getName(), field
                                    .getDeclaringClass(), i);
                            field.setAccessible(true);
                            FieldDecorator existent = keyedByFieldName.get(field.getName());
                            if (existent == null
                                    // do overwrite statics
                                    || ((existent.target.getModifiers() & Modifier.STATIC) != 0)
                                    // overwrite non-statics with non-statics only
                                    || ((field.getModifiers() & Modifier.STATIC) == 0)) {
                                keyedByFieldName.put(field.getName(), new FieldDecorator(field));
                            }
                            keyedByFieldKey.put(fieldKey, new FieldDecorator(field));
                        }
                        cache.put(cls, new Entry(keyedByFieldName, sorter.sort(type, keyedByFieldKey)));
                    }
                    Entry e = cache.get(cls);
                    lastKeyedByFieldName = e.keyedByFieldName;
                    lastKeyedByFieldKey = e.keyedByFieldKey;
                }
            }
//        }

        Entry e = cache.get(type);
        return tupleKeyed ? e.keyedByFieldKey : e.keyedByFieldName;

//        return (Map)(tupleKeyed ? keyedByFieldKeyCache.get(type) : keyedByFieldNameCache
//            .get(type));
    }

    protected Object readResolve() {
        init();
        return this;
    }

}
