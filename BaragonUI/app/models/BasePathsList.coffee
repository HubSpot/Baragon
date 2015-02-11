Model = require './model'

class BasePathsList extends Model

    url: => "#{ config.apiRoot }/load-balancer/#{@groupId}/base-path/all"

    initialize: ({ @groupId }) ->

    parse: (data) =>
        newData = {}
        newData.paths = data
        newData

module.exports = BasePathsList
