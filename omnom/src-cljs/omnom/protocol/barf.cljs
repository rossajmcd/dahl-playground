(ns omnom.protocol.barf
  (:require [re-frame.core :as rf]
            [form.juice :as fj]
            [omnom.protocol.hiccup :as h]
            [omnom.utils :as u]))

;; Barfing records

(defrecord DahlJson [media-type])

(defrecord Form [media-type])

(defprotocol Barf (barf [this p1 p2] "Media Type independent markup barfing"))

(extend-protocol Barf

  DahlJson
  (barf [_ {:keys [states controls errors]} host]
    [:div
      (when-not (empty? errors) (h/hiccup (h/->Error errors)))
      (h/hiccup (h/->Title "States"))
      (h/hiccup states)
      (when-not (empty? controls) (h/hiccup (h/->Title "Controls")))
      (h/hiccup (set (for [[k {:keys [href method body title]}] controls]
        {(u/field-title k) (h/->Link href host method body title)})))])

  Form
  (barf [_ {:keys [uri method body errors]} host]
    (let [clj-body (js->clj (.parse js/JSON body) :keywordize-keys true)]
      (when-not (empty? errors) (h/hiccup (h/->Error errors)))
      [:form {:class "clearfix"
              :id "dahl-form"
              :onSubmit (fn [e]
                          (.preventDefault e)
                          (let [d (form.juice/squeeze e)]
                            (rf/dispatch [:handler-with-http uri method d])))}
        clj-body
        [:button {:type "submit" :id "send-btn" :class "btn btn-primary float-right"} "Send"]])))
