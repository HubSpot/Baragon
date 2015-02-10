Controller = require './Controller'

GroupsView = require '../views/groups'

GroupsList = require '../models/GroupsList'

class GroupDetailController extends Controller

    initialize: (@groupId) ->
        app.showPageLoader()

        @models.group = new GroupsList

        @setView new GroupsView
            model: @models.groupslist

        app.showView @view

        @refresh()

    refresh: ->
        @models.groupslist.fetch()

module.exports = GroupDetailController
