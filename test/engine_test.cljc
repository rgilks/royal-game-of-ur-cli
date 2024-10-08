(ns engine-test
  (:require [clojure.test :refer [are deftest is testing]]
            [config]
            [engine]
            [test-utils :refer [thrown-with-msg?]]))

(def test-board
  [:A nil :B :A nil nil :B :A nil :B nil nil nil
   nil nil nil :A :B nil nil nil nil :A :B])

(def test-game
  {:board test-board
   :players {:A {:in-hand 2 :off-board 0}
             :B {:in-hand 2 :off-board 0}}
   :current-player :A
   :roll 2
   :state :choose-action
   :selected-move nil})

(deftest test-other-player
  (testing "other-player returns the correct opponent"
    (are [player expected] (= expected (engine/other-player player))
      :A :B
      :B :A)))

(deftest test-get-piece-positions
  (testing "get-piece-positions returns correct positions"
    (are [player expected] (= expected (engine/get-piece-positions test-board player))
      :A [0 3 7 16 22]
      :B [2 6 9 17 23])))

(deftest test-move-piece
  (testing "move-piece calculates correct moves"
    (are [player from roll expected] (= expected (engine/move-piece test-board player from roll))
      :A 3 2 [1 nil]
      :B 9 2 [11 nil]
      :A 0 0 nil  ; No move on roll of 0
      :A 7 1 nil  ; Can't move to occupied space
      :B 9 2 [11 nil])))

(deftest test-move-piece-entry
  (testing "move-piece calculates correct entry moves based on roll"
    (let [board (vec (repeat 24 nil))]
      (are [player roll expected] (= expected (engine/move-piece board player :entry roll))
        :A 1 [3 nil]
        :A 2 [2 nil]
        :A 3 [1 nil]
        :A 4 [0 nil]
        :B 1 [19 nil]
        :B 2 [18 nil]
        :B 3 [17 nil]
        :B 4 [16 nil]))))

(deftest test-move-piece-off-board
  (testing "move-piece only allows moving off board with exact roll"
    (let [board (assoc (vec (repeat 24 nil)) 7 :A 23 :B)]
      (are [player from roll expected] (= expected (engine/move-piece board player from roll))
        :A 7 1 [6 nil]
        :A 7 2 [:off-board nil]
        :A 14 4 [:off-board nil]
        :B 23 1 [22 nil]
        :B 23 2 [:off-board nil]))))

(deftest test-update-board
  (testing "update-board correctly updates the game board"
    (are [player from to expected] (= expected (engine/update-board test-board player from to))
      :A 3 1 [:A :A :B nil nil nil :B :A nil :B nil nil nil
              nil nil nil :A :B nil nil nil nil :A :B]
      :B 23 :off-board [:A nil :B :A nil nil :B :A nil :B nil nil nil
                        nil nil nil :A :B nil nil nil nil :A nil]
      :A :entry 3 [:A nil :B :A nil nil :B :A nil :B nil nil nil
                   nil nil nil :A :B nil nil nil nil :A :B])))

(deftest test-get-possible-moves-entry
  (testing "get-possible-moves includes correct entry moves"
    (let [game {:board (vec (repeat 24 nil))
                :players {:A {:in-hand 7 :off-board 0}
                          :B {:in-hand 7 :off-board 0}}
                :current-player :A
                :roll 3
                :state :choose-action}]
      (is (= [{:from :entry :to 1 :captured nil}] (engine/get-possible-moves game)))

      (let [game (assoc game :current-player :B)]
        (is (= [{:from :entry :to 17 :captured nil}] (engine/get-possible-moves game)))))))

(deftest test-get-possible-moves-off-board
  (testing "get-possible-moves includes moving off board with exact roll"
    (let [game {:board (assoc (vec (repeat 24 nil)) 7 :A)
                :players {:A {:in-hand 6 :off-board 0}
                          :B {:in-hand 7 :off-board 0}}
                :current-player :A
                :roll 7
                :state :choose-action}]
      (is (= #{{:from :entry :to 10 :captured nil}}
             (set (engine/get-possible-moves game))))

      (let [game (assoc game :roll 6)]
        (is (= #{{:from :entry :to 9 :captured nil}}
               (set (engine/get-possible-moves game))))))))

(deftest test-get-possible-moves
  (testing "get-possible-moves returns all valid moves"
    (is (= #{{:from 3 :to 1 :captured nil}
             {:from 0 :to 9 :captured :B}
             {:from 16 :to 2 :captured :B}
             {:from 22 :to 2 :captured :B}
             {:from 7 :to :off-board :captured nil}}
           (set (engine/get-possible-moves test-game)))))

  (testing "get-possible-moves with no valid moves"
    (let [no-move-state (assoc test-game :roll 0)]
      (is (empty? (engine/get-possible-moves no-move-state))))))

(deftest test-get-possible-moves-multiple-options
  (testing "get-possible-moves returns all valid options including entry"
    (let [game {:board [nil nil :A nil nil nil :A nil nil :B nil nil
                        nil nil nil :A nil nil nil nil nil nil nil nil]
                :players {:A {:in-hand 4 :off-board 0}
                          :B {:in-hand 6 :off-board 0}}
                :current-player :A
                :roll 2
                :state :choose-action}]
      (is (= #{{:from 2 :to 0 :captured nil}}
             (set (engine/get-possible-moves game)))))))

