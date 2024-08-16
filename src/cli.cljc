(ns cli
  (:require [clojure.string :as str]
            [config]
            [platform]
            [state]
            [util :refer [log]]
            [view]))

(defn get-user-move [possible-moves]
  (when (seq possible-moves)
    (view/print-moves possible-moves)
    (let [choice (platform/parse-int (str/trim (platform/readln)))]
      (if (and (pos? choice) (<= choice (count possible-moves)))
        (nth possible-moves (dec choice))
        (do
          (log (view/cs (str "Invalid choice. Enter 1-" (count possible-moves)) :red))
          (platform/readln)
          (recur possible-moves))))))

(defn play-game []
  (platform/clear-console)
  (view/print-welcome-message)
  (platform/readln)
  (loop [state (state/start-game)]
    (platform/clear-console)
    (case (:state state)
      :roll-dice
      (let [new-state (state/dice-roll state)]
        (view/print-game-state new-state)
        (platform/sleep 1500)
        (recur new-state))

      :choose-action
      (let [possible-moves (state/get-moves state)]
        (view/print-game-state state)
        (if (empty? possible-moves)
          (do
            (log (str (if (= (:current-player state) :A) "You have" "AI has") " no possible moves"))
            (platform/sleep 1500)
            (recur (state/choose-action state nil)))
          (let [selected-move (if (= (:current-player state) :A)
                                (get-user-move possible-moves)
                                (state/select-move :random possible-moves))]
            (when (= (:current-player state) :B)
              (log (str "AI's move: " (view/cs (view/format-move selected-move) :yellow)))
              (platform/sleep 1500))
            (recur (state/choose-action state selected-move)))))

      :end-game
      (do
        (view/print-winner-message (:current-player state))
        (view/print-game-state state)))))

(defn -main []
  (play-game))
