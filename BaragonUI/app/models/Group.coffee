Model = require './model'

class Group extends Model

    sourceRemoveTemplate: require '../templates/vex/sourceRemove'
    sourceAddTemplate: require '../templates/vex/sourceAdd'

    url: -> "#{ config.apiRoot }/load-balancer/#{ @groupId }"

    ignoreAttributes: ['splitSources']

    initialize: ({ @groupId }) ->

    parse: (data) =>
        if data.trafficSources
            data.splitTrafficSources = utils.splitArray(data.trafficSources.sort(), Math.ceil(data.trafficSources.length / 4))
        else
            data.splitTrafficSources = []

        if data.domains
            if data.defaultDomain and data.defaultDomain not in data.domains
                data.domains.push data.defaultDomain
        else if data.defaultDomain
            data.domains = [data.defaultDomain]
        else
            data.domains = []

        data.splitDomains = utils.splitArray(data.domains.sort(), Math.ceil(data.domains.length/4))
        data

    deleteSource: (source) =>
        $.ajax
            url: "#{ @url() }/traffic-source?authkey=#{ localStorage.getItem 'baragonAuthKey' }"
            type: "DELETE"
            contentType: 'application/json'
            data: JSON.stringify(source)

    addSource: (source) =>
        body =
          name: source["source-name"]
          type: source["source-type"]
        $.ajax
            url: "#{ @url() }/traffic-source?authkey=#{ localStorage.getItem 'baragonAuthKey' }"
            type: 'POST'
            contentType: 'application/json'
            data: JSON.stringify(body)

    promptRemoveSource: (source, callback) =>
        vex.dialog.confirm
            message: @sourceRemoveTemplate source
            buttons: [
                $.extend {}, vex.dialog.buttons.YES,
                    text: 'DELETE',
                    className: 'vex-dialog-button-primary vex-dialog-button-primary-remove'
                vex.dialog.buttons.NO
            ]
            callback: (data) =>
                return if data is false
                @deleteSource(source).done callback

    promptAddSource: (callback) =>
        input = """
                <input name="source-name" type="text" placeholder="Traffic Source Name" required />
                <select name="source-type" class="form-control">
                  <option value="CLASSIC">Classic Load Balancer</option>
                  <option value="ALB_TARGET_GROUP">Target Group</option>
                </select>
            """
        vex.dialog.confirm
            message: @sourceAddTemplate
            input: input
            buttons: [
                $.extend {}, vex.dialog.buttons.YES,
                    text: 'ADD',
                    className: 'vex-dialog-button-primary vex-dialog-button-primary-remove'
                vex.dialog.buttons.NO
            ]
            callback: (data) =>
                return if data is false
                @addSource(data).done callback

module.exports = Group
