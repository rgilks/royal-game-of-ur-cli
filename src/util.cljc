(ns util
  (:require [clojure.string :as str]))

(def print-line-enabled (atom true))

(defn print-line [& args]
  (when @print-line-enabled
    (apply println " " args)))

(defn enable-print-line! []
  (reset! print-line-enabled true))

(defn disable-print-line! []
  (reset! print-line-enabled false))

(def colors
  {:reset "[0m" :bold "[1m" :red "[31m" :green "[32m"
   :yellow "[33m" :blue "[34m" :cyan "[36m"})

(defn str
  "Enhanced string function that supports color formatting.
   If the first argument is a color keyword, it applies that color to the rest of the arguments.
   Otherwise, it behaves like the regular str function."
  [& args]
  (if (and (seq args) (keyword? (first args)) (contains? colors (first args)))
    (let [color (first args)
          texts (rest args)]
      (str/join
       [(str/join ["\u001b" (colors color)])
        (apply clojure.core/str texts)
        (str/join ["\u001b" (:reset colors)])]))
    (apply clojure.core/str args)))

(defn show [& args]
  (print-line (apply str args)))
