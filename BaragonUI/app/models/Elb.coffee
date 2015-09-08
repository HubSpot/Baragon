Model = require './model'

class Elb extends Model

    url: -> "#{ config.apiRoot }/elbs/#{ @elbName }"

    initialize: ({ @elbName }) ->

    parse: (data)=>
        data.instanceCount = data.instances.length
        data

module.exports = Elb
