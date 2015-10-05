var plusControllers = angular.module('plusControllers', ['ngAnimate', 'ngResource', 'ui.bootstrap']);
console.log("INIT controllers.");

plusControllers.controller('GraphCtrl', ['$scope', '$http', '$routeParams', 'ProvenanceService', function($scope, $http, $routeParams, ProvenanceService) {
	var __settings = {
		n: 50,   // Max number of nodes per graph to return.
		maxHops: 8, // Max distance from starting point to fetch
		includeNodes: true, // whether results should include nodes
		includeEdges: true, // whether results should include edges
		includeNPEs: true, // whether results include non-provenance IDs
		followNPIDs: false, // whether graph discovery goes through NPIDs or not.
		forward: true, // whether to look forward
		backward: true, // whether to look backward
		breadthFirst: true
	};	
	
	// First, ensure settings are correct or twiddle them if they're
	// not.   That way we don't have to trust the UI layer to get it
	// right.
	if(isNaN(parseInt(this.__settings.n))) { this.__settings.n = 20; } 
	if(isNaN(parseInt(this.__settings.maxHops))) { this.__settings.maxHops = 8; }
	
	if(this.__settings.n < 5) { this.__settings.n = 5; }
	if(this.__settings.n > 200) { this.__settings.n = 200; }
	if(this.__settings.maxHops > 200) { this.__settings.maxHops = 200; } 
	if(this.__settings.maxHops < 1) { this.__settings.maxHops = 1; } 
	
	var url = "/plus/api/graph/" + $routeParams.oid + "?" + 
	       "n=" + __settings.n + "&" + 
	       "maxHops=" + __settings.maxHops + "&" + 
	       "includeNodes=" + __settings.includeNodes + "&" +
	       "includeEdges=" + __settings.includeEdges + "&" + 
	       "includeNPEs=" + __settings.includeNPEs + "&" + 
	       "followNPIDs=" + __settings.followNPIDs + "&" +  
	       "forward=" + __settings.forward + "&" + 
	       "backward=" + __settings.backward + "&" + 
	       "breadthFirst=" + __settings.breadthFirst;
	
	$http.get(url).success(function (data) {
		$scope.graph = data;
	});
}]);

plusControllers.controller('ActorCtrl', ['$scope', '$http', '$routeParams', 'ProvenanceService', function($scope, $http, $routeParams, ProvenanceService) {
	$http.get('/plus/api/actor/' + $routeParams.aid).success(function(data) {
		$scope.actor = data;
	});
}]);

plusControllers.controller('ProvCtrl', ['$scope', '$http', '$routeParams', 'ProvenanceService', function($scope, $http, $routeParams, ProvenanceService) {
	$http.get('/plus/api/object/' + $routeParams.oid).success(function(data) {
		$scope.object = data.nodes[0];
	});	
}]);

plusControllers.controller('MainCtrl', ['$scope', '$sce', '$http', 'ProvenanceService', function($scope, $sce, $http, ProvenanceService) {
   console.log("MainCtrl");
      
   $scope.dashboardQuery = function(q) {
	   var optionsWithURLs = {
		 'connectedData' : "/plus/api/feeds/connectedData?n=10&format=json",
		 'hashedContent' : "/plus/api/feeds/hashedContent?n=10&format=json",
	   };

	   if(!optionsWithURLs[q]) {
		   console.log("Invalid query '" + q + "'");
		   return;
	   }

	   $http({
		   method: 'GET',
		   url: optionsWithURLs[q]
	   }).then(function successCallback(response) {
		   var g = ProvenanceGraph(response);
		   $scope.dashboardQueryResults = g.d3json; 
	   }, function errorCallback(response) {
		   console.log("Error calling " + url);
	   });
   };
   
   $scope.getOwners = function() {
		ProvenanceService.getOwners({
			success: function(g) {
				$scope.$apply(function() { $scope.owners = g.d3json; });
			},
			error: function() { console.log("Error fetching owners"); }
		});				
   };
   
   $scope.getNonProvenanceData = function() {
		ProvenanceService.getLatestNPIDs({
			success: function(g) {
				$scope.$apply(function() { $scope.npids = g.d3json; });
			},
			error: function() { console.log("Couldn't fetch latest non-provenance edges"); }
		});
   };
   
   $scope.getLatestObjects = function() { 
	   $scope.latestObjects = null;
	   $scope.latestObjectsError = null;

	   ProvenanceService.getLatestObjects({
		   success: function(g) {
			   $scope.$apply(function () { $scope.latestObjects = g.d3json; })				
		   },
		   error: function() { $scope.latestObjectsError = "Couldn't fetch latest reported provenance"; }
	   });  
   };
   
   $scope.searchProvenance = function() {    
	   console.log("Got " + $scope.searchTerm);
	   ProvenanceService.search({
		  searchTerm : $scope.searchTerm,
		  success : function(g) {		  
			  $scope.$apply(function () { $scope.searchResults = g.d3json; }) 
		  }, 
		  error: function() { console.log("Badness: " + $scope.searchTerm); }
	   });	   
   };
}]);