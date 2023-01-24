/*
 * Copyright 2007 Ganesh Jung
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
 * Version: $Revision: 1.2 $ $Date: 2010/03/13 20:50:14 $
 *
 */
package org.j4fry.dojo.converter;

import java.util.Map;
import java.util.regex.Pattern;

import jakarta.el.ELContext;
import jakarta.el.ExpressionFactory;
import jakarta.faces.application.Application;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.ConverterException;

/**
 * some basic converter attributes ported from j4fry errorhandling
 */
public abstract class BaseConverter implements Converter {
	
	protected void validate(UIComponent component, String value) {
		String errorMessage = null;
		Map attributes = component.getAttributes();
		boolean required = attributes.get("Required") != null
				&& "true".equalsIgnoreCase((String) attributes.get("Required"));
		
		if (value == null || value.length() == 0) {
			if (required) {
				errorMessage = (String) attributes.get("RequiredMessage");
				if (errorMessage == null) {
					errorMessage = "input required";
				}
			} else {
				return;
			}
		}
		String regExp = (String) attributes.get("RegExp");
		int minLength = attributes.get("MinLength") != null ? Integer
				.parseInt((String) attributes.get("MinLength")) : 0;
		int maxLength = attributes.get("MaxLength") != null ? Integer
				.parseInt((String) attributes.get("MaxLength"))
				: Integer.MAX_VALUE;
		if (regExp != null) {
			if (!Pattern.matches(regExp, value)) {
				errorMessage = (String) attributes.get("RegExpMessage");
				if (errorMessage == null) {
					errorMessage = "follow regular expression ${RegExp}";
				}
			}
		} else if ((value != null && value.length() < minLength)
				|| (value == null && minLength > 0)) {
			errorMessage = (String) attributes.get("MinLengthMessage");
			if (errorMessage == null) {
				errorMessage = "input at least ${MinLength} characters";
			}
		} else if (value != null && value.length() > maxLength) {
			errorMessage = (String) attributes.get("MaxLengthMessage");
			if (errorMessage == null) {
				errorMessage = "input at most ${MaxLength} characters";
			}
		}

		if (errorMessage != null) {
			this.throwException(component, errorMessage);
		}
	}

	/**
	 * @see Converter#getAsObject(FacesContext, UIComponent, String)
	 */
	public final Object getAsObject(FacesContext context, UIComponent component, String value)
			throws ConverterException {
	    try {
			validate(component, value);
			return convert(context, component, value);
	    } catch (Throwable t) {
	    	throwException(context, component, t);
	    }
	    return null;
	}

	public abstract Object convert(FacesContext context, UIComponent component, String value);

	protected void throwException(UIComponent component, String message) throws ConverterException {
		this.throwException(FacesContext.getCurrentInstance(), component, message);
	}
	
	protected void throwException(FacesContext context, UIComponent component, Throwable t) throws ConverterException {
		this.throwException(context, component, t.getMessage());
	}
	
	protected void throwException(FacesContext context, UIComponent component, String message) throws ConverterException {
		if (message == null) message = "no message provided";
		StringBuffer newMessage = new StringBuffer();
		int oldPos = 0;
		int pos = message.indexOf("${");
		if (pos > 0) {
			while (pos >= 0) {
				newMessage.append(message.substring(oldPos, pos));
				oldPos = pos+2;
				pos = message.indexOf('}', oldPos);
				String param = message.substring(oldPos, pos);
				String paramValue = null;
				if (component.getAttributes().get(param) == null) {
					paramValue = "attribute" + param + "NotProvided";
				} else {
					paramValue = component.getAttributes().get(param).toString();
				}
				newMessage.append(paramValue);
				oldPos = pos+1;
				pos = message.indexOf("${", oldPos);
			}
		}
		newMessage.append(message.substring(oldPos));
		String detailMessage = context.getExternalContext().getInitParameter("J4Fry_ACTION_DETAIL_MESSAGE");
		/*if (detailMessage != null && UIComponentTag.isValueReference((detailMessage))) {
			detailMessage = context.getApplication().createValueBinding(detailMessage).getValue(context).toString();
		}*/
		// execute onError
		Map attributes = component.getAttributes();
		String onError = (String) attributes.get("onError");
		if (onError != null) {
			ELContext elContext = FacesContext.getCurrentInstance().getELContext();
			Application app = FacesContext.getCurrentInstance().getApplication();
			ExpressionFactory elFactory = app.getExpressionFactory();
			elFactory.createMethodExpression(elContext, "#{" + onError + "}", null, new Class[] {}).invoke(elContext, new Object[] {});
		}
		throw new ConverterException(new FacesMessage(FacesMessage.SEVERITY_ERROR, newMessage.toString(), detailMessage != null ? detailMessage : newMessage.toString()));
	}
	
	/**
	 * @see Converter#getAsString(FacesContext, UIComponent, Object)
	 */
	public abstract String getAsString(FacesContext context, UIComponent component, Object o);
	
}
