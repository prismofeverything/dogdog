(ns dogdog.core
  (:require [clojure.string :as string]
            [clojure.java.io :as io]
            [clojure.tools.cli :as cli]
            [twitter.oauth :as twitter-oauth]
            [twitter.api.restful :as twitter-api]
            [markov.nlp :as markov]
            [zalgo.core :as zalgo]
            [irclj.core :as irclj]
            [dogdog.novelty :as novelty]
            [dogdog.twp :as twp]))

(def cfg (atom {}))
(def irc-connection (atom nil))
(def twitter-credentials (atom nil))

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

(defn send-safe-tweet [tweet]
  (try (twitter-api/statuses-update :oauth-creds @twitter-credentials
                                    :params {:status tweet})
           (catch Exception e (println "Twitter-api error: " (.getMessage e)))))

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
            ;; experimental!
            (when @twitter-credentials
              (send-safe-tweet (str generated " #3wp")))
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

(defn numberwang-handler
  [handler]
  (let [triggers [#"\b[012456789]{1,9}\b" #"\b(one|two|four|five|six|seven|eight|nine|ten|twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety|hundred|thousand|million|billion|trillion|zillion)\b"]]
    (fn [{:keys [text] :as request}]
      (if (and (some #(re-find % text) triggers)
               (-> 10 rand int (> 8)))
        (assoc request
          :generated "THAT'S NUMBERWANG!")
        (handler request)))))

(defn nested-handler
  [handler]
  (fn [irc {:keys [text nick target] :as msg}]
    (try
      (handler (assoc msg :irc irc))
      (catch Exception e (.printStackTrace e)))))

(def dogdog-handler
  (-> root-handler
      markov-handler
      numberwang-handler
      twp-handler
      zalgo-handler
      persistence-handler
      nested-handler))

(defn load-twitter-config
  []
  (when-let [config (:twitter @cfg)]
    (try
      (when-let [loaded (-> config slurp read-string)]
        loaded)
      (catch Exception e (println "Couldn't read twitter config: " (.getMessage e))))))
      
(defn init
  ([]
    (println (str "Using config " @cfg))
    (init (or (:channel @cfg)
              ["#tripartite" "#antler"])
          (:nick @cfg)))
  ([channels]
    (init channels nil))
  ([channels nick]
    (let [twitter (load-twitter-config)
          irc (irclj/connect "irc.freenode.net" 6667 (or nick "dogdog"))
          _ (println "connected")
          nicks (tracked-nicks)
          _ (println "tracked nicks:" nicks)
          history (read-history nicks)
          _ (println "history read")]
      (when twitter
        (println twitter)
        (reset! twitter-credentials (twitter-oauth/make-oauth-creds (:app-consumer-key twitter)
                                                                    (:app-consumer-secret twitter)
                                                                    (:user-access-token twitter)
                                                                    (:user-access-secret twitter))))
      (dosync
       (alter irc assoc-in [:callbacks :privmsg] #'dogdog-handler)
       (alter irc assoc :chains history))
      (irclj/identify irc "ord9949") ;;; TODO - config this
      (doseq [channel channels]
        (irclj/join irc channel))
      irc)))

;;(declare irc)

(def cli-options
  ;; An option with a required argument
  [
   ["-c" "--channel CHANNEL" "Join this IRC channel"
    :assoc-fn (fn [m k v] (update-in m [k] conj v))]
    
   ["-n" "--nick NICK" "Join using this nick"
    :default "dogdog"]
    
   ["-t" "--twitter CONFIG" "Load twitter config and tweet 3wps"]
   
   ;; A boolean option defaulting to nil
   ["-h" "--help"]])

(defn -main
  [& args]
  (let [opts (cli/parse-opts args cli-options)]
    (if (or (:help opts)
            (:errors opts))
      (do
        (when (:errors opts)
          (doall (map println (:errors opts))))
        (println (:summary opts)))
      (do
        (reset! cfg (:options opts))
        (let [irc (init)]
          (reset! irc-connection irc))))))
