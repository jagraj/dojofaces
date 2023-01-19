/*
 * Copyright 2009 Ganesh Jung
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
package org.j4fry.json;

import java.util.Map;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.el.ValueBinding;

import org.j4fry.dojo.converter.StoreConverterBase;

/**
 * Converts a JSON String to a Java object while using a given set of JSF converters
 */
public class JSFJSONObject extends JSONObject {

    public JSFJSONObject(FacesContext context, UIComponent component,
    		String content, String changedContent, Map<String, String> valueBindingStrings,
    		Map<String, Map<String, String>> converterStrings, Map<String, ValueBinding> valueBindings,
    		Map<String, Converter> converters) throws JSONException {
        this(new JSFJSONTokener(context, component, content, changedContent, valueBindingStrings,
				converterStrings, valueBindings, converters), context, component, valueBindingStrings,
				converterStrings, valueBindings, converters);
    }


	/**
     * Construct a JSFJSONObject from a JSFJSONTokener.
     * Convert each leaf using JSF mechanisms.
     * @param x A JSFJSONTokener object containing the source string.
     * @throws JSONException If there is a syntax error in the source string 
     *  or a duplicate key.
     */
    public JSFJSONObject(JSFJSONTokener x, FacesContext context, UIComponent component, 
    		Map<String, String> valueBindingStrings,
    		Map<String, Map<String, String>> converterStrings, Map<String, ValueBinding> valueBindings,
    		Map<String, Converter> converters) throws JSONException {
        super();
        char c;
        String key;

        if (x.nextClean() != '{') {
            throw x.syntaxError("A JSONObject text must begin with '{'");
        }
        for (;;) {
            c = x.nextClean();
            switch (c) {
            case 0:
                throw x.syntaxError("A JSONObject text must end with '}'");
            case '}':
                return;
            default:
                x.back();
                key = x.nextValue().toString();
            }
            /*
             * The key is followed by ':'. We will also tolerate '=' or '=>'.
             */

            c = x.nextClean();
            if (c == '=') {
                if (x.next() != '>') {
                    x.back();
                }
            } else if (c != ':') {
                throw x.syntaxError("Expected a ':' after a key");
            }
            Object nextValue = x.nextValue();
            if (nextValue instanceof JSONArray 
            && ((JSONArray) nextValue).length() > 0 
            && ((JSONArray) nextValue).get(0) instanceof String) {
            	nextValue = ((JSONArray) nextValue).get(0);
            }
            if (nextValue instanceof String) {
            	// reached leaf, do conversion
            	Converter converter = StoreConverterBase.determineConverter(context, component, key, converters, converterStrings, 
            			valueBindings, valueBindingStrings, true);
            	if (converter != null) {
            		String previousLabel = (String) component.getAttributes().get("label");
            		String newLabel = null;
            		if (converterStrings.get(key) != null && converterStrings.get(key).get("label") != null) {
            			newLabel = converterStrings.get(key).get("label");
            			component.getAttributes().put("label", newLabel);
            		}
            		
                	nextValue = converter.getAsObject(context, component, (String) nextValue);
                	if (previousLabel == null) {
                		if (newLabel != null) {
                			component.getAttributes().remove("label");
                		}
                	} else {
            			component.getAttributes().put("label", previousLabel);
                	}
            	}
            }
            map.put(key, nextValue);

            /*
             * Pairs are separated by ','. We will also tolerate ';'.
             */

            switch (x.nextClean()) {
            case ';':
            case ',':
                if (x.nextClean() == '}') {
                    return;
                }
                x.back();
                break;
            case '}':
                return;
            default:
                throw x.syntaxError("Expected a ',' or '}'");
            }
        }
    }
}
