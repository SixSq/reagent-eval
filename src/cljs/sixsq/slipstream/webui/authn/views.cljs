(ns  sixsq.slipstream.webui.authn.views
  (:require
    [re-frame.core :refer [subscribe dispatch]]

    [sixsq.slipstream.webui.authn.events :as authn-events]
    [sixsq.slipstream.webui.authn.subs :as authn-subs]
    [sixsq.slipstream.webui.history.events :as history-events]
    [sixsq.slipstream.webui.cimi.subs :as cimi-subs]
    [sixsq.slipstream.webui.i18n.subs :as i18n-subs]

    [sixsq.slipstream.webui.utils.semantic-ui :as ui]
    [taoensso.timbre :as log]
    [reagent.core :as r]
    [clojure.string :as str]))

(defn method-comparator
  "Compares two login method types. The value 'internal' will always compare
   as less than anything other than itself."
  [x y]
  (cond
    (= x y) 0
    (= "internal" x) -1
    (= "internal" y) 1
    (< x y) -1
    :else 1))

(defn sort-value [[tag [{:keys [authn-method]}]]]
  (if (= "internal" authn-method)
    "internal"
    (or tag authn-method)))

(defn order-and-group
  "Sorts the methods by ID and then groups them (true/false) on whether it is
   an internal method or not."
  [methods]
  (->> methods
       (sort-by :id)
       (group-by #(or (:group %) (:authn-method %)))
       (sort-by sort-value method-comparator)))

(defn internal-or-api-key
  [[_ methods]]
  (let [authn-method (:authn-method (first methods))]
    (#{"internal" "api-key"} authn-method)))

(defn hidden? [{:keys [type] :as param-desc}]
  (= "hidden" type))

(defn ordered-params
  "Extracts and orders the parameter descriptions for rendering the form."
  [method]
  (->> method
       :params-desc
       seq
       (sort-by (fn [[_ {:keys [order]}]] order))))

(defn select-method-by-id
  [id methods]
  (->> methods
       (filter #(= id (:id %)))
       first))

(defn form-component
  "Provides a single element of a form. This should provide a reasonable
   control for each defined type, but this initial implementation just provides
   either a text or password field."
  [[param-name {:keys [data type displayName] :as param}]]
  (case type
    "hidden" [ui/FormField [:input {:name param-name :type "hidden" :value (or data "")}]]
    "password" [ui/FormInput {:name param-name :type type :placeholder displayName :icon "lock" :iconPosition "left"}]
    [ui/FormInput {:name param-name :type type :placeholder displayName :icon "user" :iconPosition "left"}]))

(defn method-form
  "Renders the form for a particular login method. The fields are taken from
   the login method description."
  [method-type methods]
  (let [cep (subscribe [::cimi-subs/cloud-entry-point])
        selected-method-group (r/atom (when (= 1 (count methods)) (first methods)))
        server-redirect-uri (subscribe [::authn-subs/server-redirect-uri])]
    (fn [method-type methods]
      (let [{:keys [id label] :as method} @selected-method-group
            {:keys [baseURI collection-href]} @cep
            id (or id method-type)
            post-uri (str baseURI (:sessions collection-href)) ;; FIXME: Should be part of CIMI API.
            inputs-method (conj (ordered-params method)
                                ["href" {:displayName "href" :data id :type "hidden"}]
                                ["redirectURI" {:displayName "redirectURI" :data @server-redirect-uri :type "hidden"}])
            ]
        (log/info "creating login form for method" id)
        (vec (concat [ui/Form {:id     (str "login_" id)
                               :action post-uri
                               :method "post"}]
                     (map #(form-component %) inputs-method)
                     [(if @selected-method-group
                        [ui/FormButton {:primary true :fluid true} label]
                        [ui/FormField
                         [ui/ButtonGroup {:color "blue" :fluid true}
                          [ui/Dropdown
                           {:options     (map #(identity {:key   (:id %)
                                                          :text  (:label %)
                                                          :value (:id %)}) methods)
                            :placeholder method-type
                            :button      true
                            :onChange    #(let [id (-> (js->clj %2 :keywordize-keys true) :value)
                                                selected-method (select-method-by-id id methods)
                                                with-params? (-> selected-method :params-desc true?)]
                                            (reset! selected-method-group selected-method))
                            :style       {:text-align "center"}}]]])]))))))

(defn login-form-container
  "Container that holds all of the login forms. These will be placed into two
   columns. The first has the 'internal' login forms and the second contains
   all of the rest."
  []
  (let [methods (subscribe [::authn-subs/methods])
        total (subscribe [::authn-subs/total])
        count (subscribe [::authn-subs/count])
        tr (subscribe [::i18n-subs/tr])
        error-message (subscribe [::authn-subs/error-message])]
    (fn []
      (let [method-groups (order-and-group @methods)
            internals (filter internal-or-api-key method-groups)
            externals (remove internal-or-api-key method-groups)
            externals? (empty? externals)
            loading (or (= @total 0) (> @total @count))]

        [ui/Segment {:basic true}
         (when @error-message
           [ui/Message {:negative  true
                        :size      "tiny"
                        :onDismiss #(dispatch [::authn-events/clear-error-message])}
            [ui/MessageHeader (@tr [:login-failed])]
            [:p @error-message]])

         (when loading [ui/Dimmer {:active true :inverted true} [ui/Loader (@tr [:loading])]])

         [ui/Grid {:columns 2 :textAlign "center" :stackable true :celled "internally"}

          [ui/GridColumn {:stretched true}
           [ui/Segment {:basic externals? :textAlign "left"}
            (when-not externals? [ui/Label {:color "red" :ribbon true} "Internal methods"]) ; TODO translate
            (vec (concat [:div]
                         (map (fn [[k v]] [method-form k v]) internals)))]]

          (when-not externals?
            [ui/GridColumn {:stretched true}
             [ui/Segment {:textAlign "left"}
              (vec (concat [:span {:style {:position "absolute"}}]
                           (map #(identity [ui/Icon {:name % :size "big"}]) ["world" "github"])))
              [ui/Label {:color "teal" :ribbon "right"} "External methods"] ; TODO translate
              [:div
               (vec (concat [:div]
                            (map (fn [[k v]] [method-form k v]) externals)))]]])
          ]
         ]))))

(defn modal-login []
  (let [tr (subscribe [::i18n-subs/tr])
        modal-open? (subscribe [::authn-subs/modal-open?])]
    (fn []
      [ui/Modal
       {:size     "small"
        :id       "modal-login-id"
        :open     @modal-open?
        :dimmer   "blurring"
        :on-close #(dispatch [::authn-events/close-modal])}
       [ui/ModalHeader (@tr [:login])]
       [ui/ModalContent {:scrolling true}
        [login-form-container]]])))

(defn login-button
  "This panel shows the login button and modal (if open)."
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn []
      [:div
       [ui/Button
        {:size "tiny" :primary true :on-click #(dispatch [::authn-events/open-modal])}
        (@tr [:login])]
       [modal-login]])))


(defn logout-button
  "Button to close the user's session. Triggers a logout from the server and
   navigates to the welcome page."
  []
  (let [tr (subscribe [::i18n-subs/tr])
        on-click (fn []
                   (dispatch [::authn-events/logout])
                   #_(history/navigate "welcome"))]         ;; FIXME
    (fn []
      [:div
       [ui/Button {:size "tiny" :on-click on-click :color "red"} (@tr [:logout])]])))


(defn authn-button
  "Provides either a login or logout button depending on whether the user has
   an active session. The login button will bring up a modal dialog."
  []
  (let [session (subscribe [::authn-subs/session])]
    (fn []
      (if-not @session
        [login-button]
        [logout-button]))))

(defn ^:export open-authn-modal []
  (log/debug "dispatch open-modal for authn view")
  (dispatch [::authn-events/open-modal]))
