(ns cli
  (:gen-class)
  (:require [clojure.string :as str]
            [config]
            [platform]
            [play]
            [sim]))

(defn parse-value [v]
  (cond
    (number? v) v
    (string? v)
    (cond
      (re-matches #"^\d+(\.\d+)?$" v)
      (if (re-matches #"\." v)
        (platform/parse-float v)
        (platform/parse-int v))
      (re-matches #"(?i)true|false" v) (platform/parse-bool v)
      :else (keyword v))
    :else v))

(defn parse-args [args]
  (let [arg-map (into {} (map #(let [[k v] (str/split % #"=")] [k (parse-value v)]) args))
        strategies (reduce (fn [acc [k v]]
                             (if-let [[_ player param] (re-matches #"strategy-(A|B)-(.*)" k)]
                               (assoc-in acc [(keyword player) :params (keyword param)] v)
                               acc))
                           {}
                           arg-map)]
    (swap! config/game merge
           {:num-games (or (platform/parse-int (get arg-map "num-games")) (:num-games @config/game))
            :debug? (platform/parse-bool (get arg-map "debug" "false"))
            :show? (platform/parse-bool (get arg-map "show" "false"))
            :parallel (or (platform/parse-int (get arg-map "parallel")) (:parallel @config/game))
            :validate? (platform/parse-bool (get arg-map "validate" "true"))  ; New line for validate flag
            :strategies
            (merge-with
             merge
             (:strategies @config/game)
             {:A (merge (:A strategies)
                        {:name (keyword (get arg-map "strategy-A"))})
              :B (merge (:B strategies)
                        {:name (keyword (get arg-map "strategy-B"))})})})))

(defn print-strategy-info [player]
  (let [{:keys [name params]} (get-in @config/game [:strategies player])]
    (println
     (str "Player " (clojure.core/name player)
          " strategy: " (clojure.core/name name)
          (when (seq params)
            (str " " params))))))

(defn print-config []
  (println "Running" (:num-games @config/game) "games...")
  (print-strategy-info :A)
  (print-strategy-info :B)
  (println "Debug mode:" (:debug? @config/game))
  (println "Show mode:" (:show? @config/game))
  (println "Parallel:" (:parallel @config/game))
  (println "Validation:" (:validate? @config/game)))

(defn -main [& args]
  (parse-args (rest args))
  (if (= (first args) "sim")
    (do
      (print-config)
      (sim/run-and-report))
    (play/ur :minimax 10)))
