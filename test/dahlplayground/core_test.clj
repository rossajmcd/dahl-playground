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

(def better-drink-machine
  {nil {:init :Ready}
   :Ready {:make-tea :MakingTea}
   :MakingTea {:get-status :GettingTeaStatus}
   :GettingTeaStatus {:making-tea :MakingTea
                      :completing-tea :MadeTea
                      :erroring-tea :ErroredTea}})

(deftest blurb
  (viz/generate-image better-drink-machine
                      "fsm.png")
  (is (= (next-node drink-machine-config nil :init) :Ready))
  (is (= (next-node drink-machine-config :Ready :make-tea) :MakingTea)))
