(ns new-engine
  (:require [clojure.pprint :refer [cl-format]]
            [clojure.string :as str]
            [util]))

;; Constants
(def ^:const AOB-MASK 0x7)
(def ^:const BOB-MASK 0x38)
(def ^:const AC-MASK 0x1C0)
(def ^:const BC-MASK 0xE00)
(def ^:const PLAYER-MASK 0x1000)
(def ^:const DICE-MASK 0xE000)
(def ^:const BOARD-MASK 0xFFFFFFFFFFFF0000)
(def ^:const EXTRA-TURN-MASK 0x3000000)

(def ROSETTES #{0 6 11 16 22})
(def PATHS {:A [3 2 1 0 8 9 10 11 12 13 14 15 7 6]
            :B [19 18 17 16 8 9 10 11 12 13 14 15 23 22]})
(def EXCLUDED-POSITIONS #{4 5 20 21})

(defn binary-str [n]
  (cl-format nil "2r~64,'0',B" n))

(defn pad-left [s n]
  (let [padding (apply str (repeat (max 0 (- n (count s))) " "))]
    (str padding s)))

(defn print-state [state]
  (let [binary (binary-str state)
        labels ["23" "22" "--" "--" "19" "18" "17" "16" "15" "14" "13" "12" "11" "10" "9" "8" "7" "6"
                "--" "ET" "3" "2" "1" "0" "ROL" "C" "BC" "AC" "BOB" "AOB"]
        values (concat (reverse (take 48 binary))
                       [(subs binary 48 51)]  ; ROL
                       [(subs binary 51 52)]  ; C
                       [(subs binary 52 55)]  ; BC
                       [(subs binary 55 58)]  ; AC
                       [(subs binary 58 61)]  ; BOB
                       [(subs binary 61 64)]) ; AOB
        label-line (str/join " "  labels)
        value-line (str/join " " values)]
    (println label-line)
    (println value-line)))

(defn get-position [state pos]
  (when-not (EXCLUDED-POSITIONS pos)
    (bit-and 3 (bit-shift-right state (+ 16 (* 2 pos))))))

(defn set-position [state pos value]
  (let [shift (+ 16 (* 2 pos))
        clear-mask (bit-not (bit-shift-left 3 shift))]
    (bit-or (bit-and state clear-mask)
            (bit-shift-left (bit-and value 3) shift))))

(defn get-off-board [state player]
  (bit-and 7 (bit-shift-right state (if (= player :A) 0 3))))

(defn set-off-board [state player value]
  (let [mask (if (= player :A) AOB-MASK BOB-MASK)
        shift (if (= player :A) 0 3)]
    (bit-or (bit-and state (bit-not mask))
            (bit-shift-left (bit-and value 7) shift))))

(defn get-completed [state player]
  (bit-and 7 (bit-shift-right state (if (= player :A) 6 9))))

(defn set-completed [state player value]
  (let [mask (if (= player :A) AC-MASK BC-MASK)
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

(defn get-board-positions [state]
  (let [board-array (vec (repeat 24 nil))]
    (reduce
     (fn [board pos]
       (if (EXCLUDED-POSITIONS pos)
         board
         (let [piece (get-position state pos)]
           (case piece
             1 (assoc board pos :A)
             2 (assoc board pos :B)
             board))))
     board-array
     (range 24))))

;; Game logic functions
(defn initial-state []
  (-> 0
      (set-off-board :A 7)
      (set-off-board :B 7)
      (set-current-player :A)))

(defn roll-dice []
  (apply + (repeatedly 4 #(rand-int 2))))

(defn move-piece [state from to player]
  (let [opponent (if (= player :A) :B :A)
        player-value (if (= player :A) 1 2)]
    (if (= to :off-board)
      (-> state
          (set-position from 0)
          (set-completed player (inc (get-completed state player))))
      (let [captured-piece (get-position state to)
            new-state (-> state
                          (set-position from 0)
                          (set-position to player-value))]
        (cond-> new-state
          (and (not= captured-piece 0) (not= captured-piece player-value) (not= to 11))
          (set-off-board opponent (inc (get-off-board new-state opponent)))

          (ROSETTES to)
          (set-extra-turn true))))))

(defn apply-move [state from to player]
  (let [new-state (move-piece state from to player)]
    (if (get-extra-turn new-state)
      (-> new-state
          (set-dice-roll 0)
          (set-current-player player))
      (-> new-state
          (set-current-player (if (= player :A) :B :A))
          (set-dice-roll 0)
          (set-extra-turn false)))))

(defn get-possible-moves [state]
  (let [player (get-current-player state)
        roll (get-dice-roll state)
        player-bit (if (= player :A) 1 2)
        path (PATHS player)
        on-board-pieces (filter #(= (get-position state %) player-bit) (range 24))
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

(defn print-game-state [state]
  (println "Board:")
  (let [board (get-board-positions state)]
    (doseq [row (partition 8 board)]
      (println (str/join " " (map #(or % ".") row)))))
  (println "A off-board:" (get-off-board state :A)
           "completed:" (get-completed state :A))
  (println "B off-board:" (get-off-board state :B)
           "completed:" (get-completed state :B))
  (println "Current player:" (get-current-player state))
  (println "Dice roll:" (get-dice-roll state))
  (println "Extra turn:" (get-extra-turn state)))

(comment
  (require 'view)
  (let [state (-> (initial-state)
                  (set-position 3 1)
                  (set-position 8 2))]

    (util/enable-print-line!)
    (println "POS:" (get-position state 8))
    (println (print-state state))
    (println (get-board-positions state))
    (view/show-board (get-board-positions state))))
