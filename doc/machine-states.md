***** GENERAL ASSUMPTIONS *****

- As we are layering events over state over time, it seems logical to use POST.
- Only the one URI into this - the entrypoint, which is basically therefore the
self link equivalent.


***** Entrypoint *****

GET /drinkmachine

{
  :resource "/drinkmachine"
  :state {:"Ready" {}}
  :controls
    {
      "select-ingredients" {
        :method "POST"
        :href "/drinkmachine"
        :required {
          :body {:event "select-ingredients"}
        }
      }
    }
}





***** select ingredients *****

POST /drinkmachine

{
    "event": "select-ingredients"
}



***** Exposing the interface for select ingredients *****
- N.B. No option to make drink as ingredients are not selected yet.

GET /drinkmachine

{
  :resource "/drinkmachine"
  :state {"selecting-ingredients" {:beverage "none" :milk "none" :sugar 0}}
  :controls {
    "add-beverage" {
      :method "POST"
      :href "/drinkmachine"
      :requires {
        :body {
          :event "add-beverage"
          :beverage #{"none" "tea" "coffee"}]
        }
      }
    }


    "add-milk" {
      :method "POST"
      :href "/drinkmachine"
      :requires {
        :body {
          :event "add-milk"
          :milk #{"none" "semi" "full"}
        }
      }
    }


    {
      :rel "add-sugar"
      :method "POST"
      :href "/drinkmachine"
      :requires {}
        :event "add-sugar"
        :sugar #{0 1 2}}}






***** add-beverage 'tea' *****

POST /drinkmachine

{
    "event": "add-beverage"
    "beverage": "tea"}



***** Exposing the interface for select ingredients after adding tea *****
- N.B. The only required ingredient is beverage, now we could make the drink
if we wanted without milk or sugar.

GET /drinkmachine

{
  :resource "/drinkmachine"
  :state {:state "selecting-ingredients" :beverage "tea" :milk "none" :sugar 0}
  :controls []
    {
      :rel "add-milk"
      :method "POST"
      :href "/drinkmachine"
      :requires {}
        :event "add-milk"
        :milk #{"none" "semi" "full"}}


    {
      :rel "add-sugar"
      :method "POST"
      :href "/drinkmachine"
      :requires {}
        :event "add-sugar"
        :sugar #{0 1 2}}


    {
      :rel "make-drink"
      :method "POST"
      :href "/drinkmachine"
      :requires {}
        :event "make-drink"}}






***** add-milk 'semi' *****

POST /drinkmachine

{
    "event": "add-milk"
    "milk": "semi"}



***** Exposing the interface for select ingredients after adding milk *****
- N.B. The only required ingredient is beverage, now we could make the drink
if we wanted without milk or sugar.

GET /drinkmachine

{
  :resource "/drinkmachine"
  :state {:state "selecting-ingredients" :beverage "tea" :milk "semi" :sugar 0}
  :controls []
    {
      :rel "add-sugar"
      :method "POST"
      :href "/drinkmachine"
      :requires {}
        :event "add-sugar"
        :sugar #{0 1 2}}


    {
      :rel "make-drink"
      :method "POST"
      :href "/drinkmachine"
      :requires {}
        :event "make-drink"}}






***** add-sugar '1' *****

POST /drinkmachine

{
    "event": "add-sugar"
    "sugar": 1}



***** Exposing the interface for select ingredients after adding sugar *****
- N.B. The only required ingredient is beverage, now we could make the drink
if we wanted without milk or sugar.

GET /drinkmachine

{
  :resource "/drinkmachine"
  :state {:state "selecting-ingredients" :beverage "tea" :milk "semi" :sugar 1}
  :controls []
    {
      :rel "make-drink"
      :method "POST"
      :href "/drinkmachine"
      :requires {}
        :event "make-drink"}}






***** make-drink *****

POST /drinkmachine

{
    "event": "make-drink"}



***** Exposing the interface while making drink *****
- N.B. For 1 minute the machine is busy making the drink.
Hitting the entrypoint during this time will yield a state of 'making-drink'.
There is no control here for get-status as we still don't think it is necessary
this entrypoint tells us everything we need to know.  We have POST a contrived
emergency shutdown control though - lets pretend the cup is overfilling ?

GET /drinkmachine

{
  :resource "/drinkmachine"
  :state {:state "making-drink" :beverage "tea" :milk "semi" :sugar 1}
  :controls []
    {
      :rel "shutdown"
      :method "POST"
      :href "/drinkmachine"
      :requires {}
        :event "shutdown"}}






***** Exposing the interface when drink is made *****
- N.B. After 1 minute the drink is made and the cup needs to be removed from
the machine.  We retain the beverage, milk and sugar information to make this
response useful to the consumer.  Lets assume that a sensor reading tells us
the cup is still in its slot so we cannot proceed to any controls bar
shutdown at this time.

GET /drinkmachine

{
  :resource "/drinkmachine"
  :state {:state "drink-ready" :beverage "tea" :milk "semi" :sugar 1}
  :controls []
    {
      :rel "shutdown"
      :method "POST"
      :href "/drinkmachine"
      :requires {}
        :event "shutdown"}}
