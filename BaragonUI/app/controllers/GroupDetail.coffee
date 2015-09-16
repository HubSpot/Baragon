Controller = require './Controller'

GroupDetailView = require '../views/groupDetail'

BasePathsList = require '../models/BasePathsList'
Group = require '../models/Group'
TargetCount = require '../models/TargetCount'

Agents = require '../collections/Agents'
KnownAgents = require '../collections/KnownAgents'

class GroupDetailController extends Controller

    initialize: ({@groupId}) ->
        app.showPageLoader()

        @models.basePaths = new BasePathsList {@groupId}
        @models.group = new Group {@groupId}
        @models.targetCount = new TargetCount {@groupId}
        @collections.agents = new Agents [], {@groupId}
        @collections.knownAgents = new KnownAgents [], {@groupId}

        @setView new GroupDetailView
            model: @models.group
            options:
                basePaths: @models.basePaths
                agents: @collections.agents
                knownAgents: @collections.knownAgents
                groupId: @groupId
                targetCount: @models.targetCount

        app.showView @view

        @refresh()

    refresh: ->
        @models.group.fetch().error =>
            app.caughtError()
        @models.basePaths.fetch()
        @models.targetCount.fetch()
        @collections.knownAgents.fetch()
        @collections.agents.fetch()

module.exports = GroupDetailController
