(ns dahlplayground.core)

(defn fsm [graph init]
  (fn [edges] (reduce #(get graph [%1 %2] :edge-error) init edges)))

(def graph-config
  {[:unfilled :fill] :filled
   [:filled :empty] :unfilled})
