(ns state
  (:require [clojure.pprint :as pprint]
            [config]
            [malli.core :as m]
            [malli.error :as me]
            [schema]))

(defn validate-total-pieces [state]
  (every? (fn [[player-key player-data]]
            (let [{:keys [in-hand off-board]} player-data
                  on-board (count (filter #{player-key} (:board state)))]
              (= 7 (+ in-hand off-board on-board))))
          (:players state)))

(defn validate-game-state [state]
  (if-let [error (m/explain schema/game-state state)]
    (do
      (println "Invalid game state:")
      (pprint/pprint state)
      (println "Validation error:")
      (pprint/pprint error)
      (throw (ex-info "Invalid game state structure" (me/humanize error))))
    (if-not (validate-total-pieces state)
      (throw (ex-info "Invalid total pieces"
                      {:error "Total pieces for each player must be exactly 7"}))
      state)))

;; Helper functions
(defn other-player [player]
  (if (= player :A) :B :A))

(defn get-piece-positions [board player]
  (keep-indexed #(when (= %2 player) %1) board))

(defn find-index [path value]
  (loop [idx 0
         remaining path]
    (cond
      (empty? remaining) nil
      (= (first remaining) value) idx
      :else (recur (inc idx) (rest remaining)))))

(defn find-next-position [path from roll]
  (let [start-index (or (find-index path from) -1)
        remaining-path (drop (inc start-index) path)]
    (nth remaining-path (dec roll) nil)))

(defn move-piece [board player from roll]
  (if (zero? roll)
    nil  ; No move possible on a roll of 0
    (let [path (get-in config/board [:paths player])
          last-square (last path)
          new-pos (if (= from :entry)
                    (first path)
                    (find-next-position path from roll))]
      (cond
        ;; If the new position is beyond the last square, move off board
        (and (number? from) (or (nil? new-pos) (> (+ (find-index path from) roll) (find-index path last-square))))
        [:off-board nil]

        (nil? new-pos)
        nil

        :else
        (let [target (get board new-pos)]
          (cond
            (nil? target) [new-pos nil]
            (= target player) nil  ; Invalid move
            (and (contains? (:rosettes config/board) new-pos)
                 (not= target player)) nil  ; Can't land on opponent's rosette
            :else [new-pos target]))))))  ; Capture opponent's piece

(defn update-board [board player from to]
  (cond-> board
    (and (not= from :entry) (number? from)
         (< from (:size config/board))) (assoc from nil)
    (and (not= to :off-board) (number? to)
         (< to (:size config/board))) (assoc to player)))

(defn get-possible-moves [{:keys [board current-player roll players]}]
  (when (pos? roll)
    (let [player-positions (get-piece-positions board current-player)
          path (get-in config/board [:paths current-player])
          entry-point (first path)
          can-enter? (and (pos? (get-in players [current-player :in-hand]))
                          (nil? (get board entry-point)))

          entry-move (when can-enter?
                       (when-let [move (move-piece board current-player :entry roll)]
                         [{:from :entry :to (first move) :captured (second move)}]))

          board-moves (for [from player-positions
                            :let [move (move-piece board current-player from roll)]
                            :when move]
                        {:from from :to (first move) :captured (second move)})

          all-moves (concat entry-move board-moves)]

      (sort-by (fn [move]
                 (cond
                   (= (:from move) :entry) -1
                   (= (:to move) :off-board) (count path)
                   :else (or (find-index path (:from move))
                             (inc (apply max (map #(find-index path %) path))))))
               all-moves))))

(defn game-over? [game-state]
  (let [off-board-pieces
        (get-in game-state [:players (:current-player game-state) :off-board])]
    (>= off-board-pieces 7)))

(defmulti transition (fn [game-state _rolls _inputs] (:state game-state)))

(defmethod transition :start-game [game-state rolls _inputs]
  [(assoc game-state :state :roll-dice) rolls])

(defmethod transition :roll-dice
  [game-state [current-roll & remaining-rolls] _inputs]
  [(-> game-state
       (assoc :roll current-roll)
       (assoc :state :choose-action))
   remaining-rolls])

(defmulti select-move (fn [strategy _ _] strategy))

(defmethod transition :choose-action
  [game-state rolls inputs]
  (let [possible-moves (get-possible-moves game-state)
        strategy (get inputs :move-strategy :random)]
    (if (empty? possible-moves)
      [(assoc game-state :state :switch-turns) rolls]
      (let [selected-move (or (some-> (:selected-move inputs))
                              (select-move strategy possible-moves game-state))]
        [(-> game-state
             (assoc :selected-move selected-move)
             (assoc :state (if (= (:from selected-move) :entry)
                             :enter-piece
                             :move-piece)))
         rolls]))))

(defmethod transition :enter-piece [game-state rolls _inputs]
  (let [{:keys [current-player selected-move]} game-state
        {:keys [to]} selected-move]
    [(-> game-state
         (update :board update-board current-player :entry to)
         (update-in [:players current-player :in-hand] dec)
         (assoc :state :switch-turns))
     rolls]))

(defmethod transition :move-piece [game-state rolls _inputs]
  (let [{:keys [current-player selected-move]} game-state
        {:keys [from to captured]} selected-move
        opponent (other-player (:current-player game-state))]
    [(cond-> game-state
       true (update :board update-board current-player from to)
       (= to :off-board) (-> (update-in [:players current-player :off-board] inc)
                             (assoc :state :move-piece-off-board))
       (and (not= to :off-board) (contains? (:rosettes config/board) to)) (assoc :state :land-on-rosette)
       captured (-> (update-in [:players opponent :in-hand] inc)
                    (assoc :state :switch-turns))
       (and (not= to :off-board) (not (contains? (:rosettes config/board) to)) (not captured)) (assoc :state :switch-turns))
     rolls]))

(defmethod transition :land-on-rosette [game-state rolls _inputs]
  [(assoc game-state :state :roll-dice) rolls])

(defmethod transition :move-piece-off-board [game-state rolls _inputs]
  [(assoc game-state :state :switch-turns) rolls])

(defmethod transition :switch-turns [game-state rolls _inputs]
  (if (game-over? game-state)
    [(assoc game-state :state :end-game) rolls]
    (let [new-player (other-player (:current-player game-state))]
      [(-> game-state
           (assoc :current-player new-player)
           (assoc :roll nil)
           (assoc :selected-move nil)
           (assoc :state :roll-dice))
       rolls])))

(defmethod transition :end-game [game-state rolls _inputs]
  [game-state rolls]) ; No transition, game is over

(defn random-first-player []
  (if (zero? (rand-int 2)) :A :B))

(defn initialize-game
  ([] (initialize-game nil))
  ([starting-player]
   {:board (vec (repeat (:size config/board) nil))
    :players {:A {:in-hand 7 :off-board 0}
              :B {:in-hand 7 :off-board 0}}
    :current-player (or starting-player (random-first-player))
    :roll nil
    :state :start-game
    :selected-move nil}))

(defn play [game-state rolls inputs]
  (loop [state (validate-game-state game-state)
         remaining-rolls rolls]
    (let [[new-state new-rolls] (transition state remaining-rolls inputs)]
      (if (contains? #{:choose-action :roll-dice :end-game} (:state new-state))
        [(validate-game-state new-state) new-rolls]
        (recur (validate-game-state new-state) new-rolls)))))

;; Public API
(defn start-game
  ([] (start-game nil))
  ([starting-player]
   (first (play (initialize-game starting-player) [] {}))))

(defn dice-roll [game-state]
  (if (= (:state game-state) :roll-dice)
    (let [roll (reduce + (repeatedly 4 #(rand-int 2)))]
      (first (play game-state [roll] {})))
    (throw (ex-info "Invalid game state for rolling dice" {:state (:state game-state)}))))

(defn get-moves [game-state]
  (if (= (:state game-state) :choose-action)
    (get-possible-moves game-state)
    (throw (ex-info "Invalid game state for getting possible moves" {:state (:state game-state)}))))

(defn choose-action [game-state selected-move]
  (if (= (:state game-state) :choose-action)
    (let [possible-moves (get-possible-moves game-state)]
      (if (empty? possible-moves)
        (first (play game-state [] {:move-strategy :random}))
        (let [new-state (-> game-state
                            (assoc :selected-move selected-move)
                            (assoc :state (if (= (:from selected-move) :entry)
                                            :enter-piece
                                            :move-piece)))]
          (first (play new-state [] {})))))
    (throw (ex-info "Invalid game state for choosing action"
                    {:state (:state game-state)}))))