(deftest test-game-over?
  (testing "game-over? correctly identifies end game state"
    (are [state expected] (= expected (engine/over? state))
      {:players {:A {:off-board 6}} :current-player :A} false
      {:players {:A {:off-board 7}} :current-player :A} true
      {:players {:B {:off-board 7}} :current-player :B} true
      {:players {:A {:off-board 0}} :current-player :A} false)))

(deftest test-transitions
  (testing "transitions between game states"
    (are [initial-state rolls inputs expected-state]
         (let [[new-state _] (engine/transition initial-state rolls inputs)]
           (contains? expected-state (:state new-state)))

      {:state :start-game} [3] {} #{:roll-dice}

      {:state :roll-dice} [3] {} #{:choose-action}

      {:state :choose-action
       :board test-board
       :players {:A {:in-hand 6 :off-board 0}
                 :B {:in-hand 7 :off-board 0}}
       :current-player :A
       :roll 2} [] {:move-strategy :random} #{:enter-piece :move-piece :switch-turns}

      {:state :enter-piece
       :board test-board
       :players {:A {:in-hand 1 :off-board 0}
                 :B {:in-hand 7 :off-board 0}}
       :current-player :A
       :roll 3
       :selected-move {:from :entry :to 1 :captured nil}} [] {} #{:switch-turns}

      {:state :move-piece
       :board test-board
       :players {:A {:in-hand 0 :off-board 0}
                 :B {:in-hand 7 :off-board 0}}
       :current-player :A
       :roll 2
       :selected-move {:from 0 :to 2 :captured :B}} [] {} #{:switch-turns}

      {:state :land-on-rosette
       :board [:A nil :B :A nil nil :A :A nil :B nil nil
               nil nil nil nil :A :B nil nil nil nil :A :B]
       :players {:A {:in-hand 0 :off-board 0}
                 :B {:in-hand 7 :off-board 0}}
       :current-player :A} [] {} #{:roll-dice}

      {:state :move-piece-off-board
       :board [:A nil :B nil nil nil :B :A nil :B nil nil
               nil nil nil :A nil :B nil nil nil nil :A :B]
       :players {:A {:in-hand 0 :off-board 6}
                 :B {:in-hand 7 :off-board 0}}
       :current-player :A
       :selected-move {:from 15 :to :off-board :captured nil}} [] {} #{:switch-turns}

      {:state :switch-turns
       :board test-board
       :players {:A {:in-hand 0 :off-board 7}
                 :B {:in-hand 7 :off-board 0}}
       :current-player :A} [] {} #{:end-game})))

(deftest test-initialize-game
  (testing "initialize-game creates correct initial state"
    (let [initial-state (engine/init :A)]
      (is (= 24 (count (:board initial-state))))
      (is (every? nil? (:board initial-state)))
      (is (= 7 (get-in initial-state [:players :A :in-hand])))
      (is (= 7 (get-in initial-state [:players :B :in-hand])))
      (is (= 0 (get-in initial-state [:players :A :off-board])))
      (is (= 0 (get-in initial-state [:players :B :off-board])))
      (is (= :A (:current-player initial-state)))
      (is (nil? (:roll initial-state)))
      (is (= :start-game (:state initial-state)))
      (is (nil? (:selected-move initial-state))))))

(deftest test-start-game
  (testing "start-game initializes game correctly"
    (let [game (engine/start-game)]
      (is (= :roll-dice (:state game)))
      (is (contains? #{:A :B} (:current-player game)))
      (is (every? nil? (:board game)))
      (is (= 7 (get-in game [:players :A :in-hand])))
      (is (= 7 (get-in game [:players :B :in-hand]))))))

(deftest test-start-game-with-specified-player
  (testing "start-game initializes game correctly with specified player"
    (let [game (engine/start-game :A)]
      (is (= :roll-dice (:state game)))
      (is (= :A (:current-player game)))
      (is (every? nil? (:board game)))
      (is (= 7 (get-in game [:players :A :in-hand])))
      (is (= 7 (get-in game [:players :B :in-hand]))))))

(deftest test-roll
  (testing "roll advances game state correctly"
    (let [initial-state (assoc test-game :state :roll-dice :roll nil)
          rolled-state (engine/roll initial-state)]
      (is (= :choose-action (:state rolled-state)))
      (is (<= 0 (:roll rolled-state) 4))))

  (testing "roll throws on invalid state"
    (is (thrown-with-msg? #?(:clj Throwable :cljs :default)
                          #"Invalid game state for rolling dice"
                          (engine/roll test-game)))))

(deftest test-choose-action
  (testing "choose-action advances game state correctly"
    (let [move {:from 0 :to 2 :captured :B}
          new-state (engine/choose-action test-game move)]
      (is (= :roll-dice (:state new-state)))
      (is (nil? (:selected-move new-state))))))

(deftest test-board-config
  (testing "board-config contains correct values"
    (is (= 24 (:size config/board)))
    (is (= #{0 6 11 16 22} (:rosettes config/board)))
    (is (= #{4 5 20 21} (:exclude config/board)))
    (is (vector? (get-in config/board [:paths :A])))
    (is (vector? (get-in config/board [:paths :B])))))
