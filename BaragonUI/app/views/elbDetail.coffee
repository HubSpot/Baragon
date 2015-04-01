View = require './view'

class ElbDetailView extends View

    template: require '../templates/elb'

    initialize: () ->
        @listenTo @model, 'sync', @render

    render: =>
        @$el.html @template
            elb: @model.attributes
            config: config
            synced: @model.synced

module.exports = ElbDetailView
