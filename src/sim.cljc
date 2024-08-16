(ns sim
  (:require [clojure.string :as str]
            [config]
            [malli.core :as m]
            [malli.error :as me]
            [schema]
            [state-machine :as game]
            [util :refer [log]]))

(defn render-cell [board idx]
  (cond
    (contains? (:exclude config/board) idx) " "
    :else
    (let [cell (get board idx)]
      (cond
        (= cell :A) "1"
        (= cell :B) "2"
        (contains? (:rosettes config/board) idx) "✸"
        (nil? cell) "-"
        :else "?"))))

(defn print-board [board]
  (let [render-cell (partial render-cell board)
        row-to-string (fn [start end]
                        (str/join " " (map render-cell (range start end))))]
    (log (row-to-string 0 8))
    (log (row-to-string 8 16))
    (log (row-to-string 16 24))))

(defn print-game-state [state]
  (let [player (-> state :current-player name)
        event (case (:state state)
                :choose-action (str "rolls " (:roll state))
                :switch-turns ""
                :land-on-rosette "landed on rosette"
                :move-piece-off-board "moved piece off board"
                nil)]
    (when event
      (when-not (str/blank? event)
        (log player event))
      (when (= (:state state) :switch-turns)
        (print-board  (:board state))
        (log (mapv #(if (< % 10)
                      (str "0" %)
                      (str %)) (range 0 24)))
        (log (mapv #(if (nil? %) "  "  %) (:board state)))
        (log "")))))

(defn play-sim [game-state rolls inputs]
  (loop [state game-state
         remaining-rolls rolls]
    (assert (m/validate schema/game-state state)
            (str "Invalid game state: "
                 (me/humanize (m/explain schema/game-state state))))

    (print-game-state state)

    (let [[new-state new-rolls] (game/transition state remaining-rolls inputs)]
      (if (or (= (:state new-state) :end-game)
              (and (= (:state new-state) :roll-dice) (empty? new-rolls)))
        [new-state new-rolls]
        (recur new-state new-rolls)))))

(defn play [rolls]
  (loop [game-state (game/initialize-game)
         remaining-rolls rolls]
    (if (or (= (:state game-state) :end-game) (empty? remaining-rolls))
      game-state
      (let [[new-state new-rolls] (play-sim game-state remaining-rolls {})]
        (recur new-state new-rolls)))))

(defn rolls []
  (reduce + (repeatedly 4 #(rand-int 2))))

(defn play
  ([rolls]
   (loop [game-state (game/initialize-game)
          remaining-rolls rolls]
     (if (or (= (:state game-state) :end-game) (empty? remaining-rolls))
       game-state
       (let [[new-state new-rolls] (play-sim game-state remaining-rolls {})]
         (recur new-state new-rolls)))))
  ([] (play (repeatedly rolls))))

(defn -main []
  (play))

(comment
  (play))