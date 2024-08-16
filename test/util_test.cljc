(ns util-test
  (:require [clojure.string :as str]
            [clojure.test :refer [are deftest is testing]]
            [util :as util]))

(deftest test-logging
  (testing "log function"
    (are [enabled? input expected]
         (let [_ (if enabled? (util/enable-logging!) (util/disable-logging!))
               output (with-out-str (util/log input))]
           (if expected
             (str/includes? output input)
             (empty? output)))

      true  "Test message" true
      false "Test message" false))

  (testing "enable-logging! and disable-logging! functions"
    (are [action expected]
         (do (action)
             (= @util/logging-enabled expected))

      util/enable-logging!  true
      util/disable-logging! false)))

(deftest test-multiple-args
  (testing "log function with multiple arguments"
    (util/enable-logging!)
    (is (str/includes?
         (with-out-str (util/log "Test" "multiple" "args"))
         "Test multiple args")
        "Should print all arguments")))

(deftest test-logging-state-persistence
  (testing "logging state persists across function calls"
    (are [setup-fn expected]
         (let [_ (setup-fn)
               output (with-out-str
                        (util/log "First log")
                        (util/log "Second log"))]
           (if expected
             (and (str/includes? output "First log")
                  (str/includes? output "Second log"))
             (empty? output)))

      util/enable-logging!  true
      util/disable-logging! false)))