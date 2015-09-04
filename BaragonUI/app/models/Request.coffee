Model = require './model'

class Request extends Model

    url: -> "#{ config.apiRoot}/request/#{ @requestId}"

    initialize: ({ @requestId }) ->

module.exports = Request
