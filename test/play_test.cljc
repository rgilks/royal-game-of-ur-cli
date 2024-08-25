(ns play-test
  (:require [clojure.test :refer [are deftest is testing]]
            [engine]
            [platform]
            [play]
            [test-utils :refer [thrown-with-msg?]]
            [util]
            [view]))

(deftest test-get-user-move
  (testing "Valid single-key inputs"
    (are [input expected-move moves]
         (with-redefs [platform/read-single-key (constantly input)
                       view/show-moves (constantly nil)]
           (= expected-move (play/get-user-move moves)))

      "1" {:from :entry :to 4} [{:from :entry :to 4} {:from 0 :to 4}]
      "2" {:from 0 :to 4} [{:from :entry :to 4} {:from 0 :to 4}]))

  (testing "User quits the game"
    (with-redefs [platform/read-single-key (constantly "q")
                  view/show-moves (constantly nil)]
      (is (thrown-with-msg?
           #?(:clj Throwable :cljs :default)
           #"User quit"
           (play/get-user-move [{:from :entry :to 4} {:from 0 :to 4}])))))

  (testing "Invalid inputs followed by valid input"
    (are [inputs expected-move moves]
         (let [input-atom (atom inputs)]
           (with-redefs [platform/read-single-key #(let [input (first @input-atom)]
                                                     (swap! input-atom rest)
                                                     input)
                         view/show-moves (constantly nil)
                         view/show-invalid-choice (constantly nil)]
             (= expected-move (play/get-user-move moves))))

      ["a" "3" "2"] {:from 4 :to 8} [{:from :entry :to 4} {:from 0 :to 4} {:from 4 :to 8}]
      ["5" "0" "1"] {:from :entry :to 4} [{:from :entry :to 4} {:from 0 :to 4}]))

  (testing "Empty moves list"
    (with-redefs [platform/read-single-key (constantly "1")
                  view/show-moves (constantly nil)]
      (is (nil? (play/get-user-move []))))))

(deftest test-get-move
  (testing "Get move for player A"
    (with-redefs [play/get-user-move (constantly {:from :entry :to 4})]
      (is (= {:from :entry :to 4} (play/get-move :A [{:from :entry :to 4}] nil)))))

  (testing "Get move for player B (AI)"
    (with-redefs [engine/select-move (constantly {:from 0 :to 4})]
      (is (= {:from 0 :to 4} (play/get-move :B [{:from 0 :to 4}] nil)))))

  (testing "No moves available"
    (is (nil? (play/get-move :A [] nil)))))

(deftest test-handle
  (testing "Handle roll dice state"
    (with-redefs [engine/roll (constantly :rolled-game)]
      (is (= :rolled-game (play/handle {:state :roll-dice})))))

  (testing "Handle choose action with no moves"
    (with-redefs [engine/get-moves (constantly [])
                  view/show-no-moves (constantly nil)
                  platform/sleep (constantly nil)
                  engine/choose-action (constantly :next-game)]
      (is (= :next-game (play/handle {:state :choose-action, :current-player :A})))))

  (testing "Handle choose action for player A"
    (let [move {:from :entry :to 4}]
      (with-redefs [engine/get-moves (constantly [move])
                    play/get-move (constantly move)
                    engine/choose-action (constantly :next-game)]
        (is (= :next-game (play/handle {:state :choose-action, :current-player :A}))))))

  (testing "Handle choose action for player B (AI)"
    (let [move {:from :entry :to 4}]
      (with-redefs [engine/get-moves (constantly [move])
                    play/get-move (constantly move)
                    engine/choose-action (constantly :next-game)
                    view/show-ai-move (constantly nil)
                    platform/sleep (constantly nil)]
        (is (= :next-game (play/handle {:state :choose-action, :current-player :B}))))))

  (testing "Handle end game state"
    (with-redefs [view/show-winner (constantly nil)]
      (is (thrown-with-msg? #?(:clj Throwable :cljs :default)
                            #"Game over"
                            (play/handle {:state :end-game, :current-player :A}))))))

