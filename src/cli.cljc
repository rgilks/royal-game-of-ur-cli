(ns cli
  (:require [config]
            [platform]
            [state]
            [util]
            [view]))

(def long-wait 1000)
(def short-wait 300)

;; (def long-wait 0)
;; (def short-wait 0)

(defn get-user-move [possible-moves]
  (if (empty? possible-moves)
    nil
    (do
      (view/show-moves possible-moves)
      (loop []
        (let [input (platform/read-single-key)]
          (if (= input "q")
            :quit
            (let [choice (platform/parse-int input)]
              (if (and (pos? choice) (<= choice (count possible-moves)))
                (nth possible-moves (dec choice))
                (do
                  (view/show-invalid-choice (count possible-moves))
                  (recur))))))))))

(defmulti handle :state)

(defmethod handle :roll-dice [game]
  (state/dice-roll game))

(defmethod handle :choose-action [game]
  (let [possible-moves (state/get-moves game)]
    (if (empty? possible-moves)
      (do
        (view/show-no-moves)
        (platform/sleep long-wait)
        (state/choose-action game nil))
      (let [move (if (= (:current-player game) :A)
                   (get-user-move possible-moves)
                   (state/select-move :strategic possible-moves))]
        (if (= move :quit)
          (throw (ex-info "User quit" {:reason :expected}))
          (let [new-game (state/choose-action game move)]
            (when (= (:current-player game) :B)
              (view/show-ai-move move)
              (platform/sleep long-wait))
            new-game))))))

(defmethod handle :end-game [game]
  (view/show-winner (:current-player game))
  (throw (ex-info "Game over" {:reason :expected}))
  game)

(defn play-game []
  (platform/clear-console)
  (util/hide-cursor)
  (view/show-welcome)
  (platform/readln)
  (try
    (loop [game (state/start-game)]
      (platform/clear-console)
      (view/show-state game)
      (platform/sleep short-wait)
      (recur (handle game)))
    (catch platform/err e
      (when-not (= {:reason :expected} (ex-data e))
        (throw e)))
    (finally
      (util/show-cursor))))

(defn -main []
  (play-game)
  (view/show-goodbye))