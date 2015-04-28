Controller = require './Controller'

ElbDetailView = require '../views/elbDetail'

Elb = require '../models/Elb'

class ElbDetailController extends Controller

    initialize: ({ @elbName })->
        app.showPageLoader()

        @models.elb = new Elb {@elbName}

        @setView new ElbDetailView
            model: @models.elb

        app.showView @view

        @refresh()

    refresh: ->
        @models.elb.fetch()

module.exports = ElbDetailController
