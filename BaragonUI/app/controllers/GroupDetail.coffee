Controller = require './Controller'

GroupDetailView = require '../views/groupDetail'

BasePathsList = require '../models/BasePathsList'
Group = require '../models/Group'

Agents = require '../collections/Agents'
KnownAgents = require '../collections/KnownAgents'

class GroupDetailController extends Controller

    initialize: ({@groupId}) ->
        app.showPageLoader()

        @models.basePaths = new BasePathsList {@groupId}
        @models.group = new Group {@groupId}
        @collections.agents = new Agents [], {@groupId}
        @collections.knownAgents = new KnownAgents [], {@groupId}

        @setView new GroupDetailView
            model: @models.group
            options:
                basePaths: @models.basePaths
                agents: @collections.agents
                knownAgents: @collections.knownAgents
                groupId: @groupId

        app.showView @view

        @refresh()

    refresh: ->
        @models.group.fetch().error =>
            app.caughtError()
        @models.basePaths.fetch()
        @collections.knownAgents.fetch()
        @collections.agents.fetch()

module.exports = GroupDetailController
