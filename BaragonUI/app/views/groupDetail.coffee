View = require './view'

Agent = require '../models/Agent'
Group = require '../models/Group'

class GroupDetailView extends View

    template: require '../templates/groupDetail'
    removeBasePathTemplate: require '../templates/vex/basePathRemove'

    initialize: (@params) ->
        { @options, @groupId } = @params
        @listenTo @model, 'sync', @render
        @listenTo @options.agents, 'sync', @render
        @listenTo @options.knownAgents, 'sync', @render
        @listenTo @options.basePaths, 'sync', @render

    events: =>
        _.extend super,
            'click [data-action="remove"]':         'removeKnownAgent'
            'click [data-action="removeBasePath"]': 'removeBasePath'
            'click [data-action="removeSource"]':   'removeSource'
            'click [data-action="addSource"]':      'addSource'

    render: =>
        @$el.html @template
            group:  @model.attributes
            basePaths: @options.basePaths.attributes
            knownAgents: @options.knownAgents.toJSON()
            agents: @options.agents.toJSON()
            config: config
            synced: @model.synced || @options.agents.synced

    removeKnownAgent: (e) ->
        id = $(e.target).parents('tr').data 'agent-id'
        group = @options.groupId
        agentModel = new Agent {agentId: id, groupId: group}
        agentModel.promptRemoveKnown => @trigger 'refreshrequest'

    removeBasePath: (e) ->
        basePath = $(e.target).data 'base-path'
        group = @options.groupId
        vex.dialog.confirm
            message: @removeBasePathTemplate {basePath: basePath, group: group}
            buttons: [
                $.extend {}, vex.dialog.buttons.YES,
                    text: 'REMOVE',
                    className: 'vex-dialog-button-primary vex-dialog-button-primary-remove'
                vex.dialog.buttons.NO
            ]
            callback: (data) =>
                return if data is false
                @removeRequest(group, basePath).done @trigger 'refreshrequest'

    removeRequest: (group, basePath) ->
        $.ajax
            url: "#{ config.apiRoot }/load-balancer/#{group}/base-path?#{$.param({authkey: localStorage.getItem 'baragonAuthKey', basePath: basePath})}"
            type: "DELETE"

    removeSource: (e) ->
        source = $(e.target).data 'source'
        @model.promptRemoveSource(source, => @trigger 'refreshrequest')

    addSource: (e) ->
        @model.promptAddSource => @trigger 'refreshrequest'


module.exports = GroupDetailView
