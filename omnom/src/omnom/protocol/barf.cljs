(ns omnom.protocol.barf
  (:require [clojure.string :refer [lower-case replace split upper-case]]
            [re-frame.core :as rf]
            [omnom.protocol.hiccup :as h]
            [omnom.utils :as u]))

;; Private functions

(defn- format-embedded [embedded host]
  (mapv
    #(let [t (get-in % [:_links :self :title])
           x (get-in % [:_links :self :href])]
      (if x
        (-> % (dissoc :_links) (assoc :href (h/->Link x host "get" t)))
        (-> % (dissoc :_links))))
    embedded))

(defn- format-links
  [links host]
  (set (for [[k {:keys [href method body title]}] links]
    {(u/field-title k) (h/->Link href host method body title)})))

;; Barfing records

(defrecord DahlJson [media-type])

(defrecord Form [media-type])

(defprotocol Barf (barf [this p1 p2] "Media Type independent markup barfing"))

(extend-protocol Barf

  DahlJson
  (barf [_ json host]
    (let [title (get-in json [:_links :self :href])
          entity (dissoc json :_links :_embedded)
          links (dissoc (:_links json) :self)]
      [:div
        (h/hiccup (h/->H3LinkTitle title host))
        (h/hiccup entity)
        (for [[embed-title embed-xs] (:_embedded json)]
          (h/hiccup [(h/->H2Title (u/name2 embed-title))
                     (format-embedded embed-xs host)]))
        (when links (h/hiccup (h/->H2Title "links")))
        (h/hiccup (format-links links host))]))

  Form
  (barf [_ {:keys [uri method body]} host]
    [:form {:onSubmit (fn [e]
                        (.preventDefault e)
                        (let [d (.-value (js/document.getElementById "payload"))]
                          (rf/dispatch [:handler-with-http uri method d])))}
      [:div {:class "form-group"}
        [:label {:for "payload"} "Body"]
        [:textarea {:id "payload" :name "payload" :class "form-control" :rows 10 :defaultValue body}]]
      [:button {:type "submit" :id "send-btn" :class "btn btn-primary float-right"} "Send"]]))
