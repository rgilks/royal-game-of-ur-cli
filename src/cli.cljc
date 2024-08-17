(ns cli
  (:require [config]
            [platform]
            [state]
            [view]))

(defn get-user-move [possible-moves]
  (when (seq possible-moves)
    (view/print-moves possible-moves)
    (loop []
      (let [input (platform/read-single-key)]
        (if (= input "q")
          :quit
          (let [choice (platform/parse-int input)]
            (if (and (pos? choice) (<= choice (count possible-moves)))
              (nth possible-moves (dec choice))
              (do
                (view/print-invalid-choice (count possible-moves))
                (recur)))))))))

(defn hide-cursor []
  (print "\u001b[?25l")
  (flush))

(defn show-cursor []
  (print "\u001b[?25h")
  (flush))

(defn play-game []
  (platform/clear-console)
  (hide-cursor)
  (view/print-welcome-message)
  (platform/readln)
  (try
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
              (view/print-no-moves)
              (platform/sleep 1500)
              (recur (state/choose-action state nil)))
            (let [selected-move (if (= (:current-player state) :A)
                                  (get-user-move possible-moves)
                                  (state/select-move :strategic possible-moves))]
              (if (= selected-move :quit)
                (do
                  (view/print-goodbye-message)
                  nil)
                (do
                  (when (= (:current-player state) :B)
                    (view/print-ai-move selected-move)
                    (platform/sleep 1500))
                  (recur (state/choose-action state selected-move)))))))

        :end-game
        (do
          (view/print-winner-message (:current-player state))
          (view/print-game-state state)
          nil)))
    (finally
      (show-cursor))))

(defn -main []
  (play-game))
