var plusServices = angular.module('plusServices', []);
console.log("INIT services.");
plusServices.factory('ProvenanceService', ['$http', function($http) {
	console.log("INIT ProvenanceService");
	var p = Provenance();
    return p;
}]);

plusServices.factory('Owners', function($resource) {
    return $resource('/plus/api/feeds/objects/owners/'); // Note the full endpoint address
});

