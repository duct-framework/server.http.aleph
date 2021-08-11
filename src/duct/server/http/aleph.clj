(ns duct.server.http.aleph
  (:require [aleph.http :as aleph]
            [duct.logger :as logger]
            [duct.server.http.spec :as spec]
            [integrant.core :as ig]))

(defmethod ig/pre-init-spec :duct.server.http/aleph [_]
  ::spec/config)

(defmethod ig/init-key :duct.server.http/aleph [_ {:keys [handler logger] :as opts}]
  (let [handler (atom (delay (:handler opts)))
        logger  (atom logger)
        options (dissoc opts :handler :logger)]
    (logger/log @logger :report ::starting-server opts #_(select-keys opts [:port]))
    {:handler handler
     :logger  logger
     :server  (aleph/start-server (fn [req] (@@handler req)) options)}))

(defmethod ig/halt-key! :duct.server.http/aleph [_ {:keys [logger server]}]
  (logger/log @logger :report ::stopping-server)
  (.close ^java.io.Closeable server))

(defmethod ig/suspend-key! :duct.server.http/aleph [_ {:keys [handler]}]
  (reset! handler (promise)))

(defmethod ig/resume-key :duct.server.http/aleph [kw opts old-opts old-impl]
  (if (= (dissoc opts :handler :logger) (dissoc old-opts :handler :logger))
    (do (deliver @(:handler old-impl) (:handler opts))
        (reset! (:logger old-impl) (:logger opts))
        old-impl)
    (do (ig/halt-key! kw old-impl)
        (ig/init-key kw opts))))
