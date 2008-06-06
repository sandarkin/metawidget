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

package org.metawidget.android.widget;

import static org.metawidget.inspector.InspectionResultConstants.*;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.metawidget.MetawidgetException;
import org.metawidget.android.AndroidUtils.ResourcelessArrayAdapter;
import org.metawidget.android.widget.layout.Layout;
import org.metawidget.android.widget.layout.TableLayout;
import org.metawidget.impl.MetawidgetMixin;
import org.metawidget.inspector.Inspector;
import org.metawidget.util.ArrayUtils;
import org.metawidget.util.ClassUtils;
import org.metawidget.util.CollectionUtils;
import org.metawidget.util.simple.PathUtils;
import org.metawidget.util.simple.StringUtils;
import org.metawidget.util.simple.PathUtils.TypeAndNames;
import org.w3c.dom.Document;

import android.content.Context;
import android.graphics.Canvas;
import android.text.InputFilter;
import android.text.method.DateInputMethod;
import android.text.method.DigitsInputMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * Metawidget for Android environments.
 * <p>
 * Automatically creates native Android Views, such as <code>EditText</code> and
 * <code>Spinner</code>, to suit the inspected fields.
 * <p>
 * Note: this class extends <code>LinearLayout</code> rather than <code>FrameLayout</code>,
 * because <code>FrameLayout</code> would <em>always</em> need to have another
 * <code>Layout</code> embedded within it, whereas <code>LinearLayout</code> is occasionally
 * useful directly.
 *
 * @author Richard Kennard
 */

