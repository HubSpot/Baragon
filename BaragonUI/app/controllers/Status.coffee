Controller = require './Controller'

StatusView = require '../views/status'

Status = require '../models/Status'
WorkerList = require '../models/WorkerList'

QueuedRequests = require '../collections/QueuedRequests'

class StatusController extends Controller

    initialize: ->
        app.showPageLoader()

        @models.status = new Status
        @models.workers = new WorkerList
        @collections.queuedRequests = new QueuedRequests

        @setView new StatusView
            model: @models.status
            collection: @collections.queuedRequests
            options: @models.workers

        app.showView @view

        @refresh()

    refresh: ->
        @models.workers.fetch()
        @collections.queuedRequests.fetch()
        @models.status.fetch()

module.exports = StatusController
