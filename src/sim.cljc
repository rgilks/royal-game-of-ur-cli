(ns sim
  (:require [config]
            [platform]
            [state]
            [util :refer [enable-print-line! disable-print-line!]]
            [view]))

(def short-delay 100)

(defn handle-choose-action [game possible-moves strategy]
  (if-let [move (state/select-move strategy possible-moves)]
    (do
      (view/show-ai-move move)
      (state/choose-action game move))
    (state/choose-action game nil)))

(defmulti handle :state)

(defmethod handle :start-game [game]
  (assoc game :state :roll-dice))

(defmethod handle :roll-dice [game]
  (state/dice-roll game))

(defmethod handle :choose-action [game]
  (let [possible-moves (state/get-moves game)]
    (handle-choose-action game possible-moves (:strategy game))))

(defmethod handle :end-game [game]
  (view/show-winner (:current-player game))
  (assoc game :game-over true))

(defn play-turn [game]
  (loop [current-game game]
    (let [updated-game (handle current-game)]
      (view/show-state updated-game)
      (when (:print-output? updated-game)
        (platform/sleep short-delay))
      (if (or (:game-over updated-game) (= (:state updated-game) :roll-dice))
        updated-game
        (recur updated-game)))))

(defn play-game
  ([strategy-a strategy-b print-output?]
   (play-game (state/start-game) strategy-a strategy-b print-output?))
  ([initial-state strategy-a strategy-b print-output?]
   (if print-output?
     (enable-print-line!)
     (disable-print-line!))
   (loop [game (assoc initial-state
                      :strategy strategy-a
                      :print-output? print-output?)]
     (if (:game-over game)
       game
       (recur (-> game
                  play-turn
                  (assoc :strategy (if (= (:current-player game) :A)
                                     strategy-a
                                     strategy-b))))))))

(defn run-simulation [num-games strategy-a strategy-b print-output?]
  (loop [games-left num-games
         wins {:A 0 :B 0}]
    (if (zero? games-left)
      wins
      (let [game-result (play-game strategy-a strategy-b print-output?)
            winner (:current-player game-result)]
        (recur (dec games-left)
               (update wins winner inc))))))

(defn print-simulation-results [results num-games strategy-a strategy-b]
  (println "\nSimulation Results:")
  (println "Total games:" num-games)
  (println "Strategy A (Player A):" strategy-a)
  (println "Strategy B (Player B):" strategy-b)
  (println "Player A wins:" (:A results))
  (println "Player B wins:" (:B results))
  (let [win-percentage-a (double (* (/ (:A results) num-games) 100))
        rounded-percentage (Math/round win-percentage-a)]
    (println "Player A win percentage:" (str rounded-percentage "%"))))

(defn parse-long [s]
  #?(:clj (Long/parseLong s)
     :cljs (js/parseInt s)))

(defn parse-bool [s]
  (= s "true"))

(defn -main [& args]
  (let [num-games (or (some-> args first parse-long) 1)
        strategy-a (or (second args) :first-in-list)
        strategy-b (or (nth args 2) :strategic)
        print-output? (parse-bool (or (nth args 3 "false") "false"))]
    (println "Running" num-games "games...")
    (println "Player A strategy:" strategy-a)
    (println "Player B strategy:" strategy-b)
    (println "Print output:" print-output?)
    (let [results (run-simulation num-games strategy-a strategy-b print-output?)]
      (print-simulation-results results num-games strategy-a strategy-b))))

(comment
  (play-game :strategic :strategic true)

  ;; Run a simulation of 1000 games with different strategies and print output
  (-main "1000" "first-in-list" "strategic" "true")

  ;; Run a simulation of 1000 games with different strategies without print output
  (-main "1000" "first-in-list" "strategic" "false"))
