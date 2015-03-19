View = require './view'

class StatusView extends View

    template: require '../templates/status'

    initialize: (@params) ->
        { @options } = @params
        @listenTo @model, 'sync', @render
        @listenTo @options, 'sync', @render
        @listenTo @collection, 'sync', @render

    events: =>
        _.extend super,
            'enter input[type="search"]': 'requestSearch'

    render: =>
        @$el.html @template
            status:  @model.attributes
            workers: @options.attributes
            queued: @collection.toJSON()
            config: config
            synced: @model.synced

        @$('input').keyup( (e)->
            if e.keyCode == 13
                $(this).trigger('enter')
        )

    requestSearch: (event) ->
        $requestSearch = @$ "input[type='search']"
        requestId = $requestSearch.val()

        app.router.navigate "requests/#{requestId}", trigger: true

module.exports = StatusView
