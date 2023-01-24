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
 * Version: $Revision: 1.3 $ $Date: 2010/04/06 07:22:34 $
 */
package org.j4fry.dojo.beans;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.j4fry.dojo.converter.StoreUpdateConverter;

import jakarta.el.ELContext;
import jakarta.el.ExpressionFactory;
import jakarta.el.ValueExpression;
import jakarta.faces.application.Application;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;

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
			Application app = context.getApplication();

			ELContext elContext = context.getELContext();
			ExpressionFactory elFactory = app.getExpressionFactory();

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
				ValueExpression keyValueExpression =elFactory.createValueExpression(elContext,"#{" + keyExpression + "}",String.class);
				keyValueExpression.setValue(elContext, keyValue);
			}
	
			if(valueExpression != null && !"".equals(valueExpression)) {
				List list = (List) storeHidden.getAttributes().get("items");
				if(list != null) {
					ValueExpression vbItem = elFactory.createValueExpression(elContext,"#{" + storeHidden.getAttributes().get("var") + "}",String.class);
					ValueExpression vbKey = elFactory.createValueExpression(elContext,valueBindingStrings.get(storeHidden.getAttributes().get("key")),String.class);
					boolean valueSet = false;
					for(Object o : list) {
						vbItem.setValue(elContext, o);
						Object id = vbKey.getValue(elContext);
						if(id != null && keyValue.equals(id.toString())) {
							elFactory.createValueExpression(elContext,"#{" + valueExpression + "}",String.class).setValue(elContext, o);
							valueSet = true;
							break;
						}
					}
					if(!valueSet)
						throw new IllegalArgumentException("Cannot set value " + keyValue +
							" into expression " + valueExpression + "(" + valueBindingStrings.get(storeHidden.getAttributes().get("key")) + ")");
					vbItem.setValue(elContext, null);
				}
			}
		}
		return null;
	}
}