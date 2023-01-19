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
package org.j4fry.dojo.beans;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;

/**
 * transforms a list to a Map with params List list, String var, String key
 */
public class ListToMapMap extends MapAdapter {
	private List<Object> list;
	private String var;
	private String key;

	
	public Object get(Object o) {
		if (list == null) {
			list = (List<Object>) o;
			return this;
		} else if (var == null) {
			var = (String) o;
			return this;
		} else if (key == null) {
			key = (String) o;
			return this;
		} else {
			FacesContext context = FacesContext.getCurrentInstance();
			ValueBinding varVb = context.getApplication().createValueBinding("#{" + var + "}");
			ValueBinding keyVb = context.getApplication().createValueBinding(key);
			ValueBinding valueVb = context.getApplication().createValueBinding((String) o);
			Map<String, String> result = new LinkedHashMap<String, String>();
			for (Object element : list) {
				varVb.setValue(context, element);
				Object key = keyVb.getValue(context);
				Object value =  valueVb.getValue(context);
				result.put(key == null ? "" : key.toString(), value == null ? null : value.toString()); 
			}
			return result;
		}
	}
}