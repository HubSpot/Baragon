Controller = require './Controller'

ServiceDetailView = require '../views/serviceDetail'

Service = require '../models/Service'

class ServiceDetailController extends Controller

    initialize: ({@serviceId}) ->
        app.showPageLoader()

        @models.service = new Service {@serviceId}

        @setView new ServiceDetailView
            model: @models.service

        app.showView @view

        @refresh()

    refresh: ->
        @models.service.fetch()

module.exports = ServiceDetailController
