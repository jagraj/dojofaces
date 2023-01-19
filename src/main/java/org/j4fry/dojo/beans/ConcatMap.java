package org.j4fry.dojo.beans;

import java.util.Map;

public class ConcatMap extends MapAdapter {

	private String first;
	private boolean isSetFirst;
	@Override
	public Object get(Object key) {
		if (!isSetFirst) {
			first = (key != null ? key.toString() : "");
			isSetFirst = true;
			return this;
		} else {
			return first + key;
		}
	}	

}
