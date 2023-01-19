/*
 * Copyright 2007 Ganesh Jung
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
 * Version: $Revision: 1.2 $ $Date: 2010/03/13 20:50:14 $
 *
 */
package org.j4fry.dojo.converter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Locale;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.ConverterException;

/**
 * Special converter with fixed locale for tags with graphical representation
 * like slider or rating. The graphical numeric tags do require a unified format 
 * to pass the value to the model, which is provides with this class.
 */
public class NumberConverter extends BaseConverter {
	
	private String pattern;
	private String language;
	private String country;
	private String variant;
	private String type;
	private boolean lenient;
	private String numberMessage;

	private Number parse(String value, NumberFormat df, boolean lenient) throws ParseException {
		ParsePosition pp = new ParsePosition(0);
		Number result = df.parse(value, pp);
		if (pp.getErrorIndex() > -1) {
			throw new ParseException("ErrorIndex", pp.getErrorIndex());
		} else if (pp.getIndex() < value.length() && !lenient) {
			throw new ParseException("Index", pp.getIndex());
		} else {
			return result;
		}
	}
	
	/**
	 * @see jakarta.faces.convert.Converter#getAsString(jakarta.faces.context.FacesContext,
	 *      jakarta.faces.component.UIComponent, java.lang.Object)
	 */
	public String getAsString(FacesContext context, UIComponent component,
			Object valueObject) throws ConverterException {
		if (valueObject == null) return null;
		return getFormatter(component).format(valueObject);
	}

	private NumberFormat getFormatter(UIComponent component) {
		NumberFormat df = null;
		if (language == null) language = "en";
		if (country == null) country = "US";
		if (variant == null) variant = "";
		if ("Percent".equals(type)) {
			df = NumberFormat.getPercentInstance(new Locale(language, country, variant));
		} else if ("Currency".equals(type)) {
			df = NumberFormat.getCurrencyInstance(new Locale(language, country, variant));
		} else {
			df = NumberFormat.getInstance(new Locale(language, country, variant));
		} 
		if (df instanceof DecimalFormat && pattern != null && pattern.length() != 0) {
			((DecimalFormat) df).applyPattern(pattern);
		}
		return df;
	}

	public Object convert(FacesContext context, UIComponent component, String value) {
		if (value == null || value.length() == 0) return null;
		String errorMessage = null;
		Number result = null;
		try {
			result = this.parse(value, getFormatter(component), lenient);
		} catch (Throwable t) {
			if (numberMessage == null) {
				errorMessage = "not a number";
			} else {
				errorMessage = numberMessage;
			}
		}
		if (errorMessage != null) {
			super.throwException(context,component,errorMessage);
			return null;
		} else {
		    Class targetType = null;
		    if(component.getAttributes().containsKey("Class")) {
		    	try {
					targetType = Class.forName((String)component.getAttributes().get("Class"));
				} catch (ClassNotFoundException e) {
					throw new IllegalArgumentException("Invalid argument for attribute 'Class' " + 
							(String)component.getAttributes().get("Class"));
				}
		    } else {
		    	targetType = component.getValueBinding("value").getType(context);
		    }
			if (targetType == Integer.class) {
				if (result instanceof Integer) {
					return result;
				} else {
					return new Integer(result.intValue());
				}
			} else if (targetType == Long.class) {
				if (result instanceof Long) {
					return result;
				} else {
					return new Long(result.longValue());
				}
			} else if (targetType == Double.class) {
				if (result instanceof Double) {
					return result;
				} else {
					return new Double(result.doubleValue());
				}
			} else if (targetType == Byte.class) {
				if (result instanceof Byte) {
					return result;
				} else {
					return new Byte(result.byteValue());
				}
			} else if (targetType == Float.class) {
				if (result instanceof Float) {
					return result;
				} else {
					return new Float(result.floatValue());
				}
			} else if (targetType == Short.class) {
				if (result instanceof Short) {
					return result;
				} else {
					return new Short(result.shortValue());
				}
			} else if (targetType == BigInteger.class) {
				if (result instanceof BigInteger) {
					return result;
				} else {
					return BigInteger.valueOf(result.longValue());
				}
			} else if (targetType == BigDecimal.class) {
				if (result instanceof BigDecimal) {
					return result;
				} else {
					return BigDecimal.valueOf(result.doubleValue());
				}
			} else {
				return result;
			}
		}
	}

	public String getPattern() {
		return pattern;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getVariant() {
		return variant;
	}

	public void setVariant(String variant) {
		this.variant = variant;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public boolean isLenient() {
		return lenient;
	}

	public void setLenient(boolean lenient) {
		this.lenient = lenient;
	}

	public String getNumberMessage() {
		return numberMessage;
	}

	public void setNumberMessage(String numberMessage) {
		this.numberMessage = numberMessage;
	}
}