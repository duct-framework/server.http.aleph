(ns duct.server.http.aleph-test
  (:import java.net.ConnectException)
  (:require [clj-http.client :as http]
            [clojure.test :refer :all]
            [duct.logger :as logger]
            [duct.server.http.aleph :as aleph]
            [integrant.core :as ig]))

(defrecord TestLogger [logs]
  logger/Logger
  (-log [_ level ns-str file line event data]
    (swap! logs conj [event data])))

(deftest key-test
  (is (isa? :duct.server.http/aleph :duct.server/http)))

(deftest init-and-halt-test
  (let [response {:status 200 :headers {} :body "test"}
        logger   (->TestLogger (atom []))
        handler  (constantly response)
        config   {:duct.server.http/aleph {:port 3400, :handler handler, :logger logger}}]

    (testing "server starts"
      (let [system (ig/init config)]
        (try
          (let [response (http/get "http://127.0.0.1:3400/")]
            (is (= (:status response) 200))
            (is (= (:body response) "test")))
          (finally
            (ig/halt! system)))))

    (testing "server stops"
      (is (thrown? ConnectException (http/get "http://127.0.0.1:3400/"))))

    (testing "start and stop were logged"
      (is (= @(:logs logger)
             [[::aleph/starting-server {:port 3400}]
              [::aleph/stopping-server nil]])))

    (testing "halt is idempotent"
      (let [system (ig/init config)]
        (ig/halt! system)
        (ig/halt! system)
        (is (thrown? ConnectException (http/get "http://127.0.0.1:3400/")))))))
