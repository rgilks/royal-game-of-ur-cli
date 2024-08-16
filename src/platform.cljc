(ns platform
  #?(:clj
     (:require
      ;; [clojure.java.io :as io]
      )
     :cljs
     (:require
      ;; ["fs" :as fs]
      ["readline-sync" :as readline-sync])))

(def err
  #?(:clj Exception :cljs js/Error))

(defn parse-int [s]
  (if (and s (re-matches #"\d+" (str s)))
    #?(:clj (Integer/parseInt s)
       :cljs (js/parseInt s))
    0))

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
     (let [console (System/console)]
       (if console
         (str (.readPassword console "" (into-array Object [])))
         (str (first (read-line)))))
     :cljs
     (.keyIn readline-sync "" #js {:hideEchoBack true :mask ""})))

;; (defn load-file! [filepath]
;;   #?(:clj  (slurp (io/resource filepath))
;;      :cljs (.readFileSync fs filepath "utf8")))

;; (defn current-time-ms []
;;   #?(:clj  (System/currentTimeMillis)
;;      :cljs (.getTime (js/Date.))))

;; (defn exit [status]
;;   #?(:clj  (System/exit status)
;;      :cljs (.exit js/process status)))
