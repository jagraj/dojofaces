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
package org.j4fry.dojo.converter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.el.ValueBinding;

import org.j4fry.json.JSONArray;
import org.j4fry.json.JSONException;
import org.j4fry.json.JSONObject;

/**
 * This converter converts a Collection of Objects to a JSON String based on the dataStore's structure.
 * If the Collection is recursive and the structure references the recursive attribute the converter
 * will follow the recursion and produce a nested JSON String.   
 * To form the JSON String converters from the dataStore's structure are used to convert Java Objects.
 * {@link org.j4fry.dojo.beans.StoreMap} reads to Collection and avoids writing anything back to
 * the model. Changes in the store are reflected via a second field in the store using
 * {@link org.j4fry.dojo.beans.StoreUpdateMap} and {@link org.j4fry.dojo.converter.StoreUpdateConverter}.
 * In case of validation roundtrips the store String in stored in the submitted value and returned to the GUI.
 */
public class StoreConverter extends StoreConverterBase {

	protected Map<String, Boolean> numeric = new HashMap<String, Boolean>();

	public Object getAsObject(FacesContext context, UIComponent component, String store) throws ConverterException {
		return component.getAttributes().get("items");
	}

	public String getAsString(FacesContext context, UIComponent component, Object value) {
		if (value == null || value instanceof String) return "[]";
		Collection list = (Collection) value;
		// The attributes are nested in the fields to allow converter access
		Map attributes = component.getAttributes();
		String structure = (String) attributes.get("structure");
		String var = (String) attributes.get("var");
		ValueBinding itemVb = context.getApplication().createValueBinding("#{" + var + "}");
    	try {
    		// determine the String definitions of valueBindings and converters and store them in Maps 
			tokenizeStructure(structure, context, valueBindingStrings, childrenStrings, numeric, converterStrings, null);
			// create valueBindings from the previously determined Strings
			Iterator<String> childrenIterator = childrenStrings.keySet().iterator();
			while (childrenIterator.hasNext()) {
				String key = childrenIterator.next();
				children.put(key, context.getApplication().createValueBinding(childrenStrings.get(key)));
			}
			Iterator<String> vbIterator = valueBindingStrings.keySet().iterator();
			while (vbIterator.hasNext()) {
				String key = vbIterator.next();
				valueBindings.put(key, context.getApplication().createValueBinding(valueBindingStrings.get(key)));
			}
			// Now build the result String
			String result = buildJSON(list, itemVb, context, component).toString();
			return result;
		} catch (JSONException e) {
			throw new ConverterException(e.getMessage(), e);
		}
	}

	private JSONArray buildJSON(Collection list, ValueBinding itemVb, FacesContext context, UIComponent component) throws JSONException {
		JSONArray result = new JSONArray();
		boolean start = true;
		for (Object item : list) {
			// put the next item into the valueBinding defined by "var"
			itemVb.setValue(context, item);
			
			// prepare a Map for the JSON Object
			JSONObject jsonContent = new JSONObject();
			
			// iterate over the structure'S valueBindings to fill the Object
			Iterator<String> jsonIt = valueBindings.keySet().iterator();
			while (jsonIt.hasNext()) {
				String jsonKey = jsonIt.next();
				Object jsonValue = valueBindings.get(jsonKey).getValue(context);
				// Only look for a converter for non null values. Booleans are converted by JSONObject's toString method.
				if (jsonValue != null && !(jsonValue instanceof Boolean) 
				&& !(Boolean.TRUE.equals(numeric.get(jsonKey)))) {
	            	Converter converter = StoreConverterBase.determineConverter(context, component, 
	            			jsonKey, converters, converterStrings, valueBindings, valueBindingStrings, false);
	            	if (converter != null) {
	            		jsonValue = converter.getAsString(context, component, jsonValue);
	            	}
				}
				jsonContent.put(jsonKey, jsonValue);
			}
			
			// recurse for children
			Iterator<String> childrenIt = children.keySet().iterator();
			while (childrenIt.hasNext()) {
				String childrenKey = childrenIt.next();
				Object childList = children.get(childrenKey).getValue(context);
				if (childList instanceof Collection) {
					jsonContent.put(childrenKey, buildJSON((Collection) childList, itemVb, context, component));
				}
			}
			
			result.put(jsonContent);
		}
		return result;
	}
}
