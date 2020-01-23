package omar.wfs


import io.swagger.annotations.Api
import io.swagger.annotations.ApiImplicitParam
import io.swagger.annotations.ApiImplicitParams
import io.swagger.annotations.ApiOperation
import omar.core.BindUtil
import omar.core.OmarWebUtils
import org.grails.web.util.WebUtils

import java.nio.charset.StandardCharsets

@Api(value = "/wfs",
     description = "WFS Support"
)
class WfsController
{
  WebFeatureService webFeatureService

  static int DEFAULT_MAX_FEATURES = 1000

  static String defaultAction = "index"
  static String defaultFilename = "omar-wfs-getFeature-export.kml"

  def index()
  {
    Map wfsParams = params - params.subMap( ['controller', 'format', 'action'] )
    String operation = wfsParams.find { it.key.equalsIgnoreCase( 'request' ) }

    switch ( request?.method?.toUpperCase() )
    {
    case 'GET':
      operation = wfsParams.find { it.key.equalsIgnoreCase( 'request' ) }?.value
      break
    case 'POST':
      operation = request?.XML?.name()
      break
    case 'OPTIONS':
      def timestamp = new Date().format( "yyyy-MM-dd'T'HH:mm:ss.SSSZ" )
      render contentType: 'text/plain', text: timestamp
      break
    }

    def results

    switch ( operation?.toUpperCase() )
    {
    case "GETCAPABILITIES":
      def cmd = new GetCapabilitiesRequest()

      cmd.username = webFeatureService.extractUsernameFromRequest(request)

      switch ( request?.method?.toUpperCase() )
      {
      case 'GET':
        BindUtil.fixParamNames( GetCapabilitiesRequest, wfsParams )
        bindData( cmd, wfsParams )
        break
      case 'POST':
        cmd = cmd.fromXML( request.XML )
        break
      }

      results = webFeatureService.getCapabilities( cmd )
      break
    case "DESCRIBEFEATURETYPE":
      def cmd = new DescribeFeatureTypeRequest()

      switch ( request?.method?.toUpperCase() )
      {
      case 'GET':
        BindUtil.fixParamNames( DescribeFeatureTypeRequest, wfsParams )
        bindData( cmd, wfsParams )
        break
      case 'POST':
        cmd = cmd.fromXML( request.XML )
        break
      }

      results = webFeatureService.describeFeatureType( cmd )

      break
    case "GETFEATURE":
      def cmd = new GetFeatureRequest()

      switch ( request?.method?.toUpperCase() )
      {
      case 'GET':
        BindUtil.fixParamNames( GetFeatureRequest, wfsParams )
        bindData( cmd, wfsParams )
        break
      case 'POST':
        cmd = cmd.fromXML( request.XML )
        break
      }

      if ( !cmd.outputFormat ) {        
        cmd.outputFormat = 'GML3'
      } 
      
      results = webFeatureService.getFeature( cmd )
      break
    default:
      throw new Exception('UNKNOWN REQUEST')
    }

    String format = webFeatureService.parseOutputFormat(wfsParams?.outputFormat ?: 'GML3')

    if (operation?.toUpperCase()?.equals("GETFEATURE") && format == 'KML'){
      if(results.filename) {
        response.setHeader("Content-Disposition", "attachment;filename=${results.filename}")
      } else {
        response.setHeader("Content-Disposition", "attachment;filename=${defaultFilename}")
      }
    }

    String outputBuffer
    if (operation?.toUpperCase()?.equals("GETFEATURE") && format == null) {
      outputBuffer = encodeResponse(results)
      render outputBuffer
    } else {
      outputBuffer = encodeResponse(results.text)
      render 'contentType': results.contentType, 'text': outputBuffer
    }
  }

  @ApiOperation(value = "Get the capabilities of the server",
                produces='application/xml',
                httpMethod="GET",
                nickname = "getCapabilities")
  @ApiImplicitParams([
          @ApiImplicitParam(name = 'service', value = 'OGC Service type', allowableValues="WFS", defaultValue = 'WFS', paramType = 'query', dataType = 'string', required=true),
          @ApiImplicitParam(name = 'version', value = 'Version to request', allowableValues="1.1.0", defaultValue = '1.1.0', paramType = 'query', dataType = 'string', required=true),
          @ApiImplicitParam(name = 'request', value = 'Request type', allowableValues="GetCapabilities", defaultValue = 'GetCapabilities', paramType = 'query', dataType = 'string', required=true),
  ])
  def getCapabilities()
  {
    GetCapabilitiesRequest wfsParams = new GetCapabilitiesRequest()

    BindUtil.fixParamNames( GetCapabilitiesRequest, params )
    bindData( wfsParams, params )
    wfsParams.username = webFeatureService.extractUsernameFromRequest(request)

    def results = webFeatureService.getCapabilities( wfsParams )

    String outputBuffer = encodeResponse(results.text)

    render contentType: results.contentType, text: outputBuffer
  }

