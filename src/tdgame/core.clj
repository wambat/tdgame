(ns tdgame.core
  (:require [threedom.core :as td]
            )
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
            scene.shape.Torus
            scene.Node
            texture.Texture
            math.Vector3f
            math.Ray
            math.Quaternion
            math.FastMath
            collision.CollisionResults
            math.ColorRGBA]
           tdgame.controllers.SimpleRotation))

(defonce ^:dynamic *app-settings* (doto (AppSettings. true)
                                    (.setFullscreen false)
                                    (.setTitle "tdgame")))

(defonce desktop-cfg (.getResource (.getContextClassLoader (Thread/currentThread))
                                   "com/jme3/asset/Desktop.cfg"))

(defonce assetManager (JmeSystem/newAssetManager desktop-cfg))

;; (def app-state (atom {:geom (for [x (range -2 0)
;;                                   y (range -2 0)
;;                                   z (range -2 0)]
;;                               [:box x y z])}))

(def app-state (atom {:geom [[:box 1 1 1]
                             [:box 2 1 1]]}))

(defn clear-stage [root]
  (.clear (.getWorldLightList root))
  (.clear (.getLocalLightList root))
  (.detachAllChildren root))

(defn box-component [{:keys [x y z name selected]} owner options]
  (reify
    td/IRender
    (render [this]
      [Node [(str "Node_" x "_" y "_" z)]
       {:addControl [[SimpleRotation []
                      {:setSpeed [x]}]]
        :setLocalTranslation [[Vector3f [x y z]]]}
       (let [q (Quaternion.)]
         (.fromAngleAxis q (* 90 FastMath/DEG_TO_RAD) Vector3f/UNIT_X)
         [[Geometry [name]
           {:setMesh [[Torus [16 16 0.2 0.3]]]
            :setLocalRotation [q]
            :setLocalScale [(float (if selected 2.0
                                       1.0))]
            :setMaterial [[Material [assetManager
                                     "Common/MatDefs/Misc/Unshaded.j3md"]
                           {:setColor ["Color" ColorRGBA/White]
                            :setTexture ["ColorMap" ^Texture (.loadTexture assetManager "images/om.jpg")]}]]}]])])))

(defn root-component [data owner options]
  (reify
    td/IRender
    (render [this]
      [Node ["pivot"] {}
       (concat
        (mapv (fn [[type x y z]]
                (let [name (str "item_" x "_" y "_" z)
                      selected (= name (:selected data))]
                  (td/build box-component {:x x :y y :z z
                                           :name name
                                           :selected selected} {}))) (:geom data))
        [[DirectionalLight []
          {:setColor [ColorRGBA/White]
           :setDirection [[Vector3f [1 0 -2]
                           {:normalizeLocal []}]]}]])])))

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
          ;;(clojure.pprint/pprint (.getName g))
          g)
        nil))))

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
    ;;(.addControl root (SimpleRotation.))
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

             (.setEnabled (.getFlyByCamera this) false)
             (.setMouseCursor (.getInputManager this)
                              (.loadAsset assetManager "cursors/monkeyani.ani"))
             (init this)
             )

           (simpleUpdate [tpf]
             (if-let [selected-geom (check-mouse-collisions this (.getRootNode this))]
               (swap! app-state assoc :selected (.getName selected-geom))
               (if (:selected @app-state)
                 (swap! app-state dissoc :selected)))
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

(defn up0 []

  (swap! app-state assoc :geom [])
  )

(defn up1 []
  (swap! app-state assoc :geom 
         [[:box 1 1 1]
          [:box 2 1 1]]))

(defn up2 []
  (swap! app-state assoc :geom 
         [[:box 1 1 1]
          [:box -1 1 1]]))

(defn up9 []

  (swap! app-state assoc :geom (for [x (range -2 2)
                                     y (range -2 2)
                                     z (range -2 2)]
                                 [:box x y z]))
  )
