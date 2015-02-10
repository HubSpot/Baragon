View = require './view'

class NavView extends View

    template: require '../templates/nav'

    initialize: ->
        Backbone.history.on 'route', =>
            @render()

    render: ->
        fragment = Backbone.history.fragment?.split("/")[0]

        if not fragment
            fragment = 'status'

        @$el.html @template {fragment, title: config.title}

module.exports = NavView
