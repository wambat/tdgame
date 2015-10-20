(ns tdgame.components
  (:require [threedom.core :as td]
            [tdgame.assets :as ass])
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


(def peg-colors [ColorRGBA/Red ColorRGBA/Green ColorRGBA/Blue ColorRGBA/Yellow ColorRGBA/Pink])

(defn peg-component [{:keys [peg place name selected moving]} owner options]
  (reify
      td/IRender
    (render [this]
      [Node [(str "node_" name)]
       {:setLocalTranslation [[Vector3f [0 (+ (if (= moving peg)
                                              0.1
                                              0)
                                              (/ place 3)) 0]]]}
       (let [q (Quaternion.)
             color (nth peg-colors peg)]
         (.fromAngleAxis q (* 90 FastMath/DEG_TO_RAD) Vector3f/UNIT_X)
         [[Geometry [name]
           {:setMesh [[Torus [16 16 0.2 (* (+ peg 1) 0.1)]]]
            :setLocalRotation [q]
            :setLocalScale [(float (if (and (= (:num selected) peg)
                                            (= (:type selected) :peg))
                                     1.1
                                     1.0))]
            :setMaterial [[Material [ass/assetManager
                                     "Common/MatDefs/Misc/Unshaded.j3md"]
                           {:setTexture ["ColorMap" ^Texture (.loadTexture ass/assetManager "images/plast.jpg")]
                            :setColor ["Color" color]}]]}]])])))

(defn row-component [{:keys [row pegs selected moving]} owner options]
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
                                                  :moving moving
                                                  :selected selected} {}))) pegs)
        [[Node [(str "node_shaft_" row)]
          {:setLocalTranslation [[Vector3f [0 0.8 0]]]}
          (let [q (Quaternion.)]
            (.fromAngleAxis q (* 90 FastMath/DEG_TO_RAD) Vector3f/UNIT_X)
            [[Geometry [(str "shaft_" row)]
              {:setMesh [[Cylinder [16 16 0.15 2]]]
               :setLocalRotation [q]
               :setLocalScale [(float (if (= selected name)
                                        1.1
                                        1.0))]
               :setMaterial [[Material [ass/assetManager
                                        "Common/MatDefs/Misc/ColoredTextured.j3md"]
                              {:setColor ["Color" ColorRGBA/Brown]
                               :setTexture ["ColorMap" ^Texture (.loadTexture ass/assetManager "images/om.jpg")]}]]}]])]])])))

(defn root-component [data owner options]
  (reify
      td/IRender
    (render [this]
      [Node ["pivot"] {}
       [[Node ["board"] {}
         (map-indexed (fn [row pegs]
                 (td/build row-component {:pegs pegs :row row
                                          :moving (:moving data)
                                          :selected (:selected data)} {})) (:board data))]]
       [[DirectionalLight []
         {:setColor [ColorRGBA/White]
          :setDirection [[Vector3f [1 0 -2]
                          {:normalizeLocal []}]]}]]])))

