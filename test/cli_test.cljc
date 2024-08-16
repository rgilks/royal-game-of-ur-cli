(ns cli-test
  (:require [cli]
            [clojure.test :refer [deftest is]]
            [platform]))

(deftest test-get-user-move
  (with-redefs [platform/readln (constantly "1")]
    (let [moves [{:from :entry :to 4} {:from 0 :to 4}]]
      (is (= (first moves) (cli/get-user-move moves))))))
