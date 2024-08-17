(ns state-test
  (:require [clojure.test :refer [are deftest is testing]]
            [config]
            [platform]
            [state :as ur]
            [test-utils :refer [thrown-with-msg?]]))

(def test-board
  [:A nil :B :A nil nil :B :A nil :B nil nil nil
   nil nil nil :A :B nil nil nil nil :A :B])

(def test-game-state
  {:board test-board
   :players {:A {:in-hand 2 :off-board 0}
             :B {:in-hand 2 :off-board 0}}
   :current-player :A
   :roll 2
   :state :choose-action
   :selected-move nil})

(deftest test-other-player
  (testing "other-player returns the correct opponent"
    (are [player expected] (= expected (ur/other-player player))
      :A :B
      :B :A)))

(deftest test-get-piece-positions
  (testing "get-piece-positions returns correct positions"
    (are [player expected] (= expected (ur/get-piece-positions test-board player))
      :A [0 3 7 16 22]
      :B [2 6 9 17 23])))

(deftest test-move-piece
  (testing "move-piece calculates correct moves"
    (are [player from roll expected] (= expected (ur/move-piece test-board player from roll))
      :A 3 5 [9 :B]
      :B 9 2 [11 nil]
      :A 15 4 [:off-board nil]
      :A 0 0 nil  ; No move on roll of 0
      :A 7 1 nil  ; Can't land on own piece
      :B 9 2 [11 nil])))

(deftest test-update-board
  (testing "update-board correctly updates the game board"
    (are [player from to expected] (= expected (ur/update-board test-board player from to))
      :A 3 8 [:A nil :B nil nil nil :B :A :A :B nil nil nil
              nil nil nil :A :B nil nil nil nil :A :B]

      :B 23 :off-board [:A nil :B :A nil nil :B :A nil :B nil nil nil
                        nil nil nil :A :B nil nil nil nil :A nil])))

(deftest test-get-possible-moves
  (testing "get-possible-moves returns all valid moves"
    (is (= #{{:from 0 :to 9 :captured :B}
             {:from 3 :to 1 :captured nil}
             {:from 7 :to :off-board :captured nil}
             {:from 16 :to 2 :captured :B}
             {:from 22 :to 2 :captured :B}}
           (set (ur/get-possible-moves test-game-state)))))

  (testing "get-possible-moves with no valid moves"
    (let [no-move-state (assoc test-game-state :roll 0)]
      (is (empty? (ur/get-possible-moves no-move-state))))))

(deftest test-game-over?
  (testing "game-over? correctly identifies end game state"
    (are [state expected] (= expected (ur/game-over? state))
      {:players {:A {:off-board 6}} :current-player :A} false
      {:players {:A {:off-board 7}} :current-player :A} true
      {:players {:B {:off-board 7}} :current-player :B} true
      {:players {:A {:off-board 0}} :current-player :A} false)))

