Collection = require './collection'

Elb = require '../models/Elb'

class Elbs extends Collection

    model: Elb

    url: => "#{ config.apiRoot }/elbs?authkey=#{ config.authKey }"

    initialize: (models) =>

module.exports = Elbs
