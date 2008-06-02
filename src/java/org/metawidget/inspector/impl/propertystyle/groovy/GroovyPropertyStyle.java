// Metawidget
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

package org.metawidget.inspector.impl.propertystyle.groovy;

import groovy.lang.GroovySystem;
import groovy.lang.MetaBeanProperty;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import org.metawidget.inspector.InspectorException;
import org.metawidget.inspector.impl.propertystyle.Property;
import org.metawidget.inspector.impl.propertystyle.PropertyImpl;
import org.metawidget.inspector.impl.propertystyle.PropertyStyle;
import org.metawidget.util.CollectionUtils;

/**
 * @author Richard Kennard
 */

public class GroovyPropertyStyle
	implements PropertyStyle
{
	//
	//
	// Public methods
	//
	//

	public Map<String, Property> getProperties( Class<?> clazz )
	{
		Map<String, Property> propertiesToReturn = CollectionUtils.newHashMap();

		@SuppressWarnings( "unchecked" )
		List<MetaBeanProperty> properties = GroovySystem.getMetaClassRegistry().getMetaClass( clazz ).getProperties();

		for( MetaBeanProperty property : properties )
		{
			propertiesToReturn.put( property.getName(), new GroovyProperty( property ) );
		}

		return propertiesToReturn;
	}

	//
	//
	// Inner classes
	//
	//

	/**
	 * Groovy-based property.
	 */

	private static class GroovyProperty
		extends PropertyImpl
	{
		//
		//
		// Private members
		//
		//

		private MetaBeanProperty mProperty;

		//
		//
		// Constructor
		//
		//

		public GroovyProperty( MetaBeanProperty property )
		{
			super( property.getName(), property.getType() );

			mProperty = property;
		}

		//
		//
		// Public methods
		//
		//

		public boolean isReadable()
		{
			return ( mProperty.getGetter() != null );
		}

		public Object read( Object obj )
		{
			try
			{
				return mProperty.getProperty( obj );
			}
			catch ( Exception e )
			{
				throw InspectorException.newException( e );
			}
		}

		public boolean isWritable()
		{
			return ( mProperty.getSetter() != null );
		}

		public <T extends Annotation> T getAnnotation( Class<T> annotation )
		{
			return null;
		}

		public Type getPropertyGenericType()
		{
			return null;
		}
	}
}
