angular.module('xodus').controller('EntityController', [
    'databaseService',
    'entitiesService',
    '$routeParams',
    '$location',
    '$q',
    function (databaseService, entitiesService, $routeParams, $location, $q) {
        var entityCtrl = this;
        entityCtrl.loaded = false;
        entityCtrl.types = [];

        var db = databaseService.databases.find(function (db) {
            return db.uuid === $routeParams.databaseId;
        });

        if (db && db.opened) {
            databaseService.getTypes(db).then(function (types) {
                entityCtrl.fullDB = angular.extend({}, db, {
                    types: types
                });
            }).then(function () {
                var entityId = $routeParams.entityId;
                entityCtrl.isNew = (entityId === 'new');
                if (entityCtrl.isNew) {
                    var typeId = $location.search().q;
                    var type = entityCtrl.fullDB.types.find(function (type) {
                        return type.id === typeId;
                    });
                    return $q.when({
                        typeId: type.id,
                        type: type.name,
                        links: [],
                        properties: [],
                        blobs: []
                    });
                } else {
                    var id = entityId.split('-');
                    return entitiesService(entityCtrl.fullDB).byId(id[0], id[1]);
                }
            }).then(function (data) {
                entityCtrl.entity = data;
                entityCtrl.loaded = true;
            })
        } else {
            entityCtrl.loaded = true;
        }
    }]);