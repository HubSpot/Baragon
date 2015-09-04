path = require 'path'
fs =   require 'fs'

handlebars = require 'handlebars-brunch/node_modules/handlebars'

# Brunch settings
exports.config =
    paths:
        public: path.resolve(__dirname, '../BaragonService/target/generated-resources/assets')

    files:
        javascripts:
            joinTo: 'static/js/app.js'
            order: before: [
                /^(bower_components|vendor)/
            ]

        stylesheets:
            joinTo: 'static/css/app.css'

        templates:
            defaultExtension: 'hbs'
            joinTo: 'static/js/app.js'

    server:
        base: process.env.BARAGON_BASE_URI ? '/baragon'


    # When running BaragonUI via brunch server we need to make an index.html for it
    # based on the template that's shared with BaragonService
    #
    # After we compile the static files, compile index.html using some required configs
    onCompile: =>
        destination = path.resolve @config.paths.public, 'index.html'

        templatePath = path.resolve 'app/assets/_index.mustache'
        indexTemplate = fs.readFileSync templatePath, 'utf-8'

        templateData =
            staticRoot: process.env.BARAGON_STATIC_URI ? "#{ @config.server.base }/static"
            appRoot: "#{ @config.server.base }/ui"
            apiRoot: process.env.BARAGON_API_URI ? ''
            allowEdit: process.env.BARAGON_ALLOW_EDIT ? false
            authEnabled: process.env.BARAGON_AUTH_ENABLE ? true
            elbEnabled: process.env.ELB_ENABLED ? false
            title: process.env.BARAGON_TITLE ? 'Baragon (local dev)'

        compiledTemplate = handlebars.compile(indexTemplate)(templateData)
        fs.writeFileSync destination, compiledTemplate
