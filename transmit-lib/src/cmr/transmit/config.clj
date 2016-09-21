(ns cmr.transmit.config
  "Contains functions for retrieving application connection information from environment variables"
  (:require [cmr.common.config :as cfg :refer [defconfig]]
            [cmr.transmit.connection :as conn]
            [camel-snake-kebab.core :as csk]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Constants for help in testing.

(def mock-echo-system-group-guid
  "The guid of the mock admin group."
  "mock-admin-group-guid")

(def mock-echo-system-token
  "A token for the mock system/admin user."
  "mock-echo-system-token")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def token-header
  "echo-token")

(defmacro def-app-conn-config
  "Defines three configuration entries for an application for the host, port and relative root URL"
  [app-name defaults]
  (let [protocol-config (symbol (str (name app-name) "-protocol"))
        host-config (symbol (str (name app-name) "-host"))
        port-config (symbol (str (name app-name) "-port"))
        relative-root-url-config (symbol (str (name app-name) "-relative-root-url"))]
    `(do
       (defconfig ~protocol-config
         ~(str "The protocol to use for connections to the " (name app-name) " application.")
         {:default ~(get defaults :protocol "http")})

       (defconfig ~host-config
         ~(str "The host name to use for connections to the " (name app-name) " application.")
         {:default ~(get defaults :host "localhost")})

       (defconfig ~port-config
         ~(str "The port number to use for connections to the " (name app-name) " application.")
         {:default ~(get defaults :port 3000) :type Long})

       (defconfig ~relative-root-url-config
         ~(str "Defines a root path that will appear on all requests sent to this application. For "
               "example if the relative-root-url is '/cmr-app' and the path for a URL is '/foo' then "
               "the full url would be http://host:port/cmr-app/foo. This should be set when this "
               "application is deployed in an environment where it is accessed through a VIP.")
         {:default ~(get defaults :relative-root-url "")}))))

(def-app-conn-config metadata-db {:port 3001})
(def-app-conn-config ingest {:port 3002})
(def-app-conn-config search {:port 3003})
(def-app-conn-config indexer {:port 3004})
(def-app-conn-config index-set {:port 3005})
(def-app-conn-config bootstrap {:port 3006})
(def-app-conn-config cubby {:port 3007})
(def-app-conn-config virtual-product {:port 3009})
;; CMR open search is 3010
(def-app-conn-config access-control {:port 3011})
(def-app-conn-config kms {:port 2999, :relative-root-url "/kms"})

(def-app-conn-config urs {:port 3008, :relative-root-url "/urs"})

(defconfig urs-username
  "Defines the username that is sent from the CMR to URS to authenticate the CMR."
  {:default "mock-urs-username"})

(defconfig urs-password
  "Defines the password that is sent from the CMR to URS to authenticate the CMR."
  {***REMOVED***})

(defn mins->ms
  "Returns the number of minutes in milliseconds"
  [mins]
  (* mins 60000))

(defconfig http-socket-timeout
  "The number of milliseconds before an HTTP request will timeout."
  ;; This is set to a value bigger than what appears to the VIP timeout. There's a problem with
  ;; EI-3988 where responses longer than 5 minutes never return. We want to cause those to fail.
  {:default (mins->ms 6)
   :type Long})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ECHO Rest

(defconfig echo-rest-protocol
  "The protocol to use when contructing ECHO Rest URLs."
  {:default "http"})

(defconfig echo-rest-host
  "The host name to use for connections to ECHO Rest."
  {:default "localhost"})

(defconfig echo-rest-port
  "The port to use for connections to ECHO Rest"
  {:default 3008 :type Long})

(defconfig mock-echo-port
  "The port to start mock echo and urs on"
  {:default 3008 :type Long})

(defconfig echo-rest-context
  "The root context for connections to ECHO Rest."
  {:default ""})

(defconfig echo-http-socket-timeout
  "The number of milliseconds before an HTTP request to ECHO will timeout."
  {:default (mins->ms 60)
   :type Long})

(defconfig echo-system-token
  "The ECHO system token to use for request to ECHO."
  {:default mock-echo-system-token})

;; TODO add all these to hiera in every environment based on current values

(defconfig echo-system-username
  "The ECHO system token to use for request to ECHO."
  {:default "User101"})

(defconfig administrators-group-name
  "The name of the Administrators group which the echo system user belongs to."
  {:default "Administrators"})

(defconfig administrators-group-legacy-guid
  "The legacy guid of the administrators guid."
  {:default mock-echo-system-group-guid})

(def default-conn-info
  "The default values for connections."
  {:protocol "http"
   :context ""})

(defn app-conn-info
  "Returns the current application connection information as a map by application name"
  []
  {:metadata-db {:protocol (metadata-db-protocol)
                 :host (metadata-db-host)
                 :port (metadata-db-port)
                 :context (metadata-db-relative-root-url)}
   :ingest {:protocol (ingest-protocol)
            :host (ingest-host)
            :port (ingest-port)
            :context (ingest-relative-root-url)}
   :access-control {:protocol (access-control-protocol)
                    :host (access-control-host)
                    :port (access-control-port)
                    :context (access-control-relative-root-url)}
   :search {:protocol (search-protocol)
            :host (search-host)
            :port (search-port)
            :context (search-relative-root-url)}
   :indexer {:protocol (indexer-protocol)
             :host (indexer-host)
             :port (indexer-port)
             :context (indexer-relative-root-url)}
   :index-set {:protocol (index-set-protocol)
               :host (index-set-host)
               :port (index-set-port)
               :context (index-set-relative-root-url)}
   :bootstrap {:protocol (bootstrap-protocol)
               :host (bootstrap-host)
               :port (bootstrap-port)
               :context (bootstrap-relative-root-url)}
   :cubby {:protocol (cubby-protocol)
           :host (cubby-host)
           :port (cubby-port)
           :context (cubby-relative-root-url)}
   :virtual-product {:protocol (virtual-product-protocol)
                     :host (virtual-product-host)
                     :port (virtual-product-port)
                     :context (virtual-product-relative-root-url)}
   :echo-rest {:protocol (echo-rest-protocol)
               :host (echo-rest-host)
               :port (echo-rest-port)
               :context (echo-rest-context)}
   :kms {:protocol (kms-protocol)
         :host (kms-host)
         :port (kms-port)
         :context (kms-relative-root-url)}
   :urs {:protocol (urs-protocol)
         :host (urs-host)
         :port (urs-port)
         :context (urs-relative-root-url)}})

(defn app-connection-system-key-name
  "The name of the app connection in the system"
  [app-name]
  (keyword (str (csk/->kebab-case-string app-name) "-connection")))

(defn context->app-connection
  "Retrieves the connection from the context for the given app."
  [context app-name]
  (get-in context [:system (app-connection-system-key-name app-name)]))

(defn system-with-connections
  "Adds connection keys to the system for the given applications. They will be added in a way
  that can be retrieved with the context->app-connection function."
  [system app-names]
  (let [conn-info-map (app-conn-info)]
    (reduce (fn [sys app-name]
              (let [conn-info (merge default-conn-info (conn-info-map app-name))]
                (assoc sys
                       (app-connection-system-key-name app-name)
                       (conn/create-app-connection conn-info))))
            system
            app-names)))

(defn conn-params
  "Returns a map of connection params to merge in when making HTTP requests"
  [connection]
  {:connection-manager (conn/conn-mgr connection)
   :socket-timeout (http-socket-timeout)})

(defn application-public-root-url
  "Returns the public root url for an application given a context. Assumes public configuration is
   stored in a :public-conf key of the system."
  [context]
  (let [{:keys [protocol host port relative-root-url]} (get-in context [:system :public-conf])
        port (if (empty? relative-root-url) port (format "%s%s" port relative-root-url))]
    (format "%s://%s:%s/" protocol host port)))