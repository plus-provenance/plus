var plusDirectives = angular.module('plusDirectives', []);
console.log("INIT directives.");
plusDirectives.directive('nodelist', function() {
  return {
      restrict: 'EA',      
      scope: { 
    	  graph : "=graph",
    	  title : "=title",
      },
      templateUrl: '/plus/angular/templates/nodelist.html'
  };
});

plusDirectives.directive('actors', function() {
	  return {
	      restrict: 'EA',      
	      scope: { 
	    	  graph : "=graph",
	      },
	      templateUrl: '/plus/angular/templates/actors.html'
	  };	
});