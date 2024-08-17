(ns util)

(def show-enabled (atom true))

(defn show [& args]
  (when @show-enabled
    (apply println " " args)))

(defn enable-show! []
  (reset! show-enabled true))

(defn disable-show! []
  (reset! show-enabled false))
