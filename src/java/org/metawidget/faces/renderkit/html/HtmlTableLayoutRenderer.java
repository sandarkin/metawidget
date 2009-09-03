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

package org.metawidget.faces.renderkit.html;

import static org.metawidget.inspector.InspectionResultConstants.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UIData;
import javax.faces.component.UIInput;
import javax.faces.component.UIParameter;
import javax.faces.component.html.HtmlInputHidden;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.el.ValueBinding;

import org.metawidget.faces.FacesUtils;
import org.metawidget.faces.component.UIMetawidget;
import org.metawidget.faces.component.UIStub;
import org.metawidget.layout.iface.LayoutException;
import org.metawidget.util.simple.StringUtils;

/**
 * Layout to arrange components in a table, with one column for labels and another for the
 * component.
 * <p>
 * This implementation recognizes the following <code>&lt;f:facet&gt;</code> names:
 * <p>
 * <ul>
 * <li><code>header<code></li>
 * <li><code>footer<code></li>
 * </ul>
 * <p>
 * This implementation recognizes the following <code>&lt;f:param&gt;</code> parameters:
 * <p>
 * <ul>
 * <li><code>tableStyle</code>
 * <li><code>tableStyleClass</code>
 * <li><code>columns<code> - number of columns. Each label/component pair is considered one column
 * <li><code>columnClasses</code> - comma delimited string of CSS style classes to apply to table
 * columns in order of: label, component, required
 * <li><code>labelStyle</code> - CSS styles to apply to label column
 * <li><code>componentStyle</code> - CSS styles to apply to component column
 * <li><code>requiredStyle</code> - CSS styles to apply to required column
 * <li><code>sectionStyle</code>
 * <li><code>sectionStyleClass</code>
 * <li><code>headerStyle</code>
 * <li><code>headerStyleClass</code>
 * <li><code>footerStyle</code>
 * <li><code>footerStyleClass</code>
 * <li><code>rowClasses</code>
 * <li><code>labelSuffix</code> - defaults to a colon
 * </ul>
 * <p>
 * The parameters <code>columns</code> and <code>columnClasses</code> might more properly be named
 * <code>numberOfColumns</code> and <code>columnStyleClasses</code>, but we are trying to follow the
 * <code>javax.faces.component.html.HtmlDataTable</code> convention.
 *
 * @author Richard Kennard
 */

