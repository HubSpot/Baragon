View = require './view'

class ElbDetailView extends View

    template: require '../templates/elb'

    initialize: () ->
        @listenTo @model, 'sync', @render

    events: =>
        _.extend super,
            'click [data-action="viewJSON"]':        'viewJson'

    render: =>
        @$el.html @template
            elb: @model.attributes
            config: config
            synced: @model.synced

    viewJson: (e) ->
        utils.viewJSON @model

module.exports = ElbDetailView
