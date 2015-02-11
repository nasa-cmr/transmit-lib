(ns cmr.transmit.test.echo.conversion
  (:require [clojure.test :refer :all]
            [cmr.transmit.echo.conversion :as c]))


(def example-echo-acl
  {:acl {:id "5C1B77E7-48E5-4579-E516-7D933F500F23"
         :access_control_entries [{:permissions ["ORDER" "READ"],
                                   :sid {:group_sid {:group_guid "3730376E-4DCF-53EE-90ED-FE945351A64F"}}}
                                  {:permissions ["READ"],
                                   :sid {:user_authorization_type_sid {:user_authorization_type "GUEST"}}}
                                  {:permissions ["READ"],
                                   :sid {:user_authorization_type_sid {:user_authorization_type "REGISTERED"}}}],
         :catalog_item_identity {:collection_applicable false,
                                 :granule_applicable true,
                                 :name "All Granules",
                                 :provider_guid "CB91244B-C8B7-CA27-3089-3EC721FFF4D8"
                                 :collection_identifier
                                 {:collection_ids
                                  [{:data_set_id "Landsat 1-5 Multispectral Scanner V1"}
                                   {:data_set_id "Landsat 4-5 Thematic Mapper V1"}]
                                  :restriction_flag
                                  {:include_undefined_value false, :max_value 3.0, :min_value 3.0}}
                                 :granule_identifier
                                 {:restriction_flag {:include_undefined_value true
                                                     :max_value 5.0, :min_value 3.0}}}}})

(def example-acl-cleaned-up
  {:guid "5C1B77E7-48E5-4579-E516-7D933F500F23"
   :aces [{:permissions [:order :read],
           :group-guid "3730376E-4DCF-53EE-90ED-FE945351A64F"}
          {:permissions [:read]
           :user-type :guest}
          {:permissions [:read]
           :user-type :registered}]
   :catalog-item-identity {:collection-applicable false,
                           :granule-applicable true,
                           :name "All Granules",
                           :provider-guid "CB91244B-C8B7-CA27-3089-3EC721FFF4D8"
                           :collection-identifier
                           {:entry-titles ["Landsat 1-5 Multispectral Scanner V1"
                                           "Landsat 4-5 Thematic Mapper V1"]
                            :access-value {:include-undefined false, :max-value 3.0, :min-value 3.0}}
                           :granule-identifier
                           {:access-value {:include-undefined true, :max-value 5.0, :min-value 3.0}}}})

(deftest conversion-test
  (testing "echo -> cmr"
    (is (= example-acl-cleaned-up (c/echo-acl->cmr-acl example-echo-acl))))
  (testing "cmr -> echo"
    (is (= example-echo-acl (c/cmr-acl->echo-acl example-acl-cleaned-up))))

  (testing "sids by themselves"
    (are [cmr-sid echo-sid]
         (and (= cmr-sid (c/echo-sid->cmr-sid echo-sid))
              (= echo-sid (c/cmr-sid->echo-sid cmr-sid)))
         :guest {:sid {:user_authorization_type_sid {:user_authorization_type "GUEST"}}}
         :registered {:sid {:user_authorization_type_sid {:user_authorization_type "REGISTERED"}}}
         "group-guid" {:sid {:group_sid {:group_guid "group-guid"}}})))
