Router = require 'router'
NavView = require 'views/nav'

class Application

    views: {}

    blurred: false

    initialize: ->
        @setupGlobalErrorHandling()
        @$page = $('#page')
        @page = @$page[0]

        $body = $ 'body'

        @views.nav = new NavView
        @views.nav.render()
        $body.prepend @views.nav.$el

        $('.page-loader.fixed').hide()

        @router = new Router

        el = document.createElement('a')
        el.href = config.appRoot

        Backbone.history.start
            pushState: true
            root: el.pathname

        $(window).on 'blur',  =>
            @blurred = true

        $(window).on 'focus', =>
            @blurred = false
            @currentController.refresh()

    caughtError: ->
        @caughtThisError = true

    setupGlobalErrorHandling: ->
        unloading = false
        $(window).on 'beforeunload', ->
            unloading = true
            return

        # When an Ajax error occurs this is the default message that is displayed.
        # You can add your own custom error handling using app.caughtError() above.
        $(document).on 'ajaxError', (e, jqxhr, settings) =>
            # If we handled this already, ignore it
            if @caughtThisError
                @caughtThisError = false
                return

            return if settings.suppressErrors
            return if jqxhr.statusText is 'abort'
            return if unloading
            return if @blurred and jqxhr.statusText is 'timeout'

            url = settings.url.replace(config.appRoot, '')

            if jqxhr.status is 502
                Messenger().info
                    message:   "Baragon is deploying, your requests cannot be handled. Things should resolve in a few seconds so just hang tight!"
                    hideAfter: 10
            else if jqxhr.statusText is 'timeout'
                Messenger().error
                    message:   "<p>A <code>#{ jqxhr.statusText }</code> error occurred while accessing:</p><pre>#{ url }</pre>"
                    hideAFter: 20
            else if jqxhr.status is 0
                Messenger().error
                    message:   "<p>Could not reach the Baragon API. Please make sure BaragonUI is properly set up.</p><p>If running through Brunch, this might be your browser blocking cross-domain requests.</p>"
                    hideAfter: 20
            else
                console.log jqxhr.responseText
                try
                    serverMessage = JSON.parse(jqxhr.responseText).message or jqxhr.responseText
                catch
                    serverMessage = jqxhr.responseText

                serverMessage = _.escape serverMessage

                Messenger().error
                    message:   "<p>An uncaught error occurred with your request. The server said:</p><pre>#{ serverMessage }</pre><p>The error has been saved to your JS console.</p>"
                    hideAfter: 20

                console.error jqxhr
                throw new Error "AJAX Error"

    # Usually called by Controllers when they're initialized. Loader is overwritten by views
    showPageLoader: ->
        @$page.html "<div class='page-loader centered cushy'></div>"

    bootstrapController: (controller) ->
        @currentController = controller

    # Called by Controllers when their views are ready to take over
    showView: (view) ->
        # Clean up events & stuff
        @views.current?.remove()

        $(window).scrollTop 0

        @views.current = view
        # Render & display the view
        view.render()

        if @page.children.length
            @page.replaceChild view.el, @page.children[0]
        else
            @page.appendChild view.el

module.exports = new Application
