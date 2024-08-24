(ns run
  (:require [game]
            [platform]
            [util :refer [hide-cursor show-cursor]]
            [view]))

(defn get-user-move [possible-moves]
  (when (seq possible-moves)
    (view/show-moves possible-moves)
    (loop []
      (let [input (platform/read-single-key)]
        (case input
          "q" (throw (ex-info "User quit" {:reason :expected}))
          (let [choice (platform/parse-int input)]
            (if (and (pos? choice) (<= choice (count possible-moves)))
              (nth possible-moves (dec choice))
              (do
                (view/show-invalid-choice (count possible-moves))
                (recur)))))))))

(defn play-turn [game]
  (platform/clear-console)
  (view/show-state game)
  (platform/sleep 300)
  (case (:state game)
    :roll-dice (engine/roll game)
    :choose-action (let [possible-moves (engine/get-moves game)
                         player (:current-player game)]
                     (if-let [move (if (= player :A)
                                     (get-user-move possible-moves)
                                     (engine/select-move (:strategy game) game))]
                       (do
                         (when (= player :B)
                           (view/show-ai-move move)
                           (platform/sleep 1000))
                         (engine/choose-action game move))
                       (do
                         (view/show-no-moves)
                         (platform/sleep 1000)
                         (engine/choose-action game nil))))
    :end-game (do
                (view/show-winner (:current-player game))
                (throw (ex-info "Game over" {:reason :expected})))
    game))

(defn play-game []
  (platform/clear-console)
  (hide-cursor)
  (view/show-welcome)
  (platform/readln)
  (try
    (loop [game (engine/start)]
      (recur (play-turn game)))
    (catch #?(:clj Throwable :cljs :default) e
      (when-not (= :expected (:reason (ex-data e)))
        (throw e)))
    (finally
      (show-cursor)
      (view/show-goodbye))))
