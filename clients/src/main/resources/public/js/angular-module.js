"use strict";

const app = angular.module('app', ['ui.bootstrap']);

// Fix for unhandled rejections bug.
app.config(['$qProvider', function ($qProvider) {
    $qProvider.errorOnUnhandledRejections(false);
}]);

app.controller('Controller', function($http, $location, $uibModal) {
    const app = this;
    const apiBaseURL = "/";
    let peers = [];
    $http.get(apiBaseURL + "me").then((response) => app.thisNode = response.data.me);
    $http.get(apiBaseURL + "peers").then((response) => peers = response.data.peers);

    app.openCreateModal = () => {
        const createModal = $uibModal.open({
            templateUrl: 'createModal.html',
            controller: 'CreateModalCtrl',
            controllerAs: 'createModal',
            resolve: {
                app: () => app,
                apiBaseURL: () => apiBaseURL,
                peers: () => peers
            }
        });

        createModal.result.then(() => {}, () => {});
    };

    app.openPlayModal = (game, opponent) => {
            const playModal = $uibModal.open({
                templateUrl: 'playModal.html',
                controller: 'PlayModalCtrl',
                controllerAs: 'playModal',
                resolve: {
                    app: () => app,
                    apiBaseURL: () => apiBaseURL,
                    game: () => game,
                    opponent: () => opponent
                }
            });
            playModal.result.then(() => {}, () => {});
    };

    app.getActiveGames = () => $http.get(apiBaseURL + "activeGames")
        .then((response) => app.activeGames = Object.keys(response.data)
            .map((key) => response.data[key].state.data)
            .reverse());

    app.getPastGames = () => $http.get(apiBaseURL + "pastGames")
            .then((response) => app.pastGames = Object.keys(response.data)
                .map((key) => response.data[key].state.data)
                .reverse());

    // update data
    app.getActiveGames();
    app.getPastGames();
});

// ============= MODAL INSTANCE CONTROLLERS =====================

app.controller('CreateModalCtrl', function ($http, $location, $uibModalInstance, $uibModal, app, apiBaseURL, peers) {
    const createModal = this;

    createModal.peers = peers;
    createModal.form = {};
    createModal.formError = false;

    createModal.create = function createGame() {
        if (createModal.form.opponent == null) {
            createModal.formError = true;
        } else {
            createModal.formError = false;
            $uibModalInstance.close();

            let CREATE_GAME_PATH = apiBaseURL + "createGame"

            let createGameData = $.param({
                opponent: createModal.form.opponent,
            });

            let createGameHeaders = {
                headers : {
                    "Content-Type": "application/x-www-form-urlencoded"
                }
            };

            // Create IOU  and handles success / fail responses.
            $http.post(CREATE_GAME_PATH, createGameData, createGameHeaders).then(
                createModal.displayMessage,
                createModal.displayMessage
            );

            setTimeout(location.reload.bind(location), 2500);
        }
    };

    createModal.displayMessage = (message) => {
        const modalInstanceTwo = $uibModal.open({
            templateUrl: 'messageContent.html',
            controller: 'messageCtrl',
            controllerAs: 'modalInstanceTwo',
            resolve: { message: () => message }
        });

        // No behaviour on close / dismiss.
        modalInstanceTwo.result.then(() => {}, () => {});
    };

    // Close create IOU modal dialogue.
    createModal.cancel = () => $uibModalInstance.dismiss();

    // Validate the IOU.
    function invalidFormInput() {
        return isNaN(createModal.form.value) || (createModal.form.opponent === undefined);
    }
});

app.controller('PlayModalCtrl', function ($http, $location, $uibModalInstance, $uibModal, app, apiBaseURL, game, opponent) {
    const playModal = this;
    playModal.game = game;
    playModal.opponent = opponent;
    playModal.pos = -1;
//    playModal.formError = false;

    playModal.play = function play() {
//            if (playModal.form.opponent == null) {
//                playModal.formError = true;
//            } else {
//                playModal.formError = false;
        $uibModalInstance.close();
        let PLAY_PATH = apiBaseURL + "play"
        let playData = $.param({
            opponent: playModal.opponent,
            pos: playModal.pos
        });

        let playHeaders = {
            headers : {
                "Content-Type": "application/x-www-form-urlencoded"
            }
        };

        // Create IOU  and handles success / fail responses.
        $http.post(PLAY_PATH, playData, playHeaders).then(
            playModal.displayMessage,
            playModal.displayMessage
        );
//        setTimeout(location.reload.bind(location), 3500);
//                }
    };

    playModal.displayMessage = (message) => {
        if (message.data.includes("win")) {
//            console.log("corgi");
            playModal.displayCongrats();
        } else {
//            console.log("shiba");
            const modalInstanceTwo = $uibModal.open({
                templateUrl: 'messageContent.html',
                controller: 'messageCtrl',
                controllerAs: 'modalInstanceTwo',
                resolve: { message: () => message }
            });
            // No behaviour on close / dismiss.
            modalInstanceTwo.result.then(() => {}, () => {
                setTimeout(location.reload.bind(location), 1500);
            });
        }
    };

    playModal.displayCongrats = () => {
        const congratsModal = $uibModal.open({
            templateUrl: 'congratsModal.html',
            controller: 'congratsCtrl',
            controllerAs: 'congratsModal',
            resolve: {}
        });
        congratsModal.result.then(() => {
            setTimeout(location.reload.bind(location), 1500);
        }, () => {});
    }

    // Close create IOU modal dialogue.
    playModal.cancel = () => $uibModalInstance.dismiss();

    // Validate the IOU.
    function invalidFormInput() {
        return isNaN(playModal.pos) || (playModal.opponent === undefined);
    }
});


// Controller for success/fail modal dialogue.
app.controller('messageCtrl', function ($uibModalInstance, message) {
    const modalInstanceTwo = this;
    modalInstanceTwo.message = message.data;
});

app.controller('congratsCtrl', function ($uibModalInstance) {
    const congratsModal = this;
//    congratsModal.message = "Congrats! You win!";

    congratsModal.close = () => $uibModalInstance.close();
});





