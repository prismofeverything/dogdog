# gort

An evolving IRC bot that generates a markov chain for every nick in the channel it inhabits, and responds to that nick with dialogue generated from their own statements!

## Usage

```clj
(require '[gort.core :as gort])
(def irc (gort/init "#instrument"))
```

## License

Copyright © 2013 Ryan Spangler

Distributed under the Eclipse Public License, the same as Clojure.
