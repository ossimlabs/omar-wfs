package omar.wfs

import grails.gorm.transactions.Transactional
import groovy.json.JsonBuilder
import groovy.transform.CompileStatic
import groovy.xml.StreamingMarkupBuilder
import groovy.json.StreamingJsonBuilder
import groovy.json.JsonSlurper

import java.util.regex.Pattern
import java.util.regex.Matcher

import omar.core.DateUtil

//@Transactional(readOnly=true)
class WebFeatureService
{
  def grailsLinkGenerator
  def geoscriptService
  def grailsApplication

  static final def ogcNamespaces = [
          wfs: 'http://www.opengis.net/wfs',
          gml: 'http://www.opengis.net/gml',
          ogc: 'http://www.opengis.net/ogc',
          ows: 'http://www.opengis.net/ows',
          xs: 'http://www.w3.org/2001/XMLSchema',
          xlink: 'http://www.w3.org/1999/xlink',
          xsi: 'http://www.w3.org/2001/XMLSchema-instance'
  ]

  static final def outputFormats = [
          'application/gml+xml; version=3.2',
          'application/json',
          'application/vnd.google-earth.kml xml',
          'application/vnd.google-earth.kml+xml',
          'csv',
          'GML2',
          'gml3',
          'gml32',
          'json',
          'KML',
          'text/xml; subtype=gml/2.1.2',
          'text/xml; subtype=gml/3.1.1',
          'text/xml; subtype=gml/3.2',
          'SHAPE-ZIP'
  ]

  static final def geometryOperands = [
          'gml:Envelope',
          'gml:Point',
          'gml:LineString',
          'gml:Polygon'
  ]

  static final def spatialOperators = [
          'BBOX',
          'Beyond',
          'Contains',
          'Crosses',
          'Disjoint',
          'DWithin',
          'Equals',
          'Intersects',
          'Overlaps',
          'Touches',
          'Within'
  ]

  static final def comparisonOperators = [
          'Between',
          'EqualTo',
          'GreaterThan',
          'GreaterThanEqualTo',
          'LessThan',
          'LessThanEqualTo',
          'Like',
          'NotEqualTo',
          'NullCheck'
  ]

  static final Map<String, String> typeMappings = [
          'java.lang.Boolean': 'xsd:boolean',
          'java.math.BigDecimal': 'xsd:decimal',
          'Double': 'xsd:double',
          'Integer': 'xsd:int',
          'Long': 'xsd:long',
          'MultiLineString': 'gml:MultiLineStringPropertyType',
          'MultiPolygon': 'gml:MultiPolygonPropertyType',
          'Polygon': 'gml:PolygonPropertyType',
          'Point': 'gml:PointPropertyType',
          'String': 'xsd:string',
          'java.sql.Timestamp': 'xsd:dateTime'
  ]
  String extractUsernameFromRequest(def request)
  {
    def userInfo = grailsApplication.config.userInfo
    String requestHeaderName = request.getHeader(userInfo?.requestHeaderUserName)
    String userInfoName = requestHeaderName ?: userInfo.requestHeaderUserNameDefault
    userInfoName
  }

