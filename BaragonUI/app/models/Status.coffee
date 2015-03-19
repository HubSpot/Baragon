Model = require './model'

class Status extends Model

    url: -> "#{ config.apiRoot }/status?authkey=#{ config.authKey }"

module.exports = Status
