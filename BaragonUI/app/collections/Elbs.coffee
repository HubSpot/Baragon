Collection = require './collection'

Elb = require '../models/Elb'

class Elbs extends Collection

    model: Elb

    url: => "#{ config.apiRoot }/elbs"

    initialize: (models) =>

module.exports = Elbs
