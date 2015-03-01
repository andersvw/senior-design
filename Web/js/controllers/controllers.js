"use strict";

/* Controllers */

var app = angular.module('kaching.controllers', []);

app.controller('MainCtrl', ['$rootScope', '$scope', 
	function($rootScope, $scope){
		
		$scope.titleCheck = function(){
			var title = $rootScope.title;
			return angular.isUndefined(title) || title === null || title == 'Login' || title == 'Sign Up';
		}

	}
]);

app.controller('LogoutCtrl', ['$location', '$scope', '$window', 'LoginFactory', 
	function($location, $scope, $window, LoginFactory){

		$scope.userInfo = LoginFactory.getUserInfo();

		$scope.logout = function(){
			$window.sessionStorage["userInfo"] = null;
			LoginFactory.setUserInfo({});
			$location.path("/Not/A/Real/URL");
		}

	}
]);

app.controller('LoginCtrl', ['$location', '$scope', '$window', 'LoginFactory', 
	function($location, $scope, $window, LoginFactory){
		
		$scope.login = function(){
			LoginFactory.login($scope.user.username, $scope.user.password)
				.then(function (result){
					$location.path("/");
					$scope.userInfo = result;
				}, function (error){
					$window.alert("Invalid credentials");
				});
		};

	}
]);

app.controller('SignupCtrl', ['$location', '$scope', '$window', 'LoginFactory', 
	function($location, $scope, $window, LoginFactory){

		$scope.signup = function(){
			if($scope.user.password != null && $scope.user.password2 != null){
				if($scope.user.password !== $scope.user.password2){
					$window.alert("Passwords don't match");
				}
				else{
					LoginFactory.signup($scope.user.username, $scope.user.password)
						.then(function (result){
							$scope.userInfo = {
				                user_id: result['id'],
				                username: result['username']
				            };
							$window.sessionStorage["userInfo"] = JSON.stringify($scope.userInfo);
							LoginFactory.setUserInfo($scope.userInfo);
							$location.path("/");
						}, function (error){
							$window.alert("That username is already taken");
						});
				}
			}
		}

		$scope.cancel = function(){
			$location.path("/login");
		}

	}
]);

app.controller('AccountsCtrl', ['$location', '$scope', '$rootScope', '$window', 'AccountsFactory', 
	function($location, $scope, $rootScope, $window, AccountsFactory){

		AccountsFactory.listAll(JSON.parse($window.sessionStorage['userInfo']).user_id)
			.then(function (result){

				for(var i = 0; i < result.cash_accounts.length; i++){
					//Force decimal values to show 2 decimal places 
					result.cash_accounts[i].balance = "$" + Number(result.cash_accounts[i].balance).toFixed(2);

					//Force interest rate to also show a % sign
					result.cash_accounts[i].interest_rate += "%";
				}
				$scope.cash_accounts = result.cash_accounts;

				for(var i = 0; i < result.credit_accounts.length; i++){
					//Force decimal values to show 2 decimal places 
					result.credit_accounts[i].balance = "$" + Number(result.credit_accounts[i].balance).toFixed(2);
					result.credit_accounts[i].limit = "$" + Number(result.credit_accounts[i].limit).toFixed(2);
				}
				$scope.credit_accounts = result.credit_accounts;
			});

		$scope.viewHistory = function(account_number){
			$location.path("/history/" + account_number);
			$rootScope.historyNumber = account_number;
		}

	}
]);

app.controller('HistoryCtrl', ['$location', '$scope', '$rootScope', '$window', 'AccountsFactory', 'HistoryFactory',
	function($location, $scope, $rootScope, $window, AccountsFactory, HistoryFactory){
		
		/*
		 * Decided to add $rootScope to allow the passing of the account number from the Accounts page
		 * instead of parsing URL for account number (which could be a security issue similar to SQL injection)
		 * then having to do a check against the user_id's list of all accounts and the given number
		 */


		//Parses the URL path for the desired account number
		function getNumber(){
			return $location.path().split("/")[2];
		}

		$scope.history = [];

		HistoryFactory.listAll($rootScope.historyNumber)
			.then(function(result){

				for(var i = 0; i < result.history.length; i++){
					$scope.history.push(result.history[i]);

					//Set the color of the amount displayed
					//Force decimal values to show 2 decimal places
					if(Number(result.history[i].amount) > 0){
						$scope.history[i].display_color = "green";
						$scope.history[i].amount = "+$" + Number(result.history[i].amount).toFixed(2);
					}
					else if (Number(result.history[i].amount) < 0){
						$scope.history[i].display_color = "red";
						$scope.history[i].amount = "-$" + Number(-result.history[i].amount).toFixed(2);
					} else{
						$scope.history[i].display_color = "white";
						$scope.history[i].amount = "$" + Number(result.history[i].amount).toFixed(2);
					}
				}

			});

		//Callback function for the "Go back" button to return to the Accounts page
		$scope.cancel = function(){
			$location.path("/accounts");
		}

	}
]);

