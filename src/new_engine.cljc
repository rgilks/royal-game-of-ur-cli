(ns new-engine
  (:require [clojure.pprint :refer [cl-format]]
            [clojure.string :as str]))

;; Constants
(def ^:const BOARD-MASK 0xFFFFFFFFFF00000000) ; Bits 24-63 for board (20 positions * 2 bits each)
(def ^:const A-OFF-MASK 0x7)         ; Bits 0-2
(def ^:const B-OFF-MASK 0x38)        ; Bits 3-5
(def ^:const A-COMPLETE-MASK 0x1C0)  ; Bits 6-8
(def ^:const B-COMPLETE-MASK 0xE00)  ; Bits 9-11
(def ^:const PLAYER-MASK 0x1000)     ; Bit 12
(def ^:const DICE-MASK 0xE000)       ; Bits 13-15
(def ^:const EXTRA-TURN-MASK 0x10000) ; Bit 16

(def ROSETTES #{0 4 8 14 18})
(def PATHS {:A [3 2 1 0 8 9 10 11 12 13 14 15 6 7]
            :B [19 18 17 16 8 9 10 11 12 13 14 15 22 23]})

;; Helper functions
(defn binary-str [n]
  (cl-format nil "2r~64,'0',B" n))

(defn get-position [state pos]
  (bit-and 3 (bit-shift-right state (+ 24 (* 2 pos)))))

(defn set-position [state pos value]
  (let [shift (+ 24 (* 2 pos))
        clear-mask (bit-not (bit-shift-left 3 shift))]
    (bit-or (bit-and state clear-mask)
            (bit-shift-left (bit-and value 3) shift))))

(defn get-off-board [state player]
  (bit-and 7 (bit-shift-right state (if (= player :A) 0 3))))

(defn set-off-board [state player value]
  (let [mask (if (= player :A) A-OFF-MASK B-OFF-MASK)
        shift (if (= player :A) 0 3)]
    (bit-or (bit-and state (bit-not mask))
            (bit-shift-left (bit-and value 7) shift))))

(defn get-completed [state player]
  (bit-and 7 (bit-shift-right state (if (= player :A) 6 9))))

(defn set-completed [state player value]
  (let [mask (if (= player :A) A-COMPLETE-MASK B-COMPLETE-MASK)
        shift (if (= player :A) 6 9)]
    (bit-or (bit-and state (bit-not mask))
            (bit-shift-left (bit-and value 7) shift))))

(defn get-current-player [state]
  (if (zero? (bit-and state PLAYER-MASK)) :A :B))

(defn set-current-player [state player]
  (if (= player :A)
    (bit-and state (bit-not PLAYER-MASK))
    (bit-or state PLAYER-MASK)))

(defn get-dice-roll [state]
  (bit-and 7 (bit-shift-right state 13)))

(defn set-dice-roll [state roll]
  (bit-or (bit-and state (bit-not DICE-MASK))
          (bit-shift-left (bit-and roll 7) 13)))

(defn get-extra-turn [state]
  (not (zero? (bit-and state EXTRA-TURN-MASK))))

(defn set-extra-turn [state flag]
  (if flag
    (bit-or state EXTRA-TURN-MASK)
    (bit-and state (bit-not EXTRA-TURN-MASK))))

;; Game logic functions
(defn initial-state []
  (-> 0
      (set-off-board :A 7)
      (set-off-board :B 7)
      (set-current-player :A)))

(defn roll-dice []
  (apply + (repeatedly 4 #(rand-int 2))))

(defn move-piece [state from to player]
  (println "Moving piece for player" player "from" from "to" to)
  (println "Initial state:" (binary-str state))
  (let [opponent (if (= player :A) :B :A)
        player-value (if (= player :A) 1 2)]
    (if (= to :off-board)
      (let [new-state (-> state
                          (set-position from 0)
                          (set-completed player (inc (get-completed state player))))]
        (println "Moving off board. New state:" (binary-str new-state))
        new-state)
      (let [captured-piece (get-position state to)
            new-state (-> state
                          (set-position from 0)
                          (set-position to player-value))]
        (println "Moved piece. Intermediate state:" (binary-str new-state))
        (cond-> new-state
          (and (not= captured-piece 0) (not= captured-piece player-value) (not= to 11))
          (as-> s
                (do
                  (println "Capturing piece at" to)
                  (let [updated-state (set-off-board s opponent (inc (get-off-board s opponent)))]
                    (println "After capture. State:" (binary-str updated-state))
                    updated-state)))

          (ROSETTES to)
          (as-> s
                (do
                  (println "Landing on rosette at" to)
                  (let [updated-state (set-extra-turn s true)]
                    (println "After setting extra turn. State:" (binary-str updated-state))
                    updated-state))))))))

(defn apply-move [state from to player]
  (println "Applying move for player" player "from" from "to" to)
  (let [new-state (move-piece state from to player)]
    (if (get-extra-turn new-state)
      (do
        (println "Extra turn granted")
        (-> new-state
            (set-dice-roll 0)
            (set-current-player player)))
      (do
        (println "No extra turn, switching player")
        (-> new-state
            (set-current-player (if (= player :A) :B :A))
            (set-dice-roll 0)
            (set-extra-turn false))))))

(defn get-possible-moves [state]
  (let [player (get-current-player state)
        roll (get-dice-roll state)
        player-bit (if (= player :A) 1 2)
        path (PATHS player)
        on-board-pieces (filter #(= (get-position state %) player-bit) (range 20))
        off-board-pieces (get-off-board state player)]
    (when (pos? roll)
      (concat
       ;; Moves for pieces on the board
       (for [from on-board-pieces
             :let [from-index (.indexOf path from)
                   to-index (+ from-index roll)]
             :when (< to-index (count path))
             :let [to (nth path to-index)]
             :when (or (zero? (get-position state to))
                       (= to 11) ; safe square
                       (and (not= to 11)
                            (not= (get-position state to) player-bit)))]
         {:from from :to (if (= to-index (dec (count path))) :off-board to)})
       ;; Move for pieces off the board
       (when (and (pos? off-board-pieces)
                  (<= roll (count path))
                  (zero? (get-position state (nth path (dec roll)))))
         [{:from :off-board :to (nth path (dec roll))}])))))

(defn game-over? [state]
  (or (= (get-completed state :A) 7)
      (= (get-completed state :B) 7)))

(defn winner [state]
  (cond
    (= (get-completed state :A) 7) :A
    (= (get-completed state :B) 7) :B
    :else nil))

;; Utility functions for display and debugging
(defn board-to-string [state]
  (let [symbols {0 "·" 1 "A" 2 "B" 3 "R"}]
    (str/join "\n"
              (for [row [0 8 16]]
                (str/join " " (map #(if (ROSETTES %)
                                      (str (symbols (get-position state %)) "*")
                                      (symbols (get-position state %)))
                                   (range row (+ row 8))))))))

(defn print-game-state [state]
  (println (board-to-string state))
  (println "A off-board:" (get-off-board state :A)
           "completed:" (get-completed state :A))
  (println "B off-board:" (get-off-board state :B)
           "completed:" (get-completed state :B))
  (println "Current player:" (get-current-player state))
  (println "Dice roll:" (get-dice-roll state))
  (println "Extra turn:" (get-extra-turn state)))

(comment
  (set-off-board 0 :A 7)

  (get-off-board 0 :A)
  (get-off-board 0 :B)
  (get-completed 0 :A)
  (get-completed 0 :B)

  (set-dice-roll 0 3)

  (long DICE-MASK)

  (get-off-board 1970324836974592 :A)
  (get-off-board 1970324836974592 :B)
  (set-off-board 1970324836974592 :B 7)

  (binary-str 1970324836974592)

  (get-off-board 1970324836974592 :A)
  (get-off-board 1970324836974592 :B)

  (binary-str 17732923532771328)

  (get-completed 17732923532771328 :A)
  (set-completed 17732923532771328 :A 4)
  (get-completed 89790517570699264 :A))
