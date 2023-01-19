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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.el.EvaluationException;
import javax.faces.el.PropertyNotFoundException;
import javax.faces.el.ValueBinding;

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
		try {
			String structureString = ((String) structure).replace("__dojoFacelets_emtpyKey__", "#{" + var + "}");
			
			// tokenize structure
			Map<String, String> valueBindingStrings = new HashMap<String, String>();
			Map<String, String> childrenStrings = new HashMap<String, String>();
	    	StoreUpdateConverter.tokenizeStructure(structureString, context, valueBindingStrings, childrenStrings, null, null, null);

	    	// cache valueBindings once they are created
	    	Map<String, ValueBinding> valueBindings = new HashMap<String, ValueBinding>();
			Map<String, ValueBinding> children = new HashMap<String, ValueBinding>();

			ValueBinding itemVb = context.getApplication().createValueBinding("#{" + var + "}");
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
			
	private Object getItem(FacesContext ctx, String updateKey, ValueBinding itemVb, Map<String, String> vbStrings, 
			Map<String, ValueBinding> vbMap, Map<String, String> childrenStrings, Map<String, ValueBinding> children, Collection list) 
			throws PropertyNotFoundException, EvaluationException, JSONException {
		if (list != null) {
			for(Object item : list) {
				itemVb.setValue(ctx, item);
				
				// check if item corresponds to key 
				if (vbStrings.get(key) != null) {
					if (vbMap.get(key) == null) {
						vbMap.put(key, ctx.getApplication().createValueBinding(vbStrings.get(key)));
					}
					if (updateKey.equals(String.valueOf(vbMap.get(key).getValue(ctx)))) {
						return item;
					}
				}
				
				// recurse to check children
				for (Iterator<String> childrenIt = childrenStrings.keySet().iterator(); childrenIt.hasNext(); ) {
					String childrenAttr = childrenIt.next();
					if (children.get(childrenAttr) == null) {
						children.put(childrenAttr, ctx.getApplication().createValueBinding(childrenStrings.get(childrenAttr)));
					}
					Object result = getItem(ctx, updateKey, itemVb, vbStrings, vbMap, childrenStrings, children, 
							(Collection) children.get(childrenAttr).getValue(ctx));
					if (result != null) return result;
				}
			}
		}
		return null;
	}
	
	private Object setItem(FacesContext context, JSONObject updateAttributes, Map<String, String> vbStrings, 
			Map<String, ValueBinding> vbMap, Map<String, String> childrenStrings, Map<String, ValueBinding> children, 
			Class modelClass, ValueBinding itemVb, Object item) 
		throws PropertyNotFoundException, EvaluationException, JSONException, IllegalArgumentException, SecurityException, 
		InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException {
		itemVb.setValue(context, item);
		Iterator<String> attributeIt = updateAttributes.keys();
		while (attributeIt.hasNext()) {
			String attribute = attributeIt.next();
			Object value = updateAttributes.opt(attribute);
			if (childrenStrings.keySet().contains(attribute)) {
				// need to recurse to set this attribute
				if (children.get(attribute) == null) {
					children.put(attribute, context.getApplication().createValueBinding(childrenStrings.get(attribute)));
				}
				if (value instanceof JSONArray) {
					JSONArray json = (JSONArray) value;
					Collection list = new ArrayList();
					children.get(attribute).setValue(context, list);
					for (int i = 0; i < json.length(); i++) {
						list.add(setItem(context, (JSONObject) json.get(i), vbStrings, vbMap, childrenStrings, children, 
								modelClass, itemVb, modelClass.getConstructor().newInstance()));
					}
				} else {
					children.get(attribute).setValue(context, null);
				}
			} else {
				if (vbMap.get(attribute) == null) {
					if (vbStrings.get(attribute) != null) {
						vbMap.put(attribute, context.getApplication().createValueBinding(vbStrings.get(attribute)));
					}
				}
				if (vbMap.get(attribute) != null && !vbMap.get(attribute).isReadOnly(context)) {
					if (itemVb.getExpressionString().equals(vbMap.get(attribute).getExpressionString())) {
						item = value;
					} else if (value instanceof JSONArray) {
						// Leafs are encoded as one-element-arrays by dojo stores, so take only the first element to the model 
						JSONArray json = (JSONArray) value;
						if (json.length() == 0) {
							vbMap.get(attribute).setValue(context, null);
						} else {
							vbMap.get(attribute).setValue(context, ((JSONArray) value).get(0));
						}
					} else {
						vbMap.get(attribute).setValue(context, value);
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
			if(expression == null) return null;
			return ctx.getApplication().createMethodBinding(this.expression, new Class[] {Object.class, JSONObject.class})
				.invoke(ctx, new Object[] {o, parameter});
		}
		public Object invoke(JSONObject parameter) {
			if(expression == null) return null;
			return ctx.getApplication().createMethodBinding(this.expression, new Class[] {JSONObject.class})
				.invoke(ctx, new Object[] {parameter});
		}
		public Object invoke(Object parameter) {
			if(expression == null) return null;
			return ctx.getApplication().createMethodBinding(this.expression, new Class[] {Object.class})
				.invoke(ctx, new Object[] {parameter});
		}
	}
}