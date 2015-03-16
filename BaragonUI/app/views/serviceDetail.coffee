View = require './view'

Service = require '../models/Service'

class ServiceDetailView extends View

    template: require '../templates/serviceDetail'

    initialize: ->
        @listenTo @model, 'sync', @render

    events: =>
        _.extend super,
            'click [data-action="viewJSON"]':        'viewJson'
            'click [data-action="delete"]':          'deleteService'
            'click [data-action="removeUpstreams"]': 'removeUpstreams'
            'click [data-action="removeUpstream"]': 'removeUpstream'

    render: =>
        @$el.html @template
            service:  @model.attributes
            config:   config
            synced:   @model.synced

    viewJson: (e) ->
        utils.viewJSON @model

    deleteService: (e) ->
        @model.promptDelete =>
            @model.promptDeleteSuccess =>
                app.router.navigate 'services', trigger: true

    removeUpstreams: (e) ->
        @model.promptRemoveUpstreams =>
            @model.promptRemoveUpstreamsSuccess => @trigger 'refreshrequest'

    removeUpstream: (e) ->
        upstream = $(e.target).data 'upstream'
        @model.promptRemoveUpstream(upstream, =>
            @model.promptRemoveUpstreamsSuccess => @trigger 'refreshrequest'
        )

module.exports = ServiceDetailView
