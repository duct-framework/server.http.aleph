(ns duct.server.http.spec
  (:require [clojure.spec.alpha :as s]
            [duct.logger :as logger])
  (:import [duct.logger Logger]
           [java.net SocketAddress]
           [java.util.concurrent Executor]
           [io.netty.handler.ssl SslContext]))

(def ^:private port-max-value (int (Character/MAX_VALUE)))

(def ^:private fn-instance? (partial partial instance?))

(s/def ::port
  (s/or :any zero?
        :specific #(< 0 % port-max-value)))

(s/def ::socket-address
  (fn-instance? SocketAddress))

(s/def ::bootstrap-transform ifn?)

(s/def ::ssl-context
  (fn-instance? SslContext))

(s/def ::pipeline-transform ifn?)

(s/def ::executor
  (s/or :none (partial = :none)
        :some (fn-instance? Executor)))

(s/def ::shutdown-executor? boolean?)

(s/def ::request-buffer-size pos-int?)

(s/def ::raw-stream? boolean?)

(s/def ::rejected-handler ifn?)

(s/def ::max-initial-line-length pos-int?)

(s/def ::max-header-size pos-int?)

(s/def ::max-chunk-size pos-int?)

(s/def ::epoll? boolean?)

(s/def ::compression? boolean?)

(s/def ::compression-level
  (s/and pos-int?
         #(<= 1 % 9)))

(s/def ::idle-timeout (complement neg-int?))

(s/def ::logger (fn-instance? Logger))

(s/def ::handler ifn?)

(s/def ::config
  (s/keys :req-un [::logger ::handler]
          :opt-un [::port ::socket-address ::bootstrap-transform
                   ::ssl-context ::pipeline-transform ::executor
                   ::shutdown-executor? ::request-buffer-size
                   ::raw-stream? ::rejected-handler
                   ::max-initial-line-length ::max-header-size
                   ::max-chunk-size ::epoll? ::compression?
                   ::compression-level ::idle-timeout]))
