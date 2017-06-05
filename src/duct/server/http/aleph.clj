(ns duct.server.http.aleph
  (:require [aleph.http :as aleph]
            [integrant.core :as ig]))

(derive :duct.server.http/aleph :duct.server/http)

(defmethod ig/init-key :duct.server.http/aleph [_ {:keys [handler] :as options}]
  (aleph/start-server handler (dissoc options :handler)))

(defmethod ig/halt-key! :duct.server.http/aleph [_ ^java.io.Closeable server]
  (.close server))
