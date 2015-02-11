Collection = require './collection'

Agent = require '../models/Agent'

class KnownAgents extends Collection

    model: Agent

    url: => "#{ config.apiRoot }/load-balancer/#{@groupId}/known-agents"

    initialize: (models, {@groupId}) =>

module.exports = KnownAgents
