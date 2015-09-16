Model = require './model'

class Status extends Model

    url: -> "#{ config.apiRoot }/status/master"

module.exports = Status
