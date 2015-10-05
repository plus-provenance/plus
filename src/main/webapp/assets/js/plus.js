var app = angular.module("plus", ['ui.bootstrap', 'ngResource', 'ngRoute', 'plusControllers', 'plusServices', 'plusDirectives']);
console.log("INIT plus ");

app.config(['$routeProvider',
            function($routeProvider) {
	$routeProvider.
		when('/home', {
			templateUrl: 'templates/main.html',
			controller: 'MainCtrl'
		}).
		when('/graph/:oid', {
			templateUrl: 'templates/graph.html',
			controller: 'GraphCtrl'
		}).
		when('/actors/:aid', {
			templateUrl: 'templates/actor.html',
			controller: 'ActorCtrl'
		}).
		when('/objects/:oid', {
			templateUrl: 'templates/object.html',
			controller: 'ProvCtrl'
		}).
		otherwise({
			redirectTo: '/home'
		});
}]);
