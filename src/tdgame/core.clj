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

(defonce ^:dynamic *app-settings* (doto (AppSettings. true)
                                    (.setFullscreen false)
                                    (.setTitle "tdgame")))

(defonce desktop-cfg (.getResource (.getContextClassLoader (Thread/currentThread))
                                   "com/jme3/asset/Desktop.cfg"))

(defonce assetManager (JmeSystem/newAssetManager desktop-cfg))

(def initial-board-state [[3 2 1 0]
                         []
                         []])
(def win-game-state (reverse initial-board-state))
(def peg-colors [ColorRGBA/Red ColorRGBA/Green ColorRGBA/Blue ColorRGBA/Yellow ColorRGBA/Pink])

(def app-state (atom {:board initial-board-state}))
(defn clear-stage [root]
  (.clear (.getWorldLightList root))
  (.clear (.getLocalLightList root))
  (.detachAllChildren root))

(defn peg-component [{:keys [peg place name selected]} owner options]
  (reify
      td/IRender
    (render [this]
      [Node [(str "node_" name)]
       {:setLocalTranslation [[Vector3f [0 (/ place 3) 0]]]}
       (let [q (Quaternion.)
             color (nth peg-colors peg)]
         (.fromAngleAxis q (* 90 FastMath/DEG_TO_RAD) Vector3f/UNIT_X)
         [[Geometry [name]
           {:setMesh [[Torus [16 16 0.2 (* (+ peg 1) 0.1)]]]
            :setLocalRotation [q]
            :setLocalScale [(float (if (= selected name)
                                     1.1
                                     1.0))]
            :setMaterial [[Material [assetManager
                                     "Common/MatDefs/Misc/Unshaded.j3md"]
                           {
                            :setTexture ["ColorMap" ^Texture (.loadTexture assetManager "images/plast.jpg")]
                            ;;:setColor ["Diffuse" color]
                            :setColor ["Color" color]
                            }]]}]])])))

(defn row-component [{:keys [row pegs selected]} owner options]
  (reify
      td/IRender
    (render [this]
      [Node [(str "row_" row)]
       {:setLocalTranslation [[Vector3f [row 0 0]]]}
       (concat
        (map-indexed (fn [place peg]
                       (let [name (str "peg_" peg)]
                         (td/build peg-component {:peg peg :place place
                                                  :name name
                                                  :selected selected} {}))) pegs)
        [[Node [(str "node_shaft_" row)]
          {:setLocalTranslation [[Vector3f [0 0.8 0]]]}
          (let [q (Quaternion.)]
            (.fromAngleAxis q (* 90 FastMath/DEG_TO_RAD) Vector3f/UNIT_X)
            [[Geometry [(str "shaft_" row)]
              {:setMesh [[Cylinder [16 16 0.2 2]]]
               :setLocalRotation [q]
               :setLocalScale [(float (if (= selected name)
                                        1.1
                                        1.0))]
               :setMaterial [[Material [assetManager
                                        "Common/MatDefs/Misc/ColoredTextured.j3md"]
                              {:setColor ["Color" ColorRGBA/Brown]
                               :setTexture ["ColorMap" ^Texture (.loadTexture assetManager "images/om.jpg")]}]]}]])]])])))

(defn root-component [data owner options]
  (reify
      td/IRender
    (render [this]
      [Node ["pivot"] {}
       [[Node ["board"] {}
         (map-indexed (fn [row pegs]
                 (td/build row-component {:pegs pegs :row row
                                          :selected (:selected data)} {})) (:board data))]]
       [[DirectionalLight []
         {:setColor [ColorRGBA/White]
          :setDirection [[Vector3f [1 0 -2]
                          {:normalizeLocal []}]]}]]])))

(def acs [[[4 3 2] [0] [1]]])

(defn valid-move? [board peg to]
  (clojure.pprint/pprint [board peg to])
  (and (some #{peg} (map last board))
       (or (empty? (get board to))
           (< peg (last (get board to))))))

(comment
  (valid-move? acs 0 1)
  )
(defn move-peg [board peg to]
  (let [s (.indexOf (map last board) peg)]
    (-> board
        (update s pop)
        (update to conj peg))))

(defn on-moved [peg to]
  ;;(swap! app-state dissoc :moving)
  (clojure.pprint/pprint "Moved")
  (clojure.pprint/pprint peg)
  (clojure.pprint/pprint to)
  (if (valid-move? (:board @app-state)
                   peg
                   to)
    (do
      (clojure.pprint/pprint "Valid")
      (swap! app-state update :board move-peg peg to))
    (clojure.pprint/pprint "Not Valid"))
  (if (= win-game-state
         (:board @app-state))
    (swap! app-state assoc :board initial-board-state)))


(defn- selection-info [s]
  (if s
    (let [[_ type num] (re-find #"([a-zA-Z\-]*)_(\d+)" s)]
      {:type (keyword type)
       :num (Integer. num)})))

(defn on-click []
  (if-let [sel (selection-info (:selected @app-state))] 
    (cond
      (= (:type sel)
         :peg)
      (swap! app-state assoc :moving (:selected @app-state))
      (and (= (:type sel)
              :shaft)
           (:moving @app-state))
      (on-moved (:num (selection-info (:moving @app-state)))
                (:num sel)))
    (swap! app-state dissoc :moving))

  (clojure.pprint/pprint "Click")
  (clojure.pprint/pprint @app-state))

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

(defn camera! [cam]
  (let [q (Quaternion. -0.0023028406 0.9786839 -0.20506527 -0.010989709)]
    (.setLocation cam (Vector3f. 1.5331008 2.8518562 4.7388806))
    (.setRotation cam q)))

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
    (.addMapping  (.getInputManager app) "Click"
                  (into-array Trigger [(MouseButtonTrigger. MouseInput/BUTTON_LEFT)]))
    (.addListener (.getInputManager app)
                  (reify
                      ActionListener
                    (onAction [this name pressed? tpf]
                      (if (and (.equals name "Click")
                               pressed?)
                        (do
                          (on-click)))))
                  (into-array String ["Click"]))
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

             (camera! (.getCamera this))
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
