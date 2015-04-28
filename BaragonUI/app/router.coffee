StatusController    = require 'controllers/Status'
ServicesController    = require 'controllers/Services'
ServiceDetailController    = require 'controllers/ServiceDetail'
GroupsController    = require 'controllers/Groups'
GroupDetailController    = require 'controllers/GroupDetail'
RequestController = require 'controllers/Request'
ElbsController = require 'controllers/Elbs'
ElbDetailController = require 'controllers/ElbDetail'
NotFoundController = require 'controllers/NotFound'

class Router extends Backbone.Router

    routes:
        '(/)':                     'status'

        'services(/)':             'services'
        'services/:serviceId(/)':  'serviceDetail'

        'groups(/)':               'groups'
        'groups/:groupId(/)':      'groupDetail'

        'requests/:requestId(/)':  'request'

        'elbs(/)':                 'elbs'
        'elbs/:elbName(/)':        'elbDetail'

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

    request: (requestId) ->
        app.bootstrapController new RequestController {requestId}

    elbs: ->
        app.bootstrapController new ElbsController

    elbDetail: (elbName) ->
        app.bootstrapController new ElbDetailController {elbName}

    notFound: ->
        app.bootstrapController new NotFoundController

module.exports = Router
