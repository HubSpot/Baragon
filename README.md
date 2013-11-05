Baragon
=======

![Behold the mighty Baragon's roar](http://i.imgur.com/mCbkbcZ.jpg)

HubSpot's load balancer API.

## Architecture

```
  ZooKeeper                    ZooKeeper                ZooKeeper
      ^                            ^                        ^
      |                            |                        |
                                             /-----> Baragon Agent ------> LB
BaragonService ------> BaragonAgent (leader) ------> Baragon Agent ------> LB
                                             \-----> Baragon Agent ------> LB
```

## Endpoints

TODO

### Baragon

TODO

### Baragon Agent

TODO