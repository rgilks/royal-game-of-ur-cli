(ns schema)

(def states
  #{:start-game
    :roll-dice
    :choose-action
    :enter-piece
    :move-piece
    :land-on-rosette
    :move-piece-off-board
    :switch-turns
    :end-game})

(def player
  [:map
   [:in-hand [:int {:min 0 :max 7}]]
   [:off-board [:int {:min 0 :max 7}]]])

(def board-position [:int {:min 0 :max 23}])

(def game
  [:map
   [:board [:vector {:min 24 :max 24} [:maybe [:enum :A :B]]]]
   [:players [:map-of [:enum :A :B] player]]
   [:current-player [:enum :A :B]]
   [:roll [:maybe [:int {:min 0 :max 4}]]]
   [:state (into [:enum] states)]
   [:selected-move
    [:maybe
     [:map
      [:from [:or board-position [:enum :entry]]]
      [:to [:or board-position [:enum :off-board]]]
      [:captured [:maybe [:enum :A :B]]]]]]])

(def config
  [:map
   [:debug boolean?]
   [:show boolean?]
   [:num-games pos-int?]
   [:delay-time pos-int?]
   [:long-wait pos-int?]
   [:short-wait pos-int?]
   [:parallel pos-int?]
   [:validate boolean?]
   [:strategies
    [:map-of keyword?
     [:map
      [:name keyword?]
      [:params {:optional true} [:map-of keyword? any?]]]]]])
