(ns play
  (:gen-class)
  (:require [config]
            [engine]
            [platform]
            [strategy.first-in-list]
            [strategy.mcts]
            [strategy.minimax]
            [strategy.random]
            [strategy.strategic]
            [util]
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

(defmulti handle :state)

(defmethod handle :roll-dice [game]
  (engine/roll game))

(defn get-move [player possible-moves game]
  (cond
    (empty? possible-moves) nil
    (= player :A) (get-user-move possible-moves)
    :else (engine/select-move (get-in game [:strategy :name]) game)))

(defmethod handle :choose-action [game]
  (let [possible-moves (engine/get-moves game)
        player (:current-player game)]
    (if-let [move (get-move player possible-moves game)]
      (do
        (when (= player :B)
          (view/show-ai-move move)
          (platform/sleep (:long-wait @config/game)))
        (engine/choose-action game move))
      (do
        (view/show-no-moves)
        (platform/sleep (:long-wait @config/game))
        (engine/choose-action game nil)))))

(defmethod handle :end-game [game]
  (view/show-winner (:current-player game))
  (throw (ex-info "Game over" {:reason :expected}))
  game)

(defn ur []
  (platform/clear-console)
  (util/hide-cursor)
  (view/show-welcome)
  (try
    (let [input (platform/read-single-key)]
      (when (= input "q")
        (throw (ex-info "User quit" {:reason :expected}))))
    (loop [game (-> (engine/start-game)
                    (assoc-in [:strategy :name] (get-in @config/game [:strategies :B :name]))
                    (assoc-in [:strategy :params] (get-in @config/game [:strategies :B :params])))]
      #_(when (:debug @config/game)
          (println "Current game state:" game))
      (when (not (:debug @config/game))
        (platform/clear-console))
      (view/show-state game)
      (platform/sleep (:short-wait @config/game))
      (recur (handle game)))
    (catch #?(:clj Throwable :cljs :default) e
      (when-not (= :expected (:reason (ex-data e)))
        (throw e)))
    (finally
      (do
        (view/show-goodbye)
        (util/show-cursor)))))