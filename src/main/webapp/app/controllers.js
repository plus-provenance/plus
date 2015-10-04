var plusControllers = angular.module('plusControllers', ['ngAnimate', 'ngResource', 'ui.bootstrap']);
console.log("INIT controllers.");

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