  def getCapabilities(GetCapabilitiesRequest wfsParams)
  {
    def schemaLocation = grailsLinkGenerator.serverBaseURL
    def wfsEndpoint = grailsLinkGenerator.link( absolute: true, uri: '/wfs' )
    def model = geoscriptService.capabilitiesData

    def requestType = "GET"
    def requestMethod = "GetCapabilities"
    Date startTime = new Date()
    def responseTime
    def username = wfsParams.username ?: "(null)"

    def x = {
      mkp.xmlDeclaration()
      mkp.declareNamespace(ogcNamespaces)
      mkp.declareNamespace(model?.featureTypeNamespacesByPrefix)
      wfs.WFS_Capabilities( version:'1.1.0', xmlns: 'http://www.opengis.net/wfs',
              'xsi:schemaLocation': "http://www.opengis.net/wfs ${schemaLocation}/schemas/wfs/1.1.0/wfs.xsd",
      ) {
        ows.ServiceIdentification {
          ows.Title('O2 WFS Server')
          ows.Abstract('O2 WFS server')
          ows.Keywords {
            ows.Keyword('WFS')
            ows.Keyword('WMS')
            ows.Keyword('OMAR')
          }
          ows.ServiceType('WFS')
          ows.ServiceTypeVersion('1.1.0')
          ows.Fees('NONE')
          ows.AccessConstraints('NONE')
        }
        ows.ServiceProvider {
          ows.ProviderName('OSSIM Labs')
          ows.ServiceContact {
            ows.IndividualName('Scott Bortman')
            ows.PositionName('OMAR Developer')
            ows.ContactInfo {
              ows.Phone {
                ows.Voice()
                ows.Facsimile()
              }
                ows.Address {
                  ows.DeliveryPoint()
                  ows.City()
                  ows.AdministrativeArea()
                  ows.PostalCode()
                  ows.Country()
                  ows.ElectronicMailAddress()
              }
            }
          }
        }
        ows.OperationsMetadata {
          ows.Operation( name: 'GetCapabilities' ) {
            ows.DCP {
              ows.HTTP {
                ows.Get( 'xlink:href': wfsEndpoint )
                ows.Post( 'xlink:href': wfsEndpoint )
              }
            }
            ows.Parameter( name: 'AcceptVersions' ) {
              ows.Value('1.1.0')
            }
            ows.Parameter( name: 'AcceptFormats' ) {
              ows.Value('text/xml')
            }
          }
          ows.Operation( name: 'DescribeFeatureType' ) {
            ows.DCP {
              ows.HTTP {
                ows.Get( 'xlink:href': wfsEndpoint)
                ows.Post( 'xlink:href': wfsEndpoint)
              }
            }
            ows.Parameter( name: 'outputFormat' ) {
              ows.Value('text/xml; subtype=gml/3.1.1')
            }
          }
          ows.Operation( name: 'GetFeature' ) {
            ows.DCP {
              ows.HTTP {
                ows.Get( 'xlink:href': wfsEndpoint )
                ows.Post( 'xlink:href': wfsEndpoint )
              }
            }
            ows.Parameter( name: 'resultType' ) {
              ows.Value('results')
              ows.Value('hits')
            }
            ows.Parameter( name: 'outputFormat' ) {
              outputFormats?.each { outputFormat ->
                ows.Value(outputFormat)
              }
            }
          }
        }
        FeatureTypeList {
          Operations {
            Operation('Query')
          }
          model?.featureTypes?.each { featureType ->
            FeatureType( "xmlns:${featureType.namespace.prefix}":  featureType.namespace.uri) {
              Name("${featureType.namespace.prefix}:${featureType.name}")
              Title(featureType.title)
              Abstract(featureType.description)
              ows.Keywords {
                featureType.keywords.each { keyword ->
                  ows.Keyword(keyword)
                }
              }
              DefaultSRS("${featureType.proj}")
              ows.WGS84BoundingBox {
                def bounds = featureType.geoBounds
                ows.LowerCorner("${bounds.minX} ${bounds.minY}")
                ows.UpperCorner("${bounds.maxX} ${bounds.maxY}")
              }
            }
          }
        }
        ogc.Filter_Capabilities {
          ogc.Spatial_Capabilities {
            ogc.GeometryOperands {
              geometryOperands.each { geometryOperand ->
                ogc.GeometryOperand(geometryOperand)
              }
            }
            ogc.SpatialOperators {
              spatialOperators.each { spatialOperator ->
                ogc.SpatialOperator( name: spatialOperator )
              }
            }
          }
          ogc.Scalar_Capabilities {
            ogc.LogicalOperators()
            ogc.ComparisonOperators {
              comparisonOperators.each { comparisonOperator ->
                ogc.ComparisonOperator(comparisonOperator)
              }
            }
            ogc.ArithmeticOperators {
              ogc.SimpleArithmetic()
              ogc.Functions {
                ogc.FunctionNames {
                  model?.functionNames.each {function ->
                    ogc.FunctionName( nArgs: function.argCount, function.name)
                  }
                }
              }
            }
          }
          ogc.Id_Capabilities {
            ogc.FID()
            ogc.EID()
          }
        }
      }
    }

    def xml = new StreamingMarkupBuilder( encoding: 'utf-8' ).bind( x )
    def contentType = 'application/xml'

    Date endTime = new Date()
    responseTime = Math.abs(startTime.getTime() - endTime.getTime())

    def requestInfoLog = new JsonBuilder(timestamp: DateUtil.formatUTC(startTime), username: username, requestType: requestType,
            requestMethod: requestMethod, endTime: DateUtil.formatUTC(endTime), responseTime: responseTime,
            responseSize: xml.toString().bytes.length, contentType: contentType, params: wfsParams.toString())

    log.info requestInfoLog as String

    [contentType: contentType, text: xml.toString()]
  }

