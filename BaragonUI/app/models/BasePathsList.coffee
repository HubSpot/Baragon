Model = require './model'

class BasePathsList extends Model

    url: => "#{ config.apiRoot }/load-balancer/#{@groupId}/base-path/all?authkey=#{ config.authKey }"

    initialize: ({ @groupId }) ->

    parse: (data) =>
        newData = {}
        newData.paths = data
        newData.splitPaths = utils.splitArray(data, Math.ceil(data.length/4))
        newData

module.exports = BasePathsList
