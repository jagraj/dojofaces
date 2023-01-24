/*
 * Copyright 2010 Ganesh Jung
 * 
 * 2023 Jag Gangaraju & Volodymyr Siedlecki
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Author: Ganesh Jung (latest modification by $Author: ganeshpuri $)
 * Version: $Revision: 1.3 $ $Date: 2010/03/13 20:50:14 $
 */
package org.j4fry.dojo.converter;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.j4fry.json.JSONArray;
import org.j4fry.json.JSONException;
import org.j4fry.json.JSONObject;

import jakarta.el.ELContext;
import jakarta.el.ExpressionFactory;
import jakarta.el.ValueExpression;
import jakarta.faces.FacesException;
import jakarta.faces.application.Application;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.ConverterException;

/**
 * Base class for {@link StoreConverter} and {@link StoreUpdateConverter}
 * to handle processing of dataGrids content attribute
 */
public abstract class StoreConverterBase implements Converter {

	protected Map<String, ValueExpression> valueBindings = new HashMap<String, ValueExpression>();
	protected Map<String, String> valueBindingStrings = new HashMap<String, String>();
	protected Map<String, Map<String, String>> converterStrings = new HashMap<String, Map<String, String>>();
	protected Map<String, Converter> converters = new HashMap<String, Converter>();
	protected Map<String, String> childrenStrings = new HashMap<String, String>();
	protected Map<String, ValueExpression> children = new HashMap<String, ValueExpression>();

	private static Map<String, JSONObject> structureCache = new HashMap<String, JSONObject>();
	/**
	 * Determines the String definitions of valueBindings, converters and validators and stores them in Maps.
	 * 
	 * @param structure A JSON String defining the structure of the store  
	 * @param context The FacesContext
	 * @param valueBindingStrings The application can pass a Map that is filled with String definitions of valueBindings
	 * @param converterStrings The application can pass a Map that is filled with String definitions of converters
	 * @param validatorStrings The application can pass a Map that is filled with String definitions of validators
	 * @throws JSONException
	 */
	public static void tokenizeStructure(String structure, FacesContext context,  
    		Map<String, String> valueBindingStrings, 
    		Map<String, String> childrenStrings,
    		Map<String, Boolean> numeric,
    		Map<String, Map<String, String>> converterStrings,
    		Map<String, List<Map<String, Object>>> validatorStrings) throws JSONException {
		if (structure != null) {
			JSONObject jsonContent = structureCache.get(structure);
			if (jsonContent == null) {
				jsonContent = new JSONObject((String) structure);
				structureCache.put(structure, jsonContent);
			}
			for(Iterator<String> contentKeys = jsonContent.keys(); contentKeys.hasNext();) {
				String contentKey = contentKeys.next();
				Object column = jsonContent.get(contentKey);
				if (column instanceof JSONObject) {
					if (childrenStrings != null && ("true".equals(((JSONObject) column).opt("children"))
					|| Boolean.TRUE.equals(((JSONObject) column).opt("children")))) {
						childrenStrings.put(contentKey, (String) ((JSONObject) column).get("el"));
					} else if (valueBindingStrings != null) {
						valueBindingStrings.put(contentKey, (String) ((JSONObject) column).get("el"));
					}
					if (numeric != null && 
					("true".equals(((JSONObject) column).opt("numeric")) 
					|| Boolean.TRUE.equals(((JSONObject) column).opt("numeric"))) ) {
						numeric.put(contentKey, Boolean.TRUE);
					}
					if (converterStrings != null) {
						Object converter = ((JSONObject) column).opt("converter");
						if (converter != null) {
							Map<String, String> myConverterParams = new HashMap<String, String>();
							if (converter instanceof String) {
								myConverterParams.put("id",  (String) ((JSONObject) column).get("converter"));
							} else {
								Iterator it = ((JSONObject) converter).keys();
								while (it.hasNext()) {
									String attribute = (String) it.next();
									myConverterParams.put(attribute, (String) ((JSONObject) converter).get(attribute));						
								}
							}
							converterStrings.put(contentKey, myConverterParams);
						}
					}
					if (validatorStrings != null) {
						Object validators = ((JSONObject) column).opt("validators");
						if (validators != null) {
							List<Map<String, Object>> validatorList = new ArrayList<Map<String, Object>>();
							validatorStrings.put(contentKey, validatorList);
							if (validators instanceof JSONArray) {
								JSONArray validatorArray = (JSONArray) validators;
								for (int i = 0; i < validatorArray.length(); i++) {
									readValidator(validatorArray.get(i), validatorList);
								}
							} else {
								readValidator(validators, validatorList);
							}
						}
					}
				} else if (valueBindingStrings != null) {
					valueBindingStrings.put(contentKey, (String) column);
				}
			}
		}
	}
	
