View = require './view'

Agent = require '../models/Agent'

class GroupDetailView extends View

    template: require '../templates/groupDetail'

    initialize: (@params) ->
        { @options, @groupId } = @params
        @listenTo @model, 'sync', @render
        @listenTo @options, 'sync', @render
        @listenTo @collection, 'sync', @render

    events: =>
        _.extend super,
            'click [data-action="remove"]': 'removeKnownAgent'

    render: =>
        @$el.html @template
            basePaths:  @model.attributes
            knownAgents: @options.toJSON()
            agents: @collection.toJSON()
            config: config
            synced: @collection.synced

    removeKnownAgent: (e) ->
        id = $(e.target).parents('tr').data 'agent-id'
        group = @options.groupId
        agentModel = new Agent {agentId: id, groupId: group}
        agentModel.promptRemoveKnown => @trigger 'refreshrequest'

module.exports = GroupDetailView
