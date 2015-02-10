StatusController    = require 'controllers/Status'
ServicesController    = require 'controllers/Services'
ServiceDetailController    = require 'controllers/ServiceDetail'
GroupsController    = require 'controllers/Groups'
GroupDetailController    = require 'controllers/GroupDetail'
NotFoundController = require 'controllers/NotFound'

class Router extends Backbone.Router

    routes:
        '(/)':                     'status'

        'services(/)':             'services'
        'services/:serviceId(/)':  'serviceDetail'

        'groups(/)':               'groups'
        'groups/:groupId(/)':      'groupDetail'

        '*anything':               'notFound'

    status: ->
        app.bootstrapController new StatusController

    services: ->
        app.bootstrapController new ServicesController

    serviceDetail: (serviceId) ->
        app.bootstrapController new ServiceDetailController {serviceId}

    groups: ->
        app.bootstrapController new GroupsController

    groupDetail: (groupId) ->
        app.bootstrapController new GroupDetailController {groupId}

    notFound: ->
        app.bootstrapController new NotFoundController

module.exports = Router
