View = require './view'

Agent = require '../models/Agent'

class GroupDetailView extends View

    template: require '../templates/groupDetail'
    removeBasePathTemplate: require '../templates/vex/basePathRemove'

    initialize: (@params) ->
        { @options, @groupId } = @params
        @listenTo @model, 'sync', @render
        @listenTo @options, 'sync', @render
        @listenTo @collection, 'sync', @render

    events: =>
        _.extend super,
            'click [data-action="remove"]':         'removeKnownAgent'
            'click [data-action="removeBasePath"]': 'removeBasePath'

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
            url: "#{ config.apiRoot }/load-balancer/#{group}/base-path?#{$.param({authkey: config.authKey, basePath: basePath})}"
            type: "DELETE"

module.exports = GroupDetailView
