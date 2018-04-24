(ns dahlplayground.core-test
  (:require [clojure.test :refer :all]
            [fsmviz.core :as viz]
            [dahlplayground.core :refer [next-node]]))

(def drink-machine-config
  {nil {:init :Ready}
   :Ready {:make-tea :MakingTea
           :make-coffee :MakingCoffee}
   :MakingTea {:get-status :MakingTea
               :prepared :MadeTea}
   :MakingCoffee {:get-status :MakingCoffee
                  :prepared :MadeCoffee}})

(deftest blurb
  (viz/generate-image drink-machine-config
                      "fsm.png")
  (is (= (next-node drink-machine-config nil :init) :Ready))
  (is (= (next-node drink-machine-config :Ready :make-tea) :MakingTea)))
