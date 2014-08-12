(defproject town-client "0.81-SNAPSHOT"
  :description "A city planning infographic page."
  :url "http://dev.hel.fi/kenenkaupunki/"

  :dependencies
  [[org.clojure/clojure "1.6.0"]
   [org.clojure/clojurescript "0.0-2311"]
   [prismatic/dommy "0.1.2"]
   [com.cemerick/url "0.1.1"]
   [org.clojure/core.async "0.1.303.0-886421-alpha"]
   [camel-snake-kebab "0.1.5"]
   [ring "1.2.1"]
   [compojure "1.1.8"]
   [hiccup "1.0.5"]
   [reagent "0.4.2"]
   [kioo "0.4.1-SNAPSHOT"]]

  :plugins
  [[lein-cljsbuild "1.0.3"]
   [lein-ring "0.8.10"]]

  :repl-options
  {:init-ns town-client.server
   :timeout 120000}

  :jvm-opts ["-Xmx2g"]
  :hooks [leiningen.cljsbuild]
  :main town-client.server
  :ring {:handler town-client.server/app}
  :source-paths ["src/clj" "src/cljs"]

  :cljsbuild { 
    :builds {

      :main {
        :source-paths ["src/cljs" "src/clj"]
        :compiler {:output-dir "resources/public/js"
                   :output-to "resources/public/js/town.js"
                   :optimizations :whitespace
                   :source-map "resources/public/js/town.js.map"
                   :externs ["js/google_maps_api_v3.js"]
                   }
        :jar true}}})