app.controller('TestCtrl', ['$location', '$scope', '$rootScope', '$window', 'AccountFactory',
	function($location, $scope, $rootScope, $window, AccountFactory){

		$scope.account = {};
		$scope.accounts = [];

		$scope.listAccounts = function(){
			AccountFactory.listAll(JSON.parse($window.sessionStorage['userInfo']).user_id)
				.then(function (result){

				for(var i = 0; i < result.accounts.length; i++){
					//Force decimal values to show 2 decimal places 
					result.accounts[i].balance = "$" + Number(result.accounts[i].balance).toFixed(2);

					//Force interest rate to also show a % sign
					result.accounts[i].interest_rate += "%";
				}

				$scope.accounts = result.accounts;
			});
		}

		$scope.createAccount = function(){
           var newAccount = {   
                user_id : JSON.parse($window.sessionStorage['userInfo']).user_id,
                number : '1111111116',
                balance : 1.00,
                name : 'Test Account',
                type : 'Checking',
                interest_rate : 0.0000
            };

			$scope.account = newAccount;

            AccountFactory.createAccount(newAccount);
		}

		$scope.updateAccount = function(){
			var updatedAccount = {   
                user_id : JSON.parse($window.sessionStorage['userInfo']).user_id,
                number : '1111111116',
                balance : 1000.00,
                name : 'Test Account',
                type : 'Savings',
                interest_rate : 10.0000
			};

			$scope.account = updatedAccount;

            AccountFactory.updateAccount(updatedAccount);

		}

		$scope.deleteAccount = function(){

			$scope.account = {};

			AccountFactory.deleteAccount('1111111116');

		}

	}
]);

app.controller('TransferCtrl', ['$location', '$scope', '$rootScope', '$window', 'AccountFactory', 
	function($location, $scope, $rootScope, $window, AccountFactory){

		$scope.accountsList = [];

		AccountFactory.listAll(JSON.parse($window.sessionStorage['userInfo']).user_id)
			.then(function (result){

				for(var i = 0; i < result.cash_accounts.length; i++){
					//Force decimal values to show 2 decimal places 
					result.cash_accounts[i].balance = Number(result.cash_accounts[i].balance).toFixed(2);

					$scope.accountsList.push(result.cash_accounts[i].name + " XXXXXX" 
						+ result.cash_accounts[i].number.substring(6, 11) + " (Avail. balance = $" 
						+ result.cash_accounts[i].balance + ")");
				}

				$scope.accounts = result.cash_accounts;

			});

		$scope.fromAccount = "Select Any";
		$scope.toAccount = "Select Any";

		$scope.setFromAccount = function(fromAccount){
			$scope.fromAccount = fromAccount;
		}

		$scope.setToAccount = function(toAccount){
			$scope.toAccount = toAccount;
		}

		$scope.transfer = function(){
			var fromIndex = $scope.accountsList.indexOf($scope.fromAccount);
			var toIndex = $scope.accountsList.indexOf($scope.toAccount);

			var transferAmount = parseInt($scope.transferAmount);
			$scope.accounts[fromIndex].balance = parseFloat($scope.accounts[fromIndex].balance) - transferAmount;
			$scope.accounts[toIndex].balance = parseFloat($scope.accounts[toIndex].balance) + transferAmount;

            AccountFactory.updateAccount($scope.accounts[fromIndex]);
            AccountFactory.updateAccount($scope.accounts[toIndex]);
		}
	}
]);

