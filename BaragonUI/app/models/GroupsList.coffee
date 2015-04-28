Model = require './model'

class GroupsList extends Model

    url: -> "#{ config.apiRoot }/load-balancer?authkey=#{ config.authKey }"

    ignoreAttributes: ['splitArray']

    parse: (data) =>
        utils.splitArray(data.sort(), Math.ceil(data.length/4))

module.exports = GroupsList
