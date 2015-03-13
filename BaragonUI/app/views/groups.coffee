View = require './view'

GroupsList = require '../models/GroupsList'

class GroupsView extends View

    template: require '../templates/groups'

    initialize: ->
        @listenTo @model, 'sync', @render

    render: =>
        @$el.html @template
            lbGroups:  @model.attributes
            config: config
            synced: @model.synced

module.exports = GroupsView
