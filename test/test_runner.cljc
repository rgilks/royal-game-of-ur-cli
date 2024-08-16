(ns test-runner
  (:require [cljs.test :as test :refer-macros [run-tests] :refer [report]]
            [clojure.data :as data]
            [clojure.string :as str]
            [util-test]
            [cli-test]
            [sim-test]
            [state-machine-test]
            [util :refer [enable-logging! disable-logging!]]))

(def ^:dynamic *test-results* (atom {:pass 0 :fail 0 :error 0}))

(defn- print-diff [a b]
  (let [diff (data/diff a b)
        diff-a (first diff)
        diff-b (second diff)]
    (when (or diff-a diff-b)
      (println "Diff:")
      (when diff-a
        (println "  - Expected:" (str/replace (pr-str diff-a) #"\n" "\n    ")))
      (when diff-b
        (println "  + Actual:  " (str/replace (pr-str diff-b) #"\n" "\n    "))))))

(defmethod report [:cljs.test/default :pass] [m]
  (swap! *test-results* update :pass inc))

(defmethod report [:cljs.test/default :fail] [m]
  (swap! *test-results* update :fail inc)
  (let [{:keys [expected actual message]} m]
    (println (str "\n❌ FAIL in " (test/testing-vars-str m)))
    (when message
      (println message))
    (println "Expected:" (pr-str expected))
    (println "Actual:  " (pr-str actual))
    (print-diff expected actual)))

(defmethod report [:cljs.test/default :error] [m]
  (swap! *test-results* update :error inc)
  (let [{:keys [message expected actual]} m]
    (println (str "\n🚨 ERROR in " (test/testing-vars-str m)))
    (when message
      (println message))
    (println "Expected:" (pr-str expected))
    (println "Actual:" (pr-str actual))
    (when-let [cause (.-cause actual)]
      (println "Cause:" (pr-str cause)))))

(defmethod report [:cljs.test/default :summary] [m]
  (let [{:keys [pass fail error]} @*test-results*
        total (+ pass fail error)]
    (println "\n=== Test Summary ===")
    (println (str "Total tests: " total))
    (println (str "Passed:     " pass))
    (println (str "Failed:     " fail))
    (println (str "Errors:     " error))
    (if (pos? (+ fail error))
      (println "\n❌ SOME TESTS FAILED")
      (println "\n✅ ALL TESTS PASSED"))
    (println "=====================")))

(defn run-all-tests! []
  (reset! *test-results* {:pass 0 :fail 0 :error 0})
  ;; Disable logging for tests
  (disable-logging!)
  (run-tests
   'util-test
   'cli-test
   'sim-test
   'state-machine-test)
  ;; Enable logging after tests if needed
  (enable-logging!))

(defn -main []
  (run-all-tests!))
