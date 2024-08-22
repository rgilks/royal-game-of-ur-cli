(ns sim
  (:require #?(:clj [clojure.core.async :as async])
            [clojure.string :as str]
            [config]
            [game]
            [platform]
            [strategy.first-in-list]
            [strategy.mcts]
            [strategy.minimax]
            [strategy.random]
            [strategy.strategic]
            [util :refer [disable-print-line! enable-print-line!]]
            [validate]
            [view]))

(def delay-time 30)

(def config-atom
  (atom {:debug? false
         :show? false
         :num-games 10
         :parallel 8
         :validate? false  ; New key for validation flag
        ;;  :strategies
        ;;  {:A {:name :minimax :params {:depth 4}}
        ;;   :B {:name :minimax :params {:depth 4}}}}))
         :strategies {:A {:name :mcts
                          :params {:iterations 100
                                   :exploration 1.41}}
                      :B {:name :first-in-list}
                      ;; :B {:name :mcts
                      ;;     :params {:iterations 10
                      ;;              :exploration 1.41}}
                      ;; :B {:name :minimax
                      ;;     :params {:depth 4}}
                      }}))

(defn debug [& args]
  (when (:debug? @config-atom)
    (apply println args)))

(defn handle-choose-action [game strategy]
  (if-let [move (game/select-move strategy game)]
    (do
      (debug "Player" (:current-player game) "chose move:" (pr-str move))
      (game/choose-action game move))
    (do
      (debug "Player" (:current-player game) "has no valid moves")
      (game/choose-action game nil))))

(defmulti handle :state)

(defmethod handle :start-game [game]
  (debug "Starting new game. First player:" (:current-player game))
  (assoc game :state :roll-dice))

(defmethod handle :roll-dice [game]
  (let [rolled-game (game/roll game)]
    (debug "Player" (:current-player rolled-game) "rolled" (:roll rolled-game))
    rolled-game))

(defmethod handle :choose-action [game]
  (let [possible-moves (game/get-moves game)]
    (debug "Possible moves for" (:current-player game) ":" (pr-str possible-moves))
    (handle-choose-action game (get-in game [:strategy :name]))))

(defmethod handle :end-game [game]
  (view/show-winner (:current-player game))
  (debug "Game over. Winner:" (:current-player game))
  (assoc game :game-over true))

(defn play-turn [game]
  (let [show (:show? @config-atom)]
    (loop [current-game game]
      (let [updated-game (handle current-game)]
        (when show
          (platform/clear-console))
        (view/show-state updated-game)
        (when show
          (platform/sleep delay-time))
        (if (or (:game-over updated-game) (= (:state updated-game) :roll-dice))
          updated-game
          (recur updated-game))))))

(defn play-game
  ([]
   (play-game (game/init)))
  ([initial-state]
   (if (:show? @config-atom)
     (enable-print-line!)
     (disable-print-line!))
   (loop [game (assoc
                initial-state
                :strategy (get-in @config-atom
                                  [:strategies (:current-player initial-state)]))
          move-count 0]
     (if (:game-over game)
       (assoc game :move-count move-count)
       (recur
        (-> game
            (play-turn)
            (assoc :strategy (get-in @config-atom [:strategies (:current-player game)])))
        (inc move-count))))))

(defn run-single-chunk [chunk]
  (reduce (fn [acc _]
            (let [game-result (play-game)
                  winner (:current-player game-result)
                  moves (:move-count game-result 0)]
              (-> acc
                  (update-in [:wins winner] (fnil inc 0))
                  (update :total-moves + moves))))
          {:wins {} :total-moves 0}
          chunk))

