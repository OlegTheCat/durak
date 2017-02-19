(defproject durak "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.8.51"]
                 [rum "0.10.8"]
                 [jarohen/chord "0.7.0"]
                 [http-kit "2.2.0"]
                 [compojure "1.5.2"]
                 [org.clojure/core.async "0.2.395"]
                 [com.taoensso/timbre "4.8.0"]
                 [philoskim/debux "0.2.1"]]

  :plugins [[lein-cljsbuild "1.1.3"]
            [lein-figwheel "0.5.4-7"]]

  :clean-targets ^{:protect false} [:target-path "resources/public/js/compiled" "www"]

  :profiles {:dev {:dependencies [[com.cemerick/piggieback "0.2.1"]
                                  [figwheel-sidecar "0.5.4-7"]
                                  [cider/cider-nrepl "0.13.0"]]
                   :source-paths ["src" "dev_src"]
                   :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}}

  :main durak.app

  :cljsbuild {
              :builds [{:id "dev"
                        :source-paths ["src" "dev_src"]
                        :figwheel {:websocket-host "localhost"
                                   :on-jsload "durak.client.dev/remount"}
                        :compiler {:output-to "resources/public/js/compiled/durak.js"
                                   :output-dir "resources/public/js/compiled/out"
                                   :optimizations :none
                                   :main durak.client.dev
                                   :asset-path "js/compiled/out"
                                   :source-map true
                                   :source-map-timestamp true
                                   :cache-analysis true
                                   :warnings {:single-segment-namespace false}}}]}


  :figwheel {
             :http-server-root "public" ;; default and assumes "resources"
             :server-port 3749
             :nrepl-port 7886
             :nrepl-middleware ["cider.nrepl/cider-middleware"
                                "cemerick.piggieback/wrap-cljs-repl"]})
