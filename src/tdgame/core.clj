(ns tdgame.core
  (:require [threedom.core :as td])
  (:import [com.jme3 app.SimpleApplication
            material.Material
            material.RenderState
            material.RenderState$BlendMode
            light.DirectionalLight
            scene.Geometry
            system.AppSettings
            system.JmeSystem
            util.SkyFactory
            renderer.queue.RenderQueue$Bucket
            scene.shape.Box
            scene.Node
            texture.Texture
            math.Vector3f
            math.ColorRGBA]))

(defonce ^:dynamic *app-settings* (doto (AppSettings. true)
                                    (.setFullscreen false)
                                    (.setTitle "tdgame")))

(defonce desktop-cfg (.getResource (.getContextClassLoader (Thread/currentThread))
                                   "com/jme3/asset/Desktop.cfg"))

(defonce assetManager (JmeSystem/newAssetManager desktop-cfg))

(def app-state (atom (for [x (range -2 0)
                           y (range -2 0)
                           z (range -2 0)]
                       [:box x y z])))

(defn clear-stage [root]
  (.clear (.getWorldLightList root))
  (.clear (.getLocalLightList root))
  (.detachAllChildren root))

(defn root-component [data owner options]
  (reify
    td/IRender
    (render [this]
      [[Node ["pivot"] {}
        (mapv (fn [[type x y z]]
                [Geometry [(str "Box_" x "_" y "_" z) ]
                 {:setMesh [[Box [0.2 0.2 0.2]]]
                  :setLocalTranslation [[Vector3f [x y z]]]
                  :setMaterial [[Material [assetManager
                                           "Common/MatDefs/Misc/Unshaded.j3md"]
                                 {:setColor ["Color" ColorRGBA/White]
                                  :setTexture ["ColorMap" ^Texture (.loadTexture assetManager "images/om.jpg")]}]]}]) data)]
       [DirectionalLight []
        {:setColor [ColorRGBA/White]
         :setDirection [[Vector3f [1 0 -2]
                         {:normalizeLocal []}]]}]]
      )))

(defn init [app]
  (let [l1 (DirectionalLight.)
        ;;pivot (Node. "pivot")
        root (.getRootNode app)
        ;;cinematic (Cinematic. root)
        ]
    (clear-stage root)
    (td/root root-component app-state {:target root})
    ;;(.attach (.getStateManager app) cinematic)
    (.setColor l1 (ColorRGBA/White))
    (.addLight root l1)
    ;;(.setDirection l1 (.normalizeLocal (Vector3f. 1 0 -2)))
    ;;(set-camera (.getCamera app))
    ;;(actions/set-bindings (.getInputManager app))
    ;;(state/process-op (first state/example-state) pivot assetManager cinematic)
    ;;(state/process-op (second state/example-state) pivot assetManager cinematic)

    (comment doto root
      (.attachChild (make-sky))
      (.addLight l1)
      (.attachChild pivot))
    ;;(.rotate pivot 0.4 0.4 0)
    ))

(def app (proxy [SimpleApplication] []
           (simpleInitApp []
             (org.lwjgl.input.Mouse/setGrabbed false)
             (init this)
             )

           (simpleUpdate [tpf]
             (td/process-state-updates this tpf)

             ;;(game/update this tpf)
             )))

(defn launch-3d-app []
  (doto app
    (.setShowSettings false)
    (.setPauseOnLostFocus false)
    (.setSettings *app-settings*)
    (.start)))

(defn -main [& args]
  ;;(launch-3d-app)
  (launch-3d-app)
  )

(defn up1 []

  (reset! app-state (for [x (range 0 2)
                          y (range 0 2)
                          z (range 0 2)]
                      [:box x y z]))
  ) 

(defn up2 []

  (reset! app-state (for [x (range -2 2)
                          y (range -2 2)
                          z (range -2 2)]
                      [:box x y z]))
  ) 
