(ns cli
  (:require [state-machine :as game]
            [platform :as plat]
            [util :refer [log]]
            [clojure.string :as str]))

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
    (contains? (:exclude game/board-config) idx) "  "
    (contains? (:rosettes game/board-config) idx) (:rosette board-symbols)
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

(defn print-game-state [{:keys [board players roll]}]
  (plat/clear-console)
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
  (log))

(def index-to-coord
  (into {} (for [i (range 24)]
             [i (str (nth ["A" "B" "C"] (quot i 8)) (inc (mod i 8)))])))

(defn format-move [{:keys [from to captured]}]
  (str (if (= from :entry) "entry" (index-to-coord from))
       " → "
       (if (= to :off-board) "off board" (index-to-coord to))
       (when captured (cs " (captures)" :red))))

(defn get-user-move [possible-moves]
  (when (seq possible-moves)
    (doseq [[idx move] (map-indexed vector possible-moves)]
      (log (str (cs (str (inc idx) " ") :red) (format-move move))))
    (let [choice (plat/parse-int (str/trim (plat/readln)))]
      (if (and (pos? choice) (<= choice (count possible-moves)))
        (nth possible-moves (dec choice))
        (do
          (log (cs (str "Invalid choice. Enter 1-" (count possible-moves)) :red))
          (plat/readln)
          (recur possible-moves))))))

(defn print-winner-message [winner]
  (log (cs "\n🎉🎉🎉 GAME OVER 🎉🎉🎉" :blue))
  (log (cs (str "Congratulations to " (if (= winner :A) "You" "AI") "!") :green))
  (log "You've completed the Royal Game of Ur!")
  (if (= winner :A)
    (log (cs "Well done, human! You've outsmarted the computer!" :green))
    (log (cs "The computer wins. Better luck next time, human!" :red)))
  (log (cs "Thanks for playing the Royal Game of Ur!" :blue)))

(defn play-game []
  (plat/clear-console)
  (log (cs "Welcome to the Royal Game of Ur!" :blue))
  (log (str (cs "●" :red) " Your pieces"))
  (log (str (cs "●" :yellow) " AI pieces"))
  (log "The board is labeled with columns 1-8 and rows a-c.")
  (log "Press Enter to begin!")
  (plat/readln)
  (loop [state (game/start-game)]
    (print-game-state state)
    (case (:state state)
      :roll-dice
      (do
        (log (if (= (:current-player state) :A)
               "\nRolling dice for you..."
               "\nAI is rolling the dice..."))
        (let [new-state (game/dice-roll state)]
          (print-game-state new-state)
          (plat/sleep 1500)
          (recur new-state)))

      :choose-action
      (let [possible-moves (game/get-moves state)]
        (if (empty? possible-moves)
          (do
            (log (str (if (= (:current-player state) :A) "You have" "AI has") " no possible moves"))
            (plat/sleep 1500)
            (recur (game/choose-action state nil)))
          (let [selected-move (if (= (:current-player state) :A)
                                (get-user-move possible-moves)
                                (game/select-move :random possible-moves))]
            (when (= (:current-player state) :B)
              (log (str "AI " (cs (format-move selected-move) :yellow)))
              (plat/sleep 1500))
            (recur (game/choose-action state selected-move)))))

      :end-game
      (do
        (print-winner-message (:current-player state))
        (print-game-state state)))))

(defn -main []
  (play-game))
