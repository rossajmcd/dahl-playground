(ns omnom.protocol.barf
  (:require [re-frame.core :as rf]
            [omnom.protocol.hiccup :as h]
            [omnom.utils :as u]))

;; Barfing records

(defrecord DahlJson [media-type])

(defrecord Form [media-type])

(defprotocol Barf (barf [this p1 p2] "Media Type independent markup barfing"))

(extend-protocol Barf

  DahlJson
  (barf [_ {:keys [states controls]} host]
    [:div
      (h/hiccup (h/->Title "States"))
      (h/hiccup states)
      (when-not (empty? controls) (h/hiccup (h/->Title "Controls")))
      (h/hiccup (set (for [[k {:keys [href method body title]}] controls]
        {(u/field-title k) (h/->Link href host method body title)})))])

  Form
  (barf [_ {:keys [uri method body errors]} host]
    (let [clj-body (js->clj (.parse js/JSON body) :keywordize-keys true)
          example (into {} (map (fn [[k v]] {k (first v)}) clj-body))
          json (.stringify js/JSON (clj->js example))]
      [:form {:onSubmit (fn [e]
                          (.preventDefault e)
                          (let [d (.-value (js/document.getElementById "payload"))]
                            (rf/dispatch [:handler-with-http uri method d])))}
        (when-not (empty? errors) (h/hiccup (h/->Error errors)))
        [:div {:class "form-group"}
          [:label {:for "payload"} "Body"]
          [:textarea {:id "payload" :name "payload" :class "form-control" :rows 10 :defaultValue json}]]
        [:button {:type "submit" :id "send-btn" :class "btn btn-primary float-right"} "Send"]])))
