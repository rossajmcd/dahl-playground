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

PUT /drinkmachine HTTP/1.1

{
    "event": "select-ingredients"
}

HTTP/1.1 200 OK

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
		    :beverage #{"none" "tea" "coffee"}
		  }
    }
    {
	    :rel "add-milk"
	    :method "PUT"
	    :href "/drinkmachine"
	    :requires {
		    :milk #{"none" "semi" "full"}
		  }
    }
    {
	    :rel "add-sugar"
	    :method "PUT"
	    :href "/drinkmachine"
	    :requires {
		    :sugar #{0 1 2}
		  }
    }
  ]
}
