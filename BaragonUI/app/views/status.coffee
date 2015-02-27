View = require './view'

class StatusView extends View

    template: require '../templates/status'

    initialize: (@params) ->
        { @options } = @params
        @listenTo @model, 'sync', @render
        @listenTo @options, 'sync', @render
        @listenTo @collection, 'sync', @render

    render: =>
        @$el.html @template
            status:  @model.attributes
            workers: @options.attributes
            queued: @collection.toJSON()
            config: config
            synced: @model.synced

module.exports = StatusView