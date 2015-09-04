Model = require './model'

class Group extends Model

    sourceRemoveTemplate: require '../templates/vex/sourceRemove'
    sourceAddTemplate: require '../templates/vex/sourceAdd'

    url: -> "#{ config.apiRoot }/load-balancer/#{ @groupId }"

    ignoreAttributes: ['splitSources']

    initialize: ({ @groupId }) ->

    parse: (data) =>
        data.splitSources = utils.splitArray(data.sources.sort(), Math.ceil(data.sources.length/4))
        data

    deleteSource: (source) =>
        $.ajax
            url: "#{ @url() }/sources?authkey=#{ localStorage.getItem 'baragonAuthKey' }&source=#{source}"
            type: "DELETE"

    addSource: (source) =>
        $.ajax
            url: "#{ @url() }/sources?authkey=#{ localStorage.getItem 'baragonAuthKey' }&source=#{source}"
            type: "POST"

    promptRemoveSource: (source, callback) =>
        vex.dialog.confirm
            message: @sourceRemoveTemplate {source: source}
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
                <input name="source" type="text" placeholder="Traffic Source Name" required />
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
                @addSource(data.source).done callback

module.exports = Group
