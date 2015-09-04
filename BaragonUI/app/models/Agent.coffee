Model = require './model'

class Agent extends Model

    initialize: ({ @agentId, @groupId }) ->

    deleteTemplate: require '../templates/vex/knownAgentRemove'

    promptRemoveKnown: (callback) =>
        vex.dialog.confirm
            message: @deleteTemplate {@agentId, @groupId}
            buttons: [
                $.extend {}, vex.dialog.buttons.YES,
                    text: 'DELETE',
                    className: 'vex-dialog-button-primary vex-dialog-button-primary-remove'
                vex.dialog.buttons.NO
            ]
            callback: (data) =>
                return if data is false
                @delete().done callback

    delete: =>
        $.ajax
            url: "#{ config.apiRoot }/load-balancer/#{@groupId}/known-agents/#{@agentId}?authkey=#{ localStorage.getItem 'baragonAuthKey' }"
            type: "DELETE"

module.exports = Agent
