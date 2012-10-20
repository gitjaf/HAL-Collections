package org.sigma.code.plugins

class HalCollectionBuilderService {

	def halBuilderService

	def grailsApplication

	def buildRepresentation = { model, String controller, String action, HashMap queryParams ->

		
		//FIXME Arreglar el caso en el que se pasa un mapa vacio de queryParams sin offset ni max
		
		//TODO Delegar si es posible la creacion de links.
		
		 //TODO intentar usar menos parametros
		
		def representation = new HashMap()

		def links = new HashMap()

		def offset = (queryParams.offset) ? queryParams.offset : 0

		def max = (queryParams.max) ? queryParams.max : 10

		def total = model.size

		representation.data = queryParams

		links.self = this.getLinkTo(controller, action, queryParams)

		if(offset > 0){
			links."previous" = this.getLinkTo(controller, action, queryParams.inject([:]) { qp, k, v ->
				qp << (k == "offset" ? [k:(Math.max((v - max),0))] : [k:v]) })
		}

		if(offset != total) {
			links."next" = this.getLinkTo(controller, action, queryParams.inject([:]) { qp, k, v ->
				qp << ("$k" == "offset" ? ["$k":(Math.min((v + max), total))] : ["$k":v]) })
		}

		representation._links = links

		representation._embedded = ["collection" : halBuilderService.buildModelList(model[offset..Math.min((offset + max), total)])]

		return representation
	}

	protected getLinkTo = { String controller, String action, HashMap queryParams ->

		return (halBuilderService.getLinkTo(controller, action) + queryParams.inject("?") { qp, k, v -> qp << k + "=" + v })

	}




}