  def describeFeatureType(DescribeFeatureTypeRequest wfsParams)
  {
    def schema = geoscriptService.getSchemaInfoByTypeName(wfsParams.typeName)
    def schemaLocation = grailsLinkGenerator.serverBaseURL

    def requestType = "GET"
    def requestMethod = "DescribeFeatureType"
    Date startTime = new Date()
    def responseTime
    def requestInfoLog
    def username = wfsParams.username ?: "(null)"

    def x = {
      mkp.xmlDeclaration()
      mkp.declareNamespace(
              gml: "http://www.opengis.net/gml",
              xsd: "http://www.w3.org/2001/XMLSchema",
              (schema?.namespace?.prefix): schema?.namespace?.uri
      )
      xsd.schema( elementFormDefault: "qualified",
              targetNamespace: schema?.namespace?.uri
      ) {
        xsd.import( namespace: "http://www.opengis.net/gml",
                schemaLocation: "${schemaLocation}/schemas/gml/3.1.1/base/gml.xsd" )
        xsd.complexType( name: "${schema?.name}Type" ) {
          xsd.complexContent {
            xsd.extension( base: "gml:AbstractFeatureType" ) {
              xsd.sequence {
                schema?.attributes?.each { attribute ->
                  xsd.element( maxOccurs: attribute?.maxOccurs,
                          minOccurs: attribute?.minOccurs,
                          name: attribute?.name,
                          nillable: attribute?.nillable,
                          type: typeMappings.get( attribute.type, attribute.type ) )
                }
              }
            }
          }
        }
        xsd.element( name: schema?.name, substitutionGroup: "gml:_Feature",
                type: "${schema?.namespace?.prefix}:${schema?.name}Type" )
      }
    }

    def xml = new StreamingMarkupBuilder( encoding: 'utf-8' ).bind( x )
    def contentType = 'text/xml'

    Date endTime = new Date()
    responseTime = Math.abs(startTime.getTime() - endTime.getTime())

    requestInfoLog = new JsonBuilder(timestamp: DateUtil.formatUTC(startTime), username: username, requestType: requestType,
            requestMethod: requestMethod, endTime: DateUtil.formatUTC(endTime), responseTime: responseTime,
            responseSize: xml.toString().bytes.length, contentType: contentType, params: wfsParams.toString())

    log.info requestInfoLog.toString()

    [contentType: contentType, text: xml.toString()]
  }

  def getFeature(GetFeatureRequest wfsParams)
  {
    Map<Object, Object> options = parseOptions(wfsParams)
    String format = parseOutputFormat(wfsParams?.outputFormat)

    String requestType = "GET"
    String requestMethod = "GetFeature"
    Date startTime = new Date()
    def responseTime
    def responseSize
    def requestInfoLog
    def httpStatus
    String filter = options?.filter
    String keywordCountryCode, keywordMissionId, keywordSensorId
    Location searchLocation
    String username = wfsParams.username ?: "(null)"
    def maxFeatures = options?.max
    Boolean includeNumberMatched =  grailsApplication.config?.omar?.wfs?.includeNumberMatched ?: false
    if(wfsParams?.resultType?.toLowerCase() == "hits")
    {
      includeNumberMatched = true
    }

    def outputFormat = parseOutputFormat(wfsParams?.outputFormat)

    def results = geoscriptService.queryLayer(
            wfsParams?.typeName,
            options,
            wfsParams?.resultType ?: 'results',
            outputFormat,
            includeNumberMatched
    )

    def formattedResults = getFeatureForFormat(format, wfsParams, results)

    Date endTime = new Date()
    responseTime = Math.abs(startTime.getTime() - endTime.getTime())

    httpStatus = results != null ? 200 : 400
    responseSize = formattedResults.toString().bytes.length

    if(filter) {
      List<String> countryCode = new ArrayList<String>()
      List<String> missionId = new ArrayList<String>()
      List<String> sensorId = new ArrayList<String>()

      Pattern regex = Pattern.compile("'%(.*?)%'")   // Regex for capturing filter criteria
      Matcher compare_regex

      filter.split(' AND ').each { s ->
        compare_regex = regex.matcher(s)

        while (s.contains('country_code') && compare_regex.find()){
          countryCode.add(compare_regex.group(1))
        }

        while (s.contains('mission_id') && compare_regex.find()) {
          missionId.add(compare_regex.group(1))
        }

        while (s.contains('sensor_id') && compare_regex.find()) {
          sensorId.add(compare_regex.group(1))
        }
      }

      keywordCountryCode = countryCode ?: ['-'] 
      keywordMissionId = missionId ?: ['-']
      keywordSensorId = sensorId ?: ['-']

      // The point location is only available in the filter when zoomed in the UI.
      // We want to use the point location to exclude large search areas.
      searchLocation = getPointLocationOrNull(filter)
    }

    requestInfoLog = new JsonBuilder(
            timestamp: DateUtil.formatUTC(startTime),
            username: username, requestType: requestType,
            requestMethod: requestMethod,
            httpStatus: httpStatus,
            endTime: DateUtil.formatUTC(endTime),
            responseTime: responseTime,
            responseSize: responseSize,
            filter: filter,
            maxFeatures: maxFeatures,
            numberOfFeatures: results?.numberOfFeatures,
            numberMatched: results?.numberMatched,
            keyword_countryCode: keywordCountryCode,
            keyword_missionId: keywordMissionId,
            keyword_sensorId: keywordSensorId,
            params: wfsParams.toString(),
            location: searchLocation
    )

    log.info requestInfoLog.toString()

    return formattedResults
  }

