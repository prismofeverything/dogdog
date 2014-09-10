# dogdog

An evolving IRC bot that generates a markov chain for every nick in the channel it inhabits, and responds to that nick with dialogue generated from their own statements!  Also analyzes each nick's statements with natural language processing and builds a set of grammatical templates to build its statements with.

## Usage

```clj
(require '[dogdog.core :as dogdog])
(def irc (dogdog/init "#tripartite"))
```

## License

Copyright © 2013-2014 Ryan Spangler

Distributed under the Eclipse Public License, the same as Clojure.
