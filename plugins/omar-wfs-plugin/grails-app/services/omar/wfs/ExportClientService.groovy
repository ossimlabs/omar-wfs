package omar.wfs

class ExportClientService {
    def grailsLinkGenerator

    def getExportShapefileURL(GetFeatureRequest wfsParams) {

        // copy non-null values
        def params = ['typeName', 'filter', 'maxFeatures', 'startIndex'].inject([:]) { a, b ->
            if ( wfsParams[b] ) {
                a[b] = wfsParams[b] 
            }
            a
        }

        grailsLinkGenerator.link( absolute: true, uri: '/export/exportShapefile', params: params )
    }
}