#!/bin/bash

set -e

sed -i '' 's/"-sharing",/"-sharing", "-livereload",/g' book.json
gitbook install
gitbook build
cp BaragonUI/app/assets/static/images/favicon.ico _book/gitbook/images/favicon.ico
cd _book
git init
git add .
git commit -m "update gitbook from master branch docs"
git push --force --quiet git@github.com:HubSpot/Baragon.git master:gh-pages