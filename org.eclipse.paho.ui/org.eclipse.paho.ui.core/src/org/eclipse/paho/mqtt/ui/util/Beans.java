/*******************************************************************************
 * Copyright (c) 2013 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution. 
 *
 * The Eclipse Public License is available at 
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *    Bin Zhang - initial API and implementation and/or initial documentation
 *******************************************************************************/
package org.eclipse.paho.mqtt.ui.util;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Java Bean helper
 * 
 * @author Bin Zhang
 */
public final class Beans {

	/**
	 * Returns the contents of the given property for the given bean.
	 * 
	 * @param bean the source bean
	 * @param descriptor the property to retrieve
	 * @return the contents of the given property for the given bean.
	 */
	public static Object readProperty(Object bean, PropertyDescriptor descriptor) {
		try {
			Method readMethod = descriptor.getReadMethod();
			if (readMethod == null) {
				throw new IllegalArgumentException(String.format(
						"%s property does not have a read method.", descriptor.getName())); //$NON-NLS-1$
			}
			if (!readMethod.isAccessible()) {
				readMethod.setAccessible(true);
			}
			return readMethod.invoke(bean);
		}
		catch (InvocationTargetException e) {
			throw new RuntimeException(e.getCause());
		}
		catch (Exception e) {
			// ignore
		}

		return null;
	}

	/**
	 * Sets the contents of the given property on the given source object to the given value.
	 * 
	 * @param bean the source object which has the property being updated
	 * @param descriptor the property being changed
	 * @param value the new value of the property
	 */
	public static void writeProperty(Object bean, PropertyDescriptor descriptor, Object value) {
		try {
			Method writeMethod = descriptor.getWriteMethod();
			if (null == writeMethod) {
				throw new IllegalArgumentException(String.format(
						"Missing public setter method for %s property", descriptor.getName())); //$NON-NLS-1$
			}
			if (!writeMethod.isAccessible()) {
				writeMethod.setAccessible(true);
			}
			writeMethod.invoke(bean, new Object[] { value });
		}
		catch (InvocationTargetException e) {
			throw new RuntimeException(e.getCause());
		}
		catch (Exception e) {
			// ignore
		}
	}

	/**
	 * @param beanClass
	 */
	public static Map<String, PropertyDescriptor> introspect(Class<?> beanClass) {
		Map<String, PropertyDescriptor> mappings = new HashMap<String, PropertyDescriptor>();
		try {
			BeanInfo beanInfo = Introspector.getBeanInfo(beanClass);
			PropertyDescriptor[] descriptors = beanInfo.getPropertyDescriptors();
			for (PropertyDescriptor descriptor : descriptors) {
				String prop = descriptor.getName();
				if (prop.equals("class")) { //$NON-NLS-1$
					continue;
				}
				mappings.put(prop, descriptor);
			}
		}
		catch (IntrospectionException e) {
			// cannot introspect, give up
		}

		return mappings;
	}

}
