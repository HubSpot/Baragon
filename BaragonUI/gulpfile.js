var gulp = require('gulp');
var gutil = require('gulp-util');
var path = require('path');
var fs = require('fs');
var del = require('del');

var mustache = require('gulp-mustache');

var concat = require('gulp-concat');

var serverBase = process.env.BARAGON_BASE_URI || '/baragon';

var staticUri = process.env.BARAGON_STATIC_URI || (serverBase + '/static');
var appUri = process.env.BARAGON_APP_URI || (serverBase + '/ui');

var templateData = {
  staticRoot: staticUri,
  appRoot: appUri,
  apiRoot: process.env.BARAGON_API_URI || '',
  allowEdit: process.env.BARAGON_ALLOW_EDIT || 'false',
  authEnabled: process.env.BARAGON_AUTH_ENABLE || 'true',
  elbEnabled: process.env.ELB_ENABLED || 'false',
  title: process.env.BARAGON_TITLE || 'Baragon (local dev)',
  navColor: process.env.BARAGON_NAV_COLOR
};

var dest = path.resolve(__dirname, 'dist');

var webpackStream = require('webpack-stream');
var webpack = require('webpack');

var port = process.env.PORT || 3334;
var useHMR = process.env.USE_HMR || true;
var webpackHMRPath = serverBase + '/__webpack_hmr';

__webpack_public_path__ = serverBase;

gulp.task('clean', function() {
  return del(dest + '/*');
});

gulp.task('static', ['clean'], function() {
  return gulp.src(['app/assets/static/**/*'])
    .pipe(gulp.dest(dest + '/static'));
})

gulp.task('html', ['static'], function () {
  return gulp.src('app/assets/index.mustache')
    .pipe(mustache(templateData, {extension: '.html'}))
    .pipe(gulp.dest(dest));
});

gulp.task('debug-html', ['static'], function () {
  templateData.isDebug = true;
  return gulp.src('app/assets/index.mustache')
    .pipe(mustache(templateData, {extension: '.html'}))
    .pipe(gulp.dest(dest));
});

gulp.task('build', ['html'], function () {
  return gulp.src('app')
    .pipe(webpackStream(require('./webpack.config')))
    .pipe(gulp.dest(dest + '/static'));
});

gulp.task('serve', ['debug-html'], function () {
  var count = 0;
  var webpackConfig = require('./make-webpack-config')({
    isDebug: true,
    useHMR: useHMR,
    webpackHMRPath: webpackHMRPath,
    publicPath: staticUri
  });
  return new Promise(resolve => {
    var bs = require('browser-sync').create();
    var compiler = webpack(webpackConfig);
    // Node.js middleware that compiles application in watch mode with HMR support
    // http://webpack.github.io/docs/webpack-dev-middleware.html
    var webpackDevMiddleware = require('webpack-dev-middleware')(compiler, {
      publicPath: staticUri,
      stats: webpackConfig.stats,
    });
    var webpackHotMiddleware = require('webpack-hot-middleware')(compiler, {
      path: webpackHMRPath
    });
    compiler.plugin('done', function () {
      // Launch Browsersync after the initial bundling is complete
      if (++count === 1) {
        bs.init({
          port: port,
          startPath: appUri,
          open: false,
          socket: {
            domain: 'localhost:' + port,
            clientPath: '/baragon/browser-sync'
          },
          server: {
            baseDir: 'dist',
            middleware: [
              webpackDevMiddleware,
              webpackHotMiddleware,
              // Serve index.html for all unknown requests
              function(req, res, next) {
                if (req.headers.accept && req.headers.accept.startsWith('text/html')) {
                  req.url = '/index.html'; // eslint-disable-line no-param-reassign
                }
                next();
              },
            ],
          },
        }, resolve);
      }
    });
  });
});

gulp.task('default', ['build']);
