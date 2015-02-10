Controller = require './Controller'

StatusView = require '../views/status'

Status = require '../models/Status'
WorkerList = require '../models/WorkerList'

class StatusController extends Controller

    initialize: ->
        app.showPageLoader()

        @models.status = new Status
        @models.workers = new WorkerList

        @setView new StatusView
            model: @models.status
            options: @models.workers

        app.showView @view

        @refresh()

    refresh: ->
        @models.workers.fetch()
        @models.status.fetch()

module.exports = StatusController
