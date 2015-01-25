(defproject qdn "0.1.2"
  :description "QML in EDN; Qt for ClojureScript!"
  :url "https://github.com/nilern/qdn"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["target/clj/classes" "target/cljs/classes"
                 "target/clj/test" "target/cljs/test" "src"]

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [nilern.scriptjure "0.1.27"]]

  :profiles {:dev {:source-paths ["src" "test"]
                   :dependencies [[org.clojure/clojurescript "0.0-2371"]]
                   :plugins [[com.keminglabs/cljx "0.4.0"
                              :exclusions [org.clojure/clojure]]
                             [lein-cljsbuild "1.0.3"]]
                   :cljsbuild {:builds
                               [{:source-paths ["src"]
                                 :compiler {:output-to "qt/cljs.js"
                                            :optimizations :simple}}]}
                   :cljx {:builds [{:source-paths ["src"]
                                    :output-path "target/clj/classes"
                                    :rules :clj}

                                   {:source-paths ["src"]
                                    :output-path "target/cljs/classes"
                                    :rules :cljs}

                                   {:source-paths ["test"]
                                    :output-path "target/clj/test"
                                    :rules :clj}

                                   {:source-paths ["test"]
                                    :output-path "target/cljs/test"
                                    :rules :cljs}]}
                   :hooks [cljx.hooks]
                   :repl-options {:nrepl-middleware
                                  [cljx.repl-middleware/wrap-cljx]}}})
