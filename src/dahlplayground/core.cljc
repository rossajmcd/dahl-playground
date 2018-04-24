(ns dahlplayground.core)

(defn next-node [graph node edge] (get-in graph [node edge]))
