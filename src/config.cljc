(ns config)

(def board
  {:size 24
   :rosettes #{0 6 11 16 22}
   :paths {:A [3 2 1 0 8 9 10 11 12 13 14 15 7 6]
           :B [19 18 17 16 8 9 10 11 12 13 14 15 23 22]}
   :exclude #{4 5 20 21}})

(def game
  (atom {:debug false
         :show false
         :validate false
         :icons :fancy
         :winning-pieces 7
         :num-games 10
         :delay-time 30
         :long-wait 1000
         :short-wait 300
         :parallel 8
         :strategies {:A {:name :mcts
                          :params {:iterations 100
                                   :exploration 1.41}}
                      :B {:name :minimax
                          :params {:depth 3
                                   :damp 0.3}}}}))

