(ns view
  (:require [clojure.string :as str]
            [config]
            [util]))

(def colors
  {:reset "[0m" :bold "[1m" :red "[31m" :green "[32m"
   :yellow "[33m" :blue "[34m" :cyan "[36m"})

(defn c-str [color & texts]
  (if (keyword? color)
    (str "\u001b" (colors color) (apply str texts) "\u001b" (:reset colors))
    (apply str color texts)))

(defn show [& args]
  (util/show (apply c-str args)))

(def symbols
  {:A (c-str :red " ●")
   :B (c-str :yellow " ●")
   :rosette (c-str :cyan " ✧")
   :empty (c-str :cyan " ·")
   :blank "  "})

(defn cell [board idx]
  (cond
    (get board idx) (symbols (get board idx))
    ((:exclude config/board) idx) (:blank symbols)
    ((:rosettes config/board) idx) (:rosette symbols)
    :else (:empty symbols)))

(defn board-row [board start end label]
  (str (c-str :cyan label " ") (apply str (map #(cell board %) (range start end)))))

(defn show-board [board]
  (show "")
  (show :cyan "    1 2 3 4 5 6 7 8")
  (show :cyan "┌──────────────────┐")
  (doseq [[start end label] [[0 8 "A"] [8 16 "B"] [16 24 "C"]]]
    (show :cyan "│" (board-row board start end label) "│"))
  (show :cyan "└──────────────────┘"))

(defn show-roll [roll]
  (->> (concat (repeat roll "▲") (repeat (- 4 roll) "△"))
       shuffle
       (map #(c-str :bold %))
       str/join))

(defn player-stats [color player-data]
  (str (c-str color
              " " (:in-hand player-data)
              " → "
              (:off-board player-data))))

(defn show-state [{:keys [board players roll]}]
  (show-board board)
  (show (player-stats :red (:A players))
        (when roll (str " " (show-roll roll)))
        (player-stats :yellow (:B players)))
  (show))

(def coords
  (into {} (for [i (range 24)]
             [i (str (nth "ABC" (quot i 8)) (inc (mod i 8)))])))

(defn format-move [{:keys [from to captured]}]
  (str (if (= from :entry) "entry" (coords from))
       " → "
       (if (= to :off-board) "off" (coords to))
       (when captured (c-str :red " capture"))))

(defn show-moves [moves]
  (doseq [[idx move] (map-indexed vector moves)]
    (show :red (inc idx) " " (format-move move))))

(defn show-winner [winner]
  (show)
  (show :red "GAME OVER")
  (show :green (if (= winner :A) "You win!" "AI wins!")))

(defn show-welcome []
  (show :red "The Royal Game of Ur")
  (show)
  (show (c-str :red "●") " Your pieces")
  (show (c-str :yellow "●") " AI pieces")
  (show)
  (show "Press 'q' to quit at any time.")
  (show "Press Enter to begin!"))

(def show-no-moves #(show "  No moves"))
(def show-goodbye #(show "Thanks for playing! Goodbye."))

(defn show-invalid-choice [max-choice]
  (show :red "Invalid choice. Enter 1-" max-choice " or 'q' to quit"))

(defn show-ai-move [move]
  (show :yellow "  AI: " (format-move move)))