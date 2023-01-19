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
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.ConverterException;
import jakarta.faces.el.ValueBinding;

import org.j4fry.json.JSFJSONObject;
import org.j4fry.json.JSFJSONTokener;
import org.j4fry.json.JSONArray;
import org.j4fry.json.JSONException;
import org.j4fry.json.JSONObject;

/**
 * Converts a JSON String that represents the changes that where made in 
 * the dataStore into a JSONObject.
 * Converters that where set within a dataStore's structure attribute
 * are used to do the conversion.
 * 
 * For validation roundtrips the getAsString() method can convert the
 * JSONObject back to a JSON String. In case of a validation roundtrip
 * the changes must be restored into the dataStore using JavaScript.
 * 
 * {@link org.j4fry.dojo.beans.StoreUpdateMap} for responsible for 
 * writing the object Map to the model.
 */
public class StoreUpdateConverter extends StoreConverterBase {

	public Object getAsObject(FacesContext context, UIComponent component, String update) throws ConverterException {
		try {
			// only start if there is something to update
			if (update != null && update instanceof String 
			&& ((String) update).length() > 0 && ((String) update).startsWith("[")) {
				// transform json changes into HashMap
				Map attributes = component.getAttributes();
				String structure = (String) attributes.get("structure");
				String var = (String) attributes.get("var");
				String modelClassName = (String) attributes.get("modelClass");
				Collection items = (Collection) attributes.get("items");
		    	tokenizeStructure(structure, context, valueBindingStrings, null, null, converterStrings, null);
		    	
		    	// initialize the itemVb with either the modelClassName or with the first element of the List
		    	// to enable converter type lookup for columns where a converter is not defined
		    	ValueBinding itemVb = context.getApplication().createValueBinding("#{" + var + "}");
				if (modelClassName != null) {
					itemVb.setValue(context, Class.forName(modelClassName).getConstructor(null).newInstance(null));
				} else if (items != null && items.size() > 0) {
					itemVb.setValue(context, items.iterator().next());
				}
				
		    	// convert the String to a JSONObject 
				return new JSONArray(new JSFJSONTokener(context, component, structure, update, valueBindingStrings,
							converterStrings, valueBindings, converters));
			} else {
				return null;
			}
		} catch (Throwable t) {
			t.printStackTrace(System.out);
			if (t instanceof ConverterException) {
				throw (ConverterException) t;
			} else {
				throw new ConverterException(t.getMessage(), t);
			}
		}
	}

	public String getAsString(FacesContext context, UIComponent component, Object value) {
		if (value != null && value instanceof JSONArray) {
			return value.toString();
		} else {
			return null;
		}
	}
}
