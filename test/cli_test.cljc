(ns cli-test
  (:require [cli]
            [clojure.test :refer [are deftest is testing]]
            [game]
            [platform]
            [test-utils :refer [thrown-with-msg?]]
            [util]
            [view]))

(deftest test-get-user-move
  (testing "Valid single-key inputs"
    (are [input expected-move moves]
         (with-redefs [platform/read-single-key (constantly input)
                       view/show-moves (constantly nil)]
           (= expected-move (cli/get-user-move moves)))

      "1" {:from :entry :to 4} [{:from :entry :to 4} {:from 0 :to 4}]
      "2" {:from 0 :to 4} [{:from :entry :to 4} {:from 0 :to 4}]))

  (testing "User quits the game"
    (with-redefs [platform/read-single-key (constantly "q")
                  view/show-moves (constantly nil)]
      (is (thrown-with-msg?
           #?(:clj Throwable :cljs :default)
           #"User quit"
           (cli/get-user-move [{:from :entry :to 4} {:from 0 :to 4}])))))

  (testing "Invalid inputs followed by valid input"
    (are [inputs expected-move moves]
         (let [input-atom (atom inputs)]
           (with-redefs [platform/read-single-key #(let [input (first @input-atom)]
                                                     (swap! input-atom rest)
                                                     input)
                         view/show-moves (constantly nil)
                         view/show-invalid-choice (constantly nil)]
             (= expected-move (cli/get-user-move moves))))

      ["a" "3" "2"] {:from 4 :to 8} [{:from :entry :to 4} {:from 0 :to 4} {:from 4 :to 8}]
      ["5" "0" "1"] {:from :entry :to 4} [{:from :entry :to 4} {:from 0 :to 4}]))

  (testing "Empty moves list"
    (with-redefs [platform/read-single-key (constantly "1")
                  view/show-moves (constantly nil)]
      (is (nil? (cli/get-user-move []))))))

(deftest test-get-move
  (testing "Get move for player A"
    (with-redefs [cli/get-user-move (constantly {:from :entry :to 4})]
      (is (= {:from :entry :to 4} (cli/get-move :A [{:from :entry :to 4}] nil)))))

  (testing "Get move for player B (AI)"
    (with-redefs [game/select-move (constantly {:from 0 :to 4})]
      (is (= {:from 0 :to 4} (cli/get-move :B [{:from 0 :to 4}] nil)))))

  (testing "No moves available"
    (is (nil? (cli/get-move :A [] nil)))))

(deftest test-handle
  (testing "Handle roll dice state"
    (with-redefs [game/roll (constantly :rolled-game)]
      (is (= :rolled-game (cli/handle {:state :roll-dice})))))

  (testing "Handle choose action with no moves"
    (with-redefs [game/get-moves (constantly [])
                  view/show-no-moves (constantly nil)
                  platform/sleep (constantly nil)
                  game/choose-action (constantly :next-game)]
      (is (= :next-game (cli/handle {:state :choose-action, :current-player :A})))))

  (testing "Handle choose action for player A"
    (let [move {:from :entry :to 4}]
      (with-redefs [game/get-moves (constantly [move])
                    cli/get-move (constantly move)
                    game/choose-action (constantly :next-game)]
        (is (= :next-game (cli/handle {:state :choose-action, :current-player :A}))))))

  (testing "Handle choose action for player B (AI)"
    (let [move {:from :entry :to 4}]
      (with-redefs [game/get-moves (constantly [move])
                    cli/get-move (constantly move)
                    game/choose-action (constantly :next-game)
                    view/show-ai-move (constantly nil)
                    platform/sleep (constantly nil)]
        (is (= :next-game (cli/handle {:state :choose-action, :current-player :B}))))))

  (testing "Handle end game state"
    (with-redefs [view/show-winner (constantly nil)]
      (is (thrown-with-msg? #?(:clj Throwable :cljs :default)
                            #"Game over"
                            (cli/handle {:state :end-game, :current-player :A}))))))

(deftest test-play-game
  (testing "Play game until completion"
    (with-redefs [platform/clear-console (constantly nil)
                  util/hide-cursor (constantly nil)
                  view/show-welcome (constantly nil)
                  platform/readln (constantly nil)
                  game/start-game (constantly {:state :end-game, :current-player :A})
                  view/show-state (constantly nil)
                  platform/sleep (constantly nil)
                  cli/handle (fn [game] (throw (ex-info "Game over" {:reason :expected})))
                  util/show-cursor (constantly nil)]
      (is (nil? (cli/play-game)))))

  (testing "Play game with unexpected error"
    (with-redefs [platform/clear-console (constantly nil)
                  util/hide-cursor (constantly nil)
                  view/show-welcome (constantly nil)
                  platform/readln (constantly nil)
                  game/start-game (constantly {:state :roll-dice})
                  view/show-state (constantly nil)
                  platform/sleep (constantly nil)
                  cli/handle (fn [game] (throw (ex-info "Unexpected error" {:reason :unexpected})))
                  util/show-cursor (constantly nil)]
      (is (thrown-with-msg? #?(:clj Throwable :cljs :default)
                            #"Unexpected error" (cli/play-game))))))

(deftest test-main-function
  (testing "Main function execution"
    (with-redefs [cli/play-game (constantly nil)
                  view/show-goodbye (constantly nil)]
      (is (nil? (cli/-main))))))
