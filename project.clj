(defproject duct/server.http.aleph "0.1.1"
  :description "Integrant methods for running an Aleph web server"
  :url "https://github.com/duct-framework/server.http.aleph"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha17"]
                 [duct/core "0.6.1"]
                 [aleph "0.4.3"]
                 [duct/logger "0.1.1"]
                 [integrant "0.6.1"]]
  :profiles
  {:dev {:dependencies [[clj-http "2.1.0"]]}})
