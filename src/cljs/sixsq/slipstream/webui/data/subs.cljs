(ns sixsq.slipstream.webui.data.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.slipstream.webui.data.spec :as spec]))


(reg-sub
  ::time-period
  (fn [db]
    (::spec/time-period db)))


(reg-sub
  ::credentials
  (fn [db]
    (::spec/credentials db)))


(reg-sub
  ::content-types
  (fn [db]
    (::spec/content-types db)))


(reg-sub
  ::application-select-visible?
  (fn [db]
    (::spec/application-select-visible? db)))


(reg-sub
  ::loading-applications?
  (fn [db]
    (::spec/loading-applications? db)))


(reg-sub
  ::applications
  (fn [db]
    (::spec/applications db)))
