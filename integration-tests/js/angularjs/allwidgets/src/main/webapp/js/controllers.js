'use strict';

/* Controllers */

function AllWidgetsController( $scope ) {

	$scope.allWidgets = allWidgets;
	
	$scope.metawidgetConfig = {

		inspector: new metawidget.inspector.CompositeInspector( [ function( toInspect, type ) {

			switch ( type ) {
				case 'allWidgets':
					return metawidget.test.allWidgetsMetadata;
				case 'allWidgets.nestedWidgets':
					return metawidget.test.nestedWidgetsMetadata;
				case 'allWidgets.readOnlyNestedWidgets':
					return metawidget.test.nestedWidgetsMetadata;
				case 'allWidgets.nestedWidgetsDontExpand':
					return metawidget.test.nestedWidgetsMetadata;
			}					
		}, new metawidget.inspector.PropertyTypeInspector() ] )
	};
}
