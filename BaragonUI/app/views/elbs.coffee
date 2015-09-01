View = require './view'

class ElbsView extends View

    template: require '../templates/elbs'
    tableTemplate: require '../templates/elbsTable'

    initialize: ({@searchFilter}) ->
        @listenTo @collection, 'sync', @render
        @searchChange = _.debounce @searchChange, 200
        @currentElbs =[]

    events: =>
        _.extend super,
            'click [data-action="viewJSON"]':        'viewJson'
            'change input[type="search"]':           'searchChange'
            'keyup input[type="search"]':            'serachChange'
            'input input[type="search"]':            'searchChange'

    render: =>
        @$el.html @template
            config: config
            synced: @collection.synced
            searchFilter: @searchFilter

        @renderTable()

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
        @filterCollection()
        @$('#elbsTable').html @tableTemplate
           elbs: @currentElbs
           config: config

    filterCollection: =>
        elbs = _.pluck @collection.models, "attributes"

        if @searchFilter
            elbs = _.filter elbs, (elb) =>
                searchFilter = @searchFilter.toLowerCase().split("@")[0]
                valuesToSearch = []
                valuesToSearch.push(elb.loadBalancerName)
                valuesToSearch.push(elb.scheme)
                valuesToSearch.push(elb.dnsname)
                valuesToSearch.push(elb.vpcid)
                for instance in elb.instances
                    valuesToSearch.push(instance.instanceId)

                searchTarget = valuesToSearch.join("")
                searchTarget.toLowerCase().indexOf(searchFilter) isnt -1
        @currentElbs = elbs

    searchChange: (event) =>
        return unless @ is app.views.current

        previousSearchFilter = @searchFilter
        $search = @$ "input[type='search']"
        @searchFilter = $search.val()

        if @searchFilter isnt previousSearchFilter
            @renderTable()

    viewJson: (e) ->
        name = $(e.target).parents('tr').data 'elb-name'
        utils.viewJSON @collection.where(loadBalancerName: name)[0]


module.exports = ElbsView
