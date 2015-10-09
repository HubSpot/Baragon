Model = require './model'

class TargetCount extends Model

    url: => "#{ config.apiRoot }/load-balancer/#{@groupId}/count"

    updateCountTemplate: require '../templates/vex/updateTargetCount'

    initialize: ({ @groupId }) ->

    parse: (data) =>
        newData = {}
        newData.count = data
        return newData

    updateCount: (newCount) =>
        $.ajax
            url: "#{ config.apiRoot }/load-balancer/#{@groupId}/count?authkey=#{ localStorage.getItem 'baragonAuthKey' }&count=#{ newCount }"
            type: "POST"

    promptUpdateTargetCount: (callback) =>
        input = """
                <input name="targetCount" type="text" placeholder="#{this.attributes.count}" required />
            """
        vex.dialog.confirm
            message: @updateCountTemplate
            input: input
            buttons: [
                $.extend {}, vex.dialog.buttons.YES,
                    text: 'UPDATE',
                    className: 'vex-dialog-button-primary vex-dialog-button-primary-remove'
                vex.dialog.buttons.NO
            ]
            callback: (data) =>
                return if data is false
                @updateCount(data.targetCount).done callback


module.exports = TargetCount
