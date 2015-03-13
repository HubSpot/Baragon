# Set up the only app globals
window.utils = require 'utils'
window.app = require 'application'

# Set up third party configurations
require 'thirdPartyConfigurations'
# Set up the Handlebars helpers
require 'handlebarsHelpers'

# Initialize the app on DOMContentReady
$ ->
    if config.apiRoot and (config.authKey or not config.authEnabled)
        app.initialize()
    else
        if not config.apiRoot and (not config.authKey and config.authEnabled)
            template = require './templates/vex/apiRootAuthKeyPrompt'
            input =  """
                    <input name="apiroot" type="text" placeholder="ApiRoot" required />
                    <input name="authkey" type="password" placeholder="AuthKey" required/>
                """
        else if not config.apiRoot
            template = require './templates/vex/apiRootPrompt'
            input =  """
                    <input name="apiroot" type="text" placeholder="ApiRoot" required />
                """
        else if (not config.authKey and config.authEnabled)
            template = require './templates/vex/authKeyPrompt'
            input =  """
                    <input name="authkey" type="password" placeholder="AuthKey" required />
                """

        if template and input
            vex.dialog.prompt
                message: template()
                input: input
                callback: (data) =>
                    if data.apiroot
                        localStorage.setItem "apiRootOverride", data.apiroot
                    if data.authkey
                        localStorage.setItem "authKeyOverride", data.authkey
                    window.location = window.location.href