(defn run-simulation []
  #?(:clj
     (let [num-games (:num-games @config-atom)
           parallel (:parallel @config-atom)
           chunk-size (max 1 (quot num-games parallel))
           chunks (partition-all chunk-size (range num-games))
           results-chan (async/chan)]

       (doseq [chunk chunks]
         (async/thread
           (let [chunk-result (run-single-chunk chunk)]
             (async/>!! results-chan chunk-result))))

       (loop [results []
              chunks-completed 0]
         (if (= chunks-completed (count chunks))
           (do
             (async/close! results-chan)
             (let [combined-results (reduce (fn [total-results chunk-results]
                                              (-> total-results
                                                  (update-in [:wins :A] (fnil + 0) (get-in chunk-results [:wins :A] 0))
                                                  (update-in [:wins :B] (fnil + 0) (get-in chunk-results [:wins :B] 0))
                                                  (update :total-moves + (:total-moves chunk-results))))
                                            {:wins {} :total-moves 0}
                                            results)]
               (assoc combined-results :num-games num-games)))
           (if-let [chunk-result (async/<!! results-chan)]
             (recur (conj results chunk-result) (inc chunks-completed))
             (do
               (println "Warning: Received nil result, stopping early.")
               (reduce (fn [total-results chunk-results]
                         (merge-with + total-results chunk-results))
                       {:wins {} :total-moves 0 :num-games (reduce + (map count results))}
                       results))))))
     :cljs
     (let [num-games (:num-games @config-atom)]
       (loop [games-left num-games
              wins {:A 0 :B 0}
              total-moves 0]
         (if (zero? games-left)
           {:wins wins :total-moves total-moves :num-games num-games}
           (let [game-result (play-game)
                 winner (:current-player game-result)
                 moves (:move-count game-result 0)]
             (recur (dec games-left)
                    (update wins winner (fnil inc 0))
                    (+ total-moves moves))))))))

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
    (swap! config-atom merge
           {:num-games (or (platform/parse-int (get arg-map "num-games")) (:num-games @config-atom))
            :debug? (platform/parse-bool (get arg-map "debug" "false"))
            :show? (platform/parse-bool (get arg-map "show" "false"))
            :parallel (or (platform/parse-int (get arg-map "parallel")) (:parallel @config-atom))
            :validate? (platform/parse-bool (get arg-map "validate" "true"))  ; New line for validate flag
            :strategies (merge-with merge
                                    (:strategies @config-atom)
                                    {:A (merge (:A strategies)
                                               {:name (keyword (get arg-map "strategy-A"))})
                                     :B (merge (:B strategies)
                                               {:name (keyword (get arg-map "strategy-B"))})})})))

(defn print-strategy-info [player]
  (let [{:keys [name params]} (get-in @config-atom [:strategies player])]
    (println
     (str "Player " (clojure.core/name player)
          " strategy: " (clojure.core/name name)
          (when (seq params)
            (str " " params))))))

(defn format-number [n decimals]
  (let [factor (Math/pow 10 decimals)
        rounded (/ (Math/round (* n factor)) factor)
        [whole frac] (str/split (str rounded) #"\.")
        frac (or frac "")
        frac-formatted (apply str (take decimals (concat frac (repeat "0"))))]
    (if (pos? decimals)
      (str whole "." frac-formatted)
      whole)))

(defn print-simulation-results [results elapsed-time]
  (let [{:keys [wins total-moves num-games]} results
        a-wins (:A wins 0)
        b-wins (:B wins 0)
        win-percentage-a (* (/ a-wins num-games) 100)
        avg-moves-per-game (/ total-moves num-games)
        print-metric (fn [label value decimals]
                       (println (str label ": " (format-number value decimals))))]
    (println "\nSimulation Results:")
    (println "Total games:" num-games)
    (println "Strategy A (Player A):" (get-in @config-atom [:strategies :A :name]))
    (println "Strategy B (Player B):" (get-in @config-atom [:strategies :B :name]))
    (println "Player A wins:" a-wins)
    (println "Player B wins:" b-wins)
    (print-metric "Player A win percentage" win-percentage-a 2)
    (print-metric "Average moves per game" avg-moves-per-game 0)
    (print-metric "Elapsed time" elapsed-time 2)))

(defn run-simulation-and-report []
  (let [start-time #?(:clj (System/nanoTime) :cljs (js/Date.now))
        results (run-simulation)
        end-time #?(:clj (System/nanoTime) :cljs (js/Date.now))
        elapsed-time #?(:clj (/ (- end-time start-time) 1e9)
                        :cljs (/ (- end-time start-time) 1000))]
    (print-simulation-results results elapsed-time)))

(defn print-config []
  (println "Running" (:num-games @config-atom) "games...")
  (print-strategy-info :A)
  (print-strategy-info :B)
  (println "Debug mode:" (:debug? @config-atom))
  (println "Show mode:" (:show? @config-atom))
  (println "Parallel:" (:parallel @config-atom))
  (println "Validation:" (:validate? @config-atom)))

(defn -main [& args]
  (parse-args args)
  (print-config)
  (run-simulation-and-report))

(comment
  (swap! config-atom merge
         {:debug? false
          :show? false
          :num-games 10000
          :parallel 6
          :validate? false
          :strategies
          {:A {:name :minimax :params {:depth 4}}
           :B {:name :minimax :params {:depth 4}}}})
          ;; :strategies {:A {:name :mcts
          ;;                  :params {:iterations 100
          ;;                           :exploration 1.41}}
          ;;              :B {:name :first-in-list}
          ;;           ;; :B {:name :mcts
          ;;           ;;     :params {:iterations 10
          ;;           ;;              :exploration 1.41}}
          ;;           ;; :B {:name :minimax
          ;;           ;;     :params {:depth 4}}
          ;;              }})
  (print-config)
  (run-simulation-and-report))
