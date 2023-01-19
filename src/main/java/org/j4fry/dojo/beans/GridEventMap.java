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
 * Version: $Revision: 1.3 $ $Date: 2010/04/06 07:22:34 $
 */
package org.j4fry.dojo.beans;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.el.ValueBinding;

import org.j4fry.dojo.converter.StoreUpdateConverter;

public class GridEventMap extends MapAdapter {

	private String valueExpression;
	private String keyExpression;
	private boolean isSetValueExpression;
	private boolean isSetKeyExpression;

	/**
	 * set params through EL Map syntax
	 */
	public Object get(Object o) {
		if (!isSetKeyExpression) {
			keyExpression = (String) o;
			isSetKeyExpression = true;
			return this;
		} else if (!isSetValueExpression) {
			valueExpression = (String) o;
			isSetValueExpression = true;
			return this;
		} else {
			return null;
		}
	}

	/**
	 * do the actual model update
	 */
	public Object put (Object gridId, Object keyValue) {
		if (keyValue != null && keyValue instanceof String && ((String) keyValue).length() > 0) {
			FacesContext context = FacesContext.getCurrentInstance();
	
			UIComponent storeHidden = context.getViewRoot().findComponent(DojoHelper.get().getClientId().get(gridId+"_store"));
			// tokenize structure
			Map<String, String> valueBindingStrings = new HashMap<String, String>();
			Map<String, String> childrenStrings = new HashMap<String, String>();
			try {
				StoreUpdateConverter.tokenizeStructure((String) storeHidden.getAttributes().get("structure"), context, 
						valueBindingStrings, childrenStrings, null, null, null);
			} catch (Throwable t) {
				context.addMessage(null, new FacesMessage(t.getMessage()));
				return null;
			}
	
			if(keyExpression != null && !"".equals(keyExpression)) {
				context.getApplication().createValueBinding("#{" + keyExpression + "}").setValue(context, keyValue);
			}
	
			if(valueExpression != null && !"".equals(valueExpression)) {
				List list = (List) storeHidden.getAttributes().get("items");
				if(list != null) {
					ValueBinding vbItem = context.getApplication().createValueBinding("#{" + storeHidden.getAttributes().get("var") + "}");
					ValueBinding vbKey = context.getApplication().createValueBinding(valueBindingStrings.get(storeHidden.getAttributes().get("key")));
					boolean valueSet = false;
					for(Object o : list) {
						vbItem.setValue(context, o);
						Object id = vbKey.getValue(context);
						if(id != null && keyValue.equals(id.toString())) {
							context.getApplication().createValueBinding("#{" + valueExpression + "}").setValue(context, o);
							valueSet = true;
							break;
						}
					}
					if(!valueSet)
						throw new IllegalArgumentException("Cannot set value " + keyValue +
							" into expression " + valueExpression + "(" + valueBindingStrings.get(storeHidden.getAttributes().get("key")) + ")");
					vbItem.setValue(context, null);
				}
			}
		}
		return null;
	}
}