# Set up the only app globals
window.utils = require 'utils'
window.app = require 'application'

# Set up third party configurations
require 'thirdPartyConfigurations'
# Set up the Handlebars helpers
require 'handlebarsHelpers'

# Initialize the app on DOMContentReady
$ ->
    if config.apiRoot
        app.initialize()
    else
        template = require './templates/vex/apiRootPrompt'
        input =  """
                <input name="apiroot" type="text" placeholder="ApiRoot" required />
            """

        vex.dialog.prompt
            message: template()
            input: input
            callback: (data) =>
                if data.apiroot
                    localStorage.setItem "apiRootOverride", data.apiroot
                window.location = window.location.href
