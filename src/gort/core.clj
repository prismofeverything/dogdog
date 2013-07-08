(ns gort.core
  (:require [clojure.string :as string]
            [clojure.java.io :as io]
            [markov.core :as markov]
            [zalgo.core :as zalgo]
            [irclj.core :as irclj]
            [gort.twp :as twp]))

(def nicks-path "nicks/")

(defn parse-message
  [message]
  (string/split message #" +"))

(defn persist-message
  [nick message]
  (spit (str nicks-path (name nick)) (str message "\n") :append true))

(defn nick-files
  []
  (let [nicks-dir (file-seq (io/file nicks-path))]
    (remove #(.isDirectory %) nicks-dir)))

(defn tracked-nicks
  []
  (let [nicks (nick-files)]
    (map #(keyword (.getName %)) nicks)))

(defn read-nick-history
  [nick chain]
  (with-open [lines (io/reader (str nicks-path (name nick)))]
    (reduce 
     (fn [chain line]
       (let [tokens (parse-message line)]
         (markov/add-token-stream chain tokens)))
     chain (line-seq lines))))

(defn read-history
  [nicks]
  (apply 
   merge
   (map
    (fn [nick]
      (let [chain (markov/empty-chain)
            history (read-nick-history nick chain)]
        {nick history}))
    nicks)))



                ;; (let [expanded (if track? (markov/add-token-stream chain tokens) chain)
                ;;       generated (markov/follow-strand expanded)]
                ;;   (persist-message nick text)
                ;;   (assoc env
                ;;     :chain expanded
                ;;     :generated generated)))})

(def markov-generator
  {:triggers [#"D" #"dogdog"]
   :generator (fn [{:keys [chain] :as env}]
                (markov/follow-strand chain))})

(def twp-generator
  {:triggers [#"3" #"three" #"poem"]
   :generator (fn [env]
                (twp/three-word-poem))})

(def twp-add-adjective-generator
  {:triggers [#"add-adjective ()"]})

(defn response-handler
  [irc {:keys [text nick target] :as msg}]
  (try
    (let [tokens (parse-message text)
          nick (keyword nick)
          chain (or (get-in @irc [:chains nick]) (markov/empty-chain))
          expanded (if (or (= text "D")
                           (= text "Z")
                           (= text "3")
                           (= text "poem"))
                     chain
                     (do
                       (persist-message nick text)
                       (markov/add-token-stream chain tokens)))]
      (dosync
       (alter irc assoc-in [:chains nick] expanded))

      (cond
       (re-find #"Z" text)
       (irclj/message irc target (zalgo/zalgoize (string/join " " (markov/follow-strand expanded))))

       (or (re-find #"D" text)
           (re-find #"dogdog" text))
       (irclj/message irc target (string/join " " (markov/follow-strand expanded)))

       (or (re-find #"3" text)
           (re-find #"poem" text))
       (irclj/message irc target (twp/three-word-poem))))

      ;; TODO need to refactor this into content generators and filters

      (when-let [matches (re-find #"add-adjective ([a-z]+)" text)]
        (twp/add-word-of-type (second matches) "adjective")
        (irclj/message irc target (twp/three-word-poem {:adjective (second matches)})))

      (when-let [matches (re-find #"add-noun ([a-z]+)" text)]
        (twp/add-word-of-type (second matches) "noun")
        (irclj/message irc target (twp/three-word-poem {:noun (second matches)})))

    (catch Exception e (.printStackTrace e))))

(defn init
  [channel]
  (let [irc (irclj/connect "irc.freenode.net" 6667 "dogdog")
        nicks (tracked-nicks)
        history (read-history nicks)]
    (dosync 
     (alter irc assoc-in [:callbacks :privmsg] #'response-handler)
     (alter irc assoc :chains history))
    (irclj/join irc channel)
    irc))
