Controller = require './Controller'

ElbsView = require '../views/elbs'

Elbs = require '../collections/Elbs'

class ElbsController extends Controller

    initialize: ->
        app.showPageLoader()

        @collections.elbs = new Elbs []

        @setView new ElbsView
            collection: @collections.elbs

        app.showView @view

        @refresh()

    refresh: ->
        @collections.elbs.fetch()

module.exports = ElbsController
