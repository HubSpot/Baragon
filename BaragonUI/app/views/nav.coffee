View = require './view'

class NavView extends View

    template: require '../templates/nav'
    authTemplate: require '../templates/vex/uiEnableEdit'

    initialize: ->
        Backbone.history.on 'route', =>
            @render()

    events: =>
        _.extend super,
            'click [data-action="enableEdit"]':  'enableEdit'
            'click [data-action="disableEdit"]': 'disableEdit'

    render: ->
        fragment = Backbone.history.fragment?.split("/")[0]

        if not fragment
            fragment = 'status'

        @$el.html @template {fragment, title: config.title, config: config}

    enableEdit: ->
        input = """
                <input name="authkey" type="password" placeholder="AuthKey" />
            """
        vex.dialog.prompt
            message: @authTemplate()
            input: input
            callback: (data) =>
                if data.authkey
                    $.get("#{ config.apiRoot }/auth/key/verify?authkey=#{data.authkey}", =>
                        localStorage.setItem "baragonAuthKey", data.authkey
                        config.allowEdit = true
                        vex.dialog.alert 'Authorized!'
                    ).fail( =>
                        vex.dialog.alert 'Not a valid key!'
                    )
                    Backbone.history.loadUrl(Backbone.history.fragment)

    disableEdit: ->
        localStorage.removeItem "baragonAuthKey"
        config.allowEdit = false
        vex.dialog.alert "Disabled edit mode, click 'Enable Edit' and re-enter your authkey to re-enable"
        Backbone.history.loadUrl(Backbone.history.fragment)





module.exports = NavView
