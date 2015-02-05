Baragon Testing Script
======================

This script will automatically test a number of request scenarios for a Baragon cluster.

##Usage

- Simply run `python baragon_test.py` to run the tests
- The test script will clean requests out of the state datastore as it finishes its tests

###Options
| Option        | Description                                                   | Default                         |
| ------------- | ------------------------------------------------------------- | ------------------------------- |
| -u --uri      | Baragon Service base uri                                      | 192.168.33.20:8080/baragon/v2   |
| -k --key      | Baragon Service auth key                                      | None                            |
| -m --master   | Baragon Service master auth key                               | None                            |
| --upstream    | Default upstream to use in requests                           | example.com:80                  |
| -l -lbGroup   | Load balancer group to use for testing                        | vagrant                         |
| -s --service  | Only run tests on BAragonService (no interaction with agents) | false                           |
