(ns sixsq.slipstream.webui.cimi.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.slipstream.webui.cimi-api.effects :as cimi-api-fx]
    [sixsq.slipstream.webui.cimi-api.utils :as cimi-api-utils]
    [sixsq.slipstream.webui.cimi.effects :as cimi-fx]
    [sixsq.slipstream.webui.cimi.spec :as cimi-spec]
    [sixsq.slipstream.webui.cimi.utils :as cimi-utils]
    [sixsq.slipstream.webui.client.spec :as client-spec]
    [sixsq.slipstream.webui.messages.events :as messages-events]
    [sixsq.slipstream.webui.utils.general :as general-utils]
    [sixsq.slipstream.webui.utils.response :as response]
    [taoensso.timbre :as log]))


(reg-event-fx
  ::initialize
  (fn [cofx _]
    (when-let [client (-> cofx :db ::client-spec/client)]
      {::cimi-api-fx/session [client #(dispatch [::set-session %])]})))


(reg-event-db
  ::set-first
  (fn [db [_ first-value]]
    (update db ::cimi-spec/query-params merge {:$first first-value})))


(reg-event-db
  ::set-last
  (fn [db [_ last-value]]
    (update db ::cimi-spec/query-params merge {:$last last-value})))


(reg-event-db
  ::set-filter
  (fn [db [_ filter-value]]
    (update db ::cimi-spec/query-params merge {:$filter filter-value})))


(reg-event-db
  ::set-orderby
  (fn [db [_ orderby-value]]
    (update db ::cimi-spec/query-params merge {:$orderby orderby-value})))


(reg-event-db
  ::set-select
  (fn [db [_ select-value]]
    (update db ::cimi-spec/query-params merge {:$select select-value})))

(reg-event-db
  ::set-query-params
  (fn [db [_ params]]
    (update db ::cimi-spec/query-params merge params)))

(reg-event-db
  ::show-add-modal
  (fn [db _]
    (assoc db ::cimi-spec/show-add-modal? true)))


(reg-event-db
  ::hide-add-modal
  (fn [db _]
    (assoc db ::cimi-spec/show-add-modal? false)))


(reg-event-db
  ::set-aggregation
  (fn [db [_ aggregation-value]]
    (update db ::cimi-spec/query-params merge {:$aggregation aggregation-value})))


(reg-event-db
  ::set-collection-name
  (fn [{:keys [::cimi-spec/cloud-entry-point] :as db} [_ collection-name]]
    (if (or (empty? collection-name) (-> cloud-entry-point
                                         :collection-key
                                         (get collection-name)))
      (assoc db ::cimi-spec/collection-name collection-name)
      (let [msg-map {:header  (cond-> (str "invalid resource type: " collection-name))
                     :content (str "The resource type '" collection-name "' is not valid. "
                                   "Please choose another resource type.")
                     :type    :error}]

        ;; only send error message when the cloud-entry-point is actually set, otherwise
        ;; we don't yet know if this is a valid resource or not
        (when cloud-entry-point
          (dispatch [::messages-events/add msg-map]))
        db))))


(reg-event-db
  ::set-selected-fields
  (fn [db [_ fields]]
    (assoc db ::cimi-spec/selected-fields (sort (vec fields)))))


(reg-event-db
  ::remove-field
  (fn [{:keys [::cimi-spec/selected-fields] :as db} [_ field]]
    (->> selected-fields
         (remove #{field})
         vec
         sort
         (assoc db ::cimi-spec/selected-fields))))


(reg-event-fx
  ::get-results
  (fn [{{:keys [::cimi-spec/collection-name
                ::cimi-spec/cloud-entry-point
                ::cimi-spec/query-params
                ::client-spec/client] :as db} :db} _]
    (let [resource-type (-> cloud-entry-point
                            :collection-key
                            (get collection-name))]
      {:db                  (assoc db ::cimi-spec/loading? true
                                      ::cimi-spec/aggregations nil
                                      ::cimi-spec/collection nil)
       ::cimi-api-fx/search [client
                             resource-type
                             (general-utils/prepare-params query-params)
                             #(dispatch [::set-results resource-type %])]})))


(reg-event-fx
  ::create-resource
  (fn [{{:keys [::cimi-spec/collection-name
                ::cimi-spec/cloud-entry-point
                ::client-spec/client] :as db} :db} [_ data]]
    (let [resource-type (-> cloud-entry-point
                            :collection-key
                            (get collection-name))]
      {::cimi-api-fx/add [client resource-type data
                          #(let [msg-map (if (instance? js/Error %)
                                           (let [{:keys [status message]} (response/parse-ex-info %)]
                                             {:header  (cond-> (str "failure adding " (name resource-type))
                                                               status (str " (" status ")"))
                                              :content message
                                              :type    :error})
                                           (let [{:keys [status message resource-id]} (response/parse %)]
                                             {:header  (cond-> (str "added " resource-id)
                                                               status (str " (" status ")"))
                                              :content message
                                              :type    :success}))]
                             (dispatch [::messages-events/add msg-map]))]})))

(reg-event-fx
  ::create-resource-independent
  (fn [{{:keys [::client-spec/client] :as db} :db} [_ resource-type data]]
    {::cimi-api-fx/add [client resource-type data
                        #(let [msg-map (if (instance? js/Error %)
                                         (let [{:keys [status message]} (response/parse-ex-info %)]
                                           {:header  (cond-> (str "failure adding " (name resource-type))
                                                             status (str " (" status ")"))
                                            :content message
                                            :type    :error})
                                         (let [{:keys [status message resource-id]} (response/parse %)]
                                           {:header  (cond-> (str "added " resource-id)
                                                             status (str " (" status ")"))
                                            :content message
                                            :type    :success}))]
                           (dispatch [::messages-events/add msg-map]))]}))

(reg-event-db
  ::set-results
  (fn [db [_ resource-type listing]]
    (let [error? (instance? js/Error listing)
          entries (get listing (keyword resource-type) [])
          aggregations (get listing :aggregations nil)
          fields (general-utils/merge-keys (conj entries {:id "id"}))]
      (when error?
        (dispatch [::messages-events/add
                   (let [{:keys [status message]} (response/parse-ex-info listing)]
                     {:header  (cond-> (str "failure getting " (name resource-type))
                                       status (str " (" status ")"))
                      :content message
                      :type    :error})]))
      (assoc db ::cimi-spec/aggregations aggregations
                ::cimi-spec/collection (when-not error? listing)
                ::cimi-spec/loading? false
                ::cimi-spec/available-fields fields))))


(reg-event-db
  ::set-cloud-entry-point
  (fn [db [_ {:keys [baseURI] :as cep}]]
    (let [href-map (cimi-utils/collection-href-map cep)
          key-map (cimi-utils/collection-key-map cep)
          templates-map (cimi-utils/collections-template-map cep)]
      (-> db
          (assoc ::cimi-spec/cloud-entry-point {:baseURI         baseURI
                                                :collection-href href-map
                                                :collection-key  key-map})
          (assoc ::cimi-spec/collections-templates-cache templates-map)))))


(reg-event-fx
  ::get-cloud-entry-point
  (fn [{{:keys [::client-spec/client] :as db} :db} _]
    (when client
      {::cimi-api-fx/cloud-entry-point
       [client (fn [cep]
                 (dispatch [::set-cloud-entry-point cep]))]})))

(reg-event-fx
  ::get-templates
  (fn [{{:keys [::cimi-spec/cloud-entry-point
                ::cimi-spec/collections-templates-cache
                ::client-spec/client] :as db} :db} [_ template-href]]
    (let [resource-type (-> cloud-entry-point
                            :collection-key
                            (get template-href))]
      {::cimi-api-fx/search [client
                             resource-type
                             {}
                             #(dispatch [::set-templates template-href (resource-type %) (:count %)])]})))

(reg-event-fx
  ::set-templates
  (fn [{:keys [db]} [_ template-href templates total]]
    (let [error? (instance? js/Error templates)
          entries (get templates template-href [])]
      (if error?
        (dispatch [::messages-events/add
                   (let [{:keys [status message]} (response/parse-ex-info templates)]
                     {:header  (cond-> (str "failure getting " (name template-href))
                                       status (str " (" status ")"))
                      :content message
                      :type    :error})])
        (let [template-href-key (keyword template-href)
              prepared-templates (->> templates
                                      (map cimi-api-utils/prepare-template)
                                      (into {}))]
          {:db                                 (assoc-in db [::cimi-spec/collections-templates-cache template-href-key]
                                                         {:templates prepared-templates
                                                          :loaded    0
                                                          :total     total})
           ::cimi-fx/get-templates-description [template-href-key prepared-templates]})))))

(reg-event-fx
  ::get-template-description
  (fn [{{:keys [::client-spec/client] :as db} :db} [_ template-href-key template-id]]
    (let [operation "http://sixsq.com/slipstream/1/action/describe"
          callback #(dispatch [::set-template-description template-href-key template-id %])]
      (log/info "::get-template-description template-id" template-id)
      {::cimi-api-fx/operation [client (name template-id) operation callback]})))

(reg-event-db
  ::set-template-description
  (fn [{:keys [::cimi-spec/collections-templates-cache] :as db} [_ resource-type template-id params-desc]]
    (let [template-id-key (keyword template-id)
          tpl (get-in collections-templates-cache [resource-type :templates template-id-key])
          default-values-map (->> (:default-values tpl) (map (fn [[k v]] [k {:data v}])) (into {}))
          description-with-defaults (merge-with merge params-desc default-values-map)
          description-filtered (cimi-api-utils/filter-params-desc (dissoc description-with-defaults :acl))
          template (-> tpl
                       (assoc :params-desc description-filtered)
                       (dissoc :default-values))]
      (-> db
          (update-in [::cimi-spec/collections-templates-cache resource-type :loaded] inc)
          (assoc-in [::cimi-spec/collections-templates-cache resource-type :templates template-id-key] template)))))
