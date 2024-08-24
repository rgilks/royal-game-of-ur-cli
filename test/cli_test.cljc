(ns cli-test
  (:require [cli]
            [clojure.test :refer [deftest is testing]]
            [config]))

(def test-config
  {:num-games 10
   :debug? false
   :show? false
   :parallel 8
   :validate? true
   :strategies {:A {:name :minimax :params {:depth 4}}
                :B {:name :minimax :params {:depth 4}}}})

(deftest test-parse-args
  (testing "parse-args function"
    (with-redefs [config/game (atom  test-config)]
      (cli/parse-args ["num-games=20"
                       "debug=true"
                       "show=true"
                       "parallel=4"
                       "validate=false"
                       "strategy-A=mcts"
                       "strategy-A-iterations=200"
                       "strategy-B=first-in-list"])
      (is (= {:num-games 20
              :debug? true
              :show? true
              :parallel 4
              :validate? false
              :strategies {:A {:name :mcts :params {:iterations 200}}
                           :B {:name :first-in-list :params {:depth 4}}}}
             @config/game)))))
