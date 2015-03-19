View = require './view'

class NavView extends View

    template: require '../templates/nav'
    authTemplate: require '../templates/vex/uiEnableEdit'

    initialize: ->
        Backbone.history.on 'route', =>
            @render()

    events: =>
        _.extend super,
            'click [data-action="enableEdit"]': 'enableEdit'

    render: ->
        fragment = Backbone.history.fragment?.split("/")[0]

        if not fragment
            fragment = 'status'

        @$el.html @template {fragment, title: config.title, config: config}

    enableEdit: ->
        input =  """
                <input name="authkey" type="password" placeholder="AuthKey" required />
            """
        vex.dialog.prompt
            message: @authTemplate()
            input: input
            callback: (data) =>
                if data.authkey
                    $.get("#{ config.apiRoot }/allowuiwrite?uiAuthKey=#{data.authkey}&authkey=#{config.authKey}", (data, status) =>
                        if data == 'allowed'
                            window.config.allowEdit = true
                            vex.dialog.alert 'Authorized!'
                        else
                            window.config.allowEdit = false
                            vex.dialog.alert 'Not a valid key!'
                    )
                    Backbone.history.loadUrl(Backbone.history.fragment)





module.exports = NavView
