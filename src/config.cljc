(ns config)

(def board
  {:size 24
   :rosettes #{0 6 11 16 22}
   :paths {:A [3 2 1 0 8 9 10 11 12 13 14 15 7 6]
           :B [19 18 17 16 8 9 10 11 12 13 14 15 23 22]}
   :exclude #{4 5 20 21}})
