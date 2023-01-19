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
 * Version: $Revision: 1.16 $ $Date: 2010/04/03 10:56:29 $
 */
package org.j4fry.dojo.beans;

import java.util.HashMap;
import java.util.Map;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;

/**
* Central access point with helper methods for JSF templates.
* Accessible through EL expressions. 
*/
public class DojoHelper {
	/**
	 * JSF component bindings set through EL
	 */
	private Map<String, UIComponent> binding = new HashMap<String, UIComponent>();
	/**
	 * Make components' clientIds accessible
	 */
	private ClientIdMap clientId = new ClientIdMap(binding);

	/**
	 * Using the Schicke Pattern to retrieve JSF instance
	 * 
	 * @return DojoHelper
	 */
	public static DojoHelper get() {
		FacesContext context = FacesContext.getCurrentInstance();
		return (DojoHelper) context.getApplication().createValueBinding("#{dojoHelper}").getValue(context);		
	}

	/**
	 * @return a Map to put the clicked row into the model
	 */
	public GridEventMap getGridEvent() {
		return new GridEventMap();
	}

	/**
	 * return a Map to get the component binding of a JSF tag
	 * @return {@link #binding}}
	 */
	public Map<String, UIComponent> getBinding() {
		return binding;
	}

	/**
	 * set the component binding of a JSF tag through a map
	 * @param {@link #binding}
	 */
	public void setBinding(Map<String, UIComponent> binding) {
		this.binding = binding;
	}

	/**
	 * set the clientId of a JSF tag through a map
	 * @param {@link #clientId}
	 */
	public void setClientId(ClientIdMap clientId) {
		this.clientId = clientId;
	}

	/**
	 * return a Map to get the clientId of a JSF tag
	 * @return {@link #clientId}
	 */
	public ClientIdMap getClientId() {
		return clientId;
	}
	
	/**
	 * replace all ':' with '_' in the clientId to make it usable in Javascript 
	 * @return {@link JsIdMap}
	 */
	public JsIdMap getJsId() {
		return new JsIdMap(binding);
	}

	/**
	 * determine the id of the enclosing form of a JSF component 
	 * that is identified by the key of the binding 
	 * @return {@link FormIdMap}
	 */
	public FormIdMap getFormId() {
		return new FormIdMap(binding);
	}

	/**
	 * The returned Map will execute an escaped JSF method binding.
	 * This is used to pass action through Facelets templates.
	 * @return {@link ExecMap}
	 */
	public ExecMap getExec() {
		return new ExecMap();
	}
	
	/**
	 * The returned Map will transform a list to a Map with the help of several parameters.
	 * This is used to feed a Map to JSf tags while the model holds a list.
	 * @return {@link ListToMapMap}
	 */
	public ListToMapMap getListToMap() {
		return new ListToMapMap();
	}
	
	/**
	 * The returned Map returns a value identical to the key without implementing put.
	 * This is used to avoid writing back to the list (this is done through the ContentMap)
	 * @return {@link StoreMap}
	 */
	public StoreMap getStore() {
		return new StoreMap();
	}
	
	/**
	 * The returned Map takes several params to transform a JSON String to a Map.
	 * The Map forms the stores submitted value and is thus transformed 
	 * back to a JSON String in case of a validation roundtrip.
	 * To Map is further processed by {@link org.j4fry.dojo.converter.StoreUpdateConverter} 
	 * @return {@link StoreUpdateMap}
	 */
	public StoreUpdateMap getStoreUpdate() {
		return new StoreUpdateMap();
	}
	
	/**
	 * The Map is to create a JSON store from a server side list
	 * @return {@link ComboBoxStoreMap}
	 */
	public ComboBoxStoreMap getComboBoxStore() {
		return new ComboBoxStoreMap();
	}
	
	/**
	 * returns an empty HashMap in cases where returning a null value is to be avoided 
	 * @return {@link Map}
	 */
	public Map getEmptyMap() {
		return new HashMap();
	}
	
	/**
	 * returns an Map that can concatenate two items 
	 * @return {@link Map}
	 */
	public Map getConcat() {
		return new ConcatMap();
	}
}
