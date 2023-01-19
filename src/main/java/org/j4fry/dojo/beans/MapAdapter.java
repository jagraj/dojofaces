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
 * Version: $Revision: 1.1 $ $Date: 2010/03/13 20:50:14 $
 */
package org.j4fry.dojo.beans;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
* An implementation of interface Map with dummy implementations
* for all methods besides {@link #get}.
* <p>
* <strong>Aims</strong>
* DojoFaces uses several Map implementations, to pass parameters
* through JSF EL expressions. This class serves as a base for 
* those maps.
* {@link https://www.xing.com/net/jsf/newsletters-archive-362954/group-newsletter-the-map-hack-26501345/26501345/}
* </p>
*/
public abstract class MapAdapter implements Map<Object, Object> {

	public abstract Object get(Object key);
	
	public void clear() {
	}

	public boolean containsKey(Object key) {
		return false;
	}

	public boolean containsValue(Object value) {
		return false;
	}

	public Set<Map.Entry<Object, Object>> entrySet() {
		return null;
	}

	public boolean isEmpty() {
		return false;
	}

	public Set<Object> keySet() {
		return null;
	}

	public Object put(Object arg0, Object arg1) {
		return null;
	}

	public void putAll(Map<? extends Object, ? extends Object> arg0) {

	}

	public Object remove(Object key) {
		return null;
	}

	public int size() {
		return 0;
	}

	public Collection<Object> values() {
		return null;
	}
}