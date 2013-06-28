(ns gort.core
  (:require [clojure.string :as string]
            [clojure.java.io :as io]
            [irclj.core :as irclj]))

(defn respond-sensibly
  [irc m]
  (let [text (:text m)]
    (if (re-find #"dogdog" text)
      (irclj/message irc "#instrument" "DOGDOG LOVES YOU"))))

(defn init
  [irc]
  (dosync (alter irc assoc-in [:callbacks :privmsg] respond-sensibly)))
