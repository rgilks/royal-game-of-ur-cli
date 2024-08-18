(ns platform
  #?(:clj (:import
           [org.jline.terminal TerminalBuilder]))
  #?(:cljs (:require

            ["readline-sync" :as readline-sync])))

(defn get-core-count []
  #?(:clj (.availableProcessors (Runtime/getRuntime))
     :cljs (count (.cpus (js/require "os")))))

(defn parse-int [s]
  (if (and s (re-matches #"\d+" (str s)))
    #?(:clj (Integer/parseInt s)
       :cljs (js/parseInt s))
    0))

(defn parse-bool [s]
  (= s "true"))

(defn readln []
  #?(:clj  (read-line)
     :cljs (.question readline-sync "")))

(defn sleep [ms]
  #?(:clj  (Thread/sleep ms)
     :cljs (let [start (js/Date.now)]
             (while (< (- (js/Date.now) start) ms)))))

(defn clear-console []
  #?(:clj  (print (str (char 27) "[2J" (char 27) "[;H"))
     :cljs (do
             (.clear js/console)
             (.write js/process.stdout "\u001b[2J\u001b[0;0H"))))

(defn read-single-key []
  #?(:clj
     (let [terminal (.. (TerminalBuilder/builder)
                        (system true)
                        (build))
           reader (.reader terminal)]
       (try
         (.enterRawMode terminal)
         (str (char (.read reader)))
         (finally
           (.close reader)
           (.close terminal)))))

  #?(:cljs
     (.keyIn readline-sync "" #js {:hideEchoBack true :mask ""})))
