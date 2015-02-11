View = require './view'

class GroupDetailView extends View

    template: require '../templates/groupDetail'

    initialize: (@params) ->
        { @options } = @params
        @listenTo @model, 'sync', @render
        @listenTo @options, 'sync', @render
        @listenTo @collection, 'sync', @render


    render: =>
        @$el.html @template
            basePaths:  @model.attributes
            knownAgents: @options.toJSON()
            agents: @collection.toJSON()
            config: config
            synced: @collection.synced

module.exports = GroupDetailView
