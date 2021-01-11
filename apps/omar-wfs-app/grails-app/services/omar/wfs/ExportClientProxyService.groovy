package omar.wfs

import org.springframework.beans.factory.annotation.Value
import grails.web.mapping.LinkGenerator

class ExportClientProxyService extends  ExportClientService {
    
    LinkGenerator grailsLinkGenerator

    def getExportShapefileURL(GetFeatureRequest wfsParams) {
        // copy non-null values
        def params = ['typeName', 'filter', 'maxFeatures', 'startIndex'].inject([:]) { a, b ->
            if ( wfsParams[b] ) {
                a[b] = wfsParams[b] 
            }
            a
        }

        grailsLinkGenerator.link( 
            absolute: true, uri: '/export/exportShapefile', params: params 
        ).replace("omar-wfs", "omar-geoscript")
    }

}
