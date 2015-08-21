(ns cmr.transmit.index-set
  "Provide functions to invoke index set app"
  (:require [clj-http.client :as client]
            [cmr.common.services.errors :as errors]
            [cmr.common.services.health-helper :as hh]
            [cheshire.core :as cheshire]
            [cmr.system-trace.core :refer [deftracefn]]
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

(defn get-index-set-health-fn
  "Returns the health status of the index set"
  [context]
  (let [conn (config/context->app-connection context :index-set)
        request-url (str (conn/root-url conn) "/health")
        response (client/get request-url (merge (config/conn-params conn)
                                                {:accept :json
                                                 :throw-exceptions false}))
        {:keys [status body]} response
        result (cheshire/decode body true)]
    (if (= 200 status)
      {:ok? true :dependencies result}
      {:ok? false :problem result})))

(defn get-index-set-health
  "Returns the index-set health with timeout handling."
  [context]
  (let [timeout-ms (* 1000 (+ 2 (hh/health-check-timeout-seconds)))]
    (hh/get-health #(get-index-set-health-fn context) timeout-ms)))
