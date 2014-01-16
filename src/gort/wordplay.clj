(ns gort.wordplay
  (:require [clojure.string :as string]))

(defn calculate-entropy
  [text]
  (let [stripped (-> text
                     string/lower-case
                     (string/replace #"[^a-z ]" ""))
        coll (reduce conj #{} stripped)
        words (string/split stripped #"\s+")
        lengths (reduce #(+ %1 (count %2)) 0 words)
        avg (/ lengths (count words))]
    {:average-word-length avg :char-distribution coll}))