Model = require './model'

class WorkerList extends Model

    url: -> "#{ config.apiRoot }/workers"

module.exports = WorkerList
