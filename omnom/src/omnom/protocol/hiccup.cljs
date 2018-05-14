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

(defrecord H1LinkTitle [title host])

(defrecord H3LinkTitle [title host])

(defrecord H2Title [title])

(defrecord Link [title host method body title-attr])

(defprotocol Hiccup (hiccup [this] "Hiccup markup"))

(extend-protocol Hiccup
  nil
  (hiccup [_] [:span nil])

  H1LinkTitle
  (hiccup
    [{:keys [title host method body]}]
    [:h1 [:a {:href "#" :onClick #(click host title method body)} title]])

  H3LinkTitle
  (hiccup
    [{:keys [title host method body]}]
    [:h3 [:a {:href "#" :onClick #(click host title method body)} title]])

  H2Title
  (hiccup [this] [:h2 (:title this)])

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
