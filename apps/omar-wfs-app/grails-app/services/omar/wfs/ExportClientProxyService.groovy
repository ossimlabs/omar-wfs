package omar.wfs

class ExportClientProxyService {
    @Value('${omar.wfs.app.geoscript.url}')
    def geoscriptEndpoint

    def grailsLinkGenerator

    def getExportShapefileURL(GetFeatureRequest wfsParams) {
        // copy non-null values
        def params = ['typeName', 'filter', 'maxFeatures', 'startIndex'].inject([:]) { a, b ->
            if ( wfsParams[b] ) {
                a[b] = wfsParams[b] 
            }
            a
        }

        grailsLinkGenerator.link( 
            base: geoscriptEndpoint - '/geoscriptApi',
            absolute: true, uri: '/export/exportShapefile', params: params )
    }

}
