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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.el.ValueBinding;


/**
 * Works together with {@link JSFJSONObject} to convert a JSON String to a 
 * Java object while using a given set of JSF converters
 */
public class JSFJSONTokener extends JSONTokener {

	private Map<String, ValueBinding> valueBindings = new HashMap<String, ValueBinding>();
	private Map<String, String> valueBindingStrings = new HashMap<String, String>();
	private Map<String, Map<String, String>> converterStrings = new HashMap<String, Map<String, String>>();
	private Map<String, Converter> converters = new HashMap<String, Converter>();
	private FacesContext context; 
	private UIComponent component;

	public JSFJSONTokener(FacesContext context, UIComponent component,
    		String content, String changedContent,	Map<String, String> valueBindingStrings,
    		Map<String, Map<String, String>> converterStrings, Map<String, ValueBinding> valueBindings,
    		Map<String, Converter> converters) throws JSONException {
		super(changedContent);
		this.valueBindingStrings = valueBindingStrings;
		this.valueBindings = valueBindings;
		this.converterStrings = converterStrings;
		this.converters = converters;
		this.context = context;
		this.component = component;
	}

	/**
     * Get the next value. The value can be a Boolean, Double, Integer,
     * JSONArray, JSONObject, Long, or String, or the JSONObject.NULL object.
     * @throws JSONException If syntax error.
     *
     * @return An object.
     */
    public Object nextValue() throws JSONException {
        char c = nextClean();
        String s;

        switch (c) {
            case '"':
            case '\'':
                return nextString(c);
            case '{':
                back();
                return new JSFJSONObject(this, context, component, valueBindingStrings, converterStrings, valueBindings, converters);
            case '[':
            case '(':
                back();
                return new JSONArray(this);
        }

        /*
         * Handle unquoted text. This could be the values true, false, or
         * null, or it can be a number. An implementation (such as this one)
         * is allowed to also accept non-standard forms.
         *
         * Accumulate characters until we reach the end of the text or a
         * formatting character.
         */

        StringBuffer sb = new StringBuffer();
        while (c >= ' ' && ",:]}/\\\"[{;=#".indexOf(c) < 0) {
            sb.append(c);
            c = next();
        }
        back();

        s = sb.toString().trim();
        if (s.equals("")) {
            throw syntaxError("Missing value");
        }
        if ("undefined".equals(s)) return null;
        if (s.startsWith("new Date(") && s.endsWith(")") && s.length() == 23) {
        	return new Date(Long.parseLong(s.substring(9, 22)));
        }
        return s;
    }
}
