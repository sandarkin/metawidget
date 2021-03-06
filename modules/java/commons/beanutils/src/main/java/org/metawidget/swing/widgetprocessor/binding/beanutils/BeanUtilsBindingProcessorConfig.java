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

package org.metawidget.swing.widgetprocessor.binding.beanutils;

import org.metawidget.inspector.impl.propertystyle.PropertyStyle;
import org.metawidget.inspector.impl.propertystyle.javabean.JavaBeanPropertyStyle;
import org.metawidget.util.simple.ObjectUtils;

/**
 * Configures a BeanUtilsBindingProcessor prior to use. Once instantiated, WidgetProcessors are
 * immutable.
 *
 * @author Richard Kennard
 */

public class BeanUtilsBindingProcessorConfig {

	//
	// Private statics
	//

	private static PropertyStyle	DEFAULT_PROPERTY_STYLE;

	//
	// Private members
	//

	private PropertyStyle			mPropertyStyle;

	private boolean					mNullPropertyStyle;

	//
	// Public methods
	//

	/**
	 * Sets the style used to recognize properties.
	 *
	 * @return this, as part of a fluent interface
	 */

	public BeanUtilsBindingProcessorConfig setPropertyStyle( PropertyStyle propertyStyle ) {

		mPropertyStyle = propertyStyle;
		mNullPropertyStyle = ( propertyStyle == null );

		// Fluent interface

		return this;
	}

	@Override
	public boolean equals( Object that ) {

		if ( this == that ) {
			return true;
		}

		if ( !ObjectUtils.nullSafeClassEquals( this, that ) ) {
			return false;
		}

		if ( !ObjectUtils.nullSafeEquals( mPropertyStyle, ( (BeanUtilsBindingProcessorConfig) that ).mPropertyStyle ) ) {
			return false;
		}

		if ( mNullPropertyStyle != ( (BeanUtilsBindingProcessorConfig) that ).mNullPropertyStyle ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {

		int hashCode = 1;
		hashCode = 31 * hashCode + ObjectUtils.nullSafeHashCode( mPropertyStyle );
		hashCode = 31 * hashCode + ObjectUtils.nullSafeHashCode( mNullPropertyStyle );

		return hashCode;
	}

	//
	// Protected methods
	//

	/**
	 * Gets the style used to recognize properties.
	 */

	protected PropertyStyle getPropertyStyle() {

		if ( mPropertyStyle == null && !mNullPropertyStyle ) {
			// Do not initialise unless needed, so that we can be shipped without

			if ( DEFAULT_PROPERTY_STYLE == null ) {
				DEFAULT_PROPERTY_STYLE = new JavaBeanPropertyStyle();
			}

			return DEFAULT_PROPERTY_STYLE;
		}

		return mPropertyStyle;
	}
}
