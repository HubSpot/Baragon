View = require './view'

Service = require '../models/Service'

class ServiceDetailView extends View

    template: require '../templates/serviceDetail'

    initialize: ->
        @listenTo @model, 'sync', @render
        @listenTo @collection, 'sync', @render

    events: =>
        _.extend super,
            'click [data-action="viewJSON"]':         'viewJson'
            'click [data-action="viewRequestJSON"]':  'viewRequestJson'
            'click [data-action="delete"]':           'deleteService'
            'click [data-action="removeUpstreams"]':  'removeUpstreams'
            'click [data-action="removeUpstream"]':   'removeUpstream'
            'click [data-action="reload"]':           'reload'

    render: =>
        @$el.html @template
            service:   @model.attributes
            responses: @collection.toJSON()
            config:    config
            synced:    @model.synced

        $('table.paginated:not([id])').DataTable
          dom: "<'row'<'col-md-5'f><'col-md-4'i><'col-md-3'p>><'row't><'clear'>"
          ordering: false
          pageLength: 5
          pagingType: 'simple'
          language:
            paginate:
              previous: '<span class="glyphicon glyphicon-chevron-left"></span>'
              next: '<span class="glyphicon glyphicon-chevron-right"></span>'
            search: 'Search: _INPUT_'

    viewJson: (e) ->
        utils.viewJSON @model

    viewRequestJson: (e) ->
        id = $(e.target).parents('tr').data 'request-id'
        request = @collection.find (model) ->
            model.get('loadBalancerRequestId') == id
        utils.viewJSON request

    deleteService: (e) ->
        @model.promptDelete =>
            @model.promptDeleteSuccess =>
                app.router.navigate 'services', trigger: true

    removeUpstreams: (e) ->
        @model.promptRemoveUpstreams =>
            @model.promptRemoveUpstreamsSuccess => @trigger 'refreshrequest'

    removeUpstream: (e) ->
        upstream = $(e.target).data 'upstream'
        @model.promptRemoveUpstream(upstream, =>
            @model.promptRemoveUpstreamsSuccess => @trigger 'refreshrequest'
        )

    reload: (e) ->
        @model.promptReloadConfigs =>
            @model.promptReloadConfigsSuccess => @trigger 'refreshrequest'

module.exports = ServiceDetailView
