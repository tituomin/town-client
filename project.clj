(defproject town-client "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2202"]
                 [prismatic/dommy "0.1.2"]
                 [com.cemerick/url "0.1.1"]
                 [org.clojure/core.async "0.1.298.0-2a82a1-alpha"]
                 [camel-snake-kebab "0.1.5"]
                 [ring "1.2.1"]]
  :plugins [[lein-cljsbuild "1.0.3"]
            [lein-ring "0.8.10"]]
  :jvm-opts ["-Xmx2g"]
  :hooks [leiningen.cljsbuild]
  :source-paths ["src/clj"]
  :cljsbuild { 
    :builds {
      :main {
        :source-paths ["src/cljs"]
        :compiler {:output-dir "resources/public/js"
                   :output-to "resources/public/js/town.js"
                   :optimizations :simple
                   :source-map "resources/public/js/town.js.map"
                   :externs ["js/google_maps_api_v3.js"]
                   }
        :jar true}}}
  :main town-client.server
  :ring {:handler town-client.server/app})

