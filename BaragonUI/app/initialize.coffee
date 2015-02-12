# Set up the only app globals
window.utils = require 'utils'
window.app = require 'application'

apiRootPromptTemplate = require './templates/vex/apiRootPrompt'

# Set up third party configurations
require 'thirdPartyConfigurations'
# Set up the Handlebars helpers
require 'handlebarsHelpers'

# Initialize the app on DOMContentReady
$ ->
    if config.apiRoot
        app.initialize()
    else
        # In the event that the apiRoot isn't set (running through Brunch server)
        # prompt the user for it and refresh
        vex.dialog.prompt
            message: apiRootPromptTemplate()
            input: """
                <input name="apiroot" type="text" placeholder="http://localhost/baragon/v2" required />
                <input name="authkey" type="text" placeholder="AuthKey" />
            """
            callback: (data) =>
                if data.apiroot
                    localStorage.setItem "apiRootOverride", data.apiroot
                if data.authkey
                    localStorage.setItem "authKeyOverride", data.authkey
                window.location = window.location.href
