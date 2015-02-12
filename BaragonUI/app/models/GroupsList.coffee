Model = require './model'

class GroupsList extends Model

    url: -> "#{ config.apiRoot }/load-balancer?authkey=#{ config.authKey }"

module.exports = GroupsList
