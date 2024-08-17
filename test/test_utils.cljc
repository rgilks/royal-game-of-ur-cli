(ns test-utils)

(defmacro thrown-with-msg? [error-type regex expr]
  `(try
     ~expr
     false
     (catch ~error-type e#
       (boolean (re-find ~regex (ex-message e#))))))