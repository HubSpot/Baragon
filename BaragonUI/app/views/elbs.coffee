View = require './view'

class ElbsView extends View

    template: require '../templates/elbs'
    tableTemplate: require '../templates/elbsTable'

    initialize: () ->
        @listenTo @collection, 'sync', @render
        @currentElbs =[]

    events: =>
        _.extend super,
            'click [data-action="viewJSON"]':        'viewJson'

    render: =>
        @$el.html @template
            config: config
            synced: @collection.synced

        @$('#elbsTable').html @tableTemplate
           elbs: @collection.toJSON()
           config: config

        @$('.actions-column a[title]').tooltip()
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

    viewJson: (e) ->
        name = $(e.target).parents('tr').data 'elb-name'
        utils.viewJSON @collection.where(loadBalancerName: name)[0]


module.exports = ElbsView
