(ns cli
  (:gen-class)
  (:require [clojure.string :as str]
            [config]
            [platform]
            [play]
            [sim]))

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

(defn -main [& args]
  (parse-args (rest args))
  (if (= (first args) "sim")
    (do
      (print-config)
      (sim/run-and-report))
    (play/ur :minimax 10)))
