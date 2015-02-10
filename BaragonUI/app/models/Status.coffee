Model = require './model'

class Status extends Model

    url: -> "#{ config.apiRoot }/status"

module.exports = Status
