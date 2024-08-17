(ns sim
  (:require [config]
            [platform]
            [state]
            [util :refer [enable-print-line! disable-print-line!]]
            [view]))

(def config-atom
  (atom {:debug? false
         :show? false
         :delay 10
         :num-games 10
         :strategy-a :random
         :strategy-b :random}))

(defn debug [& args]
  (when (:debug? @config-atom)
    (apply println args)))

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
    (handle-choose-action game possible-moves (:strategy game))))

(defmethod handle :end-game [game]
  (view/show-winner (:current-player game))
  (debug "Game over. Winner:" (:current-player game))
  (assoc game :game-over true))

(defn play-turn [game]
  (loop [current-game game]
    (let [updated-game (handle current-game)]
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
                      :strategy (if (= (:current-player initial-state) :A)
                                  (:strategy-a @config-atom)
                                  (:strategy-b @config-atom)))]
     (if (:game-over game)
       game
       (recur (-> game
                  play-turn
                  (assoc :strategy (if (= (:current-player game) :A)
                                     (:strategy-a @config-atom)
                                     (:strategy-b @config-atom)))))))))

(defn run-simulation []
  (loop [games-left (:num-games @config-atom)
         wins {:A 0 :B 0}]
    (if (zero? games-left)
      wins
      (let [game (state/initialize-game)
            game-result (play-game game)
            winner (:current-player game-result)]
        (recur (dec games-left)
               (update wins winner inc))))))

(defn print-simulation-results [results]
  (println "\nSimulation Results:")
  (println "Total games:" (:num-games @config-atom))
  (println "Strategy A (Player A):"  (:strategy-a @config-atom))
  (println "Strategy B (Player B):"  (:strategy-b @config-atom))
  (println "Player A wins:" (:A results))
  (println "Player B wins:" (:B results))
  (let [win-percentage-a (double (* (/ (:A results)
                                       (:num-games @config-atom)) 100))
        rounded-percentage (Math/round win-percentage-a)]
    (println "Player A win percentage:" (str rounded-percentage "%"))))

(defn -main [& args]
  (let [num-games (or (some-> args first parse-long) (:num-games @config-atom))
        strategy-a (or (second args) (:strategy-a @config-atom))
        strategy-b (or (nth args 2) (:strategy-b @config-atom))
        debug? (platform/parse-bool (nth args 3 (str (:debug? @config-atom))))
        show? (platform/parse-bool (nth args 4 (str (:show? @config-atom))))
        delay (or (some-> args (nth 5 nil) parse-long) (:delay @config-atom))]
    (swap! config-atom assoc
           :debug? debug?
           :show? show?
           :delay delay
           :num-games num-games
           :strategy-a strategy-a
           :strategy-b strategy-b)
    (println "Running" num-games "games...")
    (println "Player A strategy:" strategy-a)
    (println "Player B strategy:" strategy-b)
    (println "Debug mode:" debug?)
    (println "Show mode:" show?)
    (println "Delay:" delay)
    (let [results (run-simulation)]
      (print-simulation-results results))))
