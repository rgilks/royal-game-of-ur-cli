(ns game-test
  (:require [clojure.test :refer [are deftest is testing]]
            [game]
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
    (are [player expected] (= expected (game/other-player player))
      :A :B
      :B :A)))

(deftest test-get-piece-positions
  (testing "get-piece-positions returns correct positions"
    (are [player expected] (= expected (game/get-piece-positions test-board player))
      :A [0 3 7 16 22]
      :B [2 6 9 17 23])))

(deftest test-move-piece
  (testing "move-piece calculates correct moves"
    (are [player from roll expected] (= expected (game/move-piece test-board player from roll))
      :A 3 5 [9 :B]
      :B 9 2 [11 nil]
      :A 15 4 [:off-board nil]
      :A 0 0 nil  ; No move on roll of 0
      :A 7 1 nil  ; Can't land on own piece
      :B 9 2 [11 nil])))

(deftest test-update-board
  (testing "update-board correctly updates the game board"
    (are [player from to expected] (= expected (game/update-board test-board player from to))
      :A 3 8 [:A nil :B nil nil nil :B :A :A :B nil nil nil
              nil nil nil :A :B nil nil nil nil :A :B]

      :B 23 :off-board [:A nil :B :A nil nil :B :A nil :B nil nil nil
                        nil nil nil :A :B nil nil nil nil :A nil])))

(deftest test-get-possible-moves
  (testing "get-possible-moves returns all valid moves"
    (is (= [{:from 3 :to 1 :captured nil}
            {:from 0 :to 9 :captured :B}
            {:from 7 :to :off-board :captured nil}
            {:from 16 :to 2 :captured :B}
            {:from 22 :to 2 :captured :B}]
           (game/get-possible-moves test-game))))

  (testing "get-possible-moves with no valid moves"
    (let [no-move-state (assoc test-game :roll 0)]
      (is (empty? (game/get-possible-moves no-move-state))))))

(deftest test-game-over?
  (testing "game-over? correctly identifies end game state"
    (are [state expected] (= expected (game/over? state))
      {:players {:A {:off-board 6}} :current-player :A} false
      {:players {:A {:off-board 7}} :current-player :A} true
      {:players {:B {:off-board 7}} :current-player :B} true
      {:players {:A {:off-board 0}} :current-player :A} false)))

(deftest test-transitions
  (testing "transitions between game states"
    (are [initial-state rolls inputs expected-state]
         (let [[new-state _] (game/transition initial-state rolls inputs)]
           (contains? expected-state (:state new-state)))

      {:state :start-game} [3] {} #{:roll-dice}

      {:state :roll-dice} [3] {} #{:choose-action}

      {:state :choose-action
       :board test-board
       :players {:A {:in-hand 6 :off-board 0}
                 :B {:in-hand 7 :off-board 0}}
       :current-player :A
       :roll 2} [] {:move-strategy :random} #{:enter-piece :move-piece}

      {:state :enter-piece
       :board test-board
       :players {:A {:in-hand 1 :off-board 0}
                 :B {:in-hand 7 :off-board 0}}
       :current-player :A
       :roll 3
       :selected-move {:from :entry :to 3 :captured nil}} [] {} #{:switch-turns}

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
    (let [initial-state (game/init :A)]  ; Specify :A as the starting player for deterministic tests
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
    (let [game (game/start-game)]
      (is (= :roll-dice (:state game)))
      (is (contains? #{:A :B} (:current-player game)))
      (is (every? nil? (:board game)))
      (is (= 7 (get-in game [:players :A :in-hand])))
      (is (= 7 (get-in game [:players :B :in-hand]))))))

(deftest test-start-game-with-specified-player
  (testing "start-game initializes game correctly with specified player"
    (let [game (game/start-game :A)]
      (is (= :roll-dice (:state game)))
      (is (= :A (:current-player game)))
      (is (every? nil? (:board game)))
      (is (= 7 (get-in game [:players :A :in-hand])))
      (is (= 7 (get-in game [:players :B :in-hand]))))))

(deftest test-dice-roll
  (testing "dice-roll advances game state correctly"
    (let [initial-state (assoc test-game :state :roll-dice :roll nil)
          rolled-state (game/roll initial-state)]
      (is (= :choose-action (:state rolled-state)))
      (is (<= 0 (:roll rolled-state) 4))))

  (testing "dice-roll throws on invalid state"
    (is (thrown-with-msg? #?(:clj Throwable :cljs :default)
                          #"Invalid game state for rolling dice"
                          (game/roll test-game)))))

(deftest test-choose-action
  (testing "choose-action advances game state correctly"
    (let [move {:from 0 :to 2 :captured :B}
          new-state (game/choose-action test-game move)]
      (is (contains? #{:roll-dice :move-piece :enter-piece :switch-turns} (:state new-state))
          (str "Expected :roll-dice, :move-piece, :enter-piece, or :switch-turns, but got " (:state new-state)))
      (is (nil? (:selected-move new-state))
          (str "Expected selected-move to be nil, but got " (:selected-move new-state))))))

(deftest test-board-config
  (testing "board-config contains correct values"
    (is (= 24 (:size config/board)))
    (is (= #{0 6 11 16 22} (:rosettes config/board)))
    (is (= #{4 5 20 21} (:exclude config/board)))
    (is (vector? (get-in config/board [:paths :A])))
    (is (vector? (get-in config/board [:paths :B])))))
