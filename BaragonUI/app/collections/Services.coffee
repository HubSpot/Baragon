Collection = require './collection'

Service = require '../models/Service'

class Services extends Collection

    model: Service

    url: => "#{ config.apiRoot }/state"

    initialize: (models) =>

module.exports = Services
