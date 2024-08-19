(ns strategy.mcts-test
  (:require [clojure.test :refer [deftest is testing]]
            [game :as game]
            [strategy.mcts :as mcts]))

(defn- create-test-game []
  {:board (vec (repeat 24 nil))
   :players {:A {:in-hand 7 :off-board 0}
             :B {:in-hand 7 :off-board 0}}
   :current-player :A
   :roll 2
   :state :choose-action
   :selected-move nil})

(deftest test-score-player
  (testing "score-player function"
    (let [game (assoc-in (create-test-game) [:players :A :off-board] 3)]
      (is (= 30 (#'mcts/score-player game :A))
          "Should return 30 for 3 off-board pieces and no pieces on board")
      (is (= 0 (#'mcts/score-player game :B))
          "Should return 0 for no off-board pieces and no pieces on board"))))

(deftest test-evaluate-state
  (testing "evaluate-state function"
    (let [game (-> (create-test-game)
                   (assoc-in [:players :A :off-board] 2)
                   (assoc-in [:players :B :off-board] 1))]
      (is (= 10 (#'mcts/evaluate-state game))
          "Should return 10 when player A has one more off-board piece than B"))))

(deftest test-get-next-state
  (testing "get-next-state function"
    (let [game (create-test-game)
          move {:from :entry :to 4 :captured nil}
          next-state (#'mcts/get-next-state game move)]
      (is (= :A (get-in next-state [:board 4]))
          "Should move piece to the correct position")
      (is (= 6 (get-in next-state [:players :A :in-hand]))
          "Should decrease in-hand pieces")
      (is (= :choose-action (:state next-state))
          "Should keep the state as :choose-action")
      (is (integer? (:roll next-state))
          "Should have rolled the dice")
      (is (<= 0 (:roll next-state) 4)
          "Roll should be between 0 and 4")
      (is (nil? (:selected-move next-state))
          "Should clear the selected move"))))

(deftest test-expand
  (testing "expand function"
    (let [game (create-test-game)
          node (mcts/->Node game nil nil [] 0 0.0)
          expanded-node (#'mcts/expand node)]
      (is (seq (:children expanded-node))
          "Should create child nodes")
      (is (every? #(instance? mcts/Node %) (:children expanded-node))
          "All children should be Node instances"))))

(deftest test-select-best-child
  (testing "select-best-child function"
    (let [game (create-test-game)
          parent (mcts/->Node game nil nil [] 10 5.0)
          child1 (mcts/->Node game nil parent [] 5 3.0)
          child2 (mcts/->Node game nil parent [] 3 2.0)
          parent-with-children (assoc parent :children [child1 child2])
          best-child (#'mcts/select-best-child parent-with-children 1.41)]
      (is (some? best-child)
          "Should select a child")
      (is (or (= child1 best-child) (= child2 best-child))
          "Should select one of the children based on UCT value"))))

(deftest test-simulate
  (testing "simulate function"
    (let [game (create-test-game)
          simulation-result (#'mcts/simulate game)]
      (is (number? simulation-result)
          "Should return a numeric evaluation of the final state"))))

(deftest test-select-move
  (testing "select-move function"
    (let [game (assoc (create-test-game)
                      :strategy {:name :mcts
                                 :params {:iterations 10 :exploration 1.41}})
          selected-move (mcts/select-move game)]
      (is (map? selected-move)
          "Should return a move map")
      (is (contains? selected-move :from)
          "Move should have a :from key")
      (is (contains? selected-move :to)
          "Move should have a :to key")
      (is (contains? selected-move :captured)
          "Move should have a :captured key"))))
