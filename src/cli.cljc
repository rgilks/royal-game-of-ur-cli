(ns cli
  (:gen-class)
  (:require [config]
            [game]
            [platform]
            [sim :as sim]
            [strategy.first-in-list]
            [strategy.minimax]
            [strategy.random]
            [strategy.strategic]
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
  (game/roll game))

(defn get-move [player possible-moves game]
  (cond
    (empty? possible-moves) nil
    (= player :A) (get-user-move possible-moves)
    :else (game/select-move (get-in game [:strategy :name]) game)))

(defmethod handle :choose-action [game]
  (let [possible-moves (game/get-moves game)
        player (:current-player game)]
    (if-let [move (get-move player possible-moves game)]
      (do
        (when (= player :B)
          (view/show-ai-move move)
          (platform/sleep long-wait))
        (game/choose-action game move))
      (do
        (view/show-no-moves)
        (platform/sleep long-wait)
        (game/choose-action game nil)))))

(defmethod handle :end-game [game]
  (view/show-winner (:current-player game))
  (throw (ex-info "Game over" {:reason :expected}))
  game)

(defn play-game [ai-strategy ai-depth]
  (platform/clear-console)
  (util/hide-cursor)
  (view/show-welcome)
  (platform/readln)
  (try
    (loop [game (-> (game/start-game)
                    (assoc-in [:strategy :name] ai-strategy)
                    (assoc-in [:strategy :params :depth] ai-depth))]
      (platform/clear-console)
      (view/show-state game)
      (platform/sleep short-wait)
      (recur (handle game)))
    (catch #?(:clj Throwable :cljs :default) e
      (when-not (= :expected (:reason (ex-data e)))
        (throw e)))
    (finally
      (util/show-cursor))))

(defn run-simulation [args]
  (sim/parse-args args)
  (sim/print-config)
  (sim/run-simulation-and-report))

(defn -main [& args]
  (if (= (first args) "simulate")
    (run-simulation (rest args))
    (do
      (play-game :minimax 10)
      (view/show-goodbye))))
