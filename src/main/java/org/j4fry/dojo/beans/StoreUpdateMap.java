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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jakarta.faces.application.Application;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.ExpressionFactory;
import jakarta.el.PropertyNotFoundException;
import jakarta.el.ValueExpression;



import org.j4fry.dojo.converter.StoreUpdateConverter;
import org.j4fry.json.JSONArray;
import org.j4fry.json.JSONException;
import org.j4fry.json.JSONObject;

/**
* Apply the changes that come from a dataStore via 
* {@link org.j4fry.dojo.converter.StoreUpdateConverter} through a JSONObject 
* to the server model. 
* 
* Items inserted into the dataStore are only inserted into the model if either
* the method binding onInsert is set and returns the new item or modelClass 
* is set so the new class to be instantiated is known. If both are set onInsert 
* can be used for validation and modelClass is used to create the new item.
* 
* Store updates are automatically propagated to the model. If this behaviour is
* not required set autoUpdate to false. Pre update validation and specialized
* update logic can be achieved with the onUpdate method binding. 
* 
* Items deleted form the store are automatically deleted from the model. If this 
* behaviour is not requested set autoDelete to false. Pre delete validation and 
* specialized delete logic can be achieved with the onDelete method binding.
* 
* @see DojoHelper#getDataGridContent()
*/
public class StoreUpdateMap extends StoreMap {
	
	private List<Object> list;
	private String var;
	private String key;
	private String onInsert;
	private String onUpdate;
	private String onDelete;
	private String modelClass;
	private String autoUpdate;
	private String autoDelete;
	private boolean setList;
	private boolean setVar;
	private boolean setKey;
	private boolean setOnInsert;
	private boolean setOnUpdate;
	private boolean setOnDelete;
	private boolean setModelClass;
	private boolean setAutoUpdate;
	private boolean setAutoDelete;

	/**
	 * set params through EL Map syntax
	 */
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
		} else if (!setKey) {
			key = (String) o;
			setKey = true;
			return this;
		} else if(!setOnInsert) {
			onInsert = (String) o;
			setOnInsert = true;
			return this;
		} else if(!setModelClass) {
			modelClass = (String) o;
			setModelClass = true;
			return this;
		} else if(!setOnUpdate) {
			onUpdate = (String) o;
			setOnUpdate = true;
			return this;
		} else if(!setAutoUpdate) {
			autoUpdate = (String) o;
			setAutoUpdate = true;
			return this;
		} else if(!setOnDelete) {
			onDelete = (String) o;
			setOnDelete = true;
			return this;
		} else if(!setAutoDelete) {
			autoDelete = (String) o;
			setAutoDelete = true;
			return this;
		} else {
			return null;
		}
	}
	
	/**
	 * do the actual model update
	 */
	public Object put (Object structure, Object updates) {
		if (updates == null || !(updates instanceof JSONArray)) return null;
		FacesContext context = FacesContext.getCurrentInstance();
		Application app = context.getApplication();

		ELContext elContext = context.getELContext();
		ExpressionFactory elFactory = app.getExpressionFactory();
		
		try {
			String structureString = ((String) structure).replace("__dojoFacelets_emtpyKey__", "#{" + var + "}");
			
			// tokenize structure
			Map<String, String> valueBindingStrings = new HashMap<String, String>();
			Map<String, String> childrenStrings = new HashMap<String, String>();
	    	StoreUpdateConverter.tokenizeStructure(structureString, context, valueBindingStrings, childrenStrings, null, null, null);

	    	// cache valueBindings once they are created
	    	Map<String, ValueExpression> valueBindings = new HashMap<String, ValueExpression>();
			Map<String, ValueExpression> children = new HashMap<String, ValueExpression>();

			ValueExpression itemVb = elFactory.createValueExpression(elContext,"#{" + var + "}",String.class);
			for(int i = 0; i < ((JSONArray)updates).length(); i++) {
				JSONObject update = (JSONObject) ((JSONArray)updates).get(i);
				
				// process insert
				JSONObject insertObject = (JSONObject) update.opt("insert");
				if (insertObject != null) {
					Object newItem = null;
					if (isSetOnInsert()) {
						// invoke insert callback
						newItem = MethodInvokator.getInstance(onInsert, context).invoke(insertObject);
					}
					if (isSetModelClass()) {
						// construct new item
						newItem = setItem(context, insertObject, valueBindingStrings, valueBindings, childrenStrings, children, 
								Class.forName(modelClass), itemVb, Class.forName(modelClass).getConstructor().newInstance());
					}
					if (list == null) throw new IllegalArgumentException("Please set the target list before dropping items on it.");
					if (newItem != null) list.add(newItem);
				}
				
				// process set
				JSONObject updateObject = (JSONObject) update.opt("set");
				if (updateObject != null) {
					for (Iterator updateIt = ((JSONObject) update.opt("set")).keys(); updateIt.hasNext(); ) {
						String updateKey = (String) updateIt.next();
						JSONObject updateAttributes = (JSONObject) ((JSONObject) update.opt("set")).get(updateKey);
						Object oldItem = getItem(context, updateKey, itemVb, valueBindingStrings, valueBindings, childrenStrings, children, list); 
						if(isSetOnUpdate()) {
							// invoke set callback
							MethodInvokator.getInstance(onUpdate, context).invoke(oldItem, updateAttributes);
						}
						if (!isSetAutoUpdate() || ("true".equalsIgnoreCase(autoUpdate))) {
							// change item
							int index = list.indexOf(oldItem);
							Object o = setItem(context, updateAttributes, valueBindingStrings, valueBindings, childrenStrings, children,  
									modelClass == null || modelClass.length() == 0 ? oldItem.getClass() : Class.forName(modelClass), 
											itemVb, oldItem);
							if (index != -1) {
								list.set(index, o);
							}
						}
					}
				}
				
				// process delete
				String deleteKey = (String) update.opt("delete"); 
				if (deleteKey != null) {
					Object deletedItem = getItem(context, deleteKey, itemVb, valueBindingStrings, valueBindings, childrenStrings, children, list);
					if (isSetOnDelete()) {
						// invoke delete callback
						MethodInvokator.getInstance(onDelete, context).invoke(deletedItem);
					}
					if (!isSetAutoDelete() || ("true".equalsIgnoreCase(autoDelete))) {
						// delete item
						list.remove(deletedItem);
					}
				}
			}
			return updates;
		} catch (Throwable t) {
			context.addMessage(null, new FacesMessage(t.getMessage()));
			return null;
		}
	}
			
	private Object getItem(FacesContext ctx, String updateKey, ValueExpression itemVb, Map<String, String> vbStrings, 
			Map<String, ValueExpression> vbMap, Map<String, String> childrenStrings, Map<String, ValueExpression> children, Collection list) 
			throws PropertyNotFoundException, ELException, JSONException {
		
		Application app = ctx.getApplication();

		ELContext elContext = ctx.getELContext();
		ExpressionFactory elFactory = app.getExpressionFactory();
		if (list != null) {
			for(Object item : list) {
				itemVb.setValue(elContext, item);
				
				// check if item corresponds to key 
				if (vbStrings.get(key) != null) {
					if (vbMap.get(key) == null) {
						vbMap.put(key, elFactory.createValueExpression(elContext,vbStrings.get(key),Object.class));
					}
					if (updateKey.equals(String.valueOf(vbMap.get(key).getValue(elContext)))) {
						return item;
					}
				}
				
				// recurse to check children
				for (Iterator<String> childrenIt = childrenStrings.keySet().iterator(); childrenIt.hasNext(); ) {
					String childrenAttr = childrenIt.next();
					if (children.get(childrenAttr) == null) {
						children.put(childrenAttr, elFactory.createValueExpression(elContext,childrenStrings.get(childrenAttr),Object.class));
					}
					Object result = getItem(ctx, updateKey, itemVb, vbStrings, vbMap, childrenStrings, children, 
							(Collection) children.get(childrenAttr).getValue(elContext));
					if (result != null) return result;
				}
			}
		}
		return null;
	}
	
	private Object setItem(FacesContext context, JSONObject updateAttributes, Map<String, String> vbStrings, 
			Map<String, ValueExpression> vbMap, Map<String, String> childrenStrings, Map<String, ValueExpression> children, 
			Class modelClass, ValueExpression itemVb, Object item) 
		throws PropertyNotFoundException, ELException, JSONException, IllegalArgumentException, SecurityException, 
		InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException {
		Application app = context.getApplication();

		ELContext elContext = context.getELContext();
		ExpressionFactory elFactory = app.getExpressionFactory();

		
		itemVb.setValue(elContext, item);
		Iterator<String> attributeIt = updateAttributes.keys();
		while (attributeIt.hasNext()) {
			String attribute = attributeIt.next();
			Object value = updateAttributes.opt(attribute);
			if (childrenStrings.keySet().contains(attribute)) {
				// need to recurse to set this attribute
				if (children.get(attribute) == null) {
					children.put(attribute, elFactory.createValueExpression(elContext,childrenStrings.get(attribute),Object.class));
				}
				if (value instanceof JSONArray) {
					JSONArray json = (JSONArray) value;
					Collection list = new ArrayList();
					children.get(attribute).setValue(elContext, list);
					for (int i = 0; i < json.length(); i++) {
						list.add(setItem(context, (JSONObject) json.get(i), vbStrings, vbMap, childrenStrings, children, 
								modelClass, itemVb, modelClass.getConstructor().newInstance()));
					}
				} else {
					children.get(attribute).setValue(elContext, null);
				}
			} else {
				if (vbMap.get(attribute) == null) {
					if (vbStrings.get(attribute) != null) {
						vbMap.put(attribute, elFactory.createValueExpression(elContext,vbStrings.get(attribute),Object.class));
					}
				}
				if (vbMap.get(attribute) != null && !vbMap.get(attribute).isReadOnly(elContext)) {
					if (itemVb.getExpressionString().equals(vbMap.get(attribute).getExpressionString())) {
						item = value;
					} else if (value instanceof JSONArray) {
						// Leafs are encoded as one-element-arrays by dojo stores, so take only the first element to the model 
						JSONArray json = (JSONArray) value;
						if (json.length() == 0) {
							vbMap.get(attribute).setValue(elContext, null);
						} else {
							vbMap.get(attribute).setValue(elContext, ((JSONArray) value).get(0));
						}
					} else {
						vbMap.get(attribute).setValue(elContext, value);
					}
				}
			}
		}
		return item;
	}
	
	private boolean isSetOnInsert() {
		return onInsert != null && !"".equals(onInsert);
	}
	
	private boolean isSetOnDelete() {
		return onDelete != null && !"".equals(onDelete);
	}
	
	private boolean isSetOnUpdate() {
		return onUpdate != null && !"".equals(onUpdate);
	}

	private boolean isSetModelClass() {
		return modelClass != null && !"".equals(modelClass);
	}
	
	private boolean isSetAutoDelete() {
		return autoDelete != null && !"".equals(autoDelete);
	}
	
	private boolean isSetAutoUpdate() {
		return autoUpdate != null && !"".equals(autoUpdate);
	}

	private static class MethodInvokator {
		private String expression;
		private FacesContext ctx;
		public static MethodInvokator getInstance(String expression, FacesContext ctx) {
			MethodInvokator mi = new MethodInvokator();
			if(expression != null && !"".equals(expression))
				mi.expression = "#{"  + expression + "}";
			mi.ctx = ctx;
			return mi;
		}
		public Object invoke(Object o, JSONObject parameter) {
			ELContext elContext = FacesContext.getCurrentInstance().getELContext();
			Application app = FacesContext.getCurrentInstance().getApplication();
			ExpressionFactory elFactory = app.getExpressionFactory();

			if(expression == null) return null;
			return elFactory.createMethodExpression(elContext, this.expression, Object.class, new Class[] {Object.class, JSONObject.class} )
				.invoke(elContext, new Object[] {o, parameter});
		}
		public Object invoke(JSONObject parameter) {
			ELContext elContext = FacesContext.getCurrentInstance().getELContext();
			Application app = FacesContext.getCurrentInstance().getApplication();
			ExpressionFactory elFactory = app.getExpressionFactory();
			if(expression == null) return null;
			return elFactory.createMethodExpression(elContext, this.expression, Object.class, new Class[] {JSONObject.class})
				.invoke(elContext, new Object[] {parameter});
		}
		public Object invoke(Object parameter) {
			ELContext elContext = FacesContext.getCurrentInstance().getELContext();
			Application app = FacesContext.getCurrentInstance().getApplication();
			ExpressionFactory elFactory = app.getExpressionFactory();
			if(expression == null) return null;
			return elFactory.createMethodExpression(elContext, this.expression, Object.class, new Class[] {Object.class})
				.invoke(elContext, new Object[] {parameter});
		}
	}
}