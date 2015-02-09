Handlebars.registerHelper 'appRoot', ->
    config.appRoot

Handlebars.registerHelper 'ifEqual', (v1, v2, options) ->
    if v1 is v2 then options.fn @ else options.inverse @

Handlebars.registerHelper 'ifInSubFilter', (needle, haystack, options) ->
    return options.fn @ if haystack is 'all'
    if haystack.indexOf(needle) isnt -1
        options.fn @
    else
        options.inverse @

# 'DRIVER_NOT_RUNNING' => 'Driver not running'
Handlebars.registerHelper 'humanizeText', (text) ->
    return '' if not text
    text = text.replace /_/g, ' '
    text = text.toLowerCase()
    text = text[0].toUpperCase() + text.substr 1
    text
