# clj-librato

A Clojure library for interacting with [Librato Metrics](https://metrics.librato.com).  Currently at version 0.0.1, and focusing on sending collated metrics.

Feel free to contribute!

## Usage

````clojure
(require '[clj-librato.metrics :as metrics])

; pass it an email, api key, list of gauges, and a list of counters
(metrics/collate "me@mydomain.com" "my-api-key" [{:name "gauge 1" value: 34 } {:name "gauge 2" value: 0}] 
                                                [{:name "a counter" :value 79213}])
````

## License

Copyright (C) 2011 Michael Gorsuch <michael.gorsuch@gmail.com>

Distributed under the Eclipse Public License, the same as Clojure.