  @ApiOperation(value = "Describe the feature from the server",
                produces='application/xml',
                httpMethod="GET")
  @ApiImplicitParams([
          @ApiImplicitParam(name = 'service', value = 'OGC Service type', allowableValues="WFS", defaultValue = 'WFS', paramType = 'query', dataType = 'string', required=true),
          @ApiImplicitParam(name = 'version', value = 'Version to request', allowableValues="1.1.0", defaultValue = '1.1.0', paramType = 'query', dataType = 'string', required=true),
          @ApiImplicitParam(name = 'request', value = 'Request type', allowableValues="DescribeFeatureType", defaultValue = 'DescribeFeatureType', paramType = 'query', dataType = 'string', required=true),
          @ApiImplicitParam(name = 'typeName', value = 'Type Name', defaultValue="omar:raster_entry", paramType = 'query', dataType = 'string', required=true)
  ])
  def describeFeatureType(/*DescribeFeatureTypeRequest wfsParams*/)
  {
    DescribeFeatureTypeRequest wfsParams = new DescribeFeatureTypeRequest()

    BindUtil.fixParamNames( DescribeFeatureTypeRequest, params )
    bindData( wfsParams, params )
    wfsParams.username = webFeatureService.extractUsernameFromRequest(request)

    def results = webFeatureService.describeFeatureType( wfsParams )

    String outputBuffer = encodeResponse(results.text)

    render contentType: results.contentType, text: outputBuffer
  }

  @ApiOperation(value = "Get features from the server",
                produces='application/xml,application/json',
                httpMethod="GET")
  @ApiImplicitParams([
          @ApiImplicitParam(name = 'service', value = 'OGC service type', allowableValues="WFS", defaultValue = 'WFS', paramType = 'query', dataType = 'string', required=true),
          @ApiImplicitParam(name = 'version', value = 'Version to request', allowableValues="1.1.0", defaultValue = '1.1.0', paramType = 'query', dataType = 'string', required=true),
          @ApiImplicitParam(name = 'request', value = 'Request type', allowableValues="GetFeature", defaultValue = 'GetFeature', paramType = 'query', dataType = 'string', required=true),
          @ApiImplicitParam(name = 'typeName', value = 'Type name', defaultValue="omar:raster_entry", paramType = 'query', dataType = 'string', required=true),
          @ApiImplicitParam(name = 'filter', value = 'Filter', paramType = 'query', dataType = 'string', required=false),
          @ApiImplicitParam(name = 'resultType', value = 'Result type', defaultValue="", allowableValues="results,hits", paramType = 'query', dataType = 'string', required=false),
          @ApiImplicitParam(name = 'outputFormat', value = 'Output format', defaultValue="", allowableValues="JSON, KML, CSV, GML2, GML3, GML32, WMS111, WMS130", paramType = 'query', dataType = 'string', required=false),
          @ApiImplicitParam(name = 'sortBy', value = 'Sort by', paramType = 'query', dataType = 'string'),
          @ApiImplicitParam(name = 'propertyName', value = 'Property name (comma separated fields)', defaultValue="", paramType = 'query', dataType = 'string', required=false),
          @ApiImplicitParam(name = 'maxFeatures', value = 'Maximum Features in the result', defaultValue="10", paramType = 'query', dataType = 'integer', required=false),
          @ApiImplicitParam(name = 'startIndex', value = 'Starting offset', defaultValue="", paramType = 'query', dataType = 'integer', required=false),
  ])
  def getFeature(/*GetFeatureRequest wfsParams*/) {
    // prevent the whole database from being returned
    if ( params.requestType != "hits" ) {
        if ( !params.containsKey("maxFeatures") || !params.maxFeatures ) {
            params.maxFeatures = DEFAULT_MAX_FEATURES
        }
    }

    def wfsParams = new GetFeatureRequest()

    BindUtil.fixParamNames( GetFeatureRequest, params )
    bindData( wfsParams, params )

    wfsParams.username = webFeatureService.extractUsernameFromRequest(request)

    def results = webFeatureService.getFeature( wfsParams )
    if(results.status != null) {
      response.status = results.status
    }

    String format = webFeatureService.parseOutputFormat(wfsParams?.outputFormat ?: 'GML3')

    if (format == 'KML'){
      if(results.filename) {
        response.setHeader("Content-Disposition", "attachment;filename=${results.filename}")
      } else {
        response.setHeader("Content-Disposition", "attachment;filename=${defaultFilename}")
      }
    }


    if ( format != null ) {
      response.setHeader 'Content-Type', results.contentType
    }
    String outputBuffer = encodeResponse( results )
    if ( outputBuffer ) {
      render outputBuffer
    }
    else { println "I am flushing."
      response.outputStream.flush()
      return
    }
  }

  private String encodeResponse(Object obj){
    return encodeResponse(obj.toString())
  }

  private String encodeResponse(String inputText) {
    String outputText
    String acceptEncoding = WebUtils.retrieveGrailsWebRequest().getCurrentRequest().getHeader('accept-encoding')

    if ( acceptEncoding?.contains( OmarWebUtils.GZIP_ENCODE_HEADER_PARAM ) ) { 
        response.setHeader 'Content-Encoding', OmarWebUtils.GZIP_ENCODE_HEADER_PARAM									    
	def targetStream = OmarWebUtils.gzippify( inputText, StandardCharsets.UTF_8.name() )
	targetStream.writeTo( response.outputStream )

	return null
    } else {
      return inputText
    }
  }
}
