View = require './view'

Request = require '../models/Request'

class RequestView extends View

    template: require '../templates/request'

    initialize: ->
        @listenTo @model, 'sync', @render

    events: =>
        _.extend super,
            'click [data-action="viewJSON"]':        'viewJson'

    render: =>
        @$el.html @template
            request:  @model.attributes
            config:   config
            synced:   @model.synced

    viewJson: (e) ->
        utils.viewJSON @model

module.exports = RequestView
