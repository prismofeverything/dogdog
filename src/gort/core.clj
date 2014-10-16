(ns gort.core
  (:require [clojure.string :as string]
            [clojure.java.io :as io]
            [markov.nlp :as markov]
            [zalgo.core :as zalgo]
            [irclj.core :as irclj]
            [gort.novelty :as novelty]
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
       (markov/distribute-sentence chain line))
       ;; (let [tokens (parse-message line)]
       ;;   (markov/add-token-stream chain tokens)))
     chain (line-seq lines))))

(defn read-history
  [nicks]
  (apply
   merge
   (mapv
    (fn [nick]
      (println "Reading nick" nick)
      (let [chain (markov/empty-chain)
            history (read-nick-history nick chain)]
        {nick history}))
    nicks)))

(defn persistence-handler
  [handler]
  (fn [{:keys [irc text nick target] :as request}]
    (let [tokens (parse-message text)
          nick (keyword nick)
          chain (or (get-in @irc [:chains nick]) (markov/empty-chain))
          inner (assoc request
                  :chain chain
                  :tokens tokens
                  :add-tokens? true)

          {:keys [irc chain generated persist? add-tokens?] :as response} (handler inner)

          expanded (if add-tokens?
                     (markov/distribute-sentence chain text)
                     ;; (markov/add-token-stream chain tokens)
                     chain)]
      (when persist?
        (persist-message nick text))
      (dosync
       (alter irc assoc-in [:chains nick] expanded))
      (if generated
        (irclj/message irc target generated))
      (assoc response :irc irc))))

(defn root-handler
  [request]
  (assoc request
    :persist? true))

(def template-limit 18)

(defn markov-handler
  [handler]
  (let [triggers [#"D" #"dogdog"]
        ignore? #(or (= % "D") (re-find #"dogdog" %))]
    (fn [{:keys [chain text tokens] :as request}]
      (if (some #(re-find % text) triggers)
        (let [persist? (not (ignore? text))
              expanded (if persist?
                         ;; (markov/add-token-stream chain tokens)
                         (markov/distribute-sentence chain text)
                         chain)
              generated (markov/generate-coherent-sentence expanded)]
              ;; generated (markov/generate-coherent-sentence expanded template-limit)
              ;; generated (markov/generate-sentence expanded)]
              ;; generated (string/join " " (markov/follow-strand expanded))]
          (assoc request
            :chain expanded
            :generated generated
            :persist? persist?
            :add-tokens? false))
        (handler request)))))

(defn twp-handler
  [handler]
  (let [triggers [#"3" #"three" #"poem"]
        ignore? (set (map str triggers))]
    (fn [{:keys [text] :as request}]
      (let [adjective-matches (re-find #"(add|forget)-adjective ([a-z]+)" text)
            adjective-command (second adjective-matches)
            adjective (nth adjective-matches 2)
            noun-matches (re-find #"(add|forget)-noun ([a-z]+)" text)
            noun-command (second noun-matches)
            noun (nth noun-matches 2)]
        (if (or adjective noun (some #(re-find % text) triggers))
          (let [persist? (not (or (ignore? text) adjective noun))
                constraints {}
                constraints (if adjective (assoc constraints :adjective adjective) constraints)
                constraints (if noun (assoc constraints :noun noun) constraints)
                _ (when (= "add" adjective-command) (twp/add-word-of-type adjective "adjective"))
                _ (when (= "add" noun-command) (twp/add-word-of-type noun "noun"))
                generated (twp/three-word-poem constraints)
                _ (when (= "forget" adjective-command) (twp/forget-word-of-type adjective "adjective"))
                _ (when (= "forget" noun-command) (twp/forget-word-of-type noun "noun"))]
            (assoc request
              :persist? persist?
              :add-tokens? persist?
              :generated generated))
          (handler request))))))

(defn zalgo-handler
  [handler]
  (let [triggers [#"Z" #"zalgo"]
        ignore? (set (map str triggers))]
    (fn [request]
      (let [{:keys [generated text] :as response} (handler request)]
        (if (some #(re-find % text) triggers)
          (let [generated (or generated (novelty/novelty-string (+ 3 (rand-int 71)) (rand)))
                zalgo (zalgo/zalgoize generated (+ 3 (rand-int 11)))
                persist? (not (ignore? text))]
            (assoc response
              :generated zalgo
              :add-tokens? persist?
              :persist? persist?))
          response)))))

(defn nested-handler
  [handler]
  (fn [irc {:keys [text nick target] :as msg}]
    (try
      (handler (assoc msg :irc irc))
      (catch Exception e (.printStackTrace e)))))

(def dogdog-handler
  (-> root-handler
      markov-handler
      twp-handler
      zalgo-handler
      persistence-handler
      nested-handler))

(defn init
  ([channels]
    (init channels nil))
  ([channels nick]
    (let [irc (irclj/connect "irc.freenode.net" 6667 (or nick "dogdog"))
          _ (println "connected")
          nicks (tracked-nicks)
          _ (println "tracked nicks:" nicks)
          history (read-history nicks)
          _ (println "history read")]
      (dosync
       (alter irc assoc-in [:callbacks :privmsg] #'dogdog-handler)
       (alter irc assoc :chains history))
      (irclj/identify irc "ord9949")
      (doseq [channel channels]
        (irclj/join irc channel))
      irc)))

(declare irc)

(defn -main
  [& args]
  (def irc (init ["#tripartite" "#antler"])))
