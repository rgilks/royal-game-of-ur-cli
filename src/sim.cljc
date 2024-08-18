(ns sim
  (:require #?(:clj [clojure.core.async :as async])
            [config]
            [platform]
            [state]
            [strategy.first-in-list]
            [strategy.mcts]
            [strategy.minimax]
            [strategy.random]
            [strategy.strategic]
            [util :refer [enable-print-line! disable-print-line!]]
            [view]))

(def config-atom
  (atom {:debug? false
         :show? false
         :delay 20
         :num-games 2
         :parallel 8
         :strategies
         {:A {:name :minimax :params {:depth 10}}
          :B {:name :minimax :params {:depth 10}}}}))
        ;;  :strategies {:A {:name :mcts
        ;;                   :params {:iterations 5000
        ;;                            :exploration 1.5}}
        ;;               :B {:name :minimax
        ;;                   :params {:depth 10}}}}))

(defn debug [& args]
  (when (:debug? @config-atom)
    (apply println args)))

(defn print-simulation-results [results]
  (println "\nSimulation Results:")
  (println "Total games:" (:num-games @config-atom))
  (println "Strategy A (Player A):"  (get-in @config-atom [:strategies :A :name]))
  (println "Strategy B (Player B):"  (get-in @config-atom [:strategies :B :name]))
  (println "Player A wins:" (:A results))
  (println "Player B wins:" (:B results))
  (let [win-percentage-a (double (* (/ (:A results)
                                       (:num-games @config-atom)) 100))
        rounded-percentage (Math/round win-percentage-a)]
    (println "Player A win percentage:" (str rounded-percentage "%"))))

(defn handle-choose-action [game possible-moves strategy]
  (if-let [move (state/select-move strategy possible-moves game)]
    (do
      (debug "Player" (:current-player game) "chose move:" (pr-str move))
      (state/choose-action game move))
    (do
      (debug "Player" (:current-player game) "has no valid moves")
      (state/choose-action game nil))))

(defmulti handle :state)

(defmethod handle :start-game [game]
  (debug "Starting new game. First player:" (:current-player game))
  (assoc game :state :roll-dice))

(defmethod handle :roll-dice [game]
  (let [rolled-game (state/dice-roll game)]
    (debug "Player" (:current-player rolled-game) "rolled" (:roll rolled-game))
    rolled-game))

(defmethod handle :choose-action [game]
  (let [possible-moves (state/get-moves game)]
    (debug "Possible moves for" (:current-player game) ":" (pr-str possible-moves))
    (handle-choose-action game possible-moves (get-in game [:strategy :name]))))

(defmethod handle :end-game [game]
  (view/show-winner (:current-player game))
  (debug "Game over. Winner:" (:current-player game))
  (assoc game :game-over true))

(defn play-turn [game]
  (loop [current-game game]
    (let [updated-game (handle current-game)]
      (when (:show? @config-atom)
        (platform/clear-console))
      (view/show-state updated-game)
      (when (:show? @config-atom)
        (platform/sleep (:delay @config-atom)))
      (if (or (:game-over updated-game) (= (:state updated-game) :roll-dice))
        updated-game
        (recur updated-game)))))

(defn play-game
  ([]
   (play-game (state/initialize-game)))
  ([initial-state]
   (if (:show? @config-atom)
     (enable-print-line!)
     (disable-print-line!))
   (loop [game (assoc initial-state
                      :strategy (get-in @config-atom [:strategies (:current-player initial-state)]))]
     (if (:game-over game)
       game
       (recur (-> game
                  play-turn
                  (assoc :strategy (get-in @config-atom [:strategies (:current-player game)]))))))))

(defn run-single-chunk [chunk]
  (reduce (fn [wins _]
            (let [game-result (play-game)
                  winner (:current-player game-result)]
              (update wins winner (fnil inc 0))))
          {:A 0 :B 0}
          chunk))

(defn run-simulation []
  #?(:clj
     (let [num-games (:num-games @config-atom)
           parallel (:parallel @config-atom)
           chunk-size (max 1 (quot num-games parallel))
           chunks (partition-all chunk-size (range num-games))
           results-chan (async/chan)]

       (doseq [chunk chunks]
         (async/go
           (let [chunk-result (run-single-chunk chunk)]
             (async/>! results-chan chunk-result))))

       (loop [results []
              chunks-completed 0]
         (if (= chunks-completed (count chunks))
           (do
             (async/close! results-chan)
             (reduce (fn [total-wins chunk-wins]
                       (merge-with + total-wins chunk-wins))
                     {:A 0 :B 0}
                     results))
           (if-let [chunk-result (async/<!! results-chan)]
             (recur (conj results chunk-result) (inc chunks-completed))
             (do
               (println "Warning: Received nil result, stopping early.")
               (reduce (fn [total-wins chunk-wins]
                         (merge-with + total-wins chunk-wins))
                       {:A 0 :B 0}
                       results)))))))
  #?(:cljs
     (loop [games-left (:num-games @config-atom)
            wins {:A 0 :B 0}]
       (if (zero? games-left)
         wins
         (let [game (state/initialize-game)
               game-result (play-game game)
               winner (:current-player game-result)]
           (when (:show? @config-atom)
             (platform/sleep 1000))
           (recur (dec games-left)
                  (update wins winner inc)))))))

(defn -main []
  (println "Running" (:num-games @config-atom) "games...")
  (println "Player A strategy:" (get-in @config-atom [:strategies :A :name])
           (get-in @config-atom [:strategies :A :params]))
  (println "Player B strategy:" (get-in @config-atom [:strategies :B :name])
           (get-in @config-atom [:strategies :B :params]))
  (println "Debug mode:" (str (:debug? @config-atom)))
  (println "Show mode:" (str (:show? @config-atom)))
  (println "Delay:" (:delay @config-atom))
  (println "Parallel:" (:parallel @config-atom))
  (time (let [results (run-simulation)]
          (print-simulation-results results))))
