Controller = require './Controller'

ServiceDetailView = require '../views/serviceDetail'

Service = require '../models/Service'

Responses = require '../collections/Responses'

class ServiceDetailController extends Controller

    initialize: ({@serviceId}) ->
        app.showPageLoader()

        @models.service = new Service {@serviceId}
        @collections.responses = new Responses [], {@serviceId}

        @setView new ServiceDetailView
            model: @models.service
            collection: @collections.responses

        app.showView @view

        @refresh()

    refresh: ->
        @models.service.fetch()
        @collections.responses.fetch()

module.exports = ServiceDetailController
