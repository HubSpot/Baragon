Model = require './model'

class Request extends Model

    url: -> "#{ config.apiRoot}/request/#{ @requestId}?authkey=#{ config.authKey }"

    initialize: ({ @requestId }) ->

module.exports = Request
