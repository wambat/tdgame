(ns tdgame.core
  (:require [threedom.core :as td]
            [clojure.core.async :refer [<!! <! chan go] :as async]
            [tdgame.app :as a]))

(defonce event-chan (chan))

(def initial-board-state [[3 2 1 0]
                         []
                         []])
(def win-game-state (reverse initial-board-state))

(def app-state (atom {:board initial-board-state}))


(defn valid-move? [board peg to]
  (clojure.pprint/pprint [board peg to])
  (and (some #{peg} (map last board))
       (or (empty? (get board to))
           (< peg (last (get board to))))))

(defn move-peg [board peg to]
  (let [s (.indexOf (map last board) peg)]
    (-> board
        (update s pop)
        (update to conj peg))))

(defn on-moved [peg to]
  (clojure.pprint/pprint "moving")
  (if (valid-move? (:board @app-state) peg to)
    (swap! app-state update :board move-peg peg to))
  (if (= win-game-state
         (:board @app-state))
    (swap! app-state assoc :board initial-board-state)))


(defmulti on-event (fn [event]
                     (:event event)))
(defmethod on-event :click [event]
  (clojure.pprint/pprint "click")
  (clojure.pprint/pprint @app-state)
  (cond
    (and (= (get-in @app-state [:selected :type])
            :shaft)
         (:moving @app-state))
    (on-moved (:moving @app-state)
              (get-in @app-state [:selected :num]))
    (= (get-in @app-state [:selected :type])
       :peg)
    (swap! app-state assoc :moving (get-in @app-state [:selected :num]))))

(defmethod on-event :selected [event]
  (if-not (= (:value event)
             (:selected @app-state))
    (swap! app-state assoc :selected (:value event))))

(defmethod on-event :deselected [event]
  (if
      (:selected @app-state)
    (swap! app-state dissoc :selected)))

(defn on-click [{:keys [value event] :as sel}]
  (if (= event :selected)
    (cond
      (= (:type value)
         :peg)
      (swap! app-state assoc :moving (:num value))
      (and (= (:type value)
              :shaft)
           (:moving @app-state))
      (on-moved (:moving @app-state)
                (:num value)))
    (swap! app-state dissoc :moving)))

(go
  (loop []
    (let [e (<! event-chan)]
      (on-event e)

      ;; (comment
      ;;   (swap! app-state assoc :selected (.getName selected-geom))
      ;;   (if (:selected @app-state)
      ;;     (swap! app-state dissoc :selected)))
      (recur))))

(defn -main [& args]
  (a/launch-3d-app event-chan app-state))
