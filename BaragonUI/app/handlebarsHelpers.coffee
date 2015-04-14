Handlebars.registerHelper 'appRoot', ->
    config.appRoot

Handlebars.registerHelper 'ifEqual', (v1, v2, options) ->
    if v1 is v2 then options.fn @ else options.inverse @

Handlebars.registerHelper 'ifGreaterThan', (v1, v2, options) ->
    if v1 > v2 then options.fn @ else options.inverse @

# 'DRIVER_NOT_RUNNING' => 'Driver not running'
Handlebars.registerHelper 'humanizeText', (text) ->
    return '' if not text
    text = text.replace /_/g, ' '
    text = text.toLowerCase()
    text = text[0].toUpperCase() + text.substr 1
    text

Handlebars.registerHelper 'humanizeFileSize', (fileSize) ->
    kilo = 1024
    mega = 1024 * 1024
    giga = 1024 * 1024 * 1024

    shorten = (which) -> Math.round fileSize / which

    if fileSize > giga
        return "#{ shorten giga } GB"
    else if fileSize > mega
        return "#{ shorten mega } MB"
    else if fileSize > kilo
        return "#{ shorten kilo } KB"
    else
        return "#{ fileSize } B"

Handlebars.registerHelper 'debug', (variable) ->
    console.dir(variable)

Handlebars.registerHelper 'roundedPercentage', (v1, v2) ->
    return Math.round((v1/v2 * 100))

Handlebars.registerHelper 'quotient', (v1, v2) ->
    return (v1/v2 * 100)

Handlebars.registerHelper 'timestampFromNow', (timestamp) ->
    return '' if not timestamp
    timeObject = moment timestamp
    "#{timeObject.fromNow()} (#{ timeObject.format 'lll'})"

Handlebars.registerHelper 'parseAgent', (urlString) ->
    return urlString.replace("http://", "").split("/", 2)[0]

Handlebars.registerHelper 'parseStatusUrl', (urlString) ->
    return urlString.replace(/request\/.*/,"status")

Handlebars.registerHelper 'jsonStringify', (object) ->
    return JSON.stringify object, undefined, 4
