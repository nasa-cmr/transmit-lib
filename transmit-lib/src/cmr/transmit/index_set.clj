(ns cmr.transmit.index-set
  "Provide functions to invoke index set app"
  (:require [clj-http.client :as client]
            [cmr.common.services.errors :as errors]
            [cmr.transmit.http-helper :as h]
            [cheshire.core :as cheshire]
            [cmr.transmit.config :as config]
            [cmr.transmit.connection :as conn]))

(defn get-index-set
  "Submit a request to index-set app to fetch an index-set assoc with an id"
  [context id]
  (let [conn (config/context->app-connection context :index-set)
        response (client/request
                   (merge
                     (config/conn-params conn)
                     {:method :get
                      :url (format "%s/index-sets/%s" (conn/root-url conn) (str id))
                      :accept :json
                      :throw-exceptions false
                      :headers {config/token-header (config/echo-system-token)}}))
        status (:status response)
        body (cheshire/decode (:body response) true)]
    (case status
      404 nil
      200 body
      (errors/internal-error! (format "Unexpected error fetching index-set with id: %s,
                                      Index set app reported status: %s, error: %s"
                                      id status (pr-str (flatten (:errors body))))))))

;; Defines health check function
(h/defhealther get-index-set-health :index-set 2)