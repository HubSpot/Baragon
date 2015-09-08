Collection = require './collection'

QueuedRequest = require '../models/QueuedRequest'

class QueuedRequests extends Collection

    model: QueuedRequest

    url: => "#{ config.apiRoot }/request"

    initialize: (models) =>

module.exports = QueuedRequests
