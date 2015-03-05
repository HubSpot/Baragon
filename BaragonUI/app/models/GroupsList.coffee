Model = require './model'

class GroupsList extends Model

    url: -> "#{ config.apiRoot }/load-balancer?authkey=#{ config.authKey }"

    parse: (data) =>
        @splitArray(data.sort(), Math.ceil(data.length/4))

    splitArray:(arr, size) =>
        arr2 = arr.slice(0)
        arrays = []
        while (arr2.length > 0)
            arrays.push(arr2.splice(0, size))
        return arrays

module.exports = GroupsList
