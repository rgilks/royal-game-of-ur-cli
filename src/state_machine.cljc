(ns state-machine
  (:require [clojure.string :as str]
            [malli.core :as m]
            [malli.error :as me]
            [util :refer [log]]))

(def board-config
  {:size 24
   :rosettes #{0 6 11 16 22}
   :paths {:A [3 2 1 0 8 9 10 11 12 13 14 15 7 6]
           :B [19 18 17 16 8 9 10 11 12 13 14 15 23 22]}
   :exclude #{4 5 20 21}})

(def states
  #{:start-game
    :roll-dice
    :choose-action
    :enter-piece
    :move-piece
    :land-on-rosette
    :move-piece-off-board
    :switch-turns
    :end-game})

(def player-schema
  [:map
   [:in-hand [:int {:min 0 :max 7}]]
   [:off-board [:int {:min 0 :max 7}]]])

(def board-position [:int {:min 0 :max 23}])

(def game-state-schema
  [:map
   [:board [:vector {:min 24 :max 24} [:maybe [:enum :A :B]]]]
   [:players [:map-of [:enum :A :B] player-schema]]
   [:current-player [:enum :A :B]]
   [:roll [:maybe [:int {:min 0 :max 4}]]]
   [:state (into [:enum] states)]
   [:selected-move
    [:maybe
     [:map
      [:from [:or board-position [:enum :entry]]]
      [:to [:or board-position [:enum :off-board]]]
      [:captured [:maybe [:enum :A :B]]]]]]])

(defn validate-total-pieces [state]
  (every? (fn [[player-key player-data]]
            (let [{:keys [in-hand off-board]} player-data
                  on-board (count (filter #{player-key} (:board state)))]
              (= 7 (+ in-hand off-board on-board))))
          (:players state)))

(defn validate-game-state [state]
  (if-let [error (m/explain game-state-schema state)]
    (throw (ex-info "Invalid game state structure" (me/humanize error)))
    (if-not (validate-total-pieces state)
      (throw (ex-info "Invalid total pieces"
                      {:error "Total pieces for each player must be exactly 7"}))
      state)))

;; Helper functions
(defn other-player [player]
  (if (= player :A) :B :A))

(defn get-piece-positions [board player]
  (keep-indexed (fn [idx piece]
                  (when (and (= piece player) (not (contains? (:exclude board-config) idx))) idx))
                board))

(defn move-piece [board player from roll]
  (if (zero? roll)
    nil  ; No move possible on a roll of 0
    (let [path (get-in board-config [:paths player])
          current-index (if (= from :entry) -1 (.indexOf path from))
          new-index (+ current-index roll)]
      (if (>= new-index (count path))
        [:off-board nil]
        (let [new-pos (nth path new-index)
              target (get board new-pos)]
          (cond
            (nil? target) [new-pos nil]
            (= target player) nil  ; Invalid move
            (and (contains? (:rosettes board-config) new-pos)
                 (not= target player)) nil  ; Can't land on opponent's rosette
            :else [new-pos target]))))))  ; Capture opponent's piece

(defn update-board [board player from to]
  (cond-> board
    (and (not= from :entry) (number? from) (< from (:size board-config))) (assoc from nil)
    (and (not= to :off-board) (number? to) (< to (:size board-config))) (assoc to player)))

(defn get-possible-moves [game-state]
  (let [{:keys [board current-player roll players]} game-state]
    (if (zero? roll)
      []  ; No moves possible on a roll of 0
      (let [player-positions (get-piece-positions board current-player)
            entry-point (first (get-in board-config [:paths current-player]))
            can-enter? (and (pos? (get-in players [current-player :in-hand]))
                            (nil? (get board entry-point)))]
        (concat
         (when can-enter?
           (when-let [move (move-piece board current-player :entry roll)]
             [{:from :entry :to (first move) :captured (second move)}]))
         (for [from player-positions
               :let [move (move-piece board current-player from roll)]
               :when move]
           {:from from :to (first move) :captured (second move)}))))))

(defn game-over? [game-state]
  (let [off-board-pieces
        (get-in game-state [:players (:current-player game-state) :off-board])]
    (>= off-board-pieces 7)))

(defmulti transition (fn [game-state _rolls _inputs] (:state game-state)))

(defmethod transition :start-game [game-state rolls]
  (log "Starting game")
  [(assoc game-state :state :roll-dice) rolls])

(defmethod transition :roll-dice [game-state [current-roll & remaining-rolls]]
  [(-> game-state
       (assoc :roll current-roll)
       (assoc :state :choose-action))
   remaining-rolls])

(defmulti select-move (fn [strategy _] strategy))

(defmethod select-move :random [_ possible-moves]
  (rand-nth possible-moves))

(defmethod select-move :first-in-list [_ possible-moves]
  (first possible-moves))

(defmethod select-move :default [_ possible-moves]
  (first possible-moves))

(defmethod transition :choose-action
  [game-state rolls inputs]
  (let [possible-moves (get-possible-moves game-state)
        strategy (get inputs :move-strategy :default)]
    (if (empty? possible-moves)
      [(assoc game-state :state :switch-turns) rolls]
      (let [selected-move (or (some-> (:selected-move inputs))
                              (select-move strategy possible-moves))]
        [(-> game-state
             (assoc :selected-move selected-move)
             (assoc :state (if (= (:from selected-move) :entry)
                             :enter-piece
                             :move-piece)))
         rolls]))))

(defmethod transition :enter-piece [game-state rolls]
  (let [{:keys [current-player selected-move]} game-state
        {:keys [to]} selected-move]
    [(-> game-state
         (update :board update-board current-player :entry to)
         (update-in [:players current-player :in-hand] dec)
         (assoc :state :switch-turns))
     rolls]))

(defmethod transition :move-piece [game-state rolls]
  (let [{:keys [current-player selected-move]} game-state
        {:keys [from to captured]} selected-move
        opponent (other-player (:current-player game-state))]
    [(cond-> game-state
       true (update :board update-board current-player from to)
       (= to :off-board) (-> (update-in [:players current-player :off-board] inc)
                             (assoc :state :move-piece-off-board))
       (and (not= to :off-board) (contains? (:rosettes board-config) to)) (assoc :state :land-on-rosette)
       captured (-> (update-in [:players opponent :in-hand] inc)
                    (assoc :state :switch-turns))
       (and (not= to :off-board) (not (contains? (:rosettes board-config) to)) (not captured)) (assoc :state :switch-turns))
     rolls]))

(defmethod transition :land-on-rosette [game-state rolls]
  [(assoc game-state :state :roll-dice) rolls])

(defmethod transition :move-piece-off-board [game-state rolls]
  [(assoc game-state :state :switch-turns) rolls])

(defmethod transition :switch-turns [game-state rolls]
  (if (game-over? game-state)
    [(assoc game-state :state :end-game) rolls]
    (let [new-player (other-player (:current-player game-state))]
      [(-> game-state
           (assoc :current-player new-player)
           (assoc :roll nil)
           (assoc :selected-move nil)
           (assoc :state :roll-dice))
       rolls])))

(defmethod transition :end-game [game-state rolls]
  [game-state rolls]) ; No transition, game is over

(defn render-cell [board idx]
  (cond
    (contains? (:exclude board-config) idx) " "
    :else
    (let [cell (get board idx)]
      (cond
        (= cell :A) "1"
        (= cell :B) "2"
        (contains? (:rosettes board-config) idx) "✸"
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

(defn initialize-game []
  {:board (vec (repeat (:size board-config) nil))
   :players {:A {:in-hand 7 :off-board 0}
             :B {:in-hand 7 :off-board 0}}
   :current-player :A
   :roll nil
   :state :start-game
   :selected-move nil})

(defn play [game-state rolls inputs]
  (loop [state (validate-game-state game-state)
         remaining-rolls rolls]
    (let [[new-state new-rolls] (transition state remaining-rolls inputs)]
      (if (contains? #{:choose-action :roll-dice :end-game} (:state new-state))
        [(validate-game-state new-state) new-rolls]
        (recur (validate-game-state new-state) new-rolls)))))

;; Public API
(defn start-game []
  (first (play (initialize-game) [] {})))

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
        (first (play game-state [] {:move-strategy :default}))  ; Switch turns if no moves are possible
        (let [new-state (-> game-state
                            (assoc :selected-move selected-move)
                            (assoc :state (if (= (:from selected-move) :entry)
                                            :enter-piece
                                            :move-piece)))]
          (first (play new-state [] {})))))
    (throw (ex-info "Invalid game state for choosing action" {:state (:state game-state)}))))

;; Simulation
(defn play-sim [game-state rolls inputs]
  (loop [state game-state
         remaining-rolls rolls]
    (assert (m/validate game-state-schema state)
            (str "Invalid game state: "
                 (me/humanize (m/explain game-state-schema state))))

    (print-game-state state)

    (let [[new-state new-rolls] (transition state remaining-rolls inputs)]
      (if (or (= (:state new-state) :end-game)
              (and (= (:state new-state) :roll-dice) (empty? new-rolls)))
        [new-state new-rolls]
        (recur new-state new-rolls)))))

(defn play-game [rolls]
  (loop [game-state (initialize-game)
         remaining-rolls rolls]
    (if (or (= (:state game-state) :end-game) (empty? remaining-rolls))
      game-state
      (let [[new-state new-rolls] (play-sim game-state remaining-rolls {})]
        (recur new-state new-rolls)))))

;; Run the game
(comment
  (let [roll-dice (reduce + (repeatedly 4 #(rand-int 2)))]
    (time (play-game (repeatedly roll-dice))))

  (def game (start-game))
  (def game-rolled (dice-roll game))
  (def possible-moves (get-moves game-rolled))
  (def game-after-move (choose-action game-rolled (first possible-moves)))
  (def game-rolled-again (dice-roll game-after-move))
  (def new-possible-moves (get-moves game-rolled-again))
  (log new-possible-moves)
  (def game-after-second-move (choose-action game-rolled-again
                                             (first new-possible-moves)))
  (log  game-after-second-move))