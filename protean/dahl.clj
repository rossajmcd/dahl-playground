(ns dahl
  (:require [clojure.string :as str]
            [clj-http.client :as http]))

(defn current-node-key
  [nodes]
  (first (keys (last nodes))))

(defn- apply-rule
  [nodes graph node-key rules]
  (let [r (when-let [rule (get rules node-key)] (apply rule [nodes]))]
    (cond
      (nil? node-key) node-key
      (map? r)        r
      (future? r)     {node-key (get graph node-key)}
      :else           {node-key (get graph node-key)})))

(defn- generate-uuids
  [nodes graph rules node-key]
  (let [result (apply-rule nodes graph node-key rules)
        edges (flatten (map keys (vals result)))]
    (into {} (for [e edges] {e (str (java.util.UUID/randomUUID))}))))

(defn- msg
  [k v opts]
  (str k ": '" v "' does not match one of: [" (str/join ", " opts) "]"))

(defn- errors
  [curr-n bodies edge body uuid edges]
  (if (nil? edge)
    [(msg "uuid" uuid (vals edges))]
    (->> (get-in bodies [curr-n edge])
         (remove (fn [[k vs]] (.contains vs (get body k))))
         (map (fn [[k vs]] (msg (name k) (get body k) vs)))
         seq)))

(defn- get-resource
  [entrypoint]
  (last (str/split entrypoint #"/")))

(defn- result
  [entrypoint states uuids node-key bodies]
  (let [edges (remove (fn [[k _]] (or (nil? k) (str/includes? (name k) "-machine"))) uuids)]
    {(get-resource entrypoint) {
     :states states
     :controls (into {} (for [[edge uuid] edges]
       {edge {:method "post"
              :href (str "/" entrypoint "/" uuid)
              :body (when-let [b (get-in bodies [node-key edge])]
                      (clojure.data.json/write-str b))}}))}}))

(defn- proxy-links
  [entrypoint apis raw-body]
  (let [reg (re-pattern (str "(" (str/join "|" (map get-resource apis)) ")/.*"))]
    (clojure.walk/prewalk #(if-let [f (and (string? %) (first (re-find reg %)))]
                            (str "/" entrypoint "/" f)
                            %)
                          raw-body)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;; API ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

(defn get
  [entrypoint nodes graph rules bodies]
  (let [curr-n (current-node-key nodes)
        uuids (generate-uuids nodes graph rules curr-n)
        res (result entrypoint nodes uuids curr-n bodies)]
    {:status 200 :edges uuids :result res}))

(defn post
  [entrypoint nodes edges graph rules bodies uuid body]
  (let [edge (get-edge edges uuid)
        curr-n (current-node-key nodes)
        errors (when-let [e (errors curr-n bodies edge body uuid edges)]
                 (assoc (result entrypoint nodes edges curr-n bodies) :errors e))]
    (cond
      (nil? edge) {:status 404 :result errors}
      errors      {:status 400 :result errors}
      :else       (let [next-n (next-node-key graph curr-n edge)
                        states (update-node nodes {next-n body})
                        uuids (generate-uuids states graph rules next-n)
                        res (result entrypoint states uuids next-n bodies)]
                    {:status 200 :nodes states :edges uuids :result res}))))

(defn proxy-get
  [entrypoint apis]
  (proxy-links entrypoint apis (into {} (map #(:body (http/get % {:as :json})) apis))))

(defn proxy-post
  [entrypoint apis resource uuid body]
  (if-let [f (first (filter #(= (get-resource %) resource) apis))]
    (let [a (http/post (str f "/" uuid) {:body (clojure.data.json/json-str body)
                                         :headers {"Content-Type" "application/json"}
                                         :as :json})
          b (into {} (map #(:body (http/get % {:as :json}))
                          (remove #(= (get-resource %) resource) apis)))]
      (proxy-links entrypoint apis (merge b (:body a) b)))
    (proxy-get entrypoint apis)))
