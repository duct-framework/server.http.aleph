(ns duct.server.http.aleph-test
  (:import java.net.ConnectException)
  (:require [clj-http.client :as http]
            [clojure.test :refer :all]
            [duct.server.http.aleph :as aleph]
            [integrant.core :as ig]))

(deftest init-and-halt-test
  (let [response {:status 200 :headers {} :body "test"}
        handler  (constantly response)
        config   {:duct.server.http/aleph {:port 3400, :handler handler}}]

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

    (testing "halt is idempotent"
      (let [system (ig/init config)]
        (ig/halt! system)
        (ig/halt! system)
        (is (thrown? ConnectException (http/get "http://127.0.0.1:3400/")))))))
