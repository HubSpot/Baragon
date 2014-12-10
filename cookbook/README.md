# baragon-cookbook

TODO: Enter the cookbook description here.

## Supported Platforms

TODO: List your supported platforms.

## Attributes

<table>
  <tr>
    <th>Key</th>
    <th>Type</th>
    <th>Description</th>
    <th>Default</th>
  </tr>
  <tr>
    <td><tt>['baragon']['bacon']</tt></td>
    <td>Boolean</td>
    <td>whether to include bacon</td>
    <td><tt>true</tt></td>
  </tr>
</table>

## Usage

### baragon::default

Include `baragon` in your node's `run_list`:

```json
{
  "run_list": [
    "recipe[baragon::default]"
  ]
}
```

## License and Authors

Author:: EverTrue, Inc. (<eric.herot@evertrue.com>)
