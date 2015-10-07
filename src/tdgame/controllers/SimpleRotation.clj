(ns tdgame.controllers.SimpleRotation
  (:gen-class
   :extends com.jme3.scene.control.AbstractControl
   :main false)
  (:import [com.jme3 scene.control.AbstractControl
            scene.control.Control
            scene.Spatial]))


(defn -controlUpdate [this tpf]
  (.rotate (.getSpatial this) 0 0 (* tpf 1)))

(defn -controlRender [this rm vp]
  )

(defn -cloneForSpatial [this spatial]
  (let [s (tdgame.controllers.SimpleRotation. )]
    (.setSpatial s spatial)
    s))

(compile 'tdgame.controllers.SimpleRotation)
