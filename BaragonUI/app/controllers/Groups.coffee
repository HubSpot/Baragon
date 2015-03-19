Controller = require './Controller'

GroupsView = require '../views/groups'

GroupsList = require '../models/GroupsList'

class GroupsController extends Controller

    initialize: ->
        app.showPageLoader()

        @models.groupslist = new GroupsList

        @setView new GroupsView
            model: @models.groupslist

        app.showView @view

        @refresh()

    refresh: ->
        @models.groupslist.fetch()

module.exports = GroupsController
