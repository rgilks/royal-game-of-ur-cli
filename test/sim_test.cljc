(ns sim-test
  (:require [clojure.test :refer [are deftest is testing]]
            [sim :as sim]
            [state :as state]
            [view :as view]))

(deftest test-debug
  (testing "debug function"
    (with-redefs [sim/config-atom (atom {:debug? true})]
      (is (= "Debug message\n"
             (with-out-str (sim/debug "Debug message")))))

    (with-redefs [sim/config-atom (atom {:debug? false})]
      (is (= "" (with-out-str (sim/debug "Debug message")))))))

(deftest test-handle-choose-action
  (testing "handle-choose-action function"
    (let [game {:current-player :A
                :board [0 0 0 0 0 0]
                :players {:A {:position 0} :B {:position 0}}
                :roll 0
                :selected-move nil}
          possible-moves [{:type :move, :from 1, :to 2}]
          strategy :first-in-list]
      (with-redefs [state/select-move (constantly (first possible-moves))
                    view/show-ai-move (constantly nil)
                    state/choose-action (fn [g m] (assoc g :selected-move m))]
        (is (= (assoc game :selected-move (first possible-moves))
               (sim/handle-choose-action game possible-moves strategy)))))))

(deftest test-handle-multimethod
  (testing "handle multimethod for different states"
    (let [base-game {:board [0 0 0 0 0 0]
                     :players {:A {:position 0} :B {:position 0}}
                     :current-player :A
                     :roll 0
                     :selected-move nil}]
      (with-redefs [state/dice-roll (fn [g] (assoc g :roll 7))]
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
                  sim/config-atom (atom {:show? false})]
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
#_(deftest test-play-game
    (testing "play-game function"
      (with-redefs [state/initialize-game (constantly {:current-player :A
                                                       :board [0 0 0 0 0 0]
                                                       :players {:A {:position 0} :B {:position 0}}
                                                       :roll 0
                                                       :selected-move nil})
                    sim/play-turn (fn [game] (assoc game :game-over true))
                    sim/config-atom (atom {:show? false})]
        (is (= {:current-player :A
                :board [0 0 0 0 0 0]
                :players {:A {:position 0} :B {:position 0}}
                :roll 0
                :selected-move nil
                :game-over true
                :strategy :strategy-a}
               (sim/play-game))))))

(deftest test-run-simulation
  (testing "run-simulation function"
    (with-redefs [state/initialize-game (constantly {:current-player :A
                                                     :board [0 0 0 0 0 0]
                                                     :players {:A {:position 0} :B {:position 0}}
                                                     :roll 0
                                                     :selected-move nil})
                  sim/play-game (constantly {:current-player :A})
                  sim/config-atom (atom {:num-games 10})]
      (is (= {:A 10, :B 0}
             (sim/run-simulation))))))

(deftest test-print-simulation-results
  (testing "print-simulation-results function"
    (with-redefs [sim/config-atom (atom {:num-games 100
                                         :strategies {:A {:name :minimax
                                                          :params {:depth 2}}
                                                      :B {:name :minimax
                                                          :params {:depth 4}}}})]
      (is (= (str "\nSimulation Results:\n"
                  "Total games: 100\n"
                  "Strategy A (Player A): :minimax\n"
                  "Strategy B (Player B): :minimax\n"
                  "Player A wins: 60\n"
                  "Player B wins: 40\n"
                  "Player A win percentage: 60%\n")
             (with-out-str (sim/print-simulation-results {:A 60, :B 40})))))))

