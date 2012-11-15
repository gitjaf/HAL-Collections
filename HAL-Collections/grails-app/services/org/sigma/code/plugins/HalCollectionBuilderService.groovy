package org.sigma.code.plugins

import java.math.*
import org.codehaus.groovy.grails.commons.GrailsApplication

class HalCollectionBuilderService {

	def halBuilderService

	def page = 0

	def itemsPerPage = 10

	def visiblePages = 5

	def lastPage = 0	

	def total = 0

	def excludedParams = ['action', 'controller']

	def buildRepresentation = {model, String httpMethod, queryParams ->
		
		def representation = new HashMap()

		def links = new HashMap()

		def controller = queryParams.controller

		def action = queryParams.action.get(httpMethod)

		excludedParams.each{queryParams.remove(it)}

		queryParams.itemsPerPage = queryParams.itemsPerPage ?: itemsPerPage

		itemsPerPage = queryParams.itemsPerPage as Integer

		queryParams.page = (queryParams.page ? queryParams.page : page)

		total = model.size

		lastPage = (new BigDecimal((total / itemsPerPage), new MathContext(1, RoundingMode.DOWN))) as Integer

		page = ((queryParams.page as Integer) <= lastPage ? queryParams.page as Integer : 0)

		representation.data = queryParams.inject([:]) {qp, k, v -> qp << ["$k":v]}

		representation.data.total = total

		links.self = ['href' : this.getLinkTo(controller, action, queryParams)]

		representation._links = this.getNavigationLinks(controller, action, links, queryParams)

		representation._embedded = this.getEmbedded(queryParams, model)

		return representation

	}

	protected getLinkTo = { String controller, String action, queryParams ->

		def query = ''

		if(!queryParams.isEmpty()){
			
			query = queryParams.inject([]) { qp, k, v -> qp << (k + "=" + v)}

			query = "?" + query.join("&")
		}

		return (halBuilderService.getLinkTo(controller, action) + query)

	}


	protected getNavigationLinks = {String controller, String action, HashMap links, queryParams -> 
		
		if(page > 0){
			links."previous" = ['href' : this.getLinkTo(controller, action, queryParams.inject([:]) { qp, k, v ->
				qp << ("$k" == "page" ? ["$k":(Math.max((page - 1),0))] : ["$k":v] )})]
		}
		
		links.pages = ((Math.max(page - visiblePages, 0))..(Math.min(page + visiblePages, lastPage))).
			collectEntries { it ->
				["$it", ['href' : this.getLinkTo(controller, action, queryParams.inject([:]){qp, k, v ->
					qp << ("$k" == "page" ? ["$k" : it] : ["$k":v] )})]]
			}
	

		if(page < lastPage && total > (itemsPerPage * page)) {
			links."next" = ['href': this.getLinkTo(controller, action, queryParams.inject([:]) { qp, k, v ->
				qp << ("$k" == "page" ? ["$k": Math.min((page + 1), lastPage)] : ["$k":v]) })]
		}

		return links
	}
	

	protected getEmbedded = { queryParams, model -> 
		if(total != 0){

			def offset = itemsPerPage * (Math.min(page, lastPage)) // Starting item of the page

			def max = Math.max(itemsPerPage + offset - 1, 0) // Max quantity of items

			def tot = Math.max(total - 1, 0) // Max quantity of items in the last page

			return ["collection" : halBuilderService.buildModelList(model[offset..Math.min(max, tot)])]
		} 

		return ["collection" : [:]]

	}

}
