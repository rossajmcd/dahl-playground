***** Entrypoint *****

GET /drinkmachine HTTP/1.1

{
  :resource "/drinkmachine"
  :state "Ready"
  :controls [
    {
      :rel "select-ingredients"
      :method "PUT"
      :href "/drinkmachine"
      :requires {:event "select-ingredients"}
    }
  ]
}

***** Opting to select ingredients *****

PUT /drinkmachine HTTP/1.1

{
    "event": "select-ingredients"
}

HTTP/1.1 200 OK


***** Exposing the interface for select ingredients *****
N.B. If consumer had already selected milk only sugar and beverage would be
present as controls... simplistic assumption at this point that we would do
one at a time.

{
	:resource "/drinkmachine"
	:state "selecting-ingredients"
  :beverage "none"
	:milk "none"
  :sugar 0
	:controls [
    {
		  :rel "get-status"
		  :method "PUT"
		  :href "/drinkmachine"
		  :requires {:event "get-status"}
	  }
    {
	    :rel "add-beverage"
	    :method "PUT"
	    :href "/drinkmachine"
	    :requires {
        :event "add-beverage"
		    :beverage #{"none" "tea" "coffee"}
		  }
    }
    {
	    :rel "add-milk"
	    :method "PUT"
	    :href "/drinkmachine"
	    :requires {
        :event "add-milk"
		    :milk #{"none" "semi" "full"}
		  }
    }
    {
	    :rel "add-sugar"
	    :method "PUT"
	    :href "/drinkmachine"
	    :requires {
        :event "add-sugar"
		    :sugar #{0 1 2}
		  }
    }
  ]
}


***** Opting to add-beverage 'tea' *****

PUT /drinkmachine HTTP/1.1

{
    "event": "add-beverage"
    "beverage": "tea"
}

HTTP/1.1 200 OK



***** Exposing the interface for select ingredients after adding tea *****
TODO: N.B. if we have self link do we need the get-status machinery ?

{
	:resource "/drinkmachine"
	:state "selecting-ingredients"
  :beverage "tea"
	:milk "none"
  :sugar 0
	:controls [
    {
		  :rel "get-status"
		  :method "PUT"
		  :href "/drinkmachine"
		  :requires {:event "get-status"}
	  }
    {
	    :rel "add-milk"
	    :method "PUT"
	    :href "/drinkmachine"
	    :requires {
        :event "add-milk"
		    :milk #{"none" "semi" "full"}
		  }
    }
    {
	    :rel "add-sugar"
	    :method "PUT"
	    :href "/drinkmachine"
	    :requires {
        :event "add-sugar"
		    :sugar #{0 1 2}
		  }
    }
  ]
}
