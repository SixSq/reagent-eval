(ns sixsq.slipstream.webui.acl.views
  (:require
    [sixsq.slipstream.webui.acl.utils :as utils]
    [sixsq.slipstream.webui.utils.semantic-ui :as ui]
    [sixsq.slipstream.webui.utils.collapsible-card :as cc]
    [sixsq.slipstream.webui.utils.style :as style]))


(defn checked
  [v]
  (if v [ui/Icon {:name "check"}] "\u0020"))


(defn acl-row
  [[[_ type principal] rights :as row-data]]
  [ui/TableRow
   [ui/TableCell {:collapsing true} principal]
   [ui/TableCell {:collapsing true} type]
   [ui/TableCell {:collapsing true} (checked (rights "VIEW"))]
   [ui/TableCell {:collapsing true} (checked (rights "MODIFY"))]
   [ui/TableCell {:collapsing true} (checked (rights "ALL"))]])


(defn acl-table
  [acl]
  (let [row-data (utils/acl-by-principal acl)
        rows (map acl-row row-data)]
    [ui/Table style/acl
     [ui/TableHeader
      [ui/TableRow
       [ui/TableHeaderCell {:collapsing true} "principal"]
       [ui/TableHeaderCell {:collapsing true} "type"]
       [ui/TableHeaderCell {:collapsing true} "view"]
       [ui/TableHeaderCell {:collapsing true} "modify"]
       [ui/TableHeaderCell {:collapsing true} "all"]]]
     (vec (concat [ui/TableBody] rows))]))


(defn acl-segment
  [acl]
  ^{:key "acl"}
  [cc/collapsible-segment
   "acl"
   (acl-table acl)])


(defn acl-test
  []
  [ui/Table {:text-align "center", :collapsing true, :celled true, :unstackable false}
   [ui/TableHeader
    [ui/TableRow
     [ui/TableHeaderCell {:rowSpan 2, :vertical-align "bottom", :collapsing true} "principal"]
     [ui/TableHeaderCell {:rowSpan 2, :vertical-align "bottom", :collapsing true} "type"]
     [ui/TableHeaderCell {:colSpan 2, :text-align "center", :collapsing true} "metadata"]
     [ui/TableHeaderCell {:colSpan 2, :text-align "center", :collapsing true} "data"]
     [ui/TableHeaderCell {:colSpan 2, :text-align "center", :collapsing true} "ACL"]
     [ui/TableHeaderCell {:rowSpan 2, :vertical-align "bottom", :collapsing true} "delete"]]
    [ui/TableRow
     [ui/TableHeaderCell {:collapsing true} "view"]
     [ui/TableHeaderCell {:collapsing true} "edit"]
     [ui/TableHeaderCell {:collapsing true} "view"]
     [ui/TableHeaderCell {:collapsing true} "edit"]
     [ui/TableHeaderCell {:collapsing true} "view"]
     [ui/TableHeaderCell {:collapsing true} "edit"]]]
   [ui/TableBody
    [ui/TableRow
     [ui/TableCell {:collapsing true} "some-owner"]
     [ui/TableCell {:collapsing true} "ROLE"]
     [ui/TableCell {:collapsing true} [ui/Icon {:name "check"}]]
     [ui/TableCell {:collapsing true} [ui/Icon {:name "check"}]]
     [ui/TableCell {:collapsing true} [ui/Icon {:name "check"}]]
     [ui/TableCell {:collapsing true} [ui/Icon {:name "check"}]]
     [ui/TableCell {:collapsing true} [ui/Icon {:name "check"}]]
     [ui/TableCell {:collapsing true} [ui/Icon {:name "check"}]]
     [ui/TableCell {:collapsing true} [ui/Icon {:name "check"}]]]
    [ui/TableRow
     [ui/TableCell {:collapsing true} "someone"]
     [ui/TableCell {:collapsing true} "USER"]
     [ui/TableCell {:collapsing true} [ui/Icon {:name "check"}]]
     [ui/TableCell {:collapsing true} "\u0020"]
     [ui/TableCell {:collapsing true} [ui/Icon {:name "check"}]]
     [ui/TableCell {:collapsing true} "\u0020"]
     [ui/TableCell {:collapsing true} [ui/Icon {:name "check"}]]
     [ui/TableCell {:collapsing true} "\u0020"]
     [ui/TableCell {:collapsing true} "\u0020"]]
    ]])
