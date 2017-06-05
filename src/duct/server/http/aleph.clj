(ns duct.server.http.aleph
  (:require [aleph.http :as aleph]
            [duct.logger :as logger]
            [integrant.core :as ig]))

(derive :duct.server.http/aleph :duct.server/http)

(defmethod ig/init-key :duct.server.http/aleph [_ {:keys [handler logger] :as opts}]
  (logger/log logger :report ::starting-server (select-keys opts [:port]))
  {:server (aleph/start-server handler (dissoc opts :handler))
   :logger logger})

(defmethod ig/halt-key! :duct.server.http/aleph [_ {:keys [logger server]}]
  (logger/log logger :report ::stopping-server)
  (.close ^java.io.Closeable server))
