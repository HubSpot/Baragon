View = require './view'

Service = require '../models/Service'

class ServicesView extends View

    template: require '../templates/services'
    tableTemplate: require '../templates/servicesTable'

    initialize: () ->
        @listenTo @collection, 'sync', @render

    events: =>
        _.extend super,
            'click [data-action="viewJSON"]':        'viewJson'
            'click [data-action="delete"]':          'deleteService'
            'click [data-action="removeUpstreams"]': 'removeUpstreams'
            'click [data-action="reload"]':          'reload'
            'change input[type="search"]':           'searchChange'
            'keyup input[type="search"]':            'serachChange'
            'input input[type="search"]':            'searchChange'


    render: =>
        @$el.html @template
            config: config
            synced: @collection.synced

        @renderTable()

        @$('.actions-column a[title]').tooltip()
        @$('.icons-column span[title]').tooltip()
        $('table.paginated:not([id])').DataTable
          dom: "<'row'<'col-md-5'f><'col-md-4'i><'col-md-3'p>><'row't><'clear'>"
          ordering: false
          lengthChange: false
          pageLength: 15
          pagingType: 'simple'
          language:
            paginate:
              previous: '<span class="glyphicon glyphicon-chevron-left"></span>'
              next: '<span class="glyphicon glyphicon-chevron-right"></span>'
            search: 'Search: _INPUT_'

    renderTable: =>
        @$('#servicesTable').html @tableTemplate
           services: @collection.toJSON()
           config: config

    viewJson: (e) ->
        id = $(e.target).parents('tr').data 'service-id'
        utils.viewJSON @collection.get id

    deleteService: (e) ->
        id = $(e.target).parents('tr').data 'service-id'
        serviceModel = new Service {serviceId: id}
        serviceModel.promptDelete =>
            serviceModel.promptDeleteSuccess => @trigger 'refreshrequest'

    removeUpstreams: (e) ->
        id = $(e.target).parents('tr').data 'service-id'
        serviceModel = new Service {serviceId: id}
        serviceModel.promptRemoveUpstreams =>
            serviceModel.promptRemoveUpstreamsSuccess => @trigger 'refreshrequest'

    reload: (e) ->
        id = $(e.target).parents('tr').data 'service-id'
        serviceModel = new Service {serviceId: id}
        serviceModel.promptReloadConfigs =>
            serviceModel.promptReloadConfigsSuccess => @trigger 'refreshrequest'

module.exports = ServicesView
