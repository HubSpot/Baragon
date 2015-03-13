Controller = require './Controller'

GroupDetailView = require '../views/groupDetail'

BasePathsList = require '../models/BasePathsList'

Agents = require '../collections/Agents'
KnownAgents = require '../collections/KnownAgents'

class GroupDetailController extends Controller

    initialize: ({@groupId}) ->
        app.showPageLoader()

        @models.basePaths = new BasePathsList {@groupId}
        @collections.agents = new Agents [], {@groupId}
        @collections.knownAgents = new KnownAgents [], {@groupId}

        @setView new GroupDetailView
            model: @models.basePaths
            collection: @collections.agents
            options: @collections.knownAgents

        app.showView @view

        @refresh()

    refresh: ->
        @models.basePaths.fetch()
        @collections.knownAgents.fetch()
        @collections.agents.fetch()

module.exports = GroupDetailController
