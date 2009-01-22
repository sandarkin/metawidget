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

import java.awt.{BorderLayout, Color, GridLayout}
import java.awt.event.{ActionEvent, ActionListener}
import java.util.HashMap
import javax.swing._
import javax.swing.border._

import org.metawidget.swing._
import org.metawidget.swing.layout._
import org.metawidget.swing.propertybinding.beanutils._
import org.metawidget.inspector.annotation._

/**
 * @author Richard Kennard
 */

package org.metawidget.example.swing.animalraces
{
	//
	// Model class
	//

	class Animal(
		var name:String,
		
		@UiComesAfter( Array( "name" )) @UiAttribute{ val name = "minimum-value", val value = "0" }
		var delay:int,
		
		@UiLookup( Array( "Elephant", "Hippo", "Panda" )) @UiRequired @UiComesAfter
		var animal:String
	)

	//
	// UI
	//

	object AnimalRaces
	{
		// Model instance
		
		val elephant = new Animal( "Eddie", 10, "Elephant" )
		val hippo = new Animal( "Harry", 5, "Hippo" )
		val panda = new Animal( "Paula", 2, "Panda" )

		// Look and feel
				
		UIManager.getInstalledLookAndFeels().foreach
		{
			info => if ( "Nimbus".equals( info.getName() ) )
			{
				UIManager.setLookAndFeel( info.getClassName() )
			}
		}
		
		// Toolbar (3 Metawidgets in a row)
		
		val elephantMetawidget = newAnimalMetawidget( elephant )
		val hippoMetawidget = newAnimalMetawidget( hippo )
		val pandaMetawidget = newAnimalMetawidget( panda )
		
		private def newAnimalMetawidget( animal:Animal ):SwingMetawidget =
		{
			val metawidget = new SwingMetawidget()
			metawidget.setInspectorConfig( "org/metawidget/example/swing/animalraces/inspector-config.xml" )
			metawidget.setPropertyBindingClass( classOf[ BeanUtilsBinding ])
			metawidget.setParameter( "propertyStyle", BeanUtilsBinding.PROPERTYSTYLE_SCALA )
			metawidget.setLayoutClass( classOf[ MigLayout ])
			metawidget.setToInspect( animal )
			metawidget.getLayout().asInstanceOf[net.miginfocom.swing.MigLayout].setLayoutConstraints( new net.miginfocom.layout.LC().insets( "10" ));
			
			return metawidget
		}
		
		def toolbar = new JPanel
		{
			setLayout( new GridLayout( 1, 3 ))
			setBorder( BorderFactory.createEtchedBorder() )
			add( elephantMetawidget )
			add( hippoMetawidget )
			add( pandaMetawidget )
		}
		
		// Labels and animation timers
		
		val images = new HashMap[String, ImageIcon]
		images.put( "Elephant", new ImageIcon( getClass().getResource( "/org/metawidget/example/swing/animalraces/media/elephant.png" )))
		images.put( "Hippo", new ImageIcon( getClass().getResource( "/org/metawidget/example/swing/animalraces/media/hippo.png" )))
		images.put( "Panda", new ImageIcon( getClass().getResource( "/org/metawidget/example/swing/animalraces/media/panda.png" )))		
	
		val elephantLabel = new JLabel( elephant.name, images.get( "Elephant" ), SwingConstants.CENTER )
		val hippoLabel = new JLabel( hippo.name, images.get( "Hippo" ), SwingConstants.CENTER )
		val pandaLabel = new JLabel( panda.name, images.get( "Panda" ), SwingConstants.CENTER )

		val elephantTimer = newTimer( elephantLabel )
		val hippoTimer = newTimer( hippoLabel )
		val pandaTimer = newTimer( pandaLabel )

		def newTimer( label:JLabel ):Timer =
		{
			implicit def actionPerformedWrapper(func: (ActionEvent) => Unit) = new ActionListener { def actionPerformed(e:ActionEvent) = func(e) }

			val timer = new Timer( 0, ((e:ActionEvent) => if ( label.getLocation().x < mainFrame.getWidth() - 200 ) label.setLocation( label.getLocation().x + 1, label.getLocation().y )))
			return timer
		}
		
		// Racetrack
		
		def racetrack = new JPanel
		{
			setLayout( null )
			setBackground( new Color( 192, 255, 192 ))
			setBorder( BorderFactory.createCompoundBorder( BorderFactory.createLineBorder( new Color( 141, 195, 141 ), 5 ), BorderFactory.createLineBorder( new Color( 147, 233, 147 ), 5 )))

			addLabel( elephantLabel, 0 )
			addLabel( hippoLabel, 200 )
			addLabel( pandaLabel, 400 )
			
			@UiAction
			def startRace()
			{
				stopRace()
				
				startRace( elephantMetawidget, elephantLabel, elephantTimer )
				startRace( hippoMetawidget, hippoLabel, hippoTimer )
				startRace( pandaMetawidget, pandaLabel, pandaTimer )
			}

			@UiAction
			def stopRace()
			{
				stopRace( elephantLabel, elephantTimer )		
				stopRace( hippoLabel, hippoTimer )		
				stopRace( pandaLabel, pandaTimer )		
			}
			
			@UiAction
			@UiComesAfter
			def close()
			{
				System.exit( 0 )
			}
			
			private def addLabel( label:JLabel, top:int )
			{
				label.setVerticalTextPosition( 1 )
	   			label.setHorizontalTextPosition( 0 )
	   			label.setLocation( 0, top );
				label.setSize( 200, 200 );
				add( label )				
			}

			private def startRace( metawidget:SwingMetawidget, label:JLabel, timer:Timer )
			{
				metawidget.save()
				val animal = metawidget.getToInspect().asInstanceOf[Animal]
				label.setText( animal.name )
				label.setIcon( images.get( animal.animal ))				
				timer.setDelay( animal.delay * 20 )
				timer.start()
			}
			
			private def stopRace( label:JLabel, timer:Timer )
			{
				timer.stop()
				label.setLocation( 0, label.getLocation().y );
			}
		}

		// Status bar (a Metawidget hooked into 'racetrack')
		
		val statusMetawidget = new SwingMetawidget();
		statusMetawidget.setInspectorConfig( "org/metawidget/example/swing/animalraces/inspector-config.xml" )
		statusMetawidget.setLayoutClass( classOf[ FlowLayout ])
		statusMetawidget.setToInspect( racetrack )			
		statusMetawidget.setBorder( BorderFactory.createEtchedBorder() )

		// JFrame
		
		val mainFrame = new JFrame( "Animal Races" )
		mainFrame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE )
		mainFrame.getContentPane().add( toolbar, BorderLayout.NORTH )
		mainFrame.getContentPane().add( racetrack, BorderLayout.CENTER )
		mainFrame.getContentPane().add( statusMetawidget, BorderLayout.SOUTH )
		mainFrame.setSize( 600, 790 )

		//
		// Main method
		//
		
		def main(args : Array[String]) =
		{
			mainFrame.setVisible( true )
		}		
	}
}
