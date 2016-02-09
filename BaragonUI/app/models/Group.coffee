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
            url: "#{ @url() }/sources?authkey=#{ localStorage.getItem 'baragonAuthKey' }"
            type: "POST"
            data: JSON.stringify(source)
            dataType: "json"
            contentType: "application/json"

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
                <input name="name" type="text" placeholder="Traffic Source Name" required />
                <input name="port" type="number" placeholder="Optional ELB listener port" />
            """
        message = null
        vex.dialog.confirm
            message: @sourceAddTemplate
            input: input
            buttons: [
                $.extend {}, vex.dialog.buttons.YES,
                    text: 'ADD',
                    className: 'vex-dialog-button-primary vex-dialog-button-primary-remove'
                vex.dialog.buttons.NO
            ]
            onSubmit: (event) =>
                event.preventDefault()
                event.stopPropagation()

                message.hide() if message

                form = event.target
                data =
                    name: form.name.value

                if form.port.value
                    data.port = form.port.value

                if data.port
                    if data.port <= 0 || data.port > 65535
                        message = Messenger().error
                            message: "<p>Port is optional and must be between 0 and 65535</p>"
                            timeout: 0
                        return

                @addSource(data).done callback
                vex.close $(form).parent().data().vex.id

module.exports = Group