(deftest test-transitions
  (testing "transitions between game states"
    (are [initial-state rolls expected-state]
         (let [[new-state _] (ur/transition initial-state rolls)]
           (contains? expected-state (:state new-state)))

      {:state :start-game} [] #{:roll-dice}

      {:state :roll-dice} [3] #{:choose-action}

      {:state :choose-action
       :board test-board
       :players {:A {:in-hand 6 :off-board 0}
                 :B {:in-hand 7 :off-board 0}}
       :current-player :A
       :roll 2} [] #{:enter-piece :move-piece}

      {:state :enter-piece
       :board test-board
       :players {:A {:in-hand 1 :off-board 0}
                 :B {:in-hand 7 :off-board 0}}
       :current-player :A
       :roll 3
       :selected-move {:from :entry :to 3 :captured nil}} [] #{:switch-turns}

      {:state :move-piece
       :board test-board
       :players {:A {:in-hand 0 :off-board 0}
                 :B {:in-hand 7 :off-board 0}}
       :current-player :A
       :roll 2
       :selected-move {:from 0 :to 2 :captured :B}} [] #{:switch-turns}

      {:state :land-on-rosette
       :board [:A nil :B :A nil nil :A :A nil :B nil nil
               nil nil nil nil :A :B nil nil nil nil :A :B]
       :players {:A {:in-hand 0 :off-board 0}
                 :B {:in-hand 7 :off-board 0}}
       :current-player :A} [] #{:roll-dice}

      {:state :move-piece-off-board
       :board [:A nil :B nil nil nil :B :A nil :B nil nil
               nil nil nil :A nil :B nil nil nil nil :A :B]
       :players {:A {:in-hand 0 :off-board 6}
                 :B {:in-hand 7 :off-board 0}}
       :current-player :A
       :selected-move {:from 15 :to :off-board :captured nil}} [] #{:switch-turns}

      {:state :switch-turns
       :board test-board
       :players {:A {:in-hand 0 :off-board 7}
                 :B {:in-hand 7 :off-board 0}}
       :current-player :A} [] #{:end-game})))

(deftest test-initialize-game
  (testing "initialize-game creates correct initial state"
    (let [initial-state (ur/initialize-game :A)]  ; Specify :A as the starting player for deterministic tests
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
    (let [game (ur/start-game)]
      (is (= :roll-dice (:state game)))
      (is (contains? #{:A :B} (:current-player game)))
      (is (every? nil? (:board game)))
      (is (= 7 (get-in game [:players :A :in-hand])))
      (is (= 7 (get-in game [:players :B :in-hand]))))))

(deftest test-start-game-with-specified-player
  (testing "start-game initializes game correctly with specified player"
    (let [game (ur/start-game :A)]
      (is (= :roll-dice (:state game)))
      (is (= :A (:current-player game)))
      (is (every? nil? (:board game)))
      (is (= 7 (get-in game [:players :A :in-hand])))
      (is (= 7 (get-in game [:players :B :in-hand]))))))

(deftest test-validate-game-state
  (testing "validate-game-state accepts valid states"
    (is (= test-game-state (ur/validate-game-state test-game-state))))

  (testing "validate-game-state throws on invalid states"
    (are [invalid-state]
         (thrown-with-msg? #?(:clj Throwable :cljs :default)
                           #"Invalid game state"
                           (ur/validate-game-state invalid-state))
      (assoc test-game-state :board [])  ; Invalid board size
      (assoc-in test-game-state [:players :A :in-hand] 8)  ; Too many pieces in hand
      (assoc test-game-state :current-player :C)  ; Invalid player
      (assoc test-game-state :roll 5)  ; Invalid roll
      (assoc test-game-state :state :invalid-state))))  ; Invalid state

(deftest test-validate-total-pieces
  (testing "validate-total-pieces accepts valid piece counts"
    (is (ur/validate-total-pieces test-game-state)))

  (testing "validate-total-pieces fails on invalid piece counts"
    (is (not (ur/validate-total-pieces
              (assoc-in test-game-state [:players :A :in-hand] 4))))))

(deftest test-dice-roll
  (testing "dice-roll advances game state correctly"
    (let [initial-state (assoc test-game-state :state :roll-dice :roll nil)
          rolled-state (ur/dice-roll initial-state)]
      (is (= :choose-action (:state rolled-state)))
      (is (<= 0 (:roll rolled-state) 4))))

  (testing "dice-roll throws on invalid state"
    (is (thrown-with-msg? #?(:clj Throwable :cljs :default)
                          #"Invalid game state for rolling dice"
                          (ur/dice-roll test-game-state)))))

(deftest test-choose-action
  (testing "choose-action advances game state correctly"
    (let [move {:from 0 :to 2 :captured :B}
          new-state (ur/choose-action test-game-state move)]
      (is (contains? #{:roll-dice :move-piece :enter-piece} (:state new-state))
          (str "Expected :roll-dice, :move-piece, or :enter-piece, but got " (:state new-state)))
      (is (nil? (:selected-move new-state))
          (str "Expected selected-move to be nil, but got " (:selected-move new-state))))))

(deftest test-board-config
  (testing "board-config contains correct values"
    (is (= 24 (:size config/board)))
    (is (= #{0 6 11 16 22} (:rosettes config/board)))
    (is (= #{4 5 20 21} (:exclude config/board)))
    (is (vector? (get-in config/board [:paths :A])))
    (is (vector? (get-in config/board [:paths :B])))))
