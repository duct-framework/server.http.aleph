# Duct server.http.aleph

[![Build Status](https://travis-ci.org/duct-framework/server.http.aleph.svg?branch=master)](https://travis-ci.org/duct-framework/server.http.aleph)

Integrant multimethods for running an [Aleph][] HTTP server for the
[Duct][] framework.

[aleph]: http://aleph.io/
[duct]: https://github.com/duct-framework/duct

## Installation

To install, add the following to your project `:dependencies`:

    [duct/server.http.aleph "0.1.2"]

## Usage

This library adds Integrant methods that dispatch off the
`:duct.server.http/aleph` key, which is derived from
`:duct.server/http`. The corresponding value is a map of options for
the [Aleph Ring adapter][], plus a `:handler` key that takes a handler
function.

For example:

```clojure
{:duct.server.http/aleph
 {:port    3000
  :handler (fn [request]
             {:status  200
              :headers {"Content-Type" "text/plain"}
              :body    "Hello World"})}}
```

A `:logger` key may also be specified, which will be used to log when
the server starts and when it stops. The value of the key should be an
implementation of the `duct.logger/Logger` protocol from the
[duct.logger][] library

[aleph ring adapter]: http://aleph.io/aleph/http.html
[duct.logger]: https://github.com/duct-framework/logger

## License

Copyright © 2017 James Reeves

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
