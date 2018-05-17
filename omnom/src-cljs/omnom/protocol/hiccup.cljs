(ns omnom.protocol.hiccup
  (:require [clojure.string :refer [escape]]
            [re-frame.core :as rf]
            [goog.string :as gs]
            [goog.string.format]
            [cemerick.url :as url]
            [omnom.utils :as u]))

;; Private functions

(defn- escape-html [s]
  (escape s {"&"  "&amp;" ">"  "&gt;" "<"  "&lt;" "\"" "&quot;"}))

(defn- includes? [xs x] (not= -1 (.indexOf (str xs) x)))

(defn- barf-number [x] (if (includes? x ".") (gs/format "%.2f" x) (str x)))

(defn- create-link
  [host path]
  (if (and (not (nil? path)) (.startsWith path "/")) (str host path) path))

(defn- click
  [host path method body]
  (if (= method "get")
    (rf/dispatch [:handler-with-http (create-link host path) method body])
    (rf/dispatch-sync [:handler-with-form (create-link host path) method body])))

;; Hiccup records

(defrecord Error [errors])

(defrecord Title [title])

(defrecord Link [title host method body title-attr])

(defprotocol Hiccup (hiccup [this] "Hiccup markup"))

(extend-protocol Hiccup
  nil
  (hiccup [_] [:span nil])

  Title
  (hiccup [this] [:h2 (:title this)])

  Error
  (hiccup
    [this]
    [:div {:class "alert alert-danger" :role "alert"}
          (hiccup (:errors this))])

  Link
  (hiccup
    [{:keys [title host method body title-attr]}]
    [:a {:href "#" :onClick #(click host title method body) :title title-attr} title])

  js/Boolean
  (hiccup [this] [:span (str this)])

  js/Number
  (hiccup [this] [:span (barf-number this)])

  js/String
  (hiccup [this] [:span (escape-html this)])

  js/Date
  (hiccup [this] [:span (.toString this)])

  Keyword
  (hiccup [this] [:span (name this)])

  PersistentArrayMap
  (hiccup [this]
    (if (empty? this)
      [:div [:span]]
      [:table {:class "table table-bordered"}
        [:tbody (for [[k v] this]
                  ^{:key k} [:tr [:th (hiccup (u/field-title k))]
                                 [:td (hiccup v)]])]]))

  PersistentHashSet
  (hiccup [this]
    (if (empty? this)
      [:div [:span]]
      [:ul {:class "list-unstyled"}
        (for [item this]
          ^{:key item} [:li (hiccup item)])]))

  PersistentVector
  (hiccup [this]
    (if (empty? this)
      [:div [:span]]
      [:ol {:class "list-unstyled"}
        (for [item this]
          ^{:key item} [:li (hiccup item)])])))
