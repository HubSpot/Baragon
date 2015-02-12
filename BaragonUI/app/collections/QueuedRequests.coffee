Collection = require './collection'

QueuedRequest = require '../models/QueuedRequest'

class QueuedRequests extends Collection

    model: QueuedRequest

    url: => "#{ config.apiRoot }/request?authkey=#{ config.authKey }"

    initialize: (models) =>

module.exports = QueuedRequests
