(ns sim
  (:require #?(:clj [clojure.core.async :as async])
            [clojure.string :as str]
            [config]
            [engine]
            [platform]
            [strategy.first-in-list]
            [strategy.mcts]
            [strategy.minimax]
            [strategy.random]
            [strategy.strategic]
            [util :refer [disable-print-line! enable-print-line! debug]]
            [validate]
            [view]))

(defn handle-choose-action [game strategy]
  (if-let [move (engine/select-move strategy game)]
    (do
      (debug "Player" (:current-player game) "chose move:" (pr-str move))
      (engine/choose-action game move))
    (do
      (debug "Player" (:current-player game) "has no valid moves")
      (engine/choose-action game nil))))

(defmulti handle :state)

(defmethod handle :start-game [game]
  (debug "Starting new game. First player:" (:current-player game))
  (assoc game :state :roll-dice))

(defmethod handle :roll-dice [game]
  (let [rolled-game (engine/roll game)]
    (debug "Player" (:current-player rolled-game) "rolled" (:roll rolled-game))
    rolled-game))

(defmethod handle :choose-action [game]
  (let [possible-moves (engine/get-moves game)]
    (debug "Possible moves for" (:current-player game) ":" (pr-str possible-moves))
    (handle-choose-action game (get-in game [:strategy :name]))))

(defmethod handle :end-game [game]
  (view/show-winner (:current-player game))
  (debug "Game over. Winner:" (:current-player game))
  (assoc game :game-over true))

(defn play-turn [game]
  (let [show (:show @config/game)]
    (loop [current-game game]
      (let [updated-game (handle current-game)]
        (when show
          (platform/clear-console))
        (view/show-state updated-game)
        (when show
          (platform/sleep (:delay-time @config/game)))
        (if (or (:game-over updated-game) (= (:state updated-game) :roll-dice))
          updated-game
          (recur updated-game))))))

(defn play-game
  ([]
   (play-game (engine/init)))
  ([initial-state]
   (if (:show @config/game)
     (enable-print-line!)
     (disable-print-line!))
   (loop [game (assoc
                initial-state
                :strategy (get-in @config/game
                                  [:strategies (:current-player initial-state)]))
          move-count 0]
     (when (:debug @config/game)
       (println "Current game state:" game))
     (if (:game-over game)
       (assoc game :move-count move-count)
       (recur
        (-> game
            (play-turn)
            (assoc :strategy (get-in @config/game [:strategies (:current-player game)])))
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
     (let [num-games (:num-games @config/game)
           parallel (:parallel @config/game)
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
     (let [num-games (:num-games @config/game)]
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
    (println "Strategy A (Player A):" (get-in @config/game [:strategies :A :name]))
    (println "Strategy B (Player B):" (get-in @config/game [:strategies :B :name]))
    (println "Player A wins:" a-wins)
    (println "Player B wins:" b-wins)
    (print-metric "Player A win percentage" win-percentage-a 2)
    (print-metric "Average moves per game" avg-moves-per-game 0)
    (print-metric "Elapsed time" elapsed-time 2)))

(defn run-and-report []
  (let [start-time #?(:clj (System/nanoTime) :cljs (js/Date.now))
        results (run-simulation)
        end-time #?(:clj (System/nanoTime) :cljs (js/Date.now))
        elapsed-time #?(:clj (/ (- end-time start-time) 1e9)
                        :cljs (/ (- end-time start-time) 1000))]
    (print-simulation-results results elapsed-time)))

(comment
  (swap! config/game merge
         {:debug false
          :show false
          :num-games 10000
          :parallel 6
          :validate false
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
  (run-and-report))
