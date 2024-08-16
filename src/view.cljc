(ns view
  (:require [clojure.string :as str]
            [config]
            [util :refer [log]]))

(def colors
  {:reset "[0m" :bold "[1m" :red "[31m" :green "[32m"
   :yellow "[33m" :blue "[34m" :cyan "[36m"})

(defn cs [text color]
  (str "\u001b" (color colors) text "\u001b" (:reset colors)))

(def board-symbols
  {:A (cs " ●" :red)
   :B (cs " ●" :yellow)
   :rosette (cs " ✧" :blue)
   :empty (cs " ·" :blue)})

(defn render-cell [board idx]
  (cond
    (some? (get board idx)) (get board-symbols (get board idx) (:empty board-symbols))
    (contains? (:exclude config/board) idx) "  "
    (contains? (:rosettes config/board) idx) (:rosette board-symbols)
    :else (:empty board-symbols)))

(defn print-board [board]
  (let [render-row (fn [start end label]
                     (str (cs label :cyan) " "
                          (str/join (map #(render-cell board %) (range start end)))))]
    (log (cs "    1 2 3 4 5 6 7 8" :cyan))
    (log (cs "┌──────────────────┐" :cyan))
    (doseq [[start end label] [[0 8 "A"] [8 16 "B"] [16 24 "C"]]]
      (log (str (cs "│" :cyan)
                (render-row start end label)
                (cs "│" :cyan))))
    (log (cs "└──────────────────┘" :cyan))))

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
    (log (str (format-player :red :A)
              (when roll (str " " (display-dice-roll roll)))
              (format-player :yellow :B))))
  (log)
  #_(when (= state :choose-action)
      (log (str (if (= current-player :A) "Your" "AI's") " turn to move."))))

(def index-to-coord
  (into {} (for [i (range 24)]
             [i (str (nth ["A" "B" "C"] (quot i 8)) (inc (mod i 8)))])))

(defn format-move [{:keys [from to captured]}]
  (str (if (= from :entry) "entry" (index-to-coord from))
       " → "
       (if (= to :off-board) "off board" (index-to-coord to))
       (when captured (cs " (captures)" :red))))

(defn print-moves [possible-moves]
  (doseq [[idx move] (map-indexed vector possible-moves)]
    (log (str (cs (str (inc idx) " ") :red) (format-move move)))))

(defn print-winner-message [winner]
  (log (cs "\n🎉🎉🎉 GAME OVER 🎉🎉🎉" :blue))
  (log (cs (str "Congratulations to " (if (= winner :A) "You" "AI") "!") :green))
  (log "You've completed the Royal Game of Ur!")
  (if (= winner :A)
    (log (cs "Well done, human! You've outsmarted the computer!" :green))
    (log (cs "The computer wins. Better luck next time, human!" :red)))
  (log (cs "Thanks for playing the Royal Game of Ur!" :blue)))

(defn print-welcome-message []
  (log (cs "Welcome to the Royal Game of Ur!" :blue))
  (log (str (cs "●" :red) " Your pieces"))
  (log (str (cs "●" :yellow) " AI pieces"))
  (log "The board is labeled with columns 1-8 and rows a-c.")
  (log "Press Enter to begin!"))
