(ns cmr.transmit.echo.acls
  "Contains functions for retrieving ACLs from the echo-rest api."
  (:require [cmr.transmit.echo.rest :as r]
            [cmr.transmit.echo.conversion :as c]
            [cmr.transmit.echo.providers :as echo-providers]
            [cmr.common.services.errors :as errors]
            [cmr.common.util :as util]
            [clojure.string :as str]
            [camel-snake-kebab.core :as csk]))

(defn- convert-provider-guid-to-id-in-acl
  "Change all provider-guid references to provider-id for the given ACL. This simplifies working
  with ACLs since provider ids are commonly used throughout the code."
  [provider-guid-id-map acl]
  (let [converter (fn [identity-map]
                    (some-> identity-map
                            (assoc :provider-id (provider-guid-id-map (:provider-guid identity-map)))
                            (dissoc :provider-guid)))]
    (-> acl
        (update-in [:catalog-item-identity] converter)
        (update-in [:provider-object-identity] converter)
        util/remove-nil-keys)))

(def acl-type->acl-key
  "A map of the acl object identity type to the field within the acl that stores the object."
  {:catalog-item :catalog-item-identity
   :system-object :system-object-identity
   :provider-object :provider-object-identity
   :single-instance-object :single-instance-object-identity})

(def valid-acl-types
  "The list of valid acl object identity types that are supported"
  #{:provider-object :system-object :single-instance-object :catalog-item})

(defn- validate-type
  "Validates the acl type is one of the expected ones."
  [acl-type]
  (when-not (valid-acl-types acl-type)
    (errors/internal-error! (format "Acl type %s is not a valid acl type." (pr-str acl-type)))))

(defn- acl-type->object-identity-type-string
  "Converts an acl type keyword into the style supported on the ECHO Rest api."
  [acl-type]
  (csk/->SCREAMING_SNAKE_CASE_STRING acl-type))

(defn get-acls-by-types
  "Fetches ACLs from ECHO by object identity type."
  ([context types]
   (get-acls-by-types context types nil))
  ([context types provider-id]
   ;; Validate the acl types
   (doseq [t types] (validate-type t))
   (let [provider-guid-id-map (echo-providers/get-provider-guid-id-map context)
         [status acls body] (r/rest-get
                              context
                              "/acls"
                              {:query-params
                               (merge {:object_identity_type
                                       (str/join "," (map acl-type->object-identity-type-string types))
                                       :reference false}
                                      (when provider-id {:provider_id provider-id}))})]
     (case status
       200 (mapv (comp (partial convert-provider-guid-to-id-in-acl provider-guid-id-map)
                       c/echo-acl->cmr-acl)
                 acls)
       (r/unexpected-status-error! status body)))))
