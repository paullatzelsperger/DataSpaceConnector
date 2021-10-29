## General thoughts

An `Asset` represents an entry in data catalog exposed by a provider. Based on an `Asset`, a consumer of the dataspace
 should be able to determine if the data represented by this `Asset` are relevant for him. 

## Structure of an `Asset` object

### Provider and data description

As mentioned above, the `Asset` is the entry point for any consumer of the dataspace connector. 
Each `Asset` will generally contain some generic information, such as:
- the `id` that will be later used to consume the data represented by this `Asset`,
- some information about the `provider` that is exposing this `Asset`,
- some information about the data that are represented by this `Asset`:
  - a free text describing the data,
  - the type of the data, i.e. Json document, xml files, images...
  - flag indicating if data is open or monetized,
  - potentially a logo associated with these data,
  - ...
  
Hereafter we provide an example of `Asset`:
```json
{
  "properties" : {
    "id": "123",
    "provider": {
      "name": "my-data-provider",
      "logoUrl": "http://data-provider-logo.com"
    },
    "data": {
        "description" : "schedule data about flights",
        "contentType" : "application/json", 
        "isOpenData": "true",
        "logoUrl": "http://my-data-logo.com"
    } 
  }
}
```

### Specific information

#### Filters

In most cases, an `Asset` represents a large set of data, that can be finite (e.g. a database) or a stream. In the case
where data are stored within a database, additional filters can/must be provided by the data consumer in the request in
order to return only the data has to be returned by the data provider. These query parameters are 
provided within the `filters` section of the `Asset`:

```json
{
  "filters": [
    {
      "name": "xxx",
      "type": "string",
      "mandatory": true
    },
    {
      "name": "yyy",
      "type": "iso-date",
      "mandatory": false
    }
  ]
}
```

The `Asset` is part of the data query that is send by the consumer to query data from the provider. Thus it can provide
the `values` associated with each `filter`, e.g.:

```json
{
  "filters": [
    {
      "name": "xxx",
      "type": "string",
      "mandatory": true,
      "value": "ABCD"
    },
    {
      "name": "yyy",
      "type": "iso-date",
      "mandatory": false,
      "value": "2021-08-15"
    }
  ]
}
```







