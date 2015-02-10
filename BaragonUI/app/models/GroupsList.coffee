Model = require './model'

class GroupsList extends Model

    url: -> "#{ config.apiRoot }/load-balancer"

module.exports = GroupsList