	private static void readValidator(Object validator, List<Map<String, Object>> validatorList) throws JSONException {
		Map<String, Object> myValidatorParams = new HashMap<String, Object>();
		if (validator instanceof String) {
			myValidatorParams.put("id",  (String) validator);
		} else {
			JSONObject validatorObject = (JSONObject) validator;
			for (Iterator<String> it = validatorObject.keys(); it.hasNext(); ) {
				String attribute = it.next();
				myValidatorParams.put(attribute, validatorObject.get(attribute));
			}
		}
		validatorList.add(myValidatorParams);
	}

	public static Converter determineConverter(FacesContext context, UIComponent component, String key,

			Map<String, Converter> converters,
			Map<String, Map<String, String>> converterStrings,
			Map<String, ValueExpression> valueBindings,
			Map<String, String> valueBindingStrings, boolean defaultDateConverter) {
		Application app = context.getApplication();

		ELContext elContext = context.getELContext();
		ExpressionFactory elFactory = app.getExpressionFactory();

		Converter converter = null;
    	if (converters.get(key) != null) {
    		converter = converters.get(key);
    	} else {
    		if (converterStrings.get(key) != null && converterStrings.get(key).get("id") != null){
    			try {
    				converter = context.getApplication().createConverter(converterStrings.get(key).get("id"));
    			} catch (FacesException e) {
   					throw new ConverterException(new FacesMessage("Unable to create converter " + converterStrings.get(key).get("id"), 
   							"Unable to create converter " + converterStrings.get(key).get("id") + " defined in your dojo DataGrid"));
    			}
        	} else {
        		if (valueBindings.get(key) == null) {
        			if (valueBindingStrings.get(key) != null) {
        				ValueExpression vb = elFactory.createValueExpression(elContext,valueBindingStrings.get(key),Object.class);
                		valueBindings.put(key, vb);
        			}
        		}
        		if (valueBindings.get(key) != null) {
	        		Class clazz = valueBindings.get(key).getType(elContext);
	        		if (clazz == java.lang.Number.class) {
	        			converter = context.getApplication().createConverter("jakarta.faces.Number");
	        		} else {
	        			converter = context.getApplication().createConverter(clazz);
	        		}
        		}
        	}
    	}
    	if (converter != null) {
    		converters.put(key, converter);
    		if (converterStrings.get(key) != null) {
    			// Add converter attributes if there are for this column
        		String lang = null;
        		String country = null;
        		for (String converterAttribute : converterStrings.get(key).keySet()) {
        			if ("lang".equals(converterAttribute)) {
        				lang = converterStrings.get(key).get("lang");
        				continue;
        			} else if ("country".equals(converterAttribute)) {
        				country = converterStrings.get(key).get("country");
        				continue;
        			} else if ("label".equals(converterAttribute)) {
        				continue;
        			} else if ("timeZone".equals(converterAttribute)) {
        				Method setter = null;
        				try {
    						setter = converter.getClass().getDeclaredMethod("setTimeZone", new Class[] {TimeZone.class});
    					} catch (Exception e) {
    						throw new IllegalArgumentException("unable to find setTimeZone for converter " + converter.getClass().getName(), e);
    					}
    					if (setter != null) {
    						try {
    							setter.invoke(converter, TimeZone.getTimeZone(converterStrings.get(key).get("timeZone")));
    						} catch (Exception e) {
    							throw new IllegalArgumentException("Exception invoking setLocale for converter " + converter.getClass().getName(), e);
    						}
    					}
        			} else if (!"id".equals(converterAttribute)) {
        				BeanInfo beanInfo;
						try {
							beanInfo = Introspector.getBeanInfo(converter.getClass());
						} catch (IntrospectionException e1) {
							throw new IllegalArgumentException("Intorspection into " + converter.getClass().getName() + " failed.", e1);
						}
        				PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
        				for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
        					if (propertyDescriptor.getName().equals(converterAttribute)) {
        						Method setter = propertyDescriptor.getWriteMethod();
        						Class paramType = setter.getParameterTypes()[0];
        						try {
        							setter.invoke(converter, StoreConverterBase.coerceTo(paramType, converterStrings.get(key).get(converterAttribute)));
        						} catch (IllegalAccessException e) {
        							throw new IllegalArgumentException("IllegalAccessException invoking setter for " + 
        									propertyDescriptor.getName() + " for validator " + converter.getClass().getName(), e);
        						} catch (InvocationTargetException e) {
        							throw new IllegalArgumentException("InvocationTargetException invoking setter for " + 
        									propertyDescriptor.getName() + " for validator " + converter.getClass().getName(), e);
        						} catch (ParseException e) {
        							throw new IllegalArgumentException("ParseException invoking setter for " + 
        									propertyDescriptor.getName() + " for validator " + converter.getClass().getName(), e);
        						}
        					}
        					
        				}
        			}
        		}
        		if (lang != null) {
    				Method setter = null;
    				try {
						setter = converter.getClass().getDeclaredMethod("setLocale", new Class[] {Locale.class});
					} catch (Exception e) {
						throw new IllegalArgumentException("unable to find setLocale for converter " + converter.getClass().getName(), e);
					}
					if (setter != null) {
						Locale myLocale = null;
						if (country == null) {
							myLocale = new Locale(lang);
						} else {
							myLocale = new Locale(lang, country);
						}
						try {
							setter.invoke(converter, myLocale);
						} catch (Exception e) {
							throw new IllegalArgumentException("Exception invoking setLocale for converter " + converter.getClass().getName(), e);
						}
					}
        		}
    		}
    	}
		return converter;
	}
	
	public static Object coerceTo(Class paramType, Object object) throws ParseException {
		if (paramType.isAssignableFrom(object.getClass())) {
			return object;
		} else if (Number.class.isAssignableFrom(paramType) || paramType == long.class || paramType == int.class
			|| paramType == double.class || paramType == float.class || paramType == short.class || paramType == byte.class) {
			if (object instanceof Number) {
				Number myNumber = (Number) object;
				if (paramType.isAssignableFrom(Number.class)) {
					return myNumber;
				} else if (paramType.isAssignableFrom(Long.class) || paramType == long.class) {
					return myNumber.longValue();
				} else if (paramType.isAssignableFrom(Integer.class) || paramType == int.class) {
					return myNumber.intValue();
				} else if (paramType.isAssignableFrom(Double.class) || paramType == double.class) {
					return myNumber.doubleValue();
				} else if (paramType.isAssignableFrom(Float.class) || paramType == float.class) {
					return myNumber.floatValue();
				} else if (paramType.isAssignableFrom(Short.class) || paramType == short.class) {
					return myNumber.shortValue();
				} else if (paramType.isAssignableFrom(Byte.class) || paramType == byte.class) {
					return myNumber.byteValue();
				} else {
					throw new IllegalArgumentException("cannot process validator argument class " + paramType.getName());
				}
			} else if (object instanceof String) {
				NumberFormat nf = NumberFormat.getInstance(new Locale("en", "us"));
				return nf.parse((String) object);
			}
		}
		return null;
	}
	
}
