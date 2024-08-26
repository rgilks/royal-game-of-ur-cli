(ns cli
  (:gen-class
   :methods [^:static [handler [java.util.Map java.util.Map] java.util.Map]])
  (:require [clojure.string :as str]
            [config]
            [engine]
            [platform]
            [play]
            [sim]
            [util :refer [debug]]))

(defn parse-arg [arg]
  (let [[k v] (str/split arg #"=")]
    [(keyword k) (platform/parse-value v)]))

(defn parse-strategy-args [args]
  (reduce
   (fn [acc [k v]]
     (if-let [[_ player param] (re-matches #"strategy-(A|B)-(.+)" (name k))]
       (assoc-in acc [(keyword player) :params (keyword param)] v)
       (if-let [[_ player] (re-matches #"strategy-(A|B)" (name k))]
         (assoc-in acc [(keyword player) :name] (keyword v))
         acc)))
   {}
   args))

(defn parse-args [args]
  (let [parsed-args (into {} (map parse-arg args))
        strategies (parse-strategy-args parsed-args)]
    (swap! config/game merge parsed-args)
    (swap! config/game update :strategies merge strategies)))

(defn print-config []
  (println "Running" (:num-games @config/game) "games...")
  (doseq [player [:A :B]]
    (let [{:keys [name params]} (get-in @config/game [:strategies player])]
      (println (str "Player " player " strategy: " (or name "not set")
                    (when params (str " " params))))))
  (doseq [k [:debug :show :parallel :validate]]
    (println (str (name k) ": " (get @config/game k)))))

(defn process-game-state [game-state]
  (debug "Processing game state:" game-state)
  (let [strategy-name (get-in game-state [:strategy :name] :random)
        strategy-params (get-in game-state [:strategy :params] {})
        move (engine/select-move strategy-name (assoc game-state :strategy {:name strategy-name :params strategy-params}))]
    (debug "Selected move:" move)
    (if move
      (-> game-state
          (engine/choose-action move)
          (engine/roll))
      (do
        (debug "No valid moves available. Rolling dice.")
        (engine/roll game-state)))))

(defn -handler [event _context]
  (try
    (debug "Received event:" event)
    (let [game-state (engine/init)
          ;; _ (debug "Initial game state:" game-state)
          ;; updated-state (process-game-state game-state)
          ;; _ (debug "Updated game state:" updated-state)
          response-body (platform/json-stringify game-state)]
      {:statusCode 200
       :headers {"Content-Type" "application/json"}
       :body response-body})
    (catch #?(:clj Throwable :cljs :default) e
      (debug "Error occurred:" e)
      {:statusCode 500
       :headers {"Content-Type" "application/json"}
       :body (platform/json-stringify {:error (str e)})})))

(defn simulate-lambda-invocation [input]
  (let [event {:body (platform/json-stringify input)}
        _ (debug "Simulating event:" event)
        context {}]
    (-handler event context)))

#?(:clj
   (defn lambda-execution-loop []
     (let [runtime-api (System/getenv "AWS_LAMBDA_RUNTIME_API")]
       (try
         (loop []
           (let [next-invocation-url (str "http://" runtime-api "/2018-06-01/runtime/invocation/next")
                 response (platform/http-request next-invocation-url "GET" nil)
                 _ (debug "Received response:" (pr-str response))
                 request-id (get-in response [:headers "lambda-runtime-aws-request-id"])
                 _ (debug "Request ID:" request-id)
                 result (-handler (platform/json-parse (:body response)) {})
                 _ (debug "Result:" result)
                 result-url (str "http://" runtime-api "/2018-06-01/runtime/invocation/" request-id "/response")]
             (platform/http-request result-url "POST" result)
             (recur)))
         (catch Exception e
           (let [error-url (str "http://" runtime-api "/2018-06-01/runtime/init/error")]
             (platform/http-request error-url "POST" {:errorMessage (str e)
                                                      :errorType (str (.getClass e))})))))))

(defn -main [& args]
  (parse-args (rest args))
  #?(:clj
     (cond
       (platform/get-env "AWS_LAMBDA_RUNTIME_API")
       (lambda-execution-loop)

       (= (first args) "lambda-test")
       (println (simulate-lambda-invocation {:game-state nil}))

       (= (first args) "sim")
       (do
         (print-config)
         (sim/run-and-report))

       :else
       (play/ur))

     :cljs
     (cond
       (= (first args) "sim")
       (do
         (print-config)
         (sim/run-and-report))

       :else
       (play/ur))))