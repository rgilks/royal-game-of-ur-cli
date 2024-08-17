(ns cli
  (:gen-class)
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

(defmulti handle :state)

(defmethod handle :roll-dice [game]
  (state/dice-roll game))

(defn get-move [player possible-moves game]
  (cond
    (empty? possible-moves) nil
    (= player :A) (get-user-move possible-moves)
    :else (state/select-move :strategic possible-moves game)))

(defmethod handle :choose-action [game]
  (let [possible-moves (state/get-moves game)
        player (:current-player game)]
    (if-let [move (get-move player possible-moves game)]
      (do
        (when (= player :B)
          (view/show-ai-move move)
          (platform/sleep long-wait))
        (state/choose-action game move))
      (do
        (view/show-no-moves)
        (platform/sleep long-wait)
        (state/choose-action game nil)))))

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
    (catch #?(:clj Throwable :cljs :default) e
      (when-not (= :expected (:reason (ex-data e)))
        (throw e)))
    (finally
      (util/show-cursor))))

(defn -main []
  (play-game)
  (view/show-goodbye))