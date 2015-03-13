Collection = require './collection'

Service = require '../models/Service'

class Services extends Collection

    model: Service

    url: => "#{ config.apiRoot }/state?authkey=#{ config.authKey }"

    initialize: (models) =>

module.exports = Services
