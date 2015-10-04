(defproject com.chiefcoder/tdgame "0.1.0-SNAPSHOT"
  :description "Study for game"
  :jvm-opts ["-Xmx512m"]
  :url ""
  :license {:name ""
            :url ""}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [com.chiefcoder/threedom "0.1.1-SNAPSHOT"]
                 [org.clojars.ndepalma/jme-game-engine "3.0"]]
  :repl-options {:init-ns tdgame.core})
