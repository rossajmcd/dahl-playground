(ns dahlplayground.core-test
  (:require [clojure.test :refer :all]
            [fsmviz.core :as viz]
            [dahlplayground.core :refer [next-node]]))

(def better-drink-machine
  {nil {:init :Ready}
   :Ready {:make-tea :MakingTea
           :make-coffee :MakingCoffee}
   :MakingTea {:get-status :GettingTeaStatus?}
   :MakingCoffee {:get-status :GettingCoffeeStatus?}
   :GettingTeaStatus? {:making-tea :MakingTea
                       :completing-tea :MadeTea
                       :erroring-tea :ErroredTea}
   :GettingCoffeeStatus? {:making-coffee :MakingCoffee
                          :completing-coffee :MadeCoffee
                          :erroring-coffee :ErroredCoffe}})

(def ingredients-drink-machine
  {nil {:init :Ready}
   :Ready {:select-ingredients :Ingredients}
   :Ingredients {:add-beverage-required :Ingredients
                 :add-milk :Ingredients
                 :add-sugar :Ingredients
                 :make-drink :MakingDrink}
   :MakingDrink {:shutdown :ShutDown
                 :complete-machine :MadeDrink}
   :MadeDrink {:shutdown :ShutDown}
   :Shutdown {}})

(deftest blurb
  (viz/generate-image ingredients-drink-machine
                      "fsm.png")
  (is (= (next-node better-drink-machine nil :init) :Ready))
  (is (= (next-node better-drink-machine :Ready :make-tea) :MakingTea)))
