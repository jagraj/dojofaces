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
 * Version: $Revision: 1.4 $ $Date: 2010/03/13 20:50:14 $
 */
package org.j4fry.dojo.beans;

import javax.faces.context.FacesContext;
import javax.faces.el.MethodBinding;

/**
 * execute a JSF method binding
 * the escaped method expression \#{ex} is passed to the facelets template
 * <param name="m" value="\#{ex}" />
 * execution is then triggered via #{dojoHelper.execMap[m]}
 */
public class ExecMap extends MapAdapter {

	private MethodBinding methodBinding;
	private String fixedResponse;

	public String trigger() {
		if (methodBinding != null) {
			FacesContext context = FacesContext.getCurrentInstance();
			return (String) methodBinding.invoke(context, null);
		} else {
			return fixedResponse;
		}
	}
	
	public ExecMap get(Object key) {
		String method = (String) key;
		if (method != null && !"dojoFacesNullAction".equals(method)) {
			if (method.startsWith("#{") && method.endsWith("}")) {
				FacesContext context = FacesContext.getCurrentInstance();
				methodBinding = context.getApplication().createMethodBinding(method, null);
			} else {
				fixedResponse = method;
			}
		}
		return this;
	}
}