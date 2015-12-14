View = require './view'

class ElbsView extends View

    template: require '../templates/elbs'
    tableTemplate: require '../templates/elbsTable'
    clearCacheTemplate: require '../templates/vex/clearElbCache'

    initialize: ({@searchFilter}) ->
        @listenTo @collection, 'sync', @render
        @currentElbs =[]

    events: =>
        _.extend super,
            'click [data-action="viewJSON"]':   'viewJson'
            'click [data-action="clearCache"]': 'clearElbCache'

    render: =>
        @$el.html @template
            config: config
            synced: @collection.synced
            searchFilter: @searchFilter

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

    clearElbCache: (e) =>
        vex.dialog.confirm
            message: @clearCacheTemplate
            buttons: [
                $.extend {}, vex.dialog.buttons.YES,
                    text: 'CLEAR',
                    className: 'vex-dialog-button-primary vex-dialog-button-primary-remove'
                vex.dialog.buttons.NO
            ]
            callback: (data) =>
                return if data is false
                $.ajax(
                    url: "#{ config.apiRoot }/elbs/cache?authkey=#{ localStorage.getItem 'baragonAuthKey' }"
                    type: "DELETE"
                ).done => @trigger 'refreshrequest'

module.exports = ElbsView
