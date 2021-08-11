(ns duct.server.http.aleph-test
  (:require [aleph
             [flow :as flow]
             [netty :as netty]]
            [clj-http.client :as http]
            [clojure.spec.alpha :as s]
            [clojure.test :refer [deftest is testing assert-expr do-report]]
            [duct.core :as duct]
            [duct.logger :as logger]
            [duct.server.http.aleph :as aleph]
            [integrant.core :as ig])
  (:import [clojure.lang ExceptionInfo]
           java.net.ConnectException
           [java.net InetSocketAddress]
           [sun.security.provider.certpath SunCertPathBuilderException]))

(defrecord TestLogger [logs]
  logger/Logger
  (-log [_ _ _ _ _ _ event data]
    (swap! logs conj [event data])))

(duct/load-hierarchy)

(defmethod assert-expr 'thrown-on-path? [msg form]
  (let [selector (nth form 1)
        body     (nthnext form 2)]
    `(try
       ~@body
       (do-report {:type     :fail
                   :message  ~msg
                   :expected '~form
                   :actual   "Nothing was thrown"})
       (catch ExceptionInfo e#
         (let [path# (some-> (ex-data e#)
                             :explain
                             ::s/problems
                             first
                             :path)]
           (if (= path# ~selector)
             (do-report {:type     :pass
                         :message  ~msg
                         :expected '~form
                         :actual   path#})
             (do-report {:type     :fail
                         :message  ~msg
                         :expected '~form
                         :actual   path#}))))
       (catch Exception e#
         (do-report {:type     :fail
                     :message  ~msg
                     :expected '~form
                     :actual   e#})))))

(deftest pre-init-spec-test
  (let [logger   (->TestLogger (atom []))
        response {:status 200
                  :body   "test"}
        config   {:duct.server.http/aleph {:port                    3400
                                           :executor                (flow/fixed-thread-executor 2)
                                           :shutdown-executor?      true
                                           :request-buffer-size     8196
                                           :raw-stream?             false
                                           :max-initial-line-length 8196
                                           :max-header-size         8196
                                           :max-chunk-size          8196
                                           :epoll?                  false
                                           :compression?            false
                                           :idle-timeout            0
                                           :logger                  logger
                                           :handler                 (constantly response)}}]
    (testing "server starts"
      (let [system (ig/init config)]
        (try
          (let [{:keys [status body]} (http/get "http://127.0.0.1:3400/")]
            (is (= 200 status))
            (is (= "test" body)))
          (finally
            (ig/halt! system)))))
    (testing "ssl context"
      (testing "servers starts"
        (let [system (ig/init (assoc-in config [:duct.server.http/aleph :ssl-context]
                                        (netty/self-signed-ssl-context)))]
          (try
            (is (thrown? SunCertPathBuilderException
                         (http/get "https://127.0.0.1:3400")))
            (finally
              (ig/halt! system)))))
      (testing "not valid cert"
        (is (thrown-on-path?
             [:ssl-context]
             (-> config
                 (assoc-in [:duct.server.http/aleph :ssl-context]
                           "Not a ssl cert.")
                 (ig/init))))))
    (testing "socket-address"
      (let [config (update config :duct.server.http/aleph
                           dissoc :port)]
        (testing "server starts"
          (let [system (-> config
                           (assoc-in [:duct.server.http/aleph :socket-address]
                                     (InetSocketAddress. 3400))
                           (ig/init))]
            (try
              (let [{:keys [status body]} (http/get "http://127.0.0.1:3400/")]
                (is (= 200 status))
                (is (= "test" body)))
              (finally
                (ig/halt! system)))))
        (testing "not valid socket"
          (is (thrown-on-path? [:socket-address]
                               (ig/init
                                (assoc-in config
                                          [:duct.server.http/aleph :socket-address]
                                          "Invalid socket object")))))))
    (testing "executor"
      (testing "server starts on :none"
        (let [system (ig/init (assoc-in config
                                        [:duct.server.http/aleph :executor]
                                        :none))]
          (try
            (let [{:keys [status body]} (http/get "http://127.0.0.1:3400/")]
              (is (= 200 status))
              (is (= "test" body)))
            (finally
              (ig/halt! system)))))
      (testing "should fail on a non valid executor"
        (is (thrown-on-path? [:executor :none]
                             (ig/init (assoc-in config
                                                [:duct.server.http/aleph :executor]
                                                :nothing))))))))

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

(deftest resume-and-suspend-test
  (let [response1 {:status 200 :headers {} :body "foo"}
        response2 {:status 200 :headers {} :body "bar"}
        logger    (->TestLogger (atom []))
        config1   {:duct.server.http/aleph {:port 3400, :handler (constantly response1), :logger logger}}
        config2   {:duct.server.http/aleph {:port 3400, :handler (constantly response2), :logger logger}}]

    (testing "suspend and resume"
      (let [system1  (doto (ig/init config1) ig/suspend!)
            response (future (http/get "http://127.0.0.1:3400/"))
            system2  (ig/resume config2 system1)]
        (try
          (is (identical? (-> system1 :duct.server.http/aleph :handler)
                          (-> system2 :duct.server.http/aleph :handler)))
          (is (identical? (-> system1 :duct.server.http/aleph :server)
                          (-> system2 :duct.server.http/aleph :server)))
          (is (= (:status @response) 200))
          (is (= (:body @response) "bar"))
          (finally
            (ig/halt! system1)
            (ig/halt! system2)))))

    (testing "suspend and resume with different config"
      (let [system1  (doto (ig/init config1) ig/suspend!)
            config2' (assoc-in config2 [:duct.server.http/aleph :port] 3401)
            system2  (ig/resume config2' system1)]
        (try
          (let [response (http/get "http://127.0.0.1:3401/")]
            (is (= (:status response) 200))
            (is (= (:body response) "bar")))
          (finally
            (ig/halt! system1)
            (ig/halt! system2)))))

    (testing "suspend and resume with extra config"
      (let [system1 (doto (ig/init {}) ig/suspend!)
            system2 (ig/resume config2 system1)]
        (try
          (let [response (http/get "http://127.0.0.1:3400/")]
            (is (= (:status response) 200))
            (is (= (:body response) "bar")))
          (finally
            (ig/halt! system2)))))

    (testing "suspend and result with missing config"
      (let [system1  (doto (ig/init config1) ig/suspend!)
            system2  (ig/resume {} system1)]
        (is (= system2 {}))))

    (testing "logger is replaced"
      (let [logs1   (atom [])
            logs2   (atom [])
            config1 (assoc-in config1 [:duct.server.http/aleph :logger] (->TestLogger logs1))
            config2 (assoc-in config2 [:duct.server.http/aleph :logger] (->TestLogger logs2))
            system1 (doto (ig/init config1) ig/suspend!)
            system2 (ig/resume config2 system1)]
        (ig/halt! system2)
        (is (= @logs1 [[::aleph/starting-server {:port 3400}]]))
        (is (= @logs2 [[::aleph/stopping-server nil]]))
        (ig/halt! system1)))))
