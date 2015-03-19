Controller = require './Controller'

ServicesView = require '../views/services'

Services = require '../collections/Services'

class ServicesController extends Controller

    initialize: ->
        app.showPageLoader()

        @collections.services = new Services []

        @setView new ServicesView
            collection: @collections.services

        app.showView @view

        @refresh()

    refresh: ->
        @collections.services.fetch()

module.exports = ServicesController