public class AndroidMetawidget
	extends LinearLayout
{
	//
	//
	// Private statics
	//
	//

	private final static List<Boolean>				LIST_BOOLEAN_VALUES	= CollectionUtils.unmodifiableList( null, Boolean.TRUE, Boolean.FALSE );

	private final static Map<Integer, Inspector>	INSPECTORS			= Collections.synchronizedMap( new HashMap<Integer, Inspector>() );

	private final static String						PARAM_PREFIX		= "param";

	//
	//
	// Private members
	//
	//

	private Object									mToInspect;

	private String									mPath;

	private int										mInspectorConfig;

	private Inspector								mInspector;

	private Class<? extends Layout>					mLayoutClass		= TableLayout.class;

	private Layout									mLayout;

	private Map<String, Object>						mParameters;

	private boolean									mNeedToBuildWidgets;

	private Set<View>								mExistingViews;

	private Set<View>								mExistingViewsUnused;

	private Map<String, Facet>						mFacets;

	private AndroidMetawidgetMixin					mMixin				= new AndroidMetawidgetMixin();

	//
	//
	// Constructor
	//
	//

	public AndroidMetawidget( Context context )
	{
		super( context );

		setOrientation( LinearLayout.VERTICAL );
	}

	@SuppressWarnings( "unchecked" )
	public AndroidMetawidget( Context context, AttributeSet attributes, Map inflateParams )
	{
		super( context, attributes, inflateParams );
		setOrientation( LinearLayout.VERTICAL );

		// For each attribute...

		for ( int loop = 0, length = attributes.getAttributeCount(); loop < length; loop++ )
		{
			// ...that looks like a parameter...

			String name = attributes.getAttributeName( loop );

			if ( !name.startsWith( PARAM_PREFIX ) )
				continue;

			name = name.substring( PARAM_PREFIX.length() );

			if ( !StringUtils.isFirstLetterUppercase( name ) )
				continue;

			// ...remember it

			String value = attributes.getAttributeValue( loop );

			// (process resource lookups)

			if ( value.startsWith( "@" ) )
			{
				setParameter( StringUtils.lowercaseFirstLetter( name ), attributes.getAttributeResourceValue( loop, 0 ) );
				continue;
			}

			setParameter( StringUtils.lowercaseFirstLetter( name ), value );
		}

		// Support configuring inspectors in the XML

		mInspectorConfig = attributes.getAttributeResourceValue( null, "inspectorConfig", 0 );

		// Support configuring layouts in the XML

		String layoutClass = attributes.getAttributeValue( null, "layout" );

		if ( layoutClass != null && !"".equals( layoutClass ) )
		{
			mLayoutClass = (Class<? extends Layout>) ClassUtils.niceForName( layoutClass );
		}

		// Support readOnly in the XML

		String readOnly = attributes.getAttributeValue( null, "readOnly" );

		if ( readOnly != null && !"".equals( readOnly ) )
		{
			mMixin.setReadOnly( Boolean.parseBoolean( readOnly ) );
		}
	}

	public AndroidMetawidget( Context context, AndroidMetawidget metawidget )
	{
		super( context );
		setOrientation( LinearLayout.VERTICAL );

		// Do not copy mPath: could lead to infinite recursion

		mToInspect = metawidget.mToInspect;
		mInspector = metawidget.mInspector;
		mInspectorConfig = metawidget.mInspectorConfig;
		mLayoutClass = metawidget.mLayoutClass;

		if ( metawidget.mParameters != null )
			mParameters = CollectionUtils.newHashMap( metawidget.mParameters );
	}

	//
	//
	// Public methods
	//
	//

	public void setPath( String path )
	{
		mPath = path;
		invalidateWidgets();
	}

	public void setToInspect( Object toInspect )
	{
		mToInspect = toInspect;

		// If no path, or path points to an old class, override it

		if ( toInspect != null && ( mPath == null || mPath.indexOf( StringUtils.SEPARATOR_FORWARD_SLASH ) == -1 ) )
			mPath = ClassUtils.getUnproxiedClass( toInspect.getClass() ).getName();

		invalidateWidgets();
	}

	/**
	 * Provides an id for the inspector configuration.
	 * <p>
	 * Typically, the id will be retrieved by <code>R.raw.inspector</code>
	 */

	public void setInspectorConfig( int inspectorConfig )
	{
		mInspectorConfig = inspectorConfig;
		mInspector = null;
		invalidateWidgets();
	}

	public void setInspector( Inspector inspector )
	{
		mInspector = inspector;
		mInspectorConfig = 0;
		invalidateWidgets();
	}

	/**
	 * @param layoutClass
	 *            may be null
	 */

	public void setLayoutClass( Class<? extends Layout> layoutClass )
	{
		mLayoutClass = layoutClass;
		mLayout = null;
		invalidateWidgets();
	}

	public String getLabelString( Map<String, String> attributes )
	{
		if ( attributes == null )
			return "";

		// Explicit label

		String label = attributes.get( LABEL );

		if ( label != null )
		{
			// (may be forced blank)

			if ( "".equals( label ) )
				return null;

			// (localize if possible)

			String localized = getLocalizedKey( StringUtils.camelCase( label ) );

			if ( localized != null )
				return localized.trim();

			return label.trim();
		}

		// Default name

		String name = attributes.get( NAME );

		if ( name != null )
		{
			// (localize if possible)

			String localized = getLocalizedKey( name );

			if ( localized != null )
				return localized.trim();

			return StringUtils.uncamelCase( name );
		}

		return "";
	}

	/**
	 * @return null if no bundle, ???key??? if bundle is missing a key
	 */

	public String getLocalizedKey( String key )
	{
		// Android doesn't support i18n yet

		return null;
	}

	public Object getParameter( String name )
	{
		if ( mParameters == null )
			return null;

		return mParameters.get( name );
	}

	/**
	 * Sets a parameter value.
	 */

	public void setParameter( String name, Object value )
	{
		if ( mParameters == null )
			mParameters = CollectionUtils.newHashMap();

		mParameters.put( name, value );
	}

	public boolean isReadOnly()
	{
		return mMixin.isReadOnly();
	}

	public void setReadOnly( boolean readOnly )
	{
		mMixin.setReadOnly( readOnly );
		invalidateWidgets();
	}

	//
	// The following methods all kick off a buildWidgets()
	//

	/**
	 * Gets the value from the View with the given name.
	 * <p>
	 * The value is returned as it is stored in the View (eg. String for EditText) so may need some
	 * conversion before being reapplied to the object being inspected. This obviously requires
	 * knowledge of which View AndroidMetawidget created, which is not ideal.
	 */

	public Object getValue( String... names )
	{
		View view = findViewWithTag( names );

		if ( view == null )
			throw MetawidgetException.newException( "No view with tag " + ArrayUtils.toString( names ) );

		// CheckBox

		if ( view instanceof CheckBox )
			return ( (CheckBox) view ).isChecked();

		// EditText

		if ( view instanceof EditText )
			return ( (EditText) view ).getText().toString();

		// TextView

		if ( view instanceof TextView )
			return ( (TextView) view ).getText();

		// AdapterView

		if ( view instanceof AdapterView )
			return ( (AdapterView<?>) view ).getSelectedItem();

		// Unknown (subclasses should override this)

		throw MetawidgetException.newException( "Don't know how to getValue from a " + view.getClass().getName() );
	}

	/**
	 * Sets the value of the View with the given name.
	 * <p>
	 * Clients must ensure the value is of the correct type to suit the View (eg. String for
	 * EditText). This obviously requires knowledge of which View AndroidMetawidget created, which
	 * is not ideal.
	 */

	public void setValue( Object value, String... names )
	{
		View view = findViewWithTag( names );

		if ( view == null )
			throw MetawidgetException.newException( "No view with tag " + ArrayUtils.toString( names ) );

		// CheckBox

		if ( view instanceof CheckBox )
		{
			( (CheckBox) view ).setChecked( (Boolean) value );
			return;
		}

		// EditView/TextView

		if ( view instanceof TextView )
		{
			( (TextView) view ).setText( StringUtils.quietValueOf( value ) );
			return;
		}

		// AdapterView

		if ( view instanceof AdapterView )
		{
			@SuppressWarnings( "unchecked" )
			AdapterView<ArrayAdapter<Object>> adapterView = (AdapterView<ArrayAdapter<Object>>) view;

			// Set the backing collection

			if ( value instanceof Collection )
			{
				@SuppressWarnings( "unchecked" )
				Collection<Object> collection = (Collection<Object>) value;
				adapterView.setAdapter( new ResourcelessArrayAdapter<Object>( getContext(), collection ) );
			}

			// Set the selected value

			else
			{
				adapterView.setSelection( adapterView.getAdapter().getPosition( value ) );
			}

			return;
		}

		// Unknown (subclasses should override this)

		throw MetawidgetException.newException( "Don't know how to setValue of a " + view.getClass().getName() );
	}

	public Facet getFacet( String name )
	{
		buildWidgets();

		return mFacets.get( name );
	}

	//
	//
	// Protected methods
	//
	//

	@Override
	protected void onDraw( Canvas canvas )
	{
		buildWidgets();
		super.onDraw( canvas );
	}

	@Override
	protected View findViewTraversal( int id )
	{
		buildWidgets();
		return super.findViewTraversal( id );
	}

	@Override
	protected View findViewWithTagTraversal( Object tag )
	{
		buildWidgets();
		return super.findViewWithTagTraversal( tag );
	}

	protected void invalidateWidgets()
	{
		if ( mNeedToBuildWidgets )
			return;

		mNeedToBuildWidgets = true;

		invalidate();
	}

	protected void buildWidgets()
	{
		// No need to build?

		if ( !mNeedToBuildWidgets )
			return;

		mNeedToBuildWidgets = false;

		try
		{
			mMixin.buildWidgets( inspect() );
		}
		catch( Exception e )
		{
			throw MetawidgetException.newException( e );
		}
	}

	protected void startBuild()
		throws Exception
	{
		if ( mExistingViews == null )
		{
			mExistingViews = CollectionUtils.newHashSet();
			mFacets = CollectionUtils.newHashMap();

			for ( int loop = 0, length = getChildCount(); loop < length; loop++ )
			{
				View view = getChildAt( loop );

				if ( view instanceof Facet )
				{
					Facet facet = (Facet) view;

					mFacets.put( facet.getName(), facet );
					continue;
				}

				mExistingViews.add( view );
			}
		}

		removeAllViews();

		mExistingViewsUnused = CollectionUtils.newHashSet( mExistingViews );

		// Start layout

		mLayout = mLayoutClass.getConstructor( AndroidMetawidget.class ).newInstance( this );
		mLayout.layoutBegin();
	}

	protected void addWidget( View view, Map<String, String> attributes )
	{
		String childName = attributes.get( NAME );
		view.setTag( childName );

		if ( mLayout != null )
			mLayout.layoutChild( view, attributes );
	}

	protected View getOverridenWidget( Map<String, String> attributes )
	{
		View view = null;
		String childName = attributes.get( NAME );

		if ( childName == null )
			return null;

		for ( View viewExisting : mExistingViewsUnused )
		{
			if ( childName.equals( viewExisting.getTag() ) )
			{
				view = viewExisting;
				break;
			}
		}

		if ( view != null )
			mExistingViewsUnused.remove( view );

		return view;
	}

	protected View buildReadOnlyWidget( Map<String, String> attributes )
		throws Exception
	{
		// Hidden

		if ( TRUE.equals( attributes.get( HIDDEN ) ) )
			return null;

		// Masked (return an invisible View, so that we DO still
		// render a label and reserve some blank space)

		if ( TRUE.equals( attributes.get( MASKED ) ) )
		{
			TextView view = new TextView( getContext() );
			view.setVisibility( View.INVISIBLE );

			return view;
		}

		// Lookups

		String lookup = attributes.get( LOOKUP );

		if ( lookup != null && !"".equals( lookup ) )
			return new TextView( getContext() );

		String type = attributes.get( TYPE );

		// If no type, fail gracefully with a JTextField

		if ( type == null || "".equals( type ) )
			return new TextView( getContext() );

		// Lookup the Class

		Class<?> clazz = ClassUtils.niceForName( type );

		if ( clazz != null )
		{
			if ( clazz.isPrimitive() )
				return new TextView( getContext() );

			if ( String.class.equals( clazz ) )
				return new TextView( getContext() );

			if ( Date.class.equals( clazz ) )
				return new TextView( getContext() );

			if ( Boolean.class.equals( clazz ) )
				return new TextView( getContext() );

			if ( Number.class.isAssignableFrom( clazz ) )
				return new TextView( getContext() );

			// Collections

			if ( Collection.class.isAssignableFrom( clazz ) )
			{
				ListView listView = new ListView( getContext() );
				listView.setEnabled( false );

				// Must set an ArrayAdpater or Android will not scroll the screen correctly

				listView.setAdapter( new ResourcelessArrayAdapter<Object>( getContext(), Collections.emptySet() ) );
				return listView;
			}
		}

		// Not simple, but don't expand

		if ( TRUE.equals( attributes.get( DONT_EXPAND ) ) )
			return new TextView( getContext() );

		// Nested Metawidget

		return createMetawidget( attributes );
	}

	protected View buildActiveWidget( Map<String, String> attributes )
		throws Exception
	{
		// Hidden

		if ( TRUE.equals( attributes.get( HIDDEN ) ) )
			return null;

		String type = attributes.get( TYPE );

		// If no type, fail gracefully with an EditText

		if ( type == null || "".equals( type ) )
			return new EditText( getContext() );

		Class<?> clazz = ClassUtils.niceForName( type );

		if ( clazz != null )
		{
			if ( clazz.isPrimitive() )
			{
				// booleans

				if ( boolean.class.equals( clazz ) )
					return new CheckBox( getContext() );

				EditText editText = new EditText( getContext() );

				// DigitsInputMethod is 0-9 and +

				if ( byte.class.equals( clazz ) || short.class.equals( clazz ) || int.class.equals( clazz ) || long.class.equals( clazz ) )
					editText.setInputMethod( new DigitsInputMethod() );

				return editText;
			}

			// String Lookups

			String lookup = attributes.get( LOOKUP );

			if ( lookup != null && !"".equals( lookup ) )
			{
				List<String> lookupList = CollectionUtils.fromString( lookup );

				// (CollectionUtils.fromString returns unmodifiable EMPTY_LIST if empty)

				if ( !lookupList.isEmpty() )
					lookupList.add( 0, null );

				List<String> lookupLabelsList = null;
				String lookupLabels = attributes.get( LOOKUP_LABELS );

				if ( lookupLabels != null && !"".equals( lookupLabels ) )
				{
					lookupLabelsList = CollectionUtils.fromString( lookupLabels );

					// (CollectionUtils.fromString returns unmodifiable EMPTY_LIST if empty)

					if ( !lookupLabelsList.isEmpty() )
						lookupLabelsList.add( 0, null );
				}

				Spinner spinner = new Spinner( getContext() );
				spinner.setAdapter( new ResourcelessArrayAdapter<String>( getContext(), lookupList, lookupLabelsList ) );

				return spinner;
			}

			// Strings

			if ( String.class.equals( clazz ) )
			{
				EditText editText = new EditText( getContext() );

				if ( TRUE.equals( attributes.get( MASKED ) ) )
					editText.setTransformationMethod( PasswordTransformationMethod.getInstance() );

				if ( TRUE.equals( attributes.get( LARGE ) ) )
					editText.setMinLines( 3 );

				String maximumLength = attributes.get( MAXIMUM_LENGTH );

				if ( maximumLength != null && !"".equals( maximumLength ))
					editText.setFilters( new InputFilter[]{ new InputFilter.LengthFilter( Integer.parseInt( maximumLength )) });

				return editText;
			}

			// Dates

			if ( Date.class.equals( clazz ) )
			{
				EditText editText = new EditText( getContext() );
				editText.setInputMethod( new DateInputMethod() );

				return editText;
			}

			// Booleans (are tri-state)

			if ( Boolean.class.equals( clazz ) )
			{
				Spinner spinner = new Spinner( getContext() );
				spinner.setAdapter( new ResourcelessArrayAdapter<Boolean>( getContext(), LIST_BOOLEAN_VALUES, null ) );

				return spinner;
			}

			// Numbers

			if ( Number.class.isAssignableFrom( clazz ) )
			{
				EditText editText = new EditText( getContext() );

				// DigitsInputMethod is 0-9 and +

				if ( Byte.class.isAssignableFrom( clazz ) || Short.class.isAssignableFrom( clazz ) || Integer.class.isAssignableFrom( clazz ) || Long.class.isAssignableFrom( clazz ) )
					editText.setInputMethod( new DigitsInputMethod() );

				return editText;
			}

			// Collections

			if ( Collection.class.isAssignableFrom( clazz ) )
			{
				ListView listView = new ListView( getContext() );

				// Must set an ArrayAdpater or Android will not scroll the screen correctly

				listView.setAdapter( new ResourcelessArrayAdapter<Object>( getContext(), Collections.emptySet() ) );
				return listView;
			}
		}

		// Not simple, but don't expand

		if ( TRUE.equals( attributes.get( DONT_EXPAND ) ) )
			return new TextView( getContext() );

		// Nested Metawidget

		return createMetawidget( attributes );
	}

	protected void endBuild()
	{
		// End layout

		if ( mLayout != null )
		{
			for ( View viewExisting : mExistingViewsUnused )
			{
				mLayout.layoutChild( viewExisting, null );
			}

			mLayout.layoutEnd();
		}

		Log.d( "AndroidMetawidget", "Creation complete" );
	}

	protected Document inspect()
	{
		Log.d( "AndroidMetawidget", "Starting inspection: " + mPath );

		try
		{
			if ( mPath == null )
				return null;

			// If this Inspector has been set externally, use it...

			Inspector inspector = mInspector;

			if ( inspector == null )
			{
				if ( mInspectorConfig == 0 )
					throw MetawidgetException.newException( "No inspector or inspectorConfig specified" );

				// ...otherwise, if this InspectorConfig has already been read, use it...

				inspector = INSPECTORS.get( mInspectorConfig );

				// ...otherwise, initialize the Inspector

				if ( inspector == null )
				{
					inspector = new AndroidConfigReader( getContext() ).read( getContext().getResources().openRawResource( mInspectorConfig ) );
					INSPECTORS.put( mInspectorConfig, inspector );
				}
			}

			// Use the inspector to inspect the path

			return inspect( inspector, mPath );
		}
		finally
		{
			Log.d( "AndroidMetawidget", "Inspection complete. Starting creation" );
		}
	}

	protected Document inspect( Inspector inspector, String path )
	{
		TypeAndNames typeAndNames = PathUtils.parsePath( path );
		return inspector.inspect( mToInspect, typeAndNames.getType(), typeAndNames.getNames() );
	}

	protected AndroidMetawidget createMetawidget( Map<String, String> attributes )
		throws Exception
	{
		try
		{
			Constructor<? extends AndroidMetawidget> constructor = getClass().getConstructor( Context.class, getClass() );
			AndroidMetawidget metawidget = constructor.newInstance( getContext(), this );

			return metawidget;
		}
		catch ( Exception e )
		{
			throw MetawidgetException.newException( e );
		}
	}

	protected void initMetawidget( AndroidMetawidget metawidget, Map<String, String> attributes )
	{
		metawidget.setPath( mPath + StringUtils.SEPARATOR_FORWARD_SLASH + attributes.get( NAME ) );
		metawidget.setToInspect( mToInspect );
	}

	//
	//
	// Private methods
	//
	//

	private View findViewWithTag( String... tags )
	{
		if ( tags == null )
			return null;

		buildWidgets();

		// Search the root, as LinearLayout uses our ViewGroup directly
		// (eg. it doesn't further embed its own layout manager)

		View view = this;

		for ( int tagsLoop = 0, tagsLength = tags.length; tagsLoop < tagsLength; tagsLoop++ )
		{
			Object tag = tags[tagsLoop];
			view = view.findViewWithTag( tag );

			if ( view == null )
				break;

			if ( tagsLoop == tagsLength - 1 )
				return view;
		}

		// Search any child layouts

		for ( int loop = 0, length = getChildCount(); loop < length; loop++ )
		{
			view = getChildAt( loop );

			if ( !( view instanceof ViewGroup ))
				continue;

			for ( int tagsLoop = 0, tagsLength = tags.length; tagsLoop < tagsLength; tagsLoop++ )
			{
				Object tag = tags[tagsLoop];
				view = view.findViewWithTag( tag );

				if ( view == null )
					break;

				if ( tagsLoop == tagsLength - 1 )
					return view;
			}
		}

		return null;
	}

	//
	//
	// Inner class
	//
	//

	protected class AndroidMetawidgetMixin
		extends MetawidgetMixin<View>
	{
		//
		//
		// Public methods
		//
		//

		@Override
		protected void startBuild()
			throws Exception
		{
			AndroidMetawidget.this.startBuild();
		}

		@Override
		protected void addWidget( View view, Map<String, String> attributes )
		{
			AndroidMetawidget.this.addWidget( view, attributes );
		}

		@Override
		protected View getOverridenWidget( Map<String, String> attributes )
		{
			return AndroidMetawidget.this.getOverridenWidget( attributes );
		}

		@Override
		protected boolean isStub( View view )
		{
			return ( view instanceof Stub );
		}

		@Override
		protected Map<String, String> getStubAttributes( View stub )
		{
			return ( (Stub) stub ).getAttributes();
		}

		@Override
		protected boolean isMetawidget( View view )
		{
			// The MetawidgetMixin base class uses .getDeclaringClass, which
			// isn't implemented in m5-rc15

			return ( view instanceof AndroidMetawidget );
		}

		@Override
		protected View buildReadOnlyWidget( Map<String, String> attributes )
			throws Exception
		{
			return AndroidMetawidget.this.buildReadOnlyWidget( attributes );
		}

		@Override
		protected View buildActiveWidget( Map<String, String> attributes )
			throws Exception
		{
			return AndroidMetawidget.this.buildActiveWidget( attributes );
		}

		@Override
		public View initMetawidget( View widget, Map<String, String> attributes )
		{
			AndroidMetawidget metawidget = (AndroidMetawidget) widget;
			AndroidMetawidget.this.initMetawidget( metawidget, attributes );
			metawidget.setReadOnly( isReadOnly( attributes ) );
			metawidget.buildWidgets();

			return metawidget;
		}

		@Override
		protected void endBuild()
		{
			AndroidMetawidget.this.endBuild();
		}
	}
}
