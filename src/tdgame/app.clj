(ns tdgame.app
  (:require [tdgame.components :as components]
            [clojure.core.async :refer [>!!] :as async]
            [tdgame.assets :as ass]
            [threedom.core :as td])
  (:import [com.jme3 app.SimpleApplication
            material.Material
            material.RenderState
            material.RenderState$BlendMode
            light.DirectionalLight
            scene.Geometry
            util.SkyFactory
            renderer.queue.RenderQueue$Bucket
            scene.shape.Box
            scene.shape.Torus
            scene.shape.Cylinder
            scene.Node
            texture.Texture
            math.Vector3f
            math.Ray
            math.Quaternion
            math.FastMath
            collision.CollisionResults
            input.controls.MouseButtonTrigger
            input.controls.ActionListener
            input.controls.Trigger
            input.MouseInput
            math.ColorRGBA]
           tdgame.controllers.SimpleRotation))

(defn clear-stage [root]
  (.clear (.getWorldLightList root))
  (.clear (.getLocalLightList root))
  (.detachAllChildren root))

(defn check-mouse-collisions [app node]
  (let [origin (.getWorldCoordinates (.getCamera app) (.getCursorPosition (.getInputManager app)) 0.0)
        direction (.getWorldCoordinates (.getCamera app) (.getCursorPosition (.getInputManager app)) 0.3)]
    (.normalizeLocal (.subtractLocal direction origin))
    (let [ray (Ray. origin direction)
          results (CollisionResults.)]
      (.collideWith node ray results)
      (if (> (.size results) 0)
        (let [closest (.getClosestCollision results)
              g (.getGeometry closest)]
          g)
        nil))))

(defn camera! [cam]
  (let [q (Quaternion. -0.0023028406 0.9786839 -0.20506527 -0.010989709)]
    (.setLocation cam (Vector3f. 0.9031008 2.8518562 4.7388806))
    (.setRotation cam q)))

(defn- selection-info [s]
  (if s
    (let [[_ type num] (re-find #"([a-zA-Z\-]*)_(\d+)" s)]
      {:original s
       :type (keyword type)
       :num (Integer. num)})))

(defn init [app event-chan app-state]
  (let [l1 (DirectionalLight.)
        root (.getRootNode app)]
    (clear-stage root)
    (td/root components/root-component app-state {:target root})
    (.setColor l1 (ColorRGBA/White))
    (.addLight root l1)
    (.addMapping  (.getInputManager app) "Click"
                  (into-array Trigger [(MouseButtonTrigger. MouseInput/BUTTON_LEFT)]))
    (.addListener (.getInputManager app)
                  (reify
                      ActionListener
                    (onAction [this name pressed? tpf]
                      (if (and (.equals name "Click")
                               pressed?)
                        (>!! event-chan {:event :click
                                         :value pressed?}))))
                  (into-array String ["Click"]))))

(defn make-app [event-chan app-state]
  (proxy [SimpleApplication] []
    (simpleInitApp []
      (org.lwjgl.input.Mouse/setGrabbed false)

      (camera! (.getCamera this))
      (.setEnabled (.getFlyByCamera this) false)
      (.setMouseCursor (.getInputManager this)
                       (.loadAsset ass/assetManager "cursors/monkeyani.ani"))
      (init this event-chan app-state))

    (simpleUpdate [tpf]
      (if-let [selected-geom (check-mouse-collisions this (.getRootNode this))]
        (>!! event-chan {:event :selected
                         :value (selection-info (.getName selected-geom))})
        (>!! event-chan {:event :deselected}))
      (td/process-state-updates this tpf))))

(defn launch-3d-app [event-chan app-state]
  (doto (make-app event-chan app-state)
    (.setShowSettings false)
    (.setPauseOnLostFocus false)
    (.setSettings ass/*app-settings*)
    (.start)))
