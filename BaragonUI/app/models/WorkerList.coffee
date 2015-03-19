Model = require './model'

class WorkerList extends Model

    url: -> "#{ config.apiRoot }/workers?authkey=#{ config.authKey }"

module.exports = WorkerList
