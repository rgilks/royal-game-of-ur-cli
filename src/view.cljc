(ns view
  (:require [clojure.string :as str]
            [config]
            [util :refer [cstr show]]))

(def fancy-symbols
  {:A (cstr :red " ●")
   :B (cstr :yellow " ●")
   :rosette (cstr :cyan " ✧")
   :empty (cstr :cyan " ·")
   :blank "  "
   :arrow " → "
   :dice-filled "▲"
   :dice-empty "△"
   :border "│"
   :top-border "┌──────────────────┐"
   :bottom-border "└──────────────────┘"})

(def simple-symbols
  {:A (cstr :red " O")
   :B (cstr :yellow " O")
   :rosette (cstr :cyan " +")
   :empty (cstr :cyan " -")
   :blank "  "
   :arrow " > "
   :dice-filled "1"
   :dice-empty "0"
   :border " "
   :top-border " ------------------ "
   :bottom-border " ------------------ "})

(defn get-symbols []
  (if (= (:view-symbols @config/game) :fancy)
    fancy-symbols
    simple-symbols))

(defn cell [board idx]
  (let [symbols (get-symbols)]
    (cond
      (get board idx) (symbols (get board idx))
      ((:exclude config/board) idx) (:blank symbols)
      ((:rosettes config/board) idx) (:rosette symbols)
      :else (:empty symbols))))

(defn board-row [board start end label]
  (let [symbols (get-symbols)]
    (cstr (cstr :cyan label " ")
          (apply cstr (map #(cell board %)
                           (range start end))))))

(defn show-board [board]
  (let [symbols (get-symbols)]
    (show)
    (show :cyan "    1 2 3 4 5 6 7 8")
    (show :cyan (:top-border symbols))
    (doseq [[start end label] [[0 8 "A"] [8 16 "B"] [16 24 "C"]]]
      (show :cyan (:border symbols)
            (board-row board start end label)
            (:border symbols)))
    (show :cyan (:bottom-border symbols))))

(defn show-roll [roll]
  (let [symbols (get-symbols)]
    (->> (concat (repeat roll (:dice-filled symbols))
                 (repeat (- 4 roll) (:dice-empty symbols)))
         shuffle
         (map #(cstr :bold %))
         str/join)))

(defn player-stats [color player-data]
  (let [symbols (get-symbols)]
    (cstr color
          " " (:in-hand player-data)
          (:arrow symbols)
          (:off-board player-data))))

(defn show-state [{:keys [board players roll]}]
  (show-board board)
  (show " " (player-stats :red (:A players))
        (if roll
          (cstr " " (show-roll roll))
          "     ")
        (player-stats :yellow (:B players)))
  (show))

(def coords
  (into {} (for [i (range 24)]
             [i (cstr (nth "ABC" (quot i 8)) (inc (mod i 8)))])))

(defn format-move [{:keys [from to captured]}]
  (let [symbols (get-symbols)]
    (cstr (if (= from :entry) "entry" (coords from))
          (:arrow symbols)
          (if (= to :off-board) "off" (coords to))
          (when captured (cstr :red " capture")))))

(defn show-moves [moves]
  (doseq [[idx move] (map-indexed vector moves)]
    (show :red " "  (inc idx) " " (format-move move))))

(defn show-winner [winner]
  (show)
  (show :red "GAME OVER")
  (show :green (if (= winner :A) "You win!" "AI wins!")))

(defn show-welcome []
  (let [symbols (get-symbols)]
    (show :red "The Royal Game of Ur")
    (show)
    (show (:A symbols) " Your pieces")
    (show (:B symbols) " AI pieces")
    (show)
    (show "Press 'q' to quit at any time.")
    (show "Press Enter to begin!")))

(defn show-no-moves []
  (show "  No moves"))

(defn show-goodbye []
  (show "Thanks for playing! Goodbye.\n"))

(defn show-invalid-choice [max-choice]
  (show :red "Invalid choice. Enter 1-" max-choice " or 'q' to quit"))

(defn show-ai-move [move]
  (show :yellow "  AI: " (format-move move)))
