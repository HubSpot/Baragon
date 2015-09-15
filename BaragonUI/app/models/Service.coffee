Model = require './model'

class Service extends Model

    url: -> "#{ config.apiRoot }/state/#{ @serviceId }"

    deleteTemplate: require '../templates/vex/serviceRemove'
    deleteSuccessTemplate: require '../templates/vex/serviceRemoveSuccess'
    reloadTemplate: require '../templates/vex/serviceReload'
    reloadSuccessTemplate: require '../templates/vex/serviceReloadSuccess'
    removeUpstreamsTemplate: require '../templates/vex/removeUpstreams'
    removeUpstreamTemplate: require '../templates/vex/removeUpstream'
    removeUpstreamsSuccessTemplate: require '../templates/vex/removeUpstreamsSuccess'

    noReloadNoValidateInput: """
            <input name="validate" type="checkbox" checked> Validate new configuration after applying changes</input>
            <br>
            <input name="reload" type="checkbox" checked> Reload configuration after applying changes</input>
        """

    noValidateInput: """
            <input name="validate" type="checkbox" checked> Validate configuration before reloading</input>
        """

    initialize: ({ @serviceId }) ->

    ignoreAttributes: ['splitLbGroups', 'splitOwners', 'splitUpstreams']

    parse: (data) ->
        data.id = data.service.serviceId
        data.splitLbGroups = utils.splitArray(data.service.loadBalancerGroups.sort(), Math.ceil(data.service.loadBalancerGroups.length/2))
        data.splitOwners = utils.splitArray(data.service.owners.sort(), Math.ceil(data.service.owners.length/2))
        data.splitUpstreams = utils.splitArray(data.upstreams, Math.ceil(data.upstreams.length/2))
        data.upstreamsCount = data.upstreams.length
        if data.upstreamsCount > 0
            data.active = true
        data

    delete: (noValidate, noReload) =>
        $.ajax
            url: "#{ @url() }?authkey=#{ localStorage.getItem 'baragonAuthKey' }&noValidate=#{ noValidate }&noReload=#{ noReload }"
            type: "DELETE"
            success: (data) =>
                console.dir(data)
                @set('request', data.loadBalancerRequestId)

    reload: (noValidate) =>
        $.ajax
            url: "#{ @url() }/reload?authkey=#{ localStorage.getItem 'baragonAuthKey' }&noValidate=#{ noValidate }"
            type: "POST"
            success: (data) =>
                @set('request', data.loadBalancerRequestId)

    undo: (noValidate, noReload) =>
        this.fetch({
            success: =>
                requestId = @requestId()
                @set('request', requestId)
                serviceData = {
                    loadBalancerRequestId: requestId
                    loadBalancerService:
                        serviceId: @id
                        owners: if @attributes.service.owners then @attributes.service.owners else []
                        serviceBasePath: @attributes.service.serviceBasePath
                        loadBalancerGroups: @attributes.service.loadBalancerGroups
                    addUpstreams: []
                    removeUpstreams: @attributes.upstreams,
                    noValidate: noValidate,
                    noReload: noReload
                }
                $.ajax
                    url: "#{ config.apiRoot }/request?authkey=#{ localStorage.getItem 'baragonAuthKey' }"
                    type: "post"
                    contentType: "application/json"
                    data: JSON.stringify(serviceData)
        })

    remove: (upstream, noValidate, noReload) =>
        this.fetch({
            success: =>
                requestId = @requestId()
                @set('request', requestId)
                serviceData = {
                    loadBalancerRequestId: requestId
                    loadBalancerService:
                        serviceId: @id
                        owners: if @attributes.service.owners then @attributes.service.owners else []
                        serviceBasePath: @attributes.service.serviceBasePath
                        loadBalancerGroups: @attributes.service.loadBalancerGroups
                        options: @attributes.options
                    addUpstreams: []
                    removeUpstreams: [{upstream: upstream, request: requestId}]
                    noValidate: noValidate,
                    noReload: noReload
                }
                $.ajax
                    url: "#{ config.apiRoot }/request?authkey=#{ localStorage.getItem 'baragonAuthKey' }"
                    type: "post"
                    contentType: "application/json"
                    data: JSON.stringify(serviceData)
        })

    requestId: -> "#{@serviceId}-#{Date.now()}"


    promptDelete: (callback) =>
        vex.dialog.confirm
            message: @deleteTemplate {@serviceId}
            input: @noReloadNoValidateInput
            buttons: [
                $.extend {}, vex.dialog.buttons.YES,
                    text: 'DELETE',
                    className: 'vex-dialog-button-primary vex-dialog-button-primary-remove'
                vex.dialog.buttons.NO
            ]
            callback: (data) =>
                return if data is false
                noValidate = (!data.validate or data.validate != 'on')
                noReload = (!data.reload or data.reload != 'on')
                @delete(noValidate, noReload).done callback

    promptDeleteSuccess: (callback) =>
        vex.dialog.confirm
            message: @deleteSuccessTemplate {request: @get('request'), config: config}
            buttons: [
                $.extend {}, vex.dialog.buttons.YES,
                    text: 'OK',
                    className: 'vex-dialog-button-primary vex-dialog-button-primary-remove'
            ]
            callback: (data) =>
                return

    promptReloadConfigs: (callback) =>
        vex.dialog.confirm
            message: @reloadTemplate {@serviceId}
            input: @noValidateInput
            buttons: [
                $.extend {}, vex.dialog.buttons.YES,
                    text: 'RELOAD',
                    className: 'vex-dialog-button-primary vex-dialog-button-primary-remove'
                vex.dialog.buttons.NO
            ]
            callback: (data) =>
                return if data is false
                noValidate = (!data.validate or data.validate != 'on')
                @reload(noValidate).done callback

    promptReloadConfigsSuccess: (callback) =>
        vex.dialog.confirm
            message: @reloadSuccessTemplate {request: @get('request'), config: config}
            buttons: [
                $.extend {}, vex.dialog.buttons.YES,
                    text: 'OK',
                    className: 'vex-dialog-button-primary vex-dialog-button-primary-remove'
            ]
            callback: (data) =>
                return

    promptRemoveUpstreams: (callback) =>
        vex.dialog.confirm
            message: @removeUpstreamsTemplate {@serviceId}
            input: @noReloadNoValidateInput
            buttons: [
                $.extend {}, vex.dialog.buttons.YES,
                    text: 'REMOVE',
                    className: 'vex-dialog-button-primary vex-dialog-button-primary-remove'
                vex.dialog.buttons.NO
            ]
            callback: (data) =>
                return if data is false
                noValidate = (!data.validate or data.validate != 'on')
                noReload = (!data.reload or data.reload != 'on')
                @undo(noValidate, noReload).done callback

    promptRemoveUpstreamsSuccess: (callback) =>
        vex.dialog.confirm
            message: @removeUpstreamsSuccessTemplate {request: @get('request'), config: config}
            buttons: [
                $.extend {}, vex.dialog.buttons.YES,
                    text: 'OK',
                    className: 'vex-dialog-button-primary vex-dialog-button-primary-remove'
            ]
            callback: (data) =>
                return

    promptRemoveUpstream: (upstream, callback) =>
        vex.dialog.confirm
            message: @removeUpstreamTemplate {upstream: upstream}
            input: @noReloadNoValidateInput
            buttons: [
                $.extend {}, vex.dialog.buttons.YES,
                    text: 'REMOVE',
                    className: 'vex-dialog-button-primary vex-dialog-button-primary-remove'
                vex.dialog.buttons.NO
            ]
            callback: (data) =>
                return if data is false
                noValidate = (!data.validate or data.validate != 'on')
                noReload = (!data.reload or data.reload != 'on')
                @remove(upstream, noValidate, noReload).done callback

module.exports = Service
