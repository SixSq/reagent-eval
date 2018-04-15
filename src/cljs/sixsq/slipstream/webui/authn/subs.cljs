(ns sixsq.slipstream.webui.authn.subs
  (:require
    [re-frame.core :refer [reg-sub subscribe]]
    [sixsq.slipstream.webui.authn.spec :as authn-spec]
    [clojure.string :as str]))


(reg-sub
  ::open-modal
  (fn [db]
    (::authn-spec/open-modal db)))

(reg-sub
  ::session
  (fn [db]
    (::authn-spec/session db)))

(defn has-role? [session role]
  (some-> session :roles (str/split  #"\s+") set (contains? role)))

(reg-sub
  ::is-admin?
  :<- [::session]
  (fn [session _]
    (has-role? session "ADMIN")))

(reg-sub
  ::is-user?
  :<- [::session]
  (fn [session _]
    (has-role? session "USER")))

(reg-sub
  ::user
  :<- [::session]
  (fn [session _]
    (some-> session :username (str/replace #"user/" ""))))

(reg-sub
  ::error-message
  (fn [db]
    (::authn-spec/error-message db)))

(reg-sub
  ::redirect-uri
  (fn [db]
    (::authn-spec/redirect-uri db)))

(reg-sub
  ::server-redirect-uri
  (fn [db]
    (::authn-spec/server-redirect-uri db)))
