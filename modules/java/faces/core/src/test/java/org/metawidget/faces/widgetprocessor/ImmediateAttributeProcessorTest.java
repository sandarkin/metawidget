// Metawidget (licensed under LGPL)
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package org.metawidget.faces.widgetprocessor;

import static org.metawidget.inspector.InspectionResultConstants.*;
import static org.metawidget.inspector.faces.FacesInspectionResultConstants.*;

import java.util.Map;

import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlInputText;

import junit.framework.TestCase;

import org.metawidget.faces.component.widgetprocessor.ImmediateAttributeProcessor;
import org.metawidget.util.CollectionUtils;

/**
 * @author Richard Kennard
 */

public class ImmediateAttributeProcessorTest
	extends TestCase {

	//
	// Public methods
	//

	public void testWidgetProcessor()
		throws Exception {

		ImmediateAttributeProcessor processor = new ImmediateAttributeProcessor();

		UIComponent component = new HtmlInputText();
		EditableValueHolder editableValueHolder = (EditableValueHolder) component;

		// Not immediate? Don't set the flag

		Map<String, String> attributes = CollectionUtils.newHashMap();
		processor.processWidget( component, PROPERTY, attributes, null );
		assertFalse( editableValueHolder.isImmediate() );

		// Immediate? Set the flag

		attributes.put( FACES_IMMEDIATE, TRUE );
		processor.processWidget( component, PROPERTY, attributes, null );
		assertTrue( editableValueHolder.isImmediate() );
	}
}
