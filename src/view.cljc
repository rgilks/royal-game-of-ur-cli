(ns view
  (:require [clojure.string :as str]
            [config]
            [util :refer [show]]))

(def colors
  {:reset "[0m" :bold "[1m" :red "[31m" :green "[32m"
   :yellow "[33m" :blue "[34m" :cyan "[36m"})

(defn cs [text color]
  (str "\u001b" (color colors) text "\u001b" (:reset colors)))

(def board-symbols
  {:A (cs " ●" :red)
   :B (cs " ●" :yellow)
   :rosette (cs " ✧" :cyan)
   :empty (cs " ·" :cyan)})

(defn render-cell [board idx]
  (cond
    (some? (get board idx)) (get board-symbols (get board idx) (:empty board-symbols))
    (contains? (:exclude config/board) idx) "  "
    (contains? (:rosettes config/board) idx) (:rosette board-symbols)
    :else (:empty board-symbols)))

(defn print-board [board]
  (show "")
  (let [render-row (fn [start end label]
                     (str (cs label :cyan) " "
                          (str/join (map #(render-cell board %) (range start end)))))]
    (show (cs "    1 2 3 4 5 6 7 8" :cyan))
    (show (cs "┌──────────────────┐" :cyan))
    (doseq [[start end label] [[0 8 "A"] [8 16 "B"] [16 24 "C"]]]
      (show (str (cs "│" :cyan)
                 (render-row start end label)
                 (cs "│" :cyan))))
    (show (cs "└──────────────────┘" :cyan))))

(defn display-dice-roll [roll]
  (let [triangles (shuffle (concat (repeat roll "▲") (repeat (- 4 roll) "△")))]
    (str/join "" (map #(cs % :bold) triangles))))

(defn print-game-state [{:keys [board players roll _current-player _state]}]
  (print-board board)
  (let [format-player
        (fn [color player]
          (str (cs " " color)
               (cs (str (get-in players [player :in-hand])) color)
               (cs " → " color)
               (cs (str (get-in players [player :off-board])) color)))]
    (show (str " "
               (format-player :red :A)
               (when roll (str " " (display-dice-roll roll)))
               (format-player :yellow :B))))
  (show)
  #_(when (= state :choose-action)
      (log (str (if (= current-player :A) "Your" "AI's") " turn to move."))))

(def index-to-coord
  (into {} (for [i (range 24)]
             [i (str (nth ["A" "B" "C"] (quot i 8)) (inc (mod i 8)))])))

(defn format-move [{:keys [from to captured]}]
  (str (if (= from :entry) "entry" (index-to-coord from))
       " → "
       (if (= to :off-board) "off" (index-to-coord to))
       (when captured (cs " capture" :red))))

(defn print-moves [possible-moves]
  (doseq [[idx move] (map-indexed vector possible-moves)]
    (show (str "  " (cs (str (inc idx) " ") :red) (format-move move)))))

(defn print-winner-message [winner]
  (show "")
  (show (cs "GAME OVER" :red))
  (show (cs (str (if (= winner :A) "You" "AI") " wins!") :green)))

(defn print-welcome-message []
  (show (cs "The Royal Game of Ur" :red))
  (show "")
  (show (str (cs "●" :red) " Your pieces"))
  (show (str (cs "●" :yellow) " AI pieces"))
  (show "")
  (show "Press 'q' to quit at any time.")
  (show "Press Enter to begin!"))

(defn print-invalid-choice [max-choice]
  (show (cs (str "Invalid choice. Enter 1-" max-choice " or 'q' to quit") :red)))

(defn print-no-moves []
  (show "  No moves"))

(defn print-goodbye-message []
  (show "Thanks for playing! Goodbye."))

(defn print-ai-move [selected-move]
  (show (cs (str "  AI: " (format-move selected-move)) :yellow)))

