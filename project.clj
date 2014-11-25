(defproject qdn "0.1.1"
  :description "QML in EDN; Qt for ClojureScript!"
  :url "https://github.com/nilern/qdn"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["target/clj/classes" "target/cljs/classes"]

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [nilern.scriptjure "0.1.27"]]

  :profiles {:dev {:source-paths ["src" "test"]
                   :plugins [[com.keminglabs/cljx "0.4.0"
                              :exclusions [org.clojure/clojure]]]
                   :cljx {:builds [{:source-paths ["src"]
                                    :output-path "target/clj/classes"
                                    :rules :clj}

                                   {:source-paths ["src"]
                                    :output-path "target/cljs/classes"
                                    :rules :cljs}]}
                   :hooks [cljx.hooks]
                   :repl-options {:nrepl-middleware
                                  [cljx.repl-middleware/wrap-cljx]}}})
