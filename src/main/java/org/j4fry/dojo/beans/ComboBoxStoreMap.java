/*
 * Copyright 2009 Ganesh Jung
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
package org.j4fry.dojo.beans;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.j4fry.json.JSONObject;

import jakarta.el.ELContext;
import jakarta.el.ExpressionFactory;
import jakarta.el.ValueExpression;
import jakarta.faces.application.Application;
import jakarta.faces.context.FacesContext;

/**
* Create a JSON store from a server side list with the params List list, String var, String key
* @see DojoHelper#getComboBoxStore()
*/
public class ComboBoxStoreMap extends MapAdapter {
	
	private List<Object> list;
	private String var;
	private String key;
	private boolean setList;
	private boolean setVar;

	public Object get(Object o) {
		if (!setList) {
			if (o instanceof List) {
				list = (List<Object>) o;
			}
			setList = true;
			return this;
		} else if (!setVar) {
			setVar = true;
			var = (String) o;
			return this;
		} else {
			key = (String) o;
			FacesContext context = FacesContext.getCurrentInstance();
			Application app = context.getApplication();

			ELContext elContext = context.getELContext();
			ExpressionFactory elFactory = app.getExpressionFactory();


	    	ValueExpression itemVb = elFactory.createValueExpression(elContext,"#{" + var + "}",String.class);
	    	String expression = "dojofaces_key_undefined".equals(key) ? var : key;
	    	ValueExpression	keyVb = elFactory.createValueExpression(elContext,expression,String.class);
	    		
			
			StringBuffer result = new StringBuffer();
			result.append("[");
			boolean start = true;
			if (list != null) {
				for (Object item : list) {
					itemVb.setValue(elContext, item);
					if (start) {
						start = false;  
					} else {
						result.append(",");
					}
					Map<String, Object> jsonContent = new HashMap<String, Object>();
					jsonContent.put("name", keyVb.getValue(elContext));
					result.append(new JSONObject(jsonContent).toString());
				}
			}
			result.append("]");
			return result.toString();
		}
	}
}