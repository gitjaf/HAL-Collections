package org.sigma.code.plugins

import java.math.*
import org.codehaus.groovy.grails.commons.GrailsApplication

class HalCollectionBuilderService {

	def halBuilderService

	def page = 0

	def itemsPerPage = 10

	def lastPage = 0	

	def total = 0

	def excludedParams = ['action', 'controller' ]

	def buildRepresentation = {model, String httpMethod, queryParams ->
		
		def representation = new HashMap()

		def links = new HashMap()

		def controller = queryParams.controller

		def action = queryParams.action.get(httpMethod)

		println queryParams

		println controller

		println action

		excludedParams.each{queryParams.remove(it)}

		queryParams.itemsPerPage = queryParams.itemsPerPage ?: itemsPerPage

		itemsPerPage = queryParams.itemsPerPage as Integer

		queryParams.page = (queryParams.page ? queryParams.page : page)

		page = queryParams.page as Integer

		total = model.size

		lastPage = (new BigDecimal((total / itemsPerPage), new MathContext(1, RoundingMode.DOWN))) as Integer

		queryParams.pages = 0..lastPage

		representation.data = queryParams

		links.self = this.getLinkTo(controller, action, queryParams)

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
			links."previous" = this.getLinkTo(controller, action, queryParams.inject([:]) { qp, k, v ->
				qp << ("$k" == "page" ? ["$k":(Math.max((page - 1),0))] : ["$k":v] )})
		}

		if(page < lastPage && total > (itemsPerPage * page)) {
			links."next" = this.getLinkTo(controller, action, queryParams.inject([:]) { qp, k, v ->
				qp << ("$k" == "page" ? ["$k": Math.min((page + 1), lastPage)] : ["$k":v]) })
		}

		return links
	}


	protected getEmbedded = { queryParams, model -> 

		def offset = itemsPerPage * ((page < lastPage) ? page : lastPage) // Starting item of the page

		def max = (itemsPerPage + offset - 1) // Max quantity of items

		return ["collection" : halBuilderService.buildModelList(model[offset..Math.min(max, (total - 1))])]

	}

}
