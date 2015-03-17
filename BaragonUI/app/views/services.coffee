View = require './view'

Service = require '../models/Service'

class ServicesView extends View

    template: require '../templates/services'
    tableTemplate: require '../templates/servicesTable'

    initialize: ({@searchFilter}) ->
        @listenTo @collection, 'sync', @render
        @searchChange = _.debounce @searchChange, 200
        @currentServices = []

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
            searchFilter: @searchFilter

        @renderTable()

        @$('.actions-column a[title]').tooltip()
        @$('.icons-column span[title]').tooltip()

    renderTable: =>
        @filterCollection()
        @$('#servicesTable').html @tableTemplate
           services: @currentServices
           config: config

    filterCollection: =>
        services = _.pluck @collection.models, "attributes"

        if @searchFilter
            services = _.filter services, (service) =>
                searchFilter = @searchFilter.toLowerCase().split("@")[0]
                valuesToSearch = []

                valuesToSearch.push(service.id)
                valuesToSearch.push(service.basePath)
                for group in service.loadBalancerGroups
                    valuesToSearch.push(group)

                searchTarget = valuesToSearch.join("")
                searchTarget.toLowerCase().indexOf(searchFilter) isnt -1
        @currentServices = services

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


    searchChange: (event) =>
        return unless @ is app.views.current

        previousSearchFilter = @searchFilter
        $search = @$ "input[type='search']"
        @searchFilter = $search.val()

        if @searchFilter isnt previousSearchFilter
            @renderTable()

module.exports = ServicesView
