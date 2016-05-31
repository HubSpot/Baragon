## BasePath Locking and Updating

When adding a service to Baragon, the service must have an associated `basePath`. Originally built around nginx, this was to ensure that no two services would attempt to be hosted on the same path on the same load balancer (causing an invalid configuration). The base path lock is always associated with a serviceId. Some notes on locking and changing base paths for a service:

- base paths are locked at the beginning of request processing to avoid conflicts
- If a service is moved off of a load balancer group (ie. changed from group A to group B) the base path lock is released on the old group
- If a base path changes the lock on the old one is released
- To have a new serviceId take over a base path that is currently locked, use the `replaceServiceId` request field. (The alternative to this is to make a request to remove the previous service than a request to add the new service. This is not an instant switch and could cause down time)
  - Example:
    - Service `A` is at base path `/test`
    - Make a request for service `B` with `replaceServiceId` set to `A`
    - Service `A`'s configs are removed, service `B`'s configs are added, base path `/test` is now associated with service `B`