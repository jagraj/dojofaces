package org.j4fry.dojo.beans;

import java.util.Map;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;

public class JsIdMap extends MapAdapter {
	private Map<String, UIComponent> bindings;

	public JsIdMap(Map<String, UIComponent> bindings) {
		this.bindings = bindings;
	}

	@Override
	public Object get(Object id) {
		UIComponent comp = ((UIComponent) bindings.get(id));
		if (comp == null) return null;
		return comp.getClientId(FacesContext.getCurrentInstance()).replace(':', '_');
	}

}
