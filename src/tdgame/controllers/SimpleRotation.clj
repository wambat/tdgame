(ns tdgame.controllers.SimpleRotation
  (:gen-class
   :extends com.jme3.scene.control.AbstractControl
   :init init
   :state state
   :methods [[setSpeed [java.lang.Long] void]]
   :main false)
  (:import [com.jme3 scene.control.AbstractControl
            scene.control.Control
            scene.Spatial]))


(defn -init []
  [[] (atom {:speed 2})])

(defn -controlUpdate [this tpf]
  (let [speed (:speed @(.state this))]
    (.rotate (.getSpatial this) 0 0 (* tpf speed))))

(defn -setSpeed [this speed]
  (swap! (.state this) assoc :speed speed))

(defn -controlRender [this rm vp]
  )

(defn -cloneForSpatial [this spatial]
  (let [s (tdgame.controllers.SimpleRotation.)
        speed (:speed @(.state this))]
    (.setSpeed s speed)
    (.setSpatial s spatial)
    s))

(compile 'tdgame.controllers.SimpleRotation)