@SuppressWarnings( "deprecation" )
public class HtmlTableLayoutRenderer
	extends HtmlLayoutRenderer
{
	//
	// Private statics
	//

	private final static String	TABLE_PREFIX						= "table-";

	private final static String	ROW_SUFFIX							= "-row";

	private final static String	LABEL_CELL_SUFFIX					= "-label-cell";

	private final static String	COMPONENT_CELL_SUFFIX				= "-cell";

	private final static int	JUST_COMPONENT_AND_REQUIRED			= 2;

	private final static int	LABEL_AND_COMPONENT_AND_REQUIRED	= 3;

	//
	// Public methods
	//

	@Override
	public void encodeBegin( FacesContext context, UIComponent metawidget )
		throws IOException
	{
		( (UIMetawidget) metawidget ).putClientProperty( HtmlTableLayoutRenderer.class, null );
		super.encodeBegin( context, metawidget );

		ResponseWriter writer = context.getResponseWriter();

		layoutHiddenChildren( context, metawidget );

		// Start table

		writer.startElement( "table", metawidget );
		writer.writeAttribute( "id", metawidget.getClientId( context ), "id" );

		// Styles

		writeStyleAndClass( metawidget, writer, "table" );

		// Determine label, component, required styles

		State state = getState( metawidget );
		UIParameter parameterLabelStyle = FacesUtils.findParameterWithName( metawidget, "labelStyle" );

		if ( parameterLabelStyle != null )
			state.labelStyle = (String) parameterLabelStyle.getValue();

		UIParameter parameterComponentStyle = FacesUtils.findParameterWithName( metawidget, "componentStyle" );

		if ( parameterComponentStyle != null )
			state.componentStyle = (String) parameterComponentStyle.getValue();

		UIParameter parameterRequiredStyle = FacesUtils.findParameterWithName( metawidget, "requiredStyle" );

		if ( parameterRequiredStyle != null )
			state.requiredStyle = (String) parameterRequiredStyle.getValue();

		// Determine section styles

		UIParameter parameterSectionStyle = FacesUtils.findParameterWithName( metawidget, "sectionStyle" );

		if ( parameterSectionStyle != null )
			state.sectionStyle = (String) parameterSectionStyle.getValue();

		UIParameter parameterSectionStyleClass = FacesUtils.findParameterWithName( metawidget, "sectionStyleClass" );

		if ( parameterSectionStyleClass != null )
			state.sectionStyleClass = (String) parameterSectionStyleClass.getValue();

		// Determine inner styles

		UIParameter parameterColumnClasses = FacesUtils.findParameterWithName( metawidget, "columnClasses" );

		if ( parameterColumnClasses != null )
			state.columnClasses = ( (String) parameterColumnClasses.getValue() ).split( StringUtils.SEPARATOR_COMMA );

		UIParameter parameterRowClasses = FacesUtils.findParameterWithName( metawidget, "rowClasses" );

		if ( parameterRowClasses != null )
			state.rowClasses = ( (String) parameterRowClasses.getValue() ).split( StringUtils.SEPARATOR_COMMA );

		// Determine number of columns

		UIParameter parameterColumns = FacesUtils.findParameterWithName( metawidget, "columns" );

		if ( parameterColumns != null )
		{
			state.columns = Integer.parseInt( (String) parameterColumns.getValue() );

			if ( state.columns < 0 )
				throw LayoutException.newException( "columns must be >= 0" );
		}

		// Render header facet

		UIComponent componentHeader = metawidget.getFacet( "header" );

		if ( componentHeader != null )
		{
			writer.startElement( "thead", metawidget );
			writer.startElement( "tr", metawidget );
			writer.startElement( "td", metawidget );

			// Header spans multiples of label/component/required

			int colspan = Math.max( JUST_COMPONENT_AND_REQUIRED, state.columns * LABEL_AND_COMPONENT_AND_REQUIRED );
			writer.writeAttribute( "colspan", String.valueOf( colspan ), null );

			writeStyleAndClass( metawidget, writer, "header" );

			// Render facet

			FacesUtils.render( context, componentHeader );

			writer.endElement( "td" );
			writer.endElement( "tr" );
			writer.endElement( "thead" );
		}

		// Render footer facet (XHTML requires TFOOT come before TBODY)

		UIComponent componentFooter = metawidget.getFacet( "footer" );

		if ( componentFooter != null )
		{
			writer.startElement( "tfoot", metawidget );
			writer.startElement( "tr", metawidget );
			writer.startElement( "td", metawidget );

			// Footer spans multiples of label/component/required

			int colspan = Math.max( JUST_COMPONENT_AND_REQUIRED, state.columns * LABEL_AND_COMPONENT_AND_REQUIRED );
			writer.writeAttribute( "colspan", String.valueOf( colspan ), null );

			writeStyleAndClass( metawidget, writer, "footer" );

			// Render facet

			FacesUtils.render( context, componentFooter );

			writer.endElement( "td" );
			writer.endElement( "tr" );
			writer.endElement( "tfoot" );
		}

		writer.startElement( "tbody", metawidget );
	}

	/**
	 * layout any hidden child components first, before the table.
	 */

	protected void layoutHiddenChildren( FacesContext context, UIComponent metawidget )
		throws IOException
	{
		List<UIComponent> children = metawidget.getChildren();

		for ( UIComponent componentChild : children )
		{
			if ( !( componentChild instanceof HtmlInputHidden ) )
				continue;

			FacesUtils.render( context, componentChild );
		}
	}

	@Override
	public void encodeChildren( FacesContext context, UIComponent metawidget )
		throws IOException
	{
		State state = getState( metawidget );

		// (layoutChildren may get called even if layoutBegin crashed. Try
		// to fail gracefully)

		if ( state == null )
			return;

		List<UIComponent> children = metawidget.getChildren();

		// Next, for each child component...

		state.currentColumn = 0;
		state.currentRow = 0;
		state.currentSection = null;

		for ( UIComponent componentChild : children )
		{
			// ...that is visible...

			if ( componentChild instanceof UIStub )
			{
				boolean visibleChildren = false;

				for ( UIComponent stubChild : componentChild.getChildren() )
				{
					if ( !stubChild.isRendered() )
						continue;

					visibleChildren = true;
					break;
				}

				if ( !visibleChildren )
					continue;
			}

			if ( componentChild instanceof UIParameter )
				continue;

			if ( componentChild instanceof HtmlInputHidden )
				continue;

			if ( !componentChild.isRendered() )
				continue;

			// ...count columns...

			state.currentColumn++;

			// ...render a label...

			layoutBeforeChild( context, metawidget, componentChild );

			// ...and render the component

			layoutChild( context, metawidget, componentChild );
			layoutAfterChild( context, metawidget, componentChild );
		}
	}

	@Override
	public void encodeEnd( FacesContext context, UIComponent metawidget )
		throws IOException
	{
		ResponseWriter writer = context.getResponseWriter();
		writer.endElement( "tbody" );
		writer.endElement( "table" );
	}

	//
	// Protected methods
	//

	protected void layoutBeforeChild( FacesContext context, UIComponent metawidget, UIComponent childComponent )
		throws IOException
	{
		ResponseWriter writer = context.getResponseWriter();

		String cssId = getCssId( childComponent );

		// Section headings

		@SuppressWarnings( "unchecked" )
		Map<String, String> metadataAttributes = (Map<String, String>) childComponent.getAttributes().get( UIMetawidget.COMPONENT_ATTRIBUTE_METADATA );

		// (layoutBeforeChild may get called even if layoutBegin crashed. Try
		// to fail gracefully)

		State state = getState( metawidget );

		if ( metadataAttributes != null )
		{
			String section = metadataAttributes.get( SECTION );

			if ( section != null && !section.equals( state.currentSection ) )
			{
				state.currentSection = section;
				layoutSection( context, metawidget, section, childComponent );
				state.currentColumn = 1;
			}

			// Large components get a whole row

			boolean largeComponent = ( metawidget instanceof UIData || TRUE.equals( metadataAttributes.get( LARGE ) ) );

			if ( largeComponent && state.currentColumn != 1 )
			{
				writer.endElement( "tr" );
				state.currentColumn = 1;
			}
		}

		// Start a new row, if necessary

		if ( state.currentColumn == 1 || state.currentColumn > state.columns )
		{
			state.currentColumn = 1;

			writer.startElement( "tr", metawidget );

			if ( cssId != null )
				writer.writeAttribute( "id", TABLE_PREFIX + cssId + ROW_SUFFIX, null );

			writeRowStyleClass( metawidget, writer, state.currentRow );
			state.currentRow++;
		}

		// Start the label column

		boolean labelWritten = layoutLabel( context, metawidget, childComponent );

		// Zero-column layouts need an extra row
		// (though we colour it the same from a CSS perspective)

		if ( labelWritten && state.columns == 0 )
		{
			writer.endElement( "tr" );
			writer.startElement( "tr", metawidget );

			if ( cssId != null )
				writer.writeAttribute( "id", TABLE_PREFIX + cssId + ROW_SUFFIX + "2", null );

			writeRowStyleClass( metawidget, writer, state.currentRow );
		}

		// Start the component column

		writer.startElement( "td", metawidget );

		if ( cssId != null )
			writer.writeAttribute( "id", TABLE_PREFIX + cssId + COMPONENT_CELL_SUFFIX, null );

		// CSS

		if ( state.componentStyle != null )
			writer.writeAttribute( "style", state.componentStyle, null );

		writeColumnStyleClass( metawidget, writer, 1 );

		// Colspan

		int colspan;

		// Metawidgets, tables and large components span all columns

		if ( childComponent instanceof UIMetawidget || childComponent instanceof UIData || ( metadataAttributes != null && TRUE.equals( metadataAttributes.get( LARGE ) ) ) )
		{
			colspan = ( state.columns * LABEL_AND_COMPONENT_AND_REQUIRED ) - 2;
			state.currentColumn = state.columns;

			if ( !labelWritten )
				colspan++;

			// Nested table Metawidgets span the required column too (as they have their own
			// required column)

			if ( childComponent instanceof UIMetawidget && "table".equals( childComponent.getRendererType() ) )
				colspan++;
		}

		// Components without labels span two columns

		else if ( !labelWritten )
		{
			colspan = 2;
		}

		// Everyone else spans just one

		else
		{
			colspan = 1;
		}

		if ( colspan > 1 )
			writer.writeAttribute( "colspan", String.valueOf( colspan ), null );
	}

	/**
	 * @return whether a label was written
	 */

	@Override
	protected boolean layoutLabel( FacesContext context, UIComponent metawidget, UIComponent componentNeedingLabel )
		throws IOException
	{
		@SuppressWarnings( "unchecked" )
		Map<String, String> metadataAttributes = (Map<String, String>) componentNeedingLabel.getAttributes().get( UIMetawidget.COMPONENT_ATTRIBUTE_METADATA );
		String label = ( (UIMetawidget) componentNeedingLabel.getParent() ).getLabelString( context, metadataAttributes );

		if ( label == null )
			return false;

		ResponseWriter writer = context.getResponseWriter();
		writer.startElement( "th", metawidget );

		String cssId = getCssId( componentNeedingLabel );
		if ( cssId != null )
			writer.writeAttribute( "id", TABLE_PREFIX + cssId + LABEL_CELL_SUFFIX, null );

		// CSS

		State state = getState( metawidget );

		if ( state.labelStyle != null )
			writer.writeAttribute( "style", state.labelStyle, null );

		writeColumnStyleClass( metawidget, writer, 0 );

		super.layoutLabel( context, metawidget, componentNeedingLabel );

		writer.endElement( "th" );

		return true;
	}

	protected void layoutSection( FacesContext context, UIComponent metawidget, String section, UIComponent childComponent )
		throws IOException
	{
		// Blank section?

		if ( "".equals( section ) )
			return;

		ResponseWriter writer = context.getResponseWriter();

		writer.startElement( "tr", metawidget );
		writer.startElement( "th", metawidget );

		// Sections span multiples of label/component/required

		State state = getState( metawidget );
		int colspan = Math.max( JUST_COMPONENT_AND_REQUIRED, state.columns * LABEL_AND_COMPONENT_AND_REQUIRED );
		writer.writeAttribute( "colspan", String.valueOf( colspan ), null );

		// CSS

		if ( state.sectionStyle != null )
			writer.writeAttribute( "style", state.sectionStyle, null );

		if ( state.sectionStyleClass != null )
			writer.writeAttribute( "class", state.sectionStyleClass, null );

		// Section name (possibly localized)

		HtmlOutputText output = (HtmlOutputText) context.getApplication().createComponent( "javax.faces.HtmlOutputText" );

		String localizedSection = ( (UIMetawidget) childComponent.getParent() ).getLocalizedKey( context, StringUtils.camelCase( section ) );

		if ( localizedSection != null )
			output.setValue( localizedSection );
		else
			output.setValue( section );

		FacesUtils.render( context, output );

		writer.endElement( "th" );
		writer.endElement( "tr" );
	}

	protected void layoutAfterChild( FacesContext context, UIComponent metawidget, UIComponent childComponent )
		throws IOException
	{
		ResponseWriter writer = context.getResponseWriter();

		// End the component column

		writer.endElement( "td" );

		// Render the 'required' column

		State state = getState( metawidget );

		if ( ( childComponent instanceof UIMetawidget && "table".equals( childComponent.getRendererType() ) ) || !childComponent.getAttributes().containsKey( UIMetawidget.COMPONENT_ATTRIBUTE_METADATA ) )
		{
			// (except embedded Metawidgets, which have their own required
			// column)
		}
		else
		{
			writer.startElement( "td", metawidget );

			// CSS

			if ( state.requiredStyle != null )
				writer.writeAttribute( "style", state.requiredStyle, null );

			writeColumnStyleClass( metawidget, writer, 2 );

			layoutRequired( context, metawidget, childComponent );

			writer.endElement( "td" );
		}

		// End the row, if necessary

		if ( state.currentColumn >= state.columns )
		{
			state.currentColumn = 0;
			writer.endElement( "tr" );
		}
	}

	protected void layoutRequired( FacesContext context, UIComponent metawidget, UIComponent child )
		throws IOException
	{
		@SuppressWarnings( "unchecked" )
		Map<String, String> attributes = (Map<String, String>) child.getAttributes().get( UIMetawidget.COMPONENT_ATTRIBUTE_METADATA );

		ResponseWriter writer = context.getResponseWriter();

		if ( attributes != null )
		{
			if ( TRUE.equals( attributes.get( REQUIRED ) ) && !TRUE.equals( attributes.get( READ_ONLY ) ) && !( (UIMetawidget) metawidget ).isReadOnly() )
			{
				// (UIStubs can have attributes="required: true")

				if ( child instanceof UIInput || child instanceof UIStub )
				{
					writer.write( "*" );
					return;
				}
			}
		}

		// Render an empty div, so that the CSS can force it to a certain
		// width if desired for the layout (browsers seem to not respect
		// widths set on empty table columns)

		writer.startElement( "div", metawidget );
		writer.endElement( "div" );
	}

	protected String getCssId( UIComponent metawidget )
	{
		ValueBinding binding = metawidget.getValueBinding( "value" );

		if ( binding == null )
			return null;

		return StringUtils.camelCase( FacesUtils.unwrapExpression( binding.getExpressionString() ), StringUtils.SEPARATOR_DOT_CHAR );
	}

	protected void writeColumnStyleClass( UIComponent metawidget, ResponseWriter writer, int columnStyleClass )
		throws IOException
	{
		State state = getState( metawidget );

		// Note: As per the JSF spec, columnClasses do not repeat like rowClasses do. See...
		//
		// http://java.sun.com/javaee/javaserverfaces/1.2_MR1/docs/renderkitdocs/HTML_BASIC/javax.faces.Datajavax.faces.Table.html
		//
		// ...where it says 'If the number of [styleClasses] is less than the number of
		// columns specified in the "columns" attribute, no "class" attribute is output for each
		// column greater than the number of [styleClasses]'

		if ( state.columnClasses == null || state.columnClasses.length <= columnStyleClass )
			return;

		String columnClass = state.columnClasses[columnStyleClass];

		if ( columnClass.length() == 0 )
			return;

		writer.writeAttribute( "class", columnClass.trim(), null );
	}

	protected void writeRowStyleClass( UIComponent metawidget, ResponseWriter writer, int rowStyleClass )
		throws IOException
	{
		State state = getState( metawidget );

		if ( state.rowClasses == null )
			return;

		String rowClass = state.rowClasses[rowStyleClass % state.rowClasses.length];

		if ( rowClass.length() == 0 )
			return;

		writer.writeAttribute( "class", rowClass.trim(), null );
	}

	//
	// Private methods
	//

	/* package private */State getState( UIComponent metawidget )
	{
		State state = (State) ( (UIMetawidget) metawidget ).getClientProperty( HtmlTableLayoutRenderer.class );

		if ( state == null )
		{
			state = new State();
			( (UIMetawidget) metawidget ).putClientProperty( HtmlTableLayoutRenderer.class, state );
		}

		return state;
	}

	//
	// Inner class
	//

	/**
	 * Simple, lightweight structure for saving state.
	 */

	/* package private */class State
	{
		/* package private */int		currentColumn;

		/* package private */int		columns	= 1;

		/* package private */int		currentRow;

		/* package private */String		currentSection;

		/* package private */String		labelStyle;

		/* package private */String		componentStyle;

		/* package private */String		requiredStyle;

		/* package private */String		sectionStyle;

		/* package private */String		sectionStyleClass;

		/* package private */String[]	columnClasses;

		/* package private */String[]	rowClasses;
	}
}