  // FIXME: Returns dynamic type as some branches return different objects.
  // FIXME: queryLayerResults is unknown type from the call: geoscriptService.queryLayer
  private def getFeatureForFormat(String format, def wfsParams, def queryLayerResults) {
    def formattedResults
    switch (format) {
      case 'GML2':
      case 'GML3':
      case 'GML3_2':
        formattedResults = getFeatureGML(queryLayerResults, wfsParams?.typeName)
        break
      case 'JSON':
        formattedResults = getFeatureJSON(queryLayerResults, wfsParams?.typeName)
        break
      case 'CSV':
        formattedResults = [contentType: 'text/csv', text: queryLayerResults.features]
        break
      case 'KML':
        formattedResults = getFeatureKML(queryLayerResults?.features, wfsParams)
        break
      case 'WMS1_1_1':
      case 'WMS1_3_0':
        formattedResults = getFeatureWMS(queryLayerResults?.features?.id, format)
        break
      default:
        formattedResults = queryLayerResults
    }
    return formattedResults
  }

  /**
   * Returns the location of the first point parsed by the regex "POINT\(([-0-9.]+)[\s]+([-0-9.]+)"
   * or null if a not found.
   */
  @CompileStatic
  private Location getPointLocationOrNull(String text) {
    // Groovy regex literal and "match" operator
    final Matcher pointLocation = text =~ ~/POINT\(([-0-9.]+)[\s]+([-0-9.]+)/

    Location locationOrNull = null
    try {
      if (pointLocation.find()) {
        double longitude = pointLocation.group(1).toDouble()
        double latitude = pointLocation.group(2).toDouble()
        locationOrNull = new Location(latitude, longitude)
      }
    } catch (NumberFormatException ignore) {
      // Ignore exception because we handle it by returning null
    }
    return locationOrNull
  }

  @CompileStatic
  private class Location {
    final double lat
    final double lon
    Location(double lat, double lon) {
      this.lat = lat
      this.lon = lon
    }
  }

  def getFeatureGML(def results, def typeName, def version='1.1.0')
  {
    def schemaLocation = grailsLinkGenerator.serverBaseURL

    def describeFeatureTypeURL = grailsLinkGenerator.link(params: [
            service: 'WFS',
            version: version,
            request: 'DescribeFeatureType',
            typeName: typeName
    ], absolute: true, controller: 'wfs')

    def x = {
      mkp.xmlDeclaration()
      mkp.declareNamespace(ogcNamespaces)
      mkp.declareNamespace((results?.namespace?.prefix): results?.namespace?.uri)
      wfs.FeatureCollection(
              numberOfFeatures: results?.numberOfFeatures,
              numberMatched: results?.numberMatched,
              timeStamp: results?.timeStamp,
              'xsi:schemaLocation': "${results?.namespace?.uri} ${describeFeatureTypeURL} http://www.opengis.net/wfs ${schemaLocation}/schemas/wfs/1.1.0/wfs.xsd"
      ) {
        if ( results?.features) {
          gml.featureMembers {
            results?.features?.each { feature ->
              mkp.yieldUnescaped(feature)
            }
          }
        }
      }
    }

    def xml = new StreamingMarkupBuilder( encoding: 'utf-8' ).bind( x )

    [contentType: 'text/xml', text: xml.toString()]
  }

  def getFeatureJSON(def results, def typeName) // def version='1.1.0')
  {
    def slurper = new JsonSlurper()

    def x = {
      type 'FeatureCollection'
      totalFeatures results?.numberOfFeatures
      features results?.features?.collect {
        if ( it instanceof String ) {
          slurper.parseText(it)
        } else {
          it
        }
      }
      crs {
        type 'name'
        properties {
          name 'urn:ogc:def:crs:EPSG::4326'
        }
      }
    }

    def jsonWriter = new StringWriter()
    def jsonBuilder = new StreamingJsonBuilder(jsonWriter)
    jsonBuilder(x)

    [contentType: 'application/json', text: jsonWriter.toString()]
  }

  def getFeatureWMS(def layerIds, def wmsVersion)
  {
    def serverData = grailsApplication.config?.geoscript?.serverData
    def version = (wmsVersion == "WMS1_1_1") ? "1.1.1" : "1.3.0"

    def contentType
    def schemaLocation = grailsLinkGenerator.link( absolute: true, uri: "/schemas/wms/1.3.0/capabilities_1_3_0.xsd" )
    def docTypeLocation = grailsLinkGenerator.link( absolute: true, uri: "/schemas/wms/1.1.1/WMS_MS_Capabilities.dtd" )
    def model = geoscriptService.capabilitiesData


    def x = {
      mkp.xmlDeclaration()

      if ( version == "1.1.1" )
      {
        mkp.yieldUnescaped """<!DOCTYPE WMT_MS_Capabilities SYSTEM "${docTypeLocation}">"""
      }

      def rootTag = (version == "1.1.1") ? "WMT_MS_Capabilities" : "WMS_Capabilities"
      def rootAttributes = [version: version]

      mkp.declareNamespace(
              xlink: "http://www.w3.org/1999/xlink",
      )

      if ( version == "1.3.0" )
      {
        mkp.declareNamespace(
                xsi: "http://www.w3.org/2001/XMLSchema-instance"
        )

        rootAttributes['xmlns'] = "http://www.opengis.net/wms"
        rootAttributes['xsi:schemaLocation'] = "http://www.opengis.net/wms ${schemaLocation}"
      }

      "${rootTag}"( rootAttributes ) {

        Service {
          Name( serverData.Service.Name )
          Title( serverData.Service.Title )
          Abstract( serverData.Service.Abstract )
          KeywordList {
            serverData.Service.KeywordList.each { keyword ->
              Keyword( keyword )
            }
          }
          OnlineResource( 'xlink:type': "simple", 'xlink:href': serverData.Service.OnlineResource )
          ContactInformation {
            ContactPersonPrimary {
              ContactPerson( serverData.Service.ContactInformation.ContactPersonPrimary.ContactPerson )
              ContactOrganization( serverData.Service.ContactInformation.ContactPersonPrimary.ContactOrganization )
            }
            ContactPosition( serverData.Service.ContactInformation.ContactPosition )
            ContactAddress {
              AddressType( serverData.Service.ContactInformation.ContactAddress.AddressType )
              Address( serverData.Service.ContactInformation.ContactAddress.Address )
              City( serverData.Service.ContactInformation.ContactAddress.City )
              StateOrProvince( serverData.Service.ContactInformation.ContactAddress.StateOrProvince )
              PostCode( serverData.Service.ContactInformation.ContactAddress.PostCode )
              Country( serverData.Service.ContactInformation.ContactAddress.Country )
            }
            ContactVoiceTelephone( serverData.Service.ContactInformation.ContactVoiceTelephone )
            ContactFacsimileTelephone( serverData.Service.ContactInformation.ContactFacsimileTelephone )
            ContactElectronicMailAddress( serverData.Service.ContactInformation.ContactElectronicMailAddress )
          }
          Fees( serverData.Service.Fees )
          AccessConstraints( serverData.Service.AccessConstraints )
        }
        Capability {
          Request {
            GetCapabilities {
              contentType = (version == '1.1.1') ? "application/vnd.ogc.wms_xml" : "text/xml"
              Format( contentType )
              DCPType {
                HTTP {
                  Get {
                    OnlineResource( 'xlink:type': "simple",
                            'xlink:href': grailsLinkGenerator.link( absolute: true, controller: 'wms', action: 'getCapabilities' ) )
                  }
                  Post {
                    OnlineResource( 'xlink:type': "simple",
                            'xlink:href': grailsLinkGenerator.link( absolute: true, controller: 'wms', action: 'getCapabilities' ) )
                  }
                }
              }
            }
            GetMap {
              serverData.Capability.Request.GetMap.Format.each { format ->
                Format( format )
              }
              DCPType {
                HTTP {
                  Get {
                    OnlineResource( 'xlink:type': "simple",
                            'xlink:href': grailsLinkGenerator.link( absolute: true, controller: 'wms', action: 'getMap' ) )
                  }
                }
              }
            }
          }
          Exception {
            serverData.Capability.Exception.Format.each { format ->
              Format( format )
            }
          }
          Layer {
            Title( serverData.Capability.Layer.Title )
            Abstract( serverData.Capability.Layer.Abstract )
            def crsTag = (version == '1.1.1') ? "SRS" : "CRS"
            layerIds?.each { layerId ->
              "${crsTag}"( layerId )
            }

            if ( version == '1.3.0' )
            {
              EX_GeographicBoundingBox {
                westBoundLongitude( serverData.Capability.Layer.BoundingBox.minLon )
                eastBoundLongitude( serverData.Capability.Layer.BoundingBox.maxLon )
                southBoundLatitude( serverData.Capability.Layer.BoundingBox.minLat )
                northBoundLatitude( serverData.Capability.Layer.BoundingBox.maxLat )
              }
              BoundingBox( CRS: serverData.Capability.Layer.BoundingBox.crs,
                      minx: serverData.Capability.Layer.BoundingBox.minLon,
                      miny: serverData.Capability.Layer.BoundingBox.minLat,
                      maxx: serverData.Capability.Layer.BoundingBox.maxLon,
                      maxy: serverData.Capability.Layer.BoundingBox.maxLat
              )
            }
            else
            {
              LatLonBoundingBox(
                      minx: serverData.Capability.Layer.BoundingBox.minLon,
                      miny: serverData.Capability.Layer.BoundingBox.minLat,
                      maxx: serverData.Capability.Layer.BoundingBox.maxLon,
                      maxy: serverData.Capability.Layer.BoundingBox.maxLat
              )
            }
            model?.featureTypes?.each { featureType ->
              Layer( queryable: "1", opaque: "0" ) {
                Name( "${featureType.namespace.prefix}:${featureType.name}" )
                Title( featureType.title )
                Abstract( featureType.description )
                Keywords {
                  featureType.keywords.each { keyword ->
                    Keyword( keyword )
                  }
                }
                def bounds = featureType.geoBounds

                "${crsTag}"( bounds?.proj )
                if ( version == "1.3.0" )
                {
                  EX_GeographicBoundingBox {
                    westBoundLongitude( bounds?.minX )
                    eastBoundLongitude( bounds?.maxX )
                    southBoundLatitude( bounds?.minY )
                    northBoundLatitude( bounds?.maxY )
                  }
                }
                else
                {
                  LatLonBoundingBox(
                          minx: bounds?.minX,
                          miny: bounds?.minY,
                          maxx: bounds?.maxX,
                          maxy: bounds?.maxY
                  )
                }
              }
            }
          }
        }
      }
    }

    def xml = new StreamingMarkupBuilder( encoding: 'utf-8' ).bind( x )

    [contentType: 'text/xml', text: xml.toString()]
  }

  def parseOptions(def wfsParams)
  {
    def wfsParamNames = [
            'maxFeatures', 'startIndex', 'propertyName', 'sortBy', 'filter', 'srsName'
    ]

    def options = wfsParamNames.inject( [:] ) { options, wfsParamName ->
      if ( wfsParams[wfsParamName] != null )
      {
        switch ( wfsParamName )
        {
          case 'maxFeatures':
            options['max'] = wfsParams[wfsParamName]
            break
          case 'startIndex':
            options['start'] = wfsParams[wfsParamName]
            break
          case 'propertyName':
            def fields = wfsParams[wfsParamName]?.split( ',' )?.collect {
              it.split( ':' )?.last()
            } as List<String>
            if ( fields && !fields?.isEmpty() && fields?.every { it } )
            {
              options['fields'] = fields
            }
            break
          case 'sortBy':
            if ( wfsParams[wfsParamName]?.trim() )
            {
              if (!wfsParams[wfsParamName].contains(',') && !wfsParams[wfsParamName].contains('ingest_date')) {
                if (wfsParams[wfsParamName] ==~ /.*D(ESC)?/)
                  wfsParams[wfsParamName] += ',ingest_date DESC'
                else {
                  wfsParams[wfsParamName] += ',ingest_date ASC'
                }
              }
              options['sort'] = wfsParams[wfsParamName].split(',').collect {
                def x = it.split(/ |\+/) as List
                if ( x[1] ==~ /.*D(ESC)?/ ) {
                  x = [x[0], 'DESC'] as List
                } else if (x[1] ==~ /.*A(SC)?/) {
                  x = [x[0], 'ASC'] as List
                }
                return x
              } as List
            }
            break
          case "srsName":
            options['srsName'] = wfsParams[wfsParamName]
            break
          default:
            if ( wfsParams[wfsParamName] )
            {
              options[wfsParamName] = wfsParams[wfsParamName]
            }
        }
      }
      options
    }

    options
  }

  def parseOutputFormat(def outputFormat)
  {
    def format = null

    switch ( outputFormat?.toUpperCase() )
    {
      case 'GML3':
      case 'TEXT/XML; SUBTYPE=GML/3.1.1':
        format = 'GML3'
        break
      case 'GML2':
      case 'TEXT/XML; SUBTYPE=GML/2.1.2':
        format = 'GML2'
        break
      case 'GML32':
      case 'TEXT/XML; SUBTYPE=GML/3.2':
        format = 'GML3_2'
        break
      case 'APPLICATION/JSON':
      case 'JSON':
        format = 'JSON'
        break
      case 'APPLICATION/CSV':
      case 'CSV':
        format = 'CSV'
        break
      case 'KML':
      case 'APPLICATION/VND.GOOGLE-EARTH.KMLl+XML':
      case 'APPLICATION/VND.GOOGLE-EARTH.KMLl XML':
        format = 'KML'
        break
      case 'WMS111':
        format = 'WMS1_1_1'
        break
      case 'WMS130':
        format = 'WMS1_3_0'
        break
    }

    format
  }


  def getFeatureKML(features, params) {
    def kmlNode = {
      mkp.xmlDeclaration()
      kml( "xmlns": "http://earth.google.com/kml/2.1" ) {
        Document() {

          mkp.yieldUnescaped( getKmlStyles() )

          def wmsParams = getKmlWmsParams(params)
          Folder() {
            name( "Images" )
            features.eachWithIndex() { value, index ->
              def feature = value
              mkp.yieldUnescaped( getKmlGroundOverlay(index, feature, wmsParams) )
            }
            open( 1 )
          }

          Folder() {
            name( "Footprints" )
            features.eachWithIndex() { value, index ->
              def feature = value
              mkp.yieldUnescaped( getKmlFootprint(index, feature) )
            }
            open( 1 )
          }
          open( 1 )
        }
      }
    }

    def kmlWriter = new StringWriter()
    def kmlBuilder = new StreamingMarkupBuilder()
    kmlWriter << kmlBuilder.bind( kmlNode )

    return [contentType: 'application/vnd.google-earth.kml+xml;', text: kmlWriter.buffer]
  }

  def getKmlDescription( def feature ) {


    def o2BaseUrl = grailsLinkGenerator.serverBaseURL - grailsApplication.config.server.contextPath

    def centerLon
    def centerLat

    if ( feature.ground_geom )
    {
      def bounds = feature.ground_geom.envelopeInternal
      centerLon = ( bounds?.minX + bounds?.maxX ) * 0.5
      centerLat = ( bounds?.minY + bounds?.maxY ) * 0.5
    }
    else
    {

      def xcoords =  feature.geometry.coordinates[0][0].collect { it[0] }
      def ycoords =  feature.geometry.coordinates[0][0].collect { it[1] }
      centerLon = xcoords.sum() / xcoords.size()
      centerLat = ycoords.sum() / ycoords.size()
    }

    def location = "${centerLat},${centerLon}"
    def filter = "in(${feature.get("id")?.toString() - 'raster_entry.'})"

    def tlvUrl = grailsLinkGenerator.link(
            base: grailsLinkGenerator.serverBaseURL - grailsApplication.config.server.contextPath,
            uri: '/tlv',
            params: [
                    location: location,
                    filter: filter
            ], absolute: true)

    def wfsUrl = grailsLinkGenerator.link(uri: '/wfs', params: [
            service: 'WFS',
            version: '1.1.0',
            request: 'GetFeature',
            typeName: 'omar:raster_entry',
            filter: filter,
            outputFormat: 'JSON'
    ], absolute: true)

    def tableMap = [
            "Acquistion Date": feature.acquisition_date ?: feature.properties.acquisition_date ?: "",
            "Azimuth Angle": feature.azimuth_angle ?: feature.properties.azimuth_angle ?: "",
            "Bit Depth": feature.bit_depth ?: feature.properties.bit_depth ?: "",
            "Cloud Cover": feature.cloud_cover ?: feature.properties.cloud_cover ?: "",
            "Country Code": feature.country_code ?: feature.properties.country_code ?: "",
            "Filename": feature.filename ?: feature.properties.filename,
            "Grazing Angle": feature.grazing_angle ?: feature.properties.grazing_angle ?: "",
            "GSD X/Y": (feature.gsdx && feature.gsdy) ? "${feature.gsdx} / ${feature.gsdy}" : "",
            "Image ID": feature.image_id ?: (feature.title ?: ""),
            "Ingest Date": feature.ingest_date ?: feature.properties.ingest_date ?: "",
            "NIIRS": feature.niirs ?: feature.properties.niirs ?: "",
            "# of Bands": feature.number_of_bands ?: feature.properties.number_of_bands ?: "",
            "Security Class.": feature.security_classification ?: feature.properties.security_classification ?: "",
            "Sensor": feature.sensor_id ?: feature.properties.sensor_id ?: "",
            "Sun Azimuth": feature.sun_azimuth ?: feature.properties.sun_azimuth ?: "",
            "Sun Elevation": feature.sun_elevation ?: feature.sun_elevation ?: "",
            "View:": "<a href = '${tlvUrl}'>Ortho</a>",
            "WFS": "<a href = '${wfsUrl}'>All Metadata</a>"
    ]

    def description = "<table style = 'width: auto; white-space: nowrap'>"
    tableMap.each() {
      description += "<tr>"
      description += "<th align = 'right'>${it.key}:</th>"
      description += "<td>${it.value}</td>"
      description += "</tr>"
    }

    def uiUrl = grailsLinkGenerator.link(base: o2BaseUrl, uri: '/omar-ui', absolute: true)

    description += "<tfoot><tr><td colspan='2'>"
    description +=     "<a href = '${uiUrl}/omar'>"
    description +=         "<img src = '${uiUrl}/assets/o2-logo.png'/>"
    description +=     "</a>"
    description += "</td></tr></tfoot>"
    description += "</table>"


    return description
  }

  def getKmlFootprint(index, feature) {
    def kmlNode = {
      Placemark() {
        description { mkp.yieldUnescaped( "<![CDATA[${getKmlDescription(feature)}]]>" ) }
        name( "${index + 1}: " + (feature.title ?: new File(feature.filename ?: feature.properties.filename).name) )

        def centerLon
        def centerLat

        if ( feature.ground_geom )
        {
          def bounds = feature.ground_geom.envelopeInternal
          centerLon = ( bounds?.minX + bounds?.maxX ) * 0.5
          centerLat = ( bounds?.minY + bounds?.maxY ) * 0.5
        }
        else
        {
          def xcoords =  feature.geometry.coordinates[0][0].collect { it[0] }
          def ycoords =  feature.geometry.coordinates[0][0].collect { it[1] }
          centerLon = xcoords.sum() / xcoords.size()
          centerLat = ycoords.sum() / ycoords.size()
        }

        LookAt() {
          altitude( 0 )
          altitudeMode( "clampToGround" )
          heading( 0 )
          latitude( centerLat )
          longitude( centerLon )
          range( 15000 )
          tilt( 0 )
        }

        // the footprint geometry
        if  ( feature.ground_geom )
        {
          mkp.yieldUnescaped( feature.ground_geom.getKml() )
        }
        else
        {
          MultiGeometry {
            Polygon {
              outerBoundaryIs {
                LinearRing {
                  coordinates(feature.geometry.coordinates[0][0].collect { it.join(',') }.join(' '))
                }
              }
            }
          }
        }

        Snippet()

        switch (feature.sensor_id ?: feature.properties.sensor_id) {
          case "msi": styleUrl( "#msi" ); break
          case "vis": styleUrl( "#vis" ); break
          default: styleUrl( "#default" ); break
        }

      }
    }

    def kmlWriter = new StringWriter()
    def kmlBuilder = new StreamingMarkupBuilder()
    kmlWriter << kmlBuilder.bind( kmlNode )


    return kmlWriter.buffer
  }

  def getKmlGroundOverlay(index, feature, wmsParams) {
    def kmlNode = {
      GroundOverlay() {
        description { mkp.yieldUnescaped( "<![CDATA[${getKmlDescription(feature)}]]>" ) }
        name( "${index + 1}: " + (feature.title ?: new File(feature.filename ?: feature.properties.filename).name) )

        Icon() {
          wmsParams.FILTER = "in(${feature.get("id")?.toString() - 'raster_entry.'})"

          def o2BaseUrl = grailsLinkGenerator.serverBaseURL - grailsApplication.config.server.contextPath
          def wmsUrl = grailsLinkGenerator.link(base: o2BaseUrl, uri: '/omar-wms/wms', params: wmsParams, absolute: true)

          href { mkp.yieldUnescaped( "<![CDATA[${wmsUrl}]]>" ) }
          viewBoundScale( 0.85 )
          viewFormat(
                  "BBOX=[bboxWest],[bboxSouth],[bboxEast],[bboxNorth]&WIDTH=[horizPixels]&HEIGHT=[vertPixels]"
          )
          viewRefreshMode( "onStop" )
          viewRefreshTime( 1 )
        }

        LookAt() {
          def centerLon
          def centerLat

          if ( feature.ground_geom)
          {
            def bounds = feature.ground_geom.envelopeInternal
            centerLon = ( bounds?.minX + bounds?.maxX ) * 0.5
            centerLat = ( bounds?.minY + bounds?.maxY ) * 0.5
          }
          else
          {
            def xcoords =  feature.geometry.coordinates[0][0].collect { it[0] }
            def ycoords =  feature.geometry.coordinates[0][0].collect { it[1] }
            centerLon = xcoords.sum() / xcoords.size()
            centerLat = ycoords.sum() / ycoords.size()
          }

          altitude( 0 )
          altitudeMode( "clampToGround" )
          heading( 0 )
          latitude( centerLat )
          longitude( centerLon )
          range( 15000 )
          tilt( 0 )
        }

        Snippet()
        visibility( 0 )
      }
    }

    def kmlWriter = new StringWriter()
    def kmlBuilder = new StreamingMarkupBuilder()
    kmlWriter << kmlBuilder.bind( kmlNode )


    return kmlWriter.buffer
  }

  def getKmlStyles() {
    def kmlNode = {
      Style( id: "default" ) {
        LineStyle() {
          color( "ffffffff" )
          width( 2 )
        }
        PolyStyle() {
          color( "ffffffff" )
          fill( 0 )
        }
      }
      Style( id: "msi" ) {
        LineStyle() {
          color( "ff0000ff" )
          width( 2 )
        }
        PolyStyle() {
          color( "ff0000ff" )
          fill( 0 )
        }
      }
      Style( id: "vis" ) {
        LineStyle() {
          color( "ff00ffff" )
          width( 2 )
        }
        PolyStyle() {
          color( "ff00ffff" )
          fill( 0 )
        }
      }
    }

    def kmlWriter = new StringWriter()
    def kmlBuilder = new StreamingMarkupBuilder()
    kmlWriter << kmlBuilder.bind( kmlNode )


    return kmlWriter.buffer
  }

  def getKmlWmsParams() {
    return [
            BANDS: "default",
            FORMAT: "image/png",
            LAYERS: "omar:raster_entry",
            REQUEST: "GetMap",
            SERVICE: "WMS",
            SRS: "EPSG:4326",
            TRANSPARENT: true,
            VERSION: "1.1.1"
    ]
  }
}
