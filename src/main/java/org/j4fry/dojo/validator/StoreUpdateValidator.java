/*
 * Copyright 2010 Ganesh Jung
 * 
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
package org.j4fry.dojo.validator;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.faces.FacesException;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.ConverterException;
import jakarta.faces.el.ValueBinding;
import jakarta.faces.validator.Validator;
import jakarta.faces.validator.ValidatorException;

import org.j4fry.dojo.converter.StoreConverterBase;
import org.j4fry.json.JSFJSONObject;
import org.j4fry.json.JSONArray;
import org.j4fry.json.JSONException;
import org.j4fry.json.JSONObject;

/**
 * This validator is used on the hidden updates of a dataStore
 * It validates store updates by callings validators defined in the structure attribute 
 */
public class StoreUpdateValidator implements Validator {

	/**
	 * @param context The FacesContext
	 * @param component The hidden input that contains the store's updates
	 * @param value The JSON Object with the updates
	 */
	public void validate(FacesContext context, UIComponent component, Object value)
			throws ValidatorException {
		if (value == null) return;
		Map componentAttributes = component.getAttributes();
		String structure = (String) componentAttributes.get("structure");
		Map<String, List<Map<String, Object>>> validatorStrings = new HashMap<String, List<Map<String, Object>>>();
		Map<String, List<Validator>> validators = new HashMap<String, List<Validator>>();
		try {
			// determine the defined validators
			StoreConverterBase.tokenizeStructure(structure, context, null, null, null, null, validatorStrings);
			// invoke recursive validation
			validate(context, component, null, value, validatorStrings, validators, 0);
		} catch (Throwable t) {
			if (t instanceof ValidatorException) {
				throw (ValidatorException) t;
			} else {
				throw new ValidatorException(new FacesMessage(t.getMessage()), t);
			}
		}
		
	}

	/**
	 * Recursively step through the tree and perform validation on the leafs
	 * Only start validation from depth 3, the first two levels contain an Array of
	 * all updates and the update type.
	 * 
	 * @param context The FacesContext
	 * @param component The hidden input that contains the store's updates
	 * @param key The key of the JSON object to validate
	 * @param value The JSON object to validate
	 * @param validatorStrings Definition of the validators defined in the store's structure
	 * @param validators Instances of the validators
	 * @param depth Recursion level
	 * @throws JSONException
	 */
	private void validate(FacesContext context, UIComponent component,
			String key, Object value,
			Map<String, List<Map<String, Object>>> validatorStrings,
			Map<String, List<Validator>> validators, int depth) throws JSONException {
		depth++;
		if (value instanceof JSONObject) {
			// simply recurse
			JSONObject jsonValue = (JSONObject) value;
			for (Iterator valueKeys = jsonValue.keys(); valueKeys.hasNext(); ) {
				key = (String) valueKeys.next();
				validate(context, component, key, jsonValue.opt(key), validatorStrings, validators, depth);
			}
		} else if (value instanceof JSONArray) {
			// simply recurse
			JSONArray jsonValue = (JSONArray) value;
			for (int i = 0; i < jsonValue.length(); i++) {
				// use the same key again, because leaf data are encapsulated in one-element-arrays
				validate(context, component, key, jsonValue.get(i), validatorStrings, validators, depth);
			}
		} else if (depth > 2) {
			List<Validator> myValidators = determineValidators(context, component, key, 
					validatorStrings, validators);
			if (myValidators != null) {
				for (int i = 0; i < myValidators.size(); i++) {
					Validator validator = myValidators.get(i);
					Map<String, Object> validatorParams = validatorStrings.get(key).get(i);
					if (validator != null) {
						String previousLabel = (String) component.getAttributes().get("label");
						String newLabel = null;
						if (validatorParams != null && validatorParams.get("label") != null) {
							newLabel = (String) validatorParams.get("label");
							component.getAttributes().put("label", newLabel);
						}
						validator.validate(context, component, value);
						if (previousLabel == null) {
							if (newLabel != null) {
								component.getAttributes().remove("label");
							}
						} else {
							component.getAttributes().put("label", newLabel);
						}
					}
				}
			}
		}
	}

	private List<Validator> determineValidators(FacesContext context, UIComponent component, String attribute,
			Map<String, List<Map<String, Object>>> validatorStrings,
			Map<String, List<Validator>> validators) {
		List<Validator> myValidators = validators.get(attribute);
		if (myValidators == null) {
			List<Map<String, Object>> validatorMaps = validatorStrings.get(attribute);
			if (validatorMaps != null) {
				myValidators = new ArrayList<Validator>();
				validators.put(attribute, myValidators);
				for (Map<String, Object> validatorMap : validatorMaps) {
					Validator validator = null;
		    		if (validatorMap != null && validatorMap.get("id") != null){
		    			try {
		    				validator = context.getApplication().createValidator((String) validatorMap.get("id"));
		    			} catch (FacesException e) {
		   					throw new ValidatorException(new FacesMessage("Unable to create validator " + validatorMap.get("id"), 
		   							"Unable to create validator " + validatorMap.get("id") + " defined in your dojo DataGrid"));
		    			}
		    		}
		    		if (validator != null) {
		    			myValidators.add(validator);
		        		for (String validatorAttribute : validatorMap.keySet()) {
		        			if ("label".equals(validatorAttribute)) {
		        				continue;
		        			} else if (!"id".equals(validatorAttribute)) {
		        				BeanInfo beanInfo;
								try {
									beanInfo = Introspector.getBeanInfo(validator.getClass());
								} catch (IntrospectionException e1) {
									throw new IllegalArgumentException("Intorspection into " + validator.getClass().getName() + " failed.", e1);
								}
		        				PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
		        				for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
		        					if (propertyDescriptor.getName().equals(validatorAttribute)) {
		        						Method setter = propertyDescriptor.getWriteMethod();
		        						Class paramType = setter.getParameterTypes()[0];
		        						try {
		        							setter.invoke(validator, StoreConverterBase.coerceTo(paramType, validatorMap.get(validatorAttribute)));
		        						} catch (IllegalAccessException e) {
		        							throw new IllegalArgumentException("IllegalAccessException invoking setter for " + propertyDescriptor.getName() + " for validator " + validator.getClass().getName(), e);
		        						} catch (InvocationTargetException e) {
		        							throw new IllegalArgumentException("InvocationTargetException invoking setter for " + propertyDescriptor.getName() + " for validator " + validator.getClass().getName(), e);
		        						} catch (ParseException e) {
		        							throw new IllegalArgumentException("ParseException invoking setter for " + propertyDescriptor.getName() + " for validator " + validator.getClass().getName(), e);
		        						}
		        					}
		        					
		        				}
		        			}
		        		}
		    		}
				}
			}
		}
		return validators.get(attribute);
	}
}
