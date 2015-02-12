Model = require './model'

class Service extends Model

    url: -> "#{ config.apiRoot }/state/#{ @serviceId }?authkey=#{ config.authKey }"

    deleteTemplate: require '../templates/vex/serviceRemove'
    removeUpstreamsTemplate: require '../templates/vex/removeUpstreams'
    removeUpstreamsSuccessTemplate: require '../templates/vex/removeUpstreamsSuccess'

    initialize: ({ @serviceId }) ->

    parse: (data) ->
        data.id = data.service.serviceId
        data.loadBalancerGroupsString = data.service.loadBalancerGroups.join()
        data.loadBalancerGroups = data.service.loadBalancerGroups
        data.basePath = data.service.serviceBasePath
        data.upstreamsCount = data.upstreams.length
        if data.upstreamsCount > 0
            data.active = true
        data

    delete: =>
        $.ajax
            url: @url()
            type: "DELETE"

    undo: =>
        this.fetch({
            success: =>
                requestId = @requestId()
                @set('request', requestId)
                serviceData = {
                    loadBalancerRequestId: requestId
                    loadBalancerService:
                        serviceId: @id
                        owners: if @attributes.owners then @attributes.owners else []
                        serviceBasePath: @attributes.basePath
                        loadBalancerGroups: @attributes.loadBalancerGroups
                    addUpstreams: []
                    removeUpstreams: @attributes.upstreams
                }
                $.ajax
                    url: "#{ config.apiRoot }/request?authkey=#{ config.authKey }"
                    type: "post"
                    contentType: "application/json"
                    data: JSON.stringify(serviceData)
        })

    requestId: -> "#{@serviceId}-#{Date.now()}"


    promptDelete: (callback) =>
        vex.dialog.confirm
            message: @deleteTemplate {@serviceId}
            buttons: [
                $.extend {}, vex.dialog.buttons.YES,
                    text: 'DELETE',
                    className: 'vex-dialog-button-primary vex-dialog-button-primary-remove'
                vex.dialog.buttons.NO
            ]
            callback: (data) =>
                return if data is false
                @delete().done callback

    promptRemoveUpstreams: (callback) =>
        vex.dialog.confirm
            message: @removeUpstreamsTemplate {@serviceId}
            buttons: [
                $.extend {}, vex.dialog.buttons.YES,
                    text: 'REMOVE',
                    className: 'vex-dialog-button-primary vex-dialog-button-primary-remove'
                vex.dialog.buttons.NO
            ]
            callback: (data) =>
                return if data is false
                @undo().done callback

    promptRemoveUpstreamsSuccess: (callback) =>
        vex.dialog.confirm
            message: @removeUpstreamsSuccessTemplate {request: @get('request')}
            buttons: [
                $.extend {}, vex.dialog.buttons.YES,
                    text: 'OK',
                    className: 'vex-dialog-button-primary vex-dialog-button-primary-remove'
            ]
            callback: (data) =>
                return

module.exports = Service
