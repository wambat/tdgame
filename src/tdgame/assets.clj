(ns tdgame.assets
  (:import [com.jme3
            system.JmeSystem
            system.AppSettings]))

(defonce ^:dynamic *app-settings* (doto (AppSettings. true)
                                    (.setFullscreen false)
                                    (.setTitle "tdgame")))
(defonce desktop-cfg (.getResource (.getContextClassLoader (Thread/currentThread))
                                   "com/jme3/asset/Desktop.cfg"))
(defonce assetManager (JmeSystem/newAssetManager desktop-cfg))

