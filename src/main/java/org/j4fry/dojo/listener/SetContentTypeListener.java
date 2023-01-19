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
 * Version: $Revision: 1.3 $ $Date: 2010/03/13 20:50:14 $
 */
package org.j4fry.dojo.listener;

import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.PhaseEvent;
import jakarta.faces.event.PhaseId;
import jakarta.faces.event.PhaseListener;
import javax.portlet.RenderResponse;
import javax.servlet.ServletResponse;

/**
 * Mojarra 2.0 sets content-type application/xhtml+xml which breaks Safari
 * Safari will throw NO_MODIFICATION_ALLOWED_ERR: DOM Exception 7
 * when trying top modify DOM tree
 * This Listener will force content-type text/html
 */
public class SetContentTypeListener implements PhaseListener {
	
	private static final long serialVersionUID = 1L;

	public PhaseId getPhaseId() {
		return PhaseId.RENDER_RESPONSE;
	}

	public void beforePhase(PhaseEvent event) {
		FacesContext context = FacesContext.getCurrentInstance();
		ExternalContext extContext = context.getExternalContext();
		Object response = extContext.getResponse();
		if (event.getPhaseId() == PhaseId.RENDER_RESPONSE) {
			String contentType = "text/html; charset=UTF-8";
			if (response instanceof ServletResponse) {
				((ServletResponse) response).setContentType(contentType);
			} else if (response instanceof RenderResponse) {
				((RenderResponse) response).setContentType(contentType);
			}
		}
	}

	public void afterPhase(PhaseEvent event) {}
}
