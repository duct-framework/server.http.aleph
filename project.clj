(defproject duct/server.http.aleph "0.2.0"
  :description "Integrant methods for running an Aleph web server"
  :url "https://github.com/duct-framework/server.http.aleph"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [duct/core "0.8.0"]
                 [aleph "0.4.7-alpha7"]
                 [duct/logger "0.3.0"]
                 [integrant "0.8.0"]]
  :profiles
  {:dev {:dependencies [[clj-http "3.7.0"]]}})
