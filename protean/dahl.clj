(ns dahl
  (:require [clojure.string :as str]))

(defn current-node-key
  [nodes]
  (first (keys (last nodes))))

(defn next-node-key
  [graph node-key edge]
  (get-in graph [node-key edge]))

(defn update-node
  [a b]
  (let [[k v] (first b)]
    (if-let [idx (some #(when (get % k) (.indexOf a %)) a)]
      (update-in a [idx k] merge v)
      (conj a b))))

(defn get-edge
  [edges uuid]
  (get (clojure.set/map-invert edges) uuid))

(defn- apply-rule
  [graph node-key rules]
  (let [r (when-let [rule (get rules node-key)] (apply rule []))]
    (cond
      (nil? node-key) node-key
      (map? r)        r
      (future? r)     {node-key (get graph node-key)}
      :else           {node-key (get graph node-key)})))

(defn generate-uuids
  [graph rules node-key]
  (let [result (apply-rule graph node-key rules)
        edges (flatten (map keys (vals result)))]
    (into {} (for [e edges] {e (str (java.util.UUID/randomUUID))}))))

(defn- msg
  [k v opts]
  (str k ": '" v "' does not match one of: [" (str/join ", " opts) "]"))

(defn errors
  [curr-n bodies edge body uuid edges]
  (if (nil? edge)
    [(msg "uuid" uuid (vals edges))]
    (->> (get-in bodies [curr-n edge])
         (remove (fn [[k vs]] (.contains vs (get body k))))
         (map (fn [[k vs]] (msg (name k) (get body k) vs)))
         seq)))

(defn result
  [entrypoint states uuids node-key bodies]
  (let [edges (remove (fn [[k _]] (or (nil? k) (str/includes? (name k) "-machine"))) uuids)]
    {:resource (last (str/split entrypoint #"/"))
     :states states
     :controls (into {} (for [[edge uuid] edges]
       {edge {:method "post"
              :href (str "/" entrypoint "/" uuid)
              :body (when-let [b (get-in bodies [node-key edge])]
                      (clojure.data.json/write-str b))}}))}))
