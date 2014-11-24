(ns cmr.transmit.echo.rest
  "A helper for making echo-rest requests"
  (:require [clj-http.client :as client]
            [cmr.common.services.errors :as errors]
            [cmr.common.services.health-helper :as hh]
            [cheshire.core :as json]
            [cmr.transmit.config :as config]
            [cmr.transmit.connection :as conn]
            [cmr.common.log :as log :refer (debug info warn error)]))

(defn request-options
  [conn]
  {:accept :json
   :throw-exceptions false
   :headers {"Echo-Token" (config/echo-system-token)}
   :connection-manager (conn/conn-mgr conn)})

(defn post-options
  [conn body-obj]
  (merge (request-options conn)
         {:content-type :json
          :body (json/encode body-obj)}))

(defn rest-get
  "Makes a get request to echo-rest. Returns a tuple of status, the parsed body, and the body."
  ([context url-path]
   (rest-get context url-path {}))
  ([context url-path options]
   (let [conn (config/context->app-connection context :echo-rest)
         url (format "%s%s" (conn/root-url conn) url-path)
         params (merge (request-options conn) options)
         response (client/get url params)
         start (System/currentTimeMillis)
         response (client/get url params)
         _ (debug (format "Completed ECHO GET Request to %s in [%d] ms" url (- (System/currentTimeMillis) start)))

         {:keys [status body headers]} response
         parsed (if (.startsWith ^String (get headers "Content-Type" "") "application/json")
                  (json/decode body true)
                  nil)]
     [status parsed body])))

(defn rest-delete
  "Makes a delete request on echo-rest. Returns a tuple of status and body"
  ([context url-path]
   (rest-delete context url-path {}))
  ([context url-path options]
   (let [conn (config/context->app-connection context :echo-rest)
         url (format "%s%s" (conn/root-url conn) url-path)
         params (merge (request-options conn) options)
         ;; Uncoment to log requests
         ; _ (debug "Making ECHO DELETE Request" url (pr-str params))
         response (client/delete url params)
         {:keys [status body]} response]
     [status body])))

(defn rest-post
  "Makes a post request to echo-rest. Returns a tuple of status, the parsed body, and the body."
  ([context url-path body-obj]
   (rest-post context url-path body-obj {}))
  ([context url-path body-obj options]
   (let [conn (config/context->app-connection context :echo-rest)
         url (format "%s%s" (conn/root-url conn) url-path)
         params (merge (post-options conn body-obj) options)
         ;; Uncoment to log requests
         ; _ (debug "Making ECHO POST Request" url (pr-str params))
         response (client/post url params)
         {:keys [status body headers]} response
         parsed (if (.startsWith ^String (get headers "Content-Type" "") "application/json")
                  (json/decode body true)
                  nil)]
     [status parsed body])))


(defn unexpected-status-error!
  [status body]
  (errors/internal-error!
    (format "Unexpected status %d from response. body: %s"
            status (pr-str body))))

(defn- get-rest-health
  "Returns the echo-rest health by calling its availability api"
  [url]
  (try
    (client/get url {:throw-exceptions false})
    (catch Exception e
      {:status 503
       :body (format "Unable to get echo health, caught exception: %s" (.getMessage e))})))

(defn health-fn
  "Returns the availability status of echo-rest by calling its availability endpoint"
  [context]
  (let [conn (config/context->app-connection context :echo-rest)
        url (format "%s%s" (conn/root-url conn) "/availability")
        response (get-rest-health url)
        status-code (:status response)]
    (if (= 200 status-code)
      {:ok? true}
      {:ok? false
       :problem (format "Received %d from availability check. %s" status-code (:body response))})))

(defn health
  "Returns the echo-rest health with timeout handling."
  [context]
  (hh/get-health #(health-fn context)))
