(ns omnom.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            [cemerick.url :as url]
            [omnom.protocol.barf :as b]
            [omnom.macro :include-macros true :refer [project-version]]))

;; ------------- Event ---------------------------------------------------------

(rf/reg-event-db
  :save-api-host
  (fn [db [_ api-host]]
    (assoc db :api-host api-host)))

(rf/reg-event-db
  :good-http-result
  (fn [db [_ response]]
    (assoc db :api-body response :api-form nil)))

(rf/reg-event-db
  :bad-http-result
  (fn [db [_ {:keys [response status]}]]
    (if (= status 400)
      (assoc-in db [:api-form :errors] (:errors response))
      (assoc db :api-body response :api-form nil))))

(rf/reg-event-fx
  :handler-with-http
  (fn [{:keys [db]} [_ uri method body]]
    {:http-xhrio {:uri uri
                  :method (keyword method)
                  :params (.parse js/JSON body)
                  :timeout 8000
                  :format (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [:good-http-result]
                  :on-failure [:bad-http-result]}}))

(rf/reg-event-db
  :handler-with-form
  (fn [db [_ uri method body]]
    (assoc db :api-form {:uri uri :method method :body body}
              :api-body nil)))

;; ------------- Query ---------------------------------------------------------

(rf/reg-sub :api-host (fn [db _] (:api-host db)))

(rf/reg-sub :api-body (fn [db _] (:api-body db)))

(rf/reg-sub :api-form (fn [db _] (:api-form db)))

;; ------------- View ----------------------------------------------------------

(defn ui
  []
  (let [bodies @(rf/subscribe [:api-body])
        form @(rf/subscribe [:api-form])
        host @(rf/subscribe [:api-host])]
    (cond
      (:body form)    (b/barf (b/->Form "dahl+json") form host)
      form            (rf/dispatch [:handler-with-http (:uri form) (:method form) nil])
      (empty? bodies) "Enter the starting point of your API and hit explore."
      :else           [:ul {:class "list-unstyled"}
                        (doall (for [[resource body] bodies]
                          [:li {:key (name resource)}
                            [:h1 (name resource)]
                            (b/barf (b/->DahlJson "dahl+json") body host)]))])))

(defn version [] (str "v" (project-version)))

;; ------------- Public Api ----------------------------------------------------

(enable-console-print!)

(defn ^:export run
  []
  (reagent/render [ui] (js/document.getElementById "ui"))
  (reagent/render [version] (js/document.getElementById "version")))

(defn ^:export submit
  [event]
  (.preventDefault event) ; prevents default event of form submission to fire
  (when-let [ep (not-empty (.-value (js/document.getElementById "entrypoint")))]
    (let [{:keys [protocol host port]} (url/url ep)]
      (rf/dispatch-sync [:save-api-host (str protocol "://" host (if port (str ":" port) ""))])
      (rf/dispatch [:handler-with-http ep "get"]))))
