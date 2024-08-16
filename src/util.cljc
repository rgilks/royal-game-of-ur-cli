(ns util)

(def logging-enabled (atom true))

(defn log [& args]
  (when @logging-enabled
    (apply println args)))

(defn enable-logging! []
  (reset! logging-enabled true))

(defn disable-logging! []
  (reset! logging-enabled false))
