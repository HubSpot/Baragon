View = require './view'

Service = require '../models/Service'

class ServicesView extends View

    template: require '../templates/services'

    initialize: ->
        @listenTo @collection, 'sync', @render

    events: =>
        _.extend super,
            'click [data-action="viewJSON"]':        'viewJson'
            'click [data-action="delete"]':          'deleteService'
            'click [data-action="removeUpstreams"]': 'removeUpstreams'


    render: =>
        @$el.html @template
            services:  @collection.toJSON()
            config: config
            synced: @collection.synced

        @$('.actions-column a[title]').tooltip()

    viewJson: (e) ->
        id = $(e.target).parents('tr').data 'service-id'
        utils.viewJSON @collection.get id

    deleteService: (e) ->
        id = $(e.target).parents('tr').data 'service-id'
        serviceModel = new Service {serviceId: id}
        serviceModel.promptDelete => @trigger 'refreshrequest'

    removeUpstreams: (e) ->
        id = $(e.target).parents('tr').data 'service-id'
        serviceModel = new Service {serviceId: id}
        serviceModel.promptRemoveUpstreams =>
            serviceModel.promptRemoveUpstreamsSuccess => @trigger 'refreshrequest'

module.exports = ServicesView
