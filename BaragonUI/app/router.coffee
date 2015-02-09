StatusController    = require 'controllers/Status'
NotFoundController = require 'controllers/NotFound'

class Router extends Backbone.Router

    routes:
        '(/)': 'status'

        '*anything': 'notFound'

    status: ->
        app.bootstrapController new StatusController

    notFound: ->
        app.bootstrapController new NotFoundController

module.exports = Router
