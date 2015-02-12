Collection = require './collection'

Agent = require '../models/Agent'

class Agents extends Collection

    model: Agent

    url: => "#{ config.apiRoot }/load-balancer/#{@groupId}/agents?authkey=#{ config.authKey }"

    initialize: (models, {@groupId}) =>

module.exports = Agents
