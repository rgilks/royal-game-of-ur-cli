(ns sim-test
  (:require [clojure.test :refer [are deftest is testing]]
            [config]
            [engine]
            [sim]
            [validate]
            [view]))

(deftest test-handle-choose-action
  (testing "handle-choose-action function"
    (let [game {:current-player :A
                :board [0 0 0 0 0 0]
                :players {:A {:position 0} :B {:position 0}}
                :roll 0
                :selected-move nil
                :strategy {:name :first-in-list}}
          possible-moves [{:type :move, :from 1, :to 2}]]
      (with-redefs [engine/select-move (constantly (first possible-moves))
                    engine/choose-action (fn [g m] (assoc g :selected-move m))]
        (is (= (assoc game :selected-move (first possible-moves))
               (sim/handle-choose-action game (:name (:strategy game)))))))))

(deftest test-handle-multimethod
  (testing "handle multimethod for different states"
    (let [base-game {:board [0 0 0 0 0 0]
                     :players {:A {:position 0} :B {:position 0}}
                     :current-player :A
                     :roll 0
                     :selected-move nil
                     :strategy {:name :first-in-list}}]
      (with-redefs [engine/roll (fn [g] (assoc g :roll 7))
                    view/show-winner (constantly nil)]
        (are [input expected] (= expected (sim/handle input))
          (assoc base-game :state :start-game)
          (assoc base-game :state :roll-dice)

          (assoc base-game :state :roll-dice)
          (assoc base-game :state :roll-dice :roll 7)

          (assoc base-game :state :end-game)
          (assoc base-game :state :end-game :game-over true))))))

(deftest test-play-turn
  (testing "play-turn function"
    (with-redefs [sim/handle (fn [game]
                               (case (:state game)
                                 :roll-dice (assoc game :state :choose-action)
                                 :choose-action (assoc game :state :roll-dice)))
                  view/show-state identity
                  config/game (atom {:show false})]
      (is (= {:state :roll-dice
              :board [0 0 0 0 0 0]
              :players {:A {:position 0} :B {:position 0}}
              :current-player :A
              :roll 0
              :selected-move nil}
             (sim/play-turn {:state :roll-dice
                             :board [0 0 0 0 0 0]
                             :players {:A {:position 0} :B {:position 0}}
                             :current-player :A
                             :roll 0
                             :selected-move nil}))))))

(deftest test-play-game
  (testing "play-game function"
    (with-redefs [engine/init (constantly {:current-player :A
                                           :board (vec (repeat 24 nil))
                                           :players {:A {:in-hand 7 :off-board 0}
                                                     :B {:in-hand 7 :off-board 0}}
                                           :roll nil
                                           :state :start-game
                                           :selected-move nil})
                  sim/play-turn (fn [game] (assoc game :game-over true))
                  config/game (atom {:show false
                                     :validate false
                                     :strategies {:A {:name :minimax :params {:depth 3}}}})]
      (is (= {:current-player :A
              :board (vec (repeat 24 nil))
              :players {:A {:in-hand 7 :off-board 0}
                        :B {:in-hand 7 :off-board 0}}
              :roll nil
              :state :start-game
              :selected-move nil
              :game-over true
              :strategy {:name :minimax :params {:depth 3}}
              :move-count 1}
             (sim/play-game))))))

(deftest test-run-single-chunk
  (testing "run-single-chunk function"
    (with-redefs [sim/play-game (constantly {:current-player :A :move-count 5})]
      (is (= {:wins {:A 3} :total-moves 15}
             (sim/run-single-chunk (range 3)))))))

;; (deftest test-run-simulation
;;   (testing "run-simulation function"
;;     (with-redefs [sim/run-single-chunk (constantly {:wins {:A 5 :B 5} :total-moves 50})
;;                   sim/config-atom (atom {:num-games 10 :parallel 2})
;;                   engine/select-move (fn [_ _] nil)] ; Add this line to prevent the multimethod error
;;       (is (= {:wins {:A 10 :B 10} :total-moves 100 :num-games 10}
;;              (sim/run-simulation))))))

(deftest test-print-simulation-results
  (testing "print-simulation-results function"
    (with-redefs [config/game (atom {:strategies {:A {:name :minimax
                                                      :params {:depth 2}}
                                                  :B {:name :minimax
                                                      :params {:depth 4}}}})]
      (is (= (str "\nSimulation Results:\n"
                  "Total games: 100\n"
                  "Strategy A (Player A): :minimax\n"
                  "Strategy B (Player B): :minimax\n"
                  "Player A wins: 60\n"
                  "Player B wins: 40\n"
                  "Player A win percentage: 60.00\n"
                  "Average moves per game: 50\n"
                  "Elapsed time: 10.50\n")
             (with-out-str (sim/print-simulation-results {:wins {:A 60 :B 40} :total-moves 5000 :num-games 100} 10.5)))))))
