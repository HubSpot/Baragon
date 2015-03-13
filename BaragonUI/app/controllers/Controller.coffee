# Base controller to be extended by other classes
class Controller
    
    # Reference to the primary view being used
    view:     undefined

    # Keep track of models, options, and collections
    models:      {}
    collections: {}
    options:     {}

    constructor: (params) -> @initialize?(params)

    initialize: ->

    refresh: ->

    setView: (@view) ->
        @view.on 'refreshrequest', => @refresh()

    ignore404: (response) -> app.caughtError() if response.status is 404

module.exports = Controller